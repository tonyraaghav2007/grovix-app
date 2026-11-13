const express = require('express');
const { getDb } = require('../database');
const { authenticate } = require('../middleware/auth');

const router = express.Router();

router.use(authenticate);

/**
 * GET /api/sync
 * Central Cloud Sync endpoint:
 * Returns all current data synchronized for the authenticated user based on role hierarchy.
 */
router.get('/', (req, res) => {
  try {
    const db = getDb();
    const user = req.user;
    const isManagerOrOwner = user.power_level >= 75;

    // 1. Fetch Tasks
    let tasksQuery = `
      SELECT t.*, 
             u1.name as assigned_to_name, 
             u2.name as assigned_by_name
      FROM tasks t
      LEFT JOIN users u1 ON t.assigned_to = u1.id
      LEFT JOIN users u2 ON t.assigned_by = u2.id
    `;
    const tasksParams = [];
    if (!isManagerOrOwner) {
      // Employees see only their assigned tasks
      tasksQuery += ' WHERE t.assigned_to = ?';
      tasksParams.push(user.id);
    }
    tasksQuery += ' ORDER BY t.created_at DESC';
    const tasks = db.prepare(tasksQuery).all(...tasksParams);

    // 2. Fetch Sales (Managers & Owners only)
    let sales = [];
    if (isManagerOrOwner) {
      sales = db.prepare(`
        SELECT s.*, u.name as created_by_name 
        FROM sales s
        LEFT JOIN users u ON s.created_by = u.id
        ORDER BY s.date DESC, s.created_at DESC
      `).all();
    }

    // 3. Fetch Expenses (Managers & Owners only)
    let expenses = [];
    if (isManagerOrOwner) {
      expenses = db.prepare(`
        SELECT e.*, u.name as created_by_name 
        FROM expenses e
        LEFT JOIN users u ON e.created_by = u.id
        ORDER BY e.date DESC, e.created_at DESC
      `).all();
    }

    // 4. Fetch Users (All users can see the team directory for assignments & mentions)
    const users = db.prepare(`
      SELECT id, email, name, role, power_level, status, phone, created_at, created_by 
      FROM users 
      ORDER BY power_level DESC, name ASC
    `).all();

    // 5. Fetch Notifications for current user
    const notifications = db.prepare(`
      SELECT * FROM notifications 
      WHERE user_id = ? 
      ORDER BY created_at DESC 
      LIMIT 100
    `).all(user.id);

    const unreadNotifs = db.prepare(`
      SELECT COUNT(*) as count FROM notifications 
      WHERE user_id = ? AND read = 0
    `).get(user.id).count;

    res.json({
      status: 'ok',
      syncTime: new Date().toISOString(),
      user: {
        id: user.id,
        email: user.email,
        name: user.name,
        role: user.role,
        power_level: user.power_level,
        status: user.status,
        phone: user.phone
      },
      tasks,
      sales,
      expenses,
      users,
      notifications,
      unreadNotifs,
      currency: 'INR (₹)'
    });
  } catch (err) {
    console.error('Central Cloud Sync error:', err);
    res.status(500).json({ error: 'Central Cloud Sync failed', details: err.message });
  }
});

module.exports = router;

