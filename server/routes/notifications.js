const express = require('express');
const { v4: uuidv4 } = require('uuid');
const { getDb } = require('../database');
const { authenticate, requireRole } = require('../middleware/auth');

const router = express.Router();

// All notification routes require authentication
router.use(authenticate);

/**
 * GET /api/notifications
 * Get notifications for the current user.
 * Supports query: unread_only=true
 */
router.get('/', requireRole(25), (req, res) => {
  try {
    const db = getDb();
    const { unread_only } = req.query;

    let query = 'SELECT * FROM notifications WHERE user_id = ?';
    const params = [req.user.id];

    if (unread_only === 'true') {
      query += ' AND read = 0';
    }

    query += ' ORDER BY created_at DESC';

    const notifications = db.prepare(query).all(...params);

    const unreadCount = db.prepare(
      'SELECT COUNT(*) as count FROM notifications WHERE user_id = ? AND read = 0'
    ).get(req.user.id).count;

    res.json({ notifications, unreadCount });
  } catch (err) {
    console.error('List notifications error:', err);
    res.status(500).json({ error: 'Internal server error.' });
  }
});

/**
 * POST /api/notifications
 * Create a notification or mention. Employees, managers, and owners can send notifications.
 */
router.post('/', requireRole(50), (req, res) => {
  try {
    const { user_id, message, type } = req.body;

    if (!user_id || !message) {
      return res.status(400).json({ error: 'User ID and message are required.' });
    }

    const db = getDb();

    // Verify target user exists
    const targetUser = db.prepare('SELECT id, name FROM users WHERE id = ?').get(user_id);
    if (!targetUser) {
      return res.status(404).json({ error: 'Target user not found.' });
    }

    const id = uuidv4();

    db.prepare(`
      INSERT INTO notifications (id, user_id, message, type, read, created_at)
      VALUES (?, ?, ?, ?, 0, datetime('now'))
    `).run(id, user_id, message.trim(), type || 'info');

    const notification = db.prepare('SELECT * FROM notifications WHERE id = ?').get(id);

    res.status(201).json({ message: 'Notification sent successfully.', notification });
  } catch (err) {
    console.error('Create notification error:', err);
    res.status(500).json({ error: 'Internal server error.' });
  }
});

/**
 * PUT /api/notifications/:id/read
 * Mark a notification as read.
 */
router.put('/:id/read', requireRole(25), (req, res) => {
  try {
    const db = getDb();
    const notification = db.prepare('SELECT * FROM notifications WHERE id = ?').get(req.params.id);

    if (!notification) {
      return res.status(404).json({ error: 'Notification not found.' });
    }

    // Users can only mark their own notifications as read
    if (notification.user_id !== req.user.id) {
      return res.status(403).json({ error: 'Insufficient permissions.' });
    }

    db.prepare('UPDATE notifications SET read = 1 WHERE id = ?').run(req.params.id);

    res.json({ message: 'Notification marked as read.' });
  } catch (err) {
    console.error('Mark notification read error:', err);
    res.status(500).json({ error: 'Internal server error.' });
  }
});

/**
 * PUT /api/notifications/read-all
 * Mark all notifications as read for the current user.
 */
router.put('/read-all', requireRole(25), (req, res) => {
  try {
    const db = getDb();
    const result = db.prepare('UPDATE notifications SET read = 1 WHERE user_id = ? AND read = 0').run(req.user.id);

    res.json({ message: `Marked ${result.changes} notifications as read.` });
  } catch (err) {
    console.error('Mark all read error:', err);
    res.status(500).json({ error: 'Internal server error.' });
  }
});

/**
 * DELETE /api/notifications/:id
 * Delete a notification. Users can delete their own, owners can delete any.
 */
router.delete('/:id', requireRole(25), (req, res) => {
  try {
    const db = getDb();
    const notification = db.prepare('SELECT * FROM notifications WHERE id = ?').get(req.params.id);

    if (!notification) {
      return res.status(404).json({ error: 'Notification not found.' });
    }

    // Users can only delete their own notifications (owners can delete any)
    if (notification.user_id !== req.user.id && req.user.power_level < 100) {
      return res.status(403).json({ error: 'Insufficient permissions.' });
    }

    db.prepare('DELETE FROM notifications WHERE id = ?').run(req.params.id);

    res.json({ message: 'Notification deleted.' });
  } catch (err) {
    console.error('Delete notification error:', err);
    res.status(500).json({ error: 'Internal server error.' });
  }
});

module.exports = router;
