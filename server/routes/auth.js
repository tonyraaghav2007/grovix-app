const express = require('express');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const { getDb, logActivity } = require('../database');
const { authenticate, JWT_SECRET } = require('../middleware/auth');

const router = express.Router();

/**
 * POST /api/auth/login
 * Authenticate user with email and password, return JWT token and user info.
 */
router.post('/login', (req, res) => {
  try {
    let { email, password } = req.body;

    if (!email || !password) {
      return res.status(400).json({ error: 'Email and password are required.' });
    }

    // Normalize email aliases
    let cleanEmail = email.toLowerCase().trim();
    if (cleanEmail === 'director1@grovix.com') cleanEmail = 'shivesh@grovix.com';
    else if (cleanEmail === 'director2@grovix.com') cleanEmail = 'sushant@grovix.com';
    else if (cleanEmail === 'director3@grovix.com') cleanEmail = 'yuvik@grovix.com';
    else if (cleanEmail === 'dev@grovix.com') cleanEmail = 'developer@grovix.com';

    const db = getDb();
    const user = db.prepare('SELECT * FROM users WHERE email = ?').get(cleanEmail);

    if (!user) {
      return res.status(401).json({ error: 'Invalid email or password.' });
    }

    if (user.status !== 'active') {
      return res.status(403).json({ error: 'Account is deactivated. Contact an administrator.' });
    }

    const passwordMatch = bcrypt.compareSync(password, user.password_hash);
    if (!passwordMatch) {
      return res.status(401).json({ error: 'Invalid email or password.' });
    }

    const token = jwt.sign({ userId: user.id, role: user.role }, JWT_SECRET, { expiresIn: '24h' });

    logActivity(user.id, 'LOGIN', `User ${user.name} logged in`);

    res.json({
      token,
      user: {
        id: user.id,
        email: user.email,
        name: user.name,
        role: user.role,
        power_level: user.power_level,
        status: user.status,
        phone: user.phone,
        created_at: user.created_at
      }
    });
  } catch (err) {
    console.error('Login error:', err);
    res.status(500).json({ error: 'Internal server error.' });
  }
});

/**
 * GET /api/auth/me & POST /api/auth/me
 * Get current authenticated user info from token.
 */
router.get('/me', authenticate, (req, res) => {
  try {
    res.json({ user: req.user });
  } catch (err) {
    console.error('Me error:', err);
    res.status(500).json({ error: 'Internal server error.' });
  }
});

router.post('/me', authenticate, (req, res) => {
  try {
    res.json({ user: req.user });
  } catch (err) {
    console.error('Me error:', err);
    res.status(500).json({ error: 'Internal server error.' });
  }
});

/**
 * POST /api/auth/change-password
 * Change the authenticated user's own password.
 */
router.post('/change-password', authenticate, (req, res) => {
  try {
    const { currentPassword, newPassword } = req.body;

    if (!currentPassword || !newPassword) {
      return res.status(400).json({ error: 'Current password and new password are required.' });
    }

    if (newPassword.length < 6) {
      return res.status(400).json({ error: 'New password must be at least 6 characters.' });
    }

    const db = getDb();
    const user = db.prepare('SELECT * FROM users WHERE id = ?').get(req.user.id);

    const passwordMatch = bcrypt.compareSync(currentPassword, user.password_hash);
    if (!passwordMatch) {
      return res.status(401).json({ error: 'Current password is incorrect.' });
    }

    const newHash = bcrypt.hashSync(newPassword, 10);
    db.prepare('UPDATE users SET password_hash = ? WHERE id = ?').run(newHash, req.user.id);

    logActivity(req.user.id, 'CHANGE_PASSWORD', `User ${req.user.name} changed their password`);

    res.json({ message: 'Password changed successfully.' });
  } catch (err) {
    console.error('Change password error:', err);
    res.status(500).json({ error: 'Internal server error.' });
  }
});

module.exports = router;
