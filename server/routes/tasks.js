const express = require('express');
const { v4: uuidv4 } = require('uuid');
const { getDb, logActivity } = require('../database');
const { authenticate, requireRole } = require('../middleware/auth');

const router = express.Router();

// All task routes require authentication
router.use(authenticate);

/**
 * GET /api/tasks/stats
 * Get task statistics. Must be defined before /:id to avoid route conflicts.
 */
router.get('/stats', requireRole(50), (req, res) => {
  try {
    const db = getDb();
    let stats;

    if (req.user.power_level >= 75) {
      // Owners/managers see all task stats
      stats = {
        total: db.prepare('SELECT COUNT(*) as count FROM tasks').get().count,
        todo: db.prepare("SELECT COUNT(*) as count FROM tasks WHERE status = 'todo'").get().count,
        inProgress: db.prepare("SELECT COUNT(*) as count FROM tasks WHERE status = 'in-progress'").get().count,
        completed: db.prepare("SELECT COUNT(*) as count FROM tasks WHERE status = 'completed'").get().count,
        urgent: db.prepare("SELECT COUNT(*) as count FROM tasks WHERE priority = 'urgent' AND status != 'completed'").get().count,
        overdue: db.prepare("SELECT COUNT(*) as count FROM tasks WHERE due_date < date('now') AND status != 'completed'").get().count
      };
    } else {
      // Employees see only their own task stats
      stats = {
        total: db.prepare('SELECT COUNT(*) as count FROM tasks WHERE assigned_to = ?').get(req.user.id).count,
        todo: db.prepare("SELECT COUNT(*) as count FROM tasks WHERE assigned_to = ? AND status = 'todo'").get(req.user.id).count,
        inProgress: db.prepare("SELECT COUNT(*) as count FROM tasks WHERE assigned_to = ? AND status = 'in-progress'").get(req.user.id).count,
        completed: db.prepare("SELECT COUNT(*) as count FROM tasks WHERE assigned_to = ? AND status = 'completed'").get(req.user.id).count,
        urgent: db.prepare("SELECT COUNT(*) as count FROM tasks WHERE assigned_to = ? AND priority = 'urgent' AND status != 'completed'").get(req.user.id).count,
        overdue: db.prepare("SELECT COUNT(*) as count FROM tasks WHERE assigned_to = ? AND due_date < date('now') AND status != 'completed'").get(req.user.id).count
      };
    }

    res.json({ stats });
  } catch (err) {
    console.error('Task stats error:', err);
    res.status(500).json({ error: 'Internal server error.' });
  }
});

/**
 * GET /api/tasks
 * Get tasks. Employees see only tasks assigned to them. Managers/owners see all.
 * Supports query filters: status, priority, assigned_to
 */
router.get('/', requireRole(50), (req, res) => {
  try {
    const db = getDb();
    const { status, priority, assigned_to } = req.query;

    let query = `
      SELECT t.*, 
             u1.name as assigned_to_name, 
             u2.name as assigned_by_name
      FROM tasks t
      LEFT JOIN users u1 ON t.assigned_to = u1.id
      LEFT JOIN users u2 ON t.assigned_by = u2.id
    `;

    const conditions = [];
    const params = [];

    // Employees see only their own tasks
    if (req.user.power_level < 75) {
      conditions.push('t.assigned_to = ?');
      params.push(req.user.id);
    }

    if (status) {
      conditions.push('t.status = ?');
      params.push(status);
    }

    if (priority) {
      conditions.push('t.priority = ?');
      params.push(priority);
    }

    if (assigned_to && req.user.power_level >= 75) {
      conditions.push('t.assigned_to = ?');
      params.push(assigned_to);
    }

    if (conditions.length > 0) {
      query += ' WHERE ' + conditions.join(' AND ');
    }

    query += ' ORDER BY t.created_at DESC';

    const tasks = db.prepare(query).all(...params);
    res.json({ tasks });
  } catch (err) {
    console.error('List tasks error:', err);
    res.status(500).json({ error: 'Internal server error.' });
  }
});

/**
 * GET /api/tasks/:id
 * Get a single task by ID.
 */
router.get('/:id', requireRole(50), (req, res) => {
  try {
    const db = getDb();
    const task = db.prepare(`
      SELECT t.*, 
             u1.name as assigned_to_name, 
             u2.name as assigned_by_name
      FROM tasks t
      LEFT JOIN users u1 ON t.assigned_to = u1.id
      LEFT JOIN users u2 ON t.assigned_by = u2.id
      WHERE t.id = ?
    `).get(req.params.id);

    if (!task) {
      return res.status(404).json({ error: 'Task not found.' });
    }

    // Employees can only view their own tasks
    if (req.user.power_level < 75 && task.assigned_to !== req.user.id) {
      return res.status(403).json({ error: 'Insufficient permissions.' });
    }

    res.json({ task });
  } catch (err) {
    console.error('Get task error:', err);
    res.status(500).json({ error: 'Internal server error.' });
  }
});

/**
 * POST /api/tasks
 * Create a new task. Owners/managers can assign to anyone below them.
 */
router.post('/', requireRole(75), (req, res) => {
  try {
    let { title, description, detailed_notes, detailedNotes, assigned_to, priority, due_date, status, assignee, dueDate } = req.body;
    assigned_to = assigned_to || assignee;
    due_date = due_date || dueDate;
    const finalNotes = detailed_notes || detailedNotes || '';

    if (!title) {
      return res.status(400).json({ error: 'Task title is required.' });
    }

    const db = getDb();

    // If assigning to someone, verify they exist and are below the assigner
    if (assigned_to) {
      const assignee = db.prepare('SELECT id, power_level, name FROM users WHERE id = ? AND status = ?').get(assigned_to, 'active');
      if (!assignee) {
        return res.status(404).json({ error: 'Assigned user not found or inactive.' });
      }
      if (assignee.power_level >= req.user.power_level && assignee.id !== req.user.id) {
        return res.status(403).json({ error: 'Cannot assign tasks to users with equal or higher power level.' });
      }
    }

    const validPriorities = ['low', 'medium', 'high', 'urgent'];
    const validStatuses = ['todo', 'in-progress', 'delayed', 'completed'];

    const id = uuidv4();
    const taskPriority = priority && validPriorities.includes(priority) ? priority : 'medium';
    const taskStatus = status && validStatuses.includes(status) ? status : 'todo';

    db.prepare(`
      INSERT INTO tasks (id, title, description, detailed_notes, assigned_to, assigned_by, status, priority, due_date, created_at, updated_at)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, datetime('now'), datetime('now'))
    `).run(id, title.trim(), description || null, finalNotes, assigned_to || null, req.user.id, taskStatus, taskPriority, due_date || null);

    logActivity(req.user.id, 'CREATE_TASK', `Created task: ${title}`);

    // Notify the assignee
    if (assigned_to && assigned_to !== req.user.id) {
      db.prepare(`
        INSERT INTO notifications (id, user_id, message, type, read, created_at)
        VALUES (?, ?, ?, 'task', 0, datetime('now'))
      `).run(uuidv4(), assigned_to, `New task assigned to you: "${title}" by ${req.user.name}`);
    }

    const task = db.prepare(`
      SELECT t.*, 
             u1.name as assigned_to_name, 
             u2.name as assigned_by_name
      FROM tasks t
      LEFT JOIN users u1 ON t.assigned_to = u1.id
      LEFT JOIN users u2 ON t.assigned_by = u2.id
      WHERE t.id = ?
    `).get(id);

    res.status(201).json({ message: 'Task created successfully.', task });
  } catch (err) {
    console.error('Create task error:', err);
    res.status(500).json({ error: 'Internal server error.' });
  }
});

/**
 * PUT /api/tasks/:id
 * Update a task. Assignee can update status only. Assigner/owners can update everything.
 */
router.put('/:id', requireRole(50), (req, res) => {
  try {
    const db = getDb();
    const task = db.prepare('SELECT * FROM tasks WHERE id = ?').get(req.params.id);

    if (!task) {
      return res.status(404).json({ error: 'Task not found.' });
    }

    const isAssignee = task.assigned_to === req.user.id;
    const isAssigner = task.assigned_by === req.user.id;
    const isOwnerOrManager = req.user.power_level >= 75;

    if (!isAssignee && !isAssigner && !isOwnerOrManager) {
      return res.status(403).json({ error: 'Insufficient permissions to update this task.' });
    }

    const { title, description, detailed_notes, detailedNotes, assigned_to, priority, due_date, status } = req.body;
    const finalNotes = detailed_notes !== undefined ? detailed_notes : detailedNotes;

    // Employees (assignees) can update status
    if (isAssignee && !isAssigner && !isOwnerOrManager) {
      if (title || description !== undefined || assigned_to || priority || due_date) {
        return res.status(403).json({ error: 'You can only update the task status.' });
      }
      if (status) {
        const validStatuses = ['todo', 'in-progress', 'delayed', 'completed'];
        if (!validStatuses.includes(status)) {
          return res.status(400).json({ error: 'Invalid status.' });
        }
        db.prepare("UPDATE tasks SET status = ?, updated_at = datetime('now') WHERE id = ?").run(status, req.params.id);
        logActivity(req.user.id, 'UPDATE_TASK_STATUS', `Updated task "${task.title}" status to ${status}`);

        // Notify the assigner
        if (task.assigned_by && task.assigned_by !== req.user.id) {
          db.prepare(`
            INSERT INTO notifications (id, user_id, message, type, read, created_at)
            VALUES (?, ?, ?, 'task', 0, datetime('now'))
          `).run(uuidv4(), task.assigned_by, `Task "${task.title}" status updated to "${status}" by ${req.user.name}`);
        }
      }
    } else {
      // Assigner/owner/manager can update everything
      const updates = [];
      const params = [];

      if (title !== undefined) {
        updates.push('title = ?');
        params.push(title.trim());
      }

      if (description !== undefined) {
        updates.push('description = ?');
        params.push(description);
      }

      if (finalNotes !== undefined) {
        updates.push('detailed_notes = ?');
        params.push(finalNotes);
      }

      if (assigned_to !== undefined) {
        if (assigned_to) {
          const assignee = db.prepare('SELECT id, power_level FROM users WHERE id = ? AND status = ?').get(assigned_to, 'active');
          if (!assignee) {
            return res.status(404).json({ error: 'Assigned user not found or inactive.' });
          }
          if (assignee.power_level >= req.user.power_level && assignee.id !== req.user.id) {
            return res.status(403).json({ error: 'Cannot assign tasks to users with equal or higher power level.' });
          }
        }
        updates.push('assigned_to = ?');
        params.push(assigned_to || null);
      }

      if (status !== undefined) {
        const validStatuses = ['todo', 'in-progress', 'delayed', 'completed'];
        if (!validStatuses.includes(status)) {
          return res.status(400).json({ error: 'Invalid status.' });
        }
        updates.push('status = ?');
        params.push(status);
      }

      if (priority !== undefined) {
        const validPriorities = ['low', 'medium', 'high', 'urgent'];
        if (!validPriorities.includes(priority)) {
          return res.status(400).json({ error: 'Invalid priority.' });
        }
        updates.push('priority = ?');
        params.push(priority);
      }

      if (due_date !== undefined) {
        updates.push('due_date = ?');
        params.push(due_date || null);
      }

      if (updates.length > 0) {
        updates.push("updated_at = datetime('now')");
        params.push(req.params.id);
        db.prepare(`UPDATE tasks SET ${updates.join(', ')} WHERE id = ?`).run(...params);
        logActivity(req.user.id, 'UPDATE_TASK', `Updated task: ${task.title}`);
      }
    }

    const updatedTask = db.prepare(`
      SELECT t.*, 
             u1.name as assigned_to_name, 
             u2.name as assigned_by_name
      FROM tasks t
      LEFT JOIN users u1 ON t.assigned_to = u1.id
      LEFT JOIN users u2 ON t.assigned_by = u2.id
      WHERE t.id = ?
    `).get(req.params.id);

    res.json({ message: 'Task updated successfully.', task: updatedTask });
  } catch (err) {
    console.error('Update task error:', err);
    res.status(500).json({ error: 'Internal server error.' });
  }
});

/**
 * DELETE /api/tasks/:id
 * Delete a task. Owners and managers only.
 */
router.delete('/:id', requireRole(75), (req, res) => {
  try {
    const db = getDb();
    const task = db.prepare('SELECT * FROM tasks WHERE id = ?').get(req.params.id);

    if (!task) {
      return res.status(404).json({ error: 'Task not found.' });
    }

    db.prepare('DELETE FROM tasks WHERE id = ?').run(req.params.id);

    logActivity(req.user.id, 'DELETE_TASK', `Deleted task: ${task.title}`);

    res.json({ message: 'Task deleted successfully.' });
  } catch (err) {
    console.error('Delete task error:', err);
    res.status(500).json({ error: 'Internal server error.' });
  }
});

module.exports = router;
