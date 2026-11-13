const express = require('express');
const bcrypt = require('bcryptjs');
const { v4: uuidv4 } = require('uuid');
const { getDb, logActivity } = require('../database');
const { authenticate, requireRole, requireOwner } = require('../middleware/auth');

const router = express.Router();

// All user routes require authentication
router.use(authenticate);

/**
 * GET /api/users
 * List all users. Owners see everyone. Managers see employees and guests only.
 * Employees/guests see nothing (403).
 */
router.get('/', requireRole(75), (req, res) => {
  try {
    const db = getDb();
    let users;

    if (req.user.power_level >= 100) {
      // Owners see all users
      users = db.prepare(
        'SELECT id, email, name, role, power_level, status, phone, created_at, created_by FROM users ORDER BY power_level DESC, name ASC'
      ).all();
    } else {
      // Managers see employees and guests only
      users = db.prepare(
        'SELECT id, email, name, role, power_level, status, phone, created_at, created_by FROM users WHERE power_level < ? ORDER BY power_level DESC, name ASC'
      ).all(req.user.power_level);
    }

    res.json({ users });
  } catch (err) {
    console.error('List users error:', err);
    res.status(500).json({ error: 'Internal server error.' });
  }
});

/**
 * GET /api/users/:id
 * Get a single user by ID.
 */
router.get('/:id', requireRole(50), (req, res) => {
  try {
    const db = getDb();
    const user = db.prepare(
      'SELECT id, email, name, role, power_level, status, phone, created_at, created_by FROM users WHERE id = ?'
    ).get(req.params.id);

    if (!user) {
      return res.status(404).json({ error: 'User not found.' });
    }

    // Employees can only view themselves
    if (req.user.power_level < 75 && req.user.id !== user.id) {
      return res.status(403).json({ error: 'Insufficient permissions.' });
    }

    // Managers cannot view owners
    if (req.user.power_level < 100 && user.power_level >= req.user.power_level && user.id !== req.user.id) {
      return res.status(403).json({ error: 'Insufficient permissions.' });
    }

    res.json({ user });
  } catch (err) {
    console.error('Get user error:', err);
    res.status(500).json({ error: 'Internal server error.' });
  }
});

/**
 * POST /api/users
 * Create a new user. Owners can create any role. Managers can create employees and guests only.
 */
router.post('/', requireRole(75), (req, res) => {
  try {
    const { email, password, name, role, phone } = req.body;

    if (!email || !password || !name || !role) {
      return res.status(400).json({ error: 'Email, password, name, and role are required.' });
    }

    if (password.length < 6) {
      return res.status(400).json({ error: 'Password must be at least 6 characters.' });
    }

    const validRoles = ['owner', 'manager', 'employee', 'guest'];
    if (!validRoles.includes(role)) {
      return res.status(400).json({ error: 'Invalid role. Must be one of: owner, manager, employee, guest.' });
    }

    const rolePowerMap = { owner: 100, manager: 75, employee: 50, guest: 25 };
    const targetPowerLevel = rolePowerMap[role];

    // Power level check:
    // Owners (100) can create any account (owners, managers, employees, guests).
    // Managers (75) can only create employees (50) and guests (25).
    if (req.user.power_level < 100 && targetPowerLevel >= req.user.power_level) {
      return res.status(403).json({ error: `You cannot create a user with role '${role}'. Insufficient permissions.` });
    }

    const db = getDb();

    // Check for duplicate email
    const existing = db.prepare('SELECT id FROM users WHERE email = ?').get(email.toLowerCase().trim());
    if (existing) {
      return res.status(409).json({ error: 'A user with this email already exists.' });
    }

    const id = uuidv4();
    const passwordHash = bcrypt.hashSync(password, 10);

    db.prepare(`
      INSERT INTO users (id, email, password_hash, name, role, power_level, status, phone, created_at, created_by)
      VALUES (?, ?, ?, ?, ?, ?, 'active', ?, datetime('now'), ?)
    `).run(id, email.toLowerCase().trim(), passwordHash, name.trim(), role, targetPowerLevel, phone || null, req.user.id);

    logActivity(req.user.id, 'CREATE_USER', `Created ${role} account for ${name} (${email})`);

    const newUser = db.prepare(
      'SELECT id, email, name, role, power_level, status, phone, created_at, created_by FROM users WHERE id = ?'
    ).get(id);

    res.status(201).json({ message: 'User created successfully.', user: newUser });
  } catch (err) {
    console.error('Create user error:', err);
    res.status(500).json({ error: 'Internal server error.' });
  }
});

/**
 * PUT /api/users/:id
 * Update a user. Power level restrictions apply.
 */
router.put('/:id', requireRole(75), (req, res) => {
  try {
    const db = getDb();
    const targetUser = db.prepare('SELECT * FROM users WHERE id = ?').get(req.params.id);

    if (!targetUser) {
      return res.status(404).json({ error: 'User not found.' });
    }

    // Cannot edit users with equal or higher power level (unless owner or self)
    if (req.user.power_level < 100 && targetUser.power_level >= req.user.power_level && targetUser.id !== req.user.id) {
      return res.status(403).json({ error: 'You cannot modify a user with equal or higher power level.' });
    }

    const { name, email, phone, role, status, password } = req.body;

    const updates = [];
    const params = [];

    if (name !== undefined) {
      updates.push('name = ?');
      params.push(name.trim());
    }

    if (email !== undefined) {
      const existing = db.prepare('SELECT id FROM users WHERE email = ? AND id != ?').get(email.toLowerCase().trim(), req.params.id);
      if (existing) {
        return res.status(409).json({ error: 'A user with this email already exists.' });
      }
      updates.push('email = ?');
      params.push(email.toLowerCase().trim());
    }

    if (phone !== undefined) {
      updates.push('phone = ?');
      params.push(phone || null);
    }

    if (role !== undefined && req.user.power_level >= 75) {
      const validRoles = ['owner', 'manager', 'employee', 'guest'];
      if (!validRoles.includes(role)) {
        return res.status(400).json({ error: 'Invalid role.' });
      }
      const rolePowerMap = { owner: 100, manager: 75, employee: 50, guest: 25 };
      const newPower = rolePowerMap[role];

      // Only owners can promote to owner
      if (role === 'owner' && req.user.power_level < 100) {
        return res.status(403).json({ error: 'Only owners can assign the owner role.' });
      }

      // Cannot promote to equal or higher level (except owners promoting to owner)
      if (newPower >= req.user.power_level && req.user.power_level < 100) {
        return res.status(403).json({ error: 'Cannot promote user to equal or higher power level.' });
      }

      updates.push('role = ?');
      params.push(role);
      updates.push('power_level = ?');
      params.push(newPower);
    }

    if (status !== undefined && req.user.power_level >= 75) {
      if (!['active', 'inactive'].includes(status)) {
        return res.status(400).json({ error: 'Invalid status. Must be active or inactive.' });
      }
      updates.push('status = ?');
      params.push(status);
    }

    if (password !== undefined) {
      if (password.length < 6) {
        return res.status(400).json({ error: 'Password must be at least 6 characters.' });
      }
      updates.push('password_hash = ?');
      params.push(bcrypt.hashSync(password, 10));
    }

    if (updates.length === 0) {
      return res.status(400).json({ error: 'No fields to update.' });
    }

    params.push(req.params.id);
    db.prepare(`UPDATE users SET ${updates.join(', ')} WHERE id = ?`).run(...params);

    logActivity(req.user.id, 'UPDATE_USER', `Updated user ${targetUser.name} (${targetUser.email})`);

    const updatedUser = db.prepare(
      'SELECT id, email, name, role, power_level, status, phone, created_at, created_by FROM users WHERE id = ?'
    ).get(req.params.id);

    res.json({ message: 'User updated successfully.', user: updatedUser });
  } catch (err) {
    console.error('Update user error:', err);
    res.status(500).json({ error: 'Internal server error.' });
  }
});

/**
 * DELETE /api/users/:id
 * Deactivate a user. Owners can deactivate managers/employees/guests.
 * Only owners can deactivate other owners. No one can deactivate themselves.
 */
/**
 * DELETE /api/users/:id
 * Remove or deactivate a user.
 * Founders cannot be deactivated or removed by ANYONE (protected).
 * Managers can only remove/deactivate employees and guests.
 * Supports query parameter: ?permanent=true for permanent deletion.
 */
router.delete('/:id', requireRole(75), (req, res) => {
  try {
    const db = getDb();
    const targetUser = db.prepare('SELECT * FROM users WHERE id = ?').get(req.params.id);

    if (!targetUser) {
      return res.status(404).json({ error: 'User not found.' });
    }

    // Cannot remove yourself
    if (targetUser.id === req.user.id) {
      return res.status(403).json({ error: 'You cannot remove your own account.' });
    }

    // Founders protection: Founders/Directors CANNOT be removed or deactivated by anyone
    if (targetUser.power_level >= 100 || targetUser.role === 'owner') {
      return res.status(403).json({ error: 'Founders and Directors are protected and cannot be deactivated or removed.' });
    }

    // Managers can only deactivate/remove users with lower power level
    if (req.user.power_level < 100 && targetUser.power_level >= req.user.power_level) {
      return res.status(403).json({ error: 'You cannot remove a user with equal or higher power level.' });
    }

    const isPermanent = req.query.permanent === 'true';

    if (isPermanent) {
      // Unassign tasks from this user first
      db.prepare('UPDATE tasks SET assigned_to = NULL WHERE assigned_to = ?').run(req.params.id);
      db.prepare('DELETE FROM users WHERE id = ?').run(req.params.id);
      logActivity(req.user.id, 'DELETE_USER', `Permanently removed team member ${targetUser.name} (${targetUser.email})`);
      res.json({ message: `User ${targetUser.name} has been permanently removed.` });
    } else {
      db.prepare('UPDATE users SET status = ? WHERE id = ?').run('inactive', req.params.id);
      logActivity(req.user.id, 'DEACTIVATE_USER', `Deactivated user ${targetUser.name} (${targetUser.email})`);
      res.json({ message: `User ${targetUser.name} has been deactivated.` });
    }
  } catch (err) {
    console.error('Delete user error:', err);
    res.status(500).json({ error: 'Internal server error.' });
  }
});

module.exports = router;
