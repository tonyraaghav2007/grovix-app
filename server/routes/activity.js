const express = require('express');
const { getDb } = require('../database');
const { authenticate, requireRole } = require('../middleware/auth');

const router = express.Router();

// All activity routes require authentication
router.use(authenticate);

/**
 * GET /api/activity
 * Get activity log entries.
 * Owners see all activity. Managers see their team's activity. Employees see their own.
 * Supports query: limit (default 50), offset (default 0)
 */
router.get('/', requireRole(50), (req, res) => {
  try {
    const db = getDb();
    const limit = Math.min(parseInt(req.query.limit) || 50, 200);
    const offset = parseInt(req.query.offset) || 0;

    let query, countQuery;
    const params = [];
    const countParams = [];

    if (req.user.power_level >= 100) {
      // Owners see all activity
      query = `
        SELECT al.*, u.name as user_name, u.role as user_role
        FROM activity_log al
        LEFT JOIN users u ON al.user_id = u.id
        ORDER BY al.timestamp DESC
        LIMIT ? OFFSET ?
      `;
      params.push(limit, offset);

      countQuery = 'SELECT COUNT(*) as total FROM activity_log';
    } else if (req.user.power_level >= 75) {
      // Managers see activity of users with lower power level + their own
      query = `
        SELECT al.*, u.name as user_name, u.role as user_role
        FROM activity_log al
        LEFT JOIN users u ON al.user_id = u.id
        WHERE u.power_level < ? OR al.user_id = ?
        ORDER BY al.timestamp DESC
        LIMIT ? OFFSET ?
      `;
      params.push(req.user.power_level, req.user.id, limit, offset);

      countQuery = `
        SELECT COUNT(*) as total FROM activity_log al
        LEFT JOIN users u ON al.user_id = u.id
        WHERE u.power_level < ? OR al.user_id = ?
      `;
      countParams.push(req.user.power_level, req.user.id);
    } else {
      // Employees see only their own activity
      query = `
        SELECT al.*, u.name as user_name, u.role as user_role
        FROM activity_log al
        LEFT JOIN users u ON al.user_id = u.id
        WHERE al.user_id = ?
        ORDER BY al.timestamp DESC
        LIMIT ? OFFSET ?
      `;
      params.push(req.user.id, limit, offset);

      countQuery = 'SELECT COUNT(*) as total FROM activity_log WHERE user_id = ?';
      countParams.push(req.user.id);
    }

    const activities = db.prepare(query).all(...params);
    const total = db.prepare(countQuery).get(...countParams).total;

    res.json({
      activities,
      pagination: {
        total,
        limit,
        offset,
        hasMore: offset + limit < total
      }
    });
  } catch (err) {
    console.error('Activity log error:', err);
    res.status(500).json({ error: 'Internal server error.' });
  }
});

module.exports = router;
