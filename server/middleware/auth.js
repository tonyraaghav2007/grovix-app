const jwt = require('jsonwebtoken');
const { getDb } = require('../database');

const JWT_SECRET = process.env.JWT_SECRET || 'grovix-secret-key-change-in-production';

/**
 * Core authentication middleware.
 * Verifies the JWT token from the Authorization header and attaches the user object to req.user.
 */
function authenticate(req, res, next) {
  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({ error: 'Authentication required. No token provided.' });
  }

  const token = authHeader.split(' ')[1];

  try {
    const decoded = jwt.verify(token, JWT_SECRET);
    const db = getDb();
    const user = db.prepare('SELECT id, email, name, role, power_level, status, phone, created_at FROM users WHERE id = ?').get(decoded.userId);

    if (!user) {
      return res.status(401).json({ error: 'User not found. Token is invalid.' });
    }

    if (user.status !== 'active') {
      return res.status(403).json({ error: 'Account is deactivated. Contact an administrator.' });
    }

    req.user = user;
    next();
  } catch (err) {
    if (err.name === 'TokenExpiredError') {
      return res.status(401).json({ error: 'Token has expired. Please log in again.' });
    }
    return res.status(401).json({ error: 'Invalid token.' });
  }
}

/**
 * Role-check middleware factory.
 * Requires the authenticated user to have at least the specified minimum power level.
 * @param {number} minPowerLevel - Minimum power level required (25, 50, 75, or 100)
 */
function requireRole(minPowerLevel) {
  return (req, res, next) => {
    if (!req.user) {
      return res.status(401).json({ error: 'Authentication required.' });
    }
    if (req.user.power_level < minPowerLevel) {
      return res.status(403).json({ error: 'Insufficient permissions. Higher role required.' });
    }
    next();
  };
}

/**
 * Convenience middleware that restricts access to owners only (power_level 100).
 */
function requireOwner() {
  return requireRole(100);
}

module.exports = { authenticate, requireRole, requireOwner, JWT_SECRET };
