const express = require('express');
const { v4: uuidv4 } = require('uuid');
const multer = require('multer');
const path = require('path');
const { getDb, logActivity } = require('../database');
const { authenticate, requireRole } = require('../middleware/auth');

const router = express.Router();

// Configure multer for receipt uploads
const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    cb(null, path.join(__dirname, '..', 'uploads'));
  },
  filename: (req, file, cb) => {
    const uniqueName = `receipt-${Date.now()}-${Math.round(Math.random() * 1E9)}${path.extname(file.originalname)}`;
    cb(null, uniqueName);
  }
});

const upload = multer({
  storage,
  limits: { fileSize: 5 * 1024 * 1024 }, // 5MB limit
  fileFilter: (req, file, cb) => {
    const allowedTypes = /jpeg|jpg|png|pdf|webp/;
    const extname = allowedTypes.test(path.extname(file.originalname).toLowerCase());
    const mimetype = allowedTypes.test(file.mimetype);
    if (extname && mimetype) {
      cb(null, true);
    } else {
      cb(new Error('Only images (jpeg, jpg, png, webp) and PDFs are allowed.'));
    }
  }
});

// All expense routes require authentication
router.use(authenticate);

/**
 * GET /api/expenses
 * Get all expense records. Owners/managers see all, employees see their own.
 * Supports query filters: date_from, date_to, category
 */
router.get('/', requireRole(50), (req, res) => {
  try {
    const db = getDb();
    const { date_from, date_to, category } = req.query;

    let query = `
      SELECT e.*, u.name as created_by_name 
      FROM expenses e
      LEFT JOIN users u ON e.created_by = u.id
    `;

    const conditions = [];
    const params = [];

    // Employees see only their own expenses
    if (req.user.power_level < 75) {
      conditions.push('e.created_by = ?');
      params.push(req.user.id);
    }

    if (date_from) {
      conditions.push('e.date >= ?');
      params.push(date_from);
    }

    if (date_to) {
      conditions.push('e.date <= ?');
      params.push(date_to);
    }

    if (category) {
      conditions.push('e.category LIKE ?');
      params.push(`%${category}%`);
    }

    if (conditions.length > 0) {
      query += ' WHERE ' + conditions.join(' AND ');
    }

    query += ' ORDER BY e.date DESC, e.created_at DESC';

    const expenses = db.prepare(query).all(...params);
    res.json({ expenses });
  } catch (err) {
    console.error('List expenses error:', err);
    res.status(500).json({ error: 'Internal server error.' });
  }
});

/**
 * GET /api/expenses/:id
 * Get a single expense record.
 */
router.get('/:id', requireRole(50), (req, res) => {
  try {
    const db = getDb();
    const expense = db.prepare(`
      SELECT e.*, u.name as created_by_name 
      FROM expenses e
      LEFT JOIN users u ON e.created_by = u.id
      WHERE e.id = ?
    `).get(req.params.id);

    if (!expense) {
      return res.status(404).json({ error: 'Expense record not found.' });
    }

    // Employees can only see their own expenses
    if (req.user.power_level < 75 && expense.created_by !== req.user.id) {
      return res.status(403).json({ error: 'Insufficient permissions.' });
    }

    res.json({ expense });
  } catch (err) {
    console.error('Get expense error:', err);
    res.status(500).json({ error: 'Internal server error.' });
  }
});

/**
 * POST /api/expenses
 * Create a new expense record with optional receipt upload.
 */
router.post('/', requireRole(50), upload.single('receipt'), (req, res) => {
  try {
    const { category, amount, date, notes } = req.body;

    if (!category || amount === undefined || !date) {
      return res.status(400).json({ error: 'Category, amount, and date are required.' });
    }

    const db = getDb();
    const id = uuidv4();
    const receiptPath = req.file ? `/uploads/${req.file.filename}` : null;

    db.prepare(`
      INSERT INTO expenses (id, category, amount, date, notes, receipt_path, created_by, created_at)
      VALUES (?, ?, ?, ?, ?, ?, ?, datetime('now'))
    `).run(id, category.trim(), parseFloat(amount), date, notes || null, receiptPath, req.user.id);

    logActivity(req.user.id, 'CREATE_EXPENSE', `Added expense: ${category} - Amount: ${amount}`);

    const expense = db.prepare(`
      SELECT e.*, u.name as created_by_name 
      FROM expenses e
      LEFT JOIN users u ON e.created_by = u.id
      WHERE e.id = ?
    `).get(id);

    res.status(201).json({ message: 'Expense recorded successfully.', expense });
  } catch (err) {
    console.error('Create expense error:', err);
    res.status(500).json({ error: 'Internal server error.' });
  }
});

/**
 * PUT /api/expenses/:id
 * Update an expense record. Creator can update their own, managers/owners can update any.
 */
router.put('/:id', requireRole(50), upload.single('receipt'), (req, res) => {
  try {
    const db = getDb();
    const expense = db.prepare('SELECT * FROM expenses WHERE id = ?').get(req.params.id);

    if (!expense) {
      return res.status(404).json({ error: 'Expense record not found.' });
    }

    // Employees can only update their own expenses
    if (req.user.power_level < 75 && expense.created_by !== req.user.id) {
      return res.status(403).json({ error: 'Insufficient permissions.' });
    }

    const { category, amount, date, notes } = req.body;

    const updates = [];
    const params = [];

    if (category !== undefined) {
      updates.push('category = ?');
      params.push(category.trim());
    }

    if (amount !== undefined) {
      updates.push('amount = ?');
      params.push(parseFloat(amount));
    }

    if (date !== undefined) {
      updates.push('date = ?');
      params.push(date);
    }

    if (notes !== undefined) {
      updates.push('notes = ?');
      params.push(notes || null);
    }

    if (req.file) {
      updates.push('receipt_path = ?');
      params.push(`/uploads/${req.file.filename}`);
    }

    if (updates.length === 0) {
      return res.status(400).json({ error: 'No fields to update.' });
    }

    params.push(req.params.id);
    db.prepare(`UPDATE expenses SET ${updates.join(', ')} WHERE id = ?`).run(...params);

    logActivity(req.user.id, 'UPDATE_EXPENSE', `Updated expense: ${expense.category}`);

    const updatedExpense = db.prepare(`
      SELECT e.*, u.name as created_by_name 
      FROM expenses e
      LEFT JOIN users u ON e.created_by = u.id
      WHERE e.id = ?
    `).get(req.params.id);

    res.json({ message: 'Expense updated successfully.', expense: updatedExpense });
  } catch (err) {
    console.error('Update expense error:', err);
    res.status(500).json({ error: 'Internal server error.' });
  }
});

/**
 * DELETE /api/expenses/:id
 * Delete an expense record. Managers/owners only.
 */
router.delete('/:id', requireRole(75), (req, res) => {
  try {
    const db = getDb();
    const expense = db.prepare('SELECT * FROM expenses WHERE id = ?').get(req.params.id);

    if (!expense) {
      return res.status(404).json({ error: 'Expense record not found.' });
    }

    db.prepare('DELETE FROM expenses WHERE id = ?').run(req.params.id);

    logActivity(req.user.id, 'DELETE_EXPENSE', `Deleted expense: ${expense.category} - Amount: ${expense.amount}`);

    res.json({ message: 'Expense deleted successfully.' });
  } catch (err) {
    console.error('Delete expense error:', err);
    res.status(500).json({ error: 'Internal server error.' });
  }
});

module.exports = router;
