const express = require('express');
const { v4: uuidv4 } = require('uuid');
const { getDb, logActivity } = require('../database');
const { authenticate, requireRole } = require('../middleware/auth');

const router = express.Router();

// All sales routes require authentication
router.use(authenticate);

/**
 * GET /api/sales
 * Get all sales records. Owners/managers see all, employees see their own.
 * Supports query filters: date_from, date_to, client
 */
router.get('/', requireRole(50), (req, res) => {
  try {
    const db = getDb();
    const { date_from, date_to, client } = req.query;

    let query = `
      SELECT s.*, u.name as created_by_name 
      FROM sales s
      LEFT JOIN users u ON s.created_by = u.id
    `;

    const conditions = [];
    const params = [];

    // Employees see only their own sales
    if (req.user.power_level < 75) {
      conditions.push('s.created_by = ?');
      params.push(req.user.id);
    }

    if (date_from) {
      conditions.push('s.date >= ?');
      params.push(date_from);
    }

    if (date_to) {
      conditions.push('s.date <= ?');
      params.push(date_to);
    }

    if (client) {
      conditions.push('s.client LIKE ?');
      params.push(`%${client}%`);
    }

    if (conditions.length > 0) {
      query += ' WHERE ' + conditions.join(' AND ');
    }

    query += ' ORDER BY s.date DESC, s.created_at DESC';

    const sales = db.prepare(query).all(...params);
    res.json({ sales });
  } catch (err) {
    console.error('List sales error:', err);
    res.status(500).json({ error: 'Internal server error.' });
  }
});

/**
 * GET /api/sales/:id
 * Get a single sale record.
 */
router.get('/:id', requireRole(50), (req, res) => {
  try {
    const db = getDb();
    const sale = db.prepare(`
      SELECT s.*, u.name as created_by_name 
      FROM sales s
      LEFT JOIN users u ON s.created_by = u.id
      WHERE s.id = ?
    `).get(req.params.id);

    if (!sale) {
      return res.status(404).json({ error: 'Sale record not found.' });
    }

    // Employees can only see their own sales
    if (req.user.power_level < 75 && sale.created_by !== req.user.id) {
      return res.status(403).json({ error: 'Insufficient permissions.' });
    }

    res.json({ sale });
  } catch (err) {
    console.error('Get sale error:', err);
    res.status(500).json({ error: 'Internal server error.' });
  }
});

/**
 * POST /api/sales
 * Create a new sale record. Employees, managers, and owners can create sales.
 */
router.post('/', requireRole(50), (req, res) => {
  try {
    const { product_service, quantity, price, client, date, notes } = req.body;

    if (!product_service || price === undefined || !date) {
      return res.status(400).json({ error: 'Product/service, price, and date are required.' });
    }

    const qty = quantity || 1;
    const total = qty * price;

    const db = getDb();
    const id = uuidv4();

    db.prepare(`
      INSERT INTO sales (id, product_service, quantity, price, total, client, date, notes, created_by, created_at)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, datetime('now'))
    `).run(id, product_service.trim(), qty, price, total, client || null, date, notes || null, req.user.id);

    logActivity(req.user.id, 'CREATE_SALE', `Added sale: ${product_service} - Total: ${total}`);

    const sale = db.prepare(`
      SELECT s.*, u.name as created_by_name 
      FROM sales s
      LEFT JOIN users u ON s.created_by = u.id
      WHERE s.id = ?
    `).get(id);

    res.status(201).json({ message: 'Sale recorded successfully.', sale });
  } catch (err) {
    console.error('Create sale error:', err);
    res.status(500).json({ error: 'Internal server error.' });
  }
});

/**
 * PUT /api/sales/:id
 * Update a sale record. Creator can update their own, managers/owners can update any.
 */
router.put('/:id', requireRole(50), (req, res) => {
  try {
    const db = getDb();
    const sale = db.prepare('SELECT * FROM sales WHERE id = ?').get(req.params.id);

    if (!sale) {
      return res.status(404).json({ error: 'Sale record not found.' });
    }

    // Employees can only update their own sales
    if (req.user.power_level < 75 && sale.created_by !== req.user.id) {
      return res.status(403).json({ error: 'Insufficient permissions.' });
    }

    const { product_service, quantity, price, client, date, notes } = req.body;

    const updates = [];
    const params = [];

    if (product_service !== undefined) {
      updates.push('product_service = ?');
      params.push(product_service.trim());
    }

    if (quantity !== undefined) {
      updates.push('quantity = ?');
      params.push(quantity);
    }

    if (price !== undefined) {
      updates.push('price = ?');
      params.push(price);
    }

    // Recalculate total if quantity or price changed
    const newQty = quantity !== undefined ? quantity : sale.quantity;
    const newPrice = price !== undefined ? price : sale.price;
    if (quantity !== undefined || price !== undefined) {
      updates.push('total = ?');
      params.push(newQty * newPrice);
    }

    if (client !== undefined) {
      updates.push('client = ?');
      params.push(client || null);
    }

    if (date !== undefined) {
      updates.push('date = ?');
      params.push(date);
    }

    if (notes !== undefined) {
      updates.push('notes = ?');
      params.push(notes || null);
    }

    if (updates.length === 0) {
      return res.status(400).json({ error: 'No fields to update.' });
    }

    params.push(req.params.id);
    db.prepare(`UPDATE sales SET ${updates.join(', ')} WHERE id = ?`).run(...params);

    logActivity(req.user.id, 'UPDATE_SALE', `Updated sale: ${sale.product_service}`);

    const updatedSale = db.prepare(`
      SELECT s.*, u.name as created_by_name 
      FROM sales s
      LEFT JOIN users u ON s.created_by = u.id
      WHERE s.id = ?
    `).get(req.params.id);

    res.json({ message: 'Sale updated successfully.', sale: updatedSale });
  } catch (err) {
    console.error('Update sale error:', err);
    res.status(500).json({ error: 'Internal server error.' });
  }
});

/**
 * DELETE /api/sales/:id
 * Delete a sale record. Managers/owners only.
 */
router.delete('/:id', requireRole(75), (req, res) => {
  try {
    const db = getDb();
    const sale = db.prepare('SELECT * FROM sales WHERE id = ?').get(req.params.id);

    if (!sale) {
      return res.status(404).json({ error: 'Sale record not found.' });
    }

    db.prepare('DELETE FROM sales WHERE id = ?').run(req.params.id);

    logActivity(req.user.id, 'DELETE_SALE', `Deleted sale: ${sale.product_service} - Total: ${sale.total}`);

    res.json({ message: 'Sale deleted successfully.' });
  } catch (err) {
    console.error('Delete sale error:', err);
    res.status(500).json({ error: 'Internal server error.' });
  }
});

module.exports = router;
