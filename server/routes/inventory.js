const express = require('express');
const { v4: uuidv4 } = require('uuid');
const { getDb, logActivity } = require('../database');
const { authenticate, requireRole } = require('../middleware/auth');

const router = express.Router();

// All inventory routes require authentication
router.use(authenticate);

/**
 * GET /api/inventory
 * Get all inventory items. All authenticated users with power >= 50 can view.
 * Supports query filters: search, low_stock (boolean)
 */
router.get('/', requireRole(50), (req, res) => {
  try {
    const db = getDb();
    const { search, low_stock } = req.query;

    let query = `
      SELECT i.*, u.name as created_by_name 
      FROM inventory i
      LEFT JOIN users u ON i.created_by = u.id
    `;

    const conditions = [];
    const params = [];

    if (search) {
      conditions.push('(i.item_name LIKE ? OR i.sku LIKE ? OR i.supplier LIKE ?)');
      params.push(`%${search}%`, `%${search}%`, `%${search}%`);
    }

    if (low_stock === 'true') {
      conditions.push('i.quantity <= i.reorder_threshold');
    }

    if (conditions.length > 0) {
      query += ' WHERE ' + conditions.join(' AND ');
    }

    query += ' ORDER BY i.item_name ASC';

    const items = db.prepare(query).all(...params);

    // Add reorder alert flag to each item
    const itemsWithAlerts = items.map(item => ({
      ...item,
      reorder_alert: item.quantity <= item.reorder_threshold
    }));

    res.json({ inventory: itemsWithAlerts });
  } catch (err) {
    console.error('List inventory error:', err);
    res.status(500).json({ error: 'Internal server error.' });
  }
});

/**
 * GET /api/inventory/alerts
 * Get items that are below or at their reorder threshold.
 */
router.get('/alerts', requireRole(50), (req, res) => {
  try {
    const db = getDb();
    const alerts = db.prepare(`
      SELECT i.*, u.name as created_by_name 
      FROM inventory i
      LEFT JOIN users u ON i.created_by = u.id
      WHERE i.quantity <= i.reorder_threshold
      ORDER BY i.quantity ASC
    `).all();

    res.json({ alerts, count: alerts.length });
  } catch (err) {
    console.error('Inventory alerts error:', err);
    res.status(500).json({ error: 'Internal server error.' });
  }
});

/**
 * GET /api/inventory/:id
 * Get a single inventory item.
 */
router.get('/:id', requireRole(50), (req, res) => {
  try {
    const db = getDb();
    const item = db.prepare(`
      SELECT i.*, u.name as created_by_name 
      FROM inventory i
      LEFT JOIN users u ON i.created_by = u.id
      WHERE i.id = ?
    `).get(req.params.id);

    if (!item) {
      return res.status(404).json({ error: 'Inventory item not found.' });
    }

    item.reorder_alert = item.quantity <= item.reorder_threshold;

    res.json({ item });
  } catch (err) {
    console.error('Get inventory item error:', err);
    res.status(500).json({ error: 'Internal server error.' });
  }
});

/**
 * POST /api/inventory
 * Create a new inventory item. Managers and owners only.
 */
router.post('/', requireRole(75), (req, res) => {
  try {
    const { item_name, sku, quantity, unit_price, supplier, reorder_threshold } = req.body;

    if (!item_name) {
      return res.status(400).json({ error: 'Item name is required.' });
    }

    const db = getDb();

    // Check for duplicate SKU if provided
    if (sku) {
      const existing = db.prepare('SELECT id FROM inventory WHERE sku = ?').get(sku.trim());
      if (existing) {
        return res.status(409).json({ error: 'An item with this SKU already exists.' });
      }
    }

    const id = uuidv4();

    db.prepare(`
      INSERT INTO inventory (id, item_name, sku, quantity, unit_price, supplier, reorder_threshold, created_by, updated_at)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, datetime('now'))
    `).run(
      id,
      item_name.trim(),
      sku ? sku.trim() : null,
      quantity || 0,
      unit_price || 0,
      supplier || null,
      reorder_threshold !== undefined ? reorder_threshold : 10,
      req.user.id
    );

    logActivity(req.user.id, 'CREATE_INVENTORY', `Added inventory item: ${item_name}`);

    const item = db.prepare(`
      SELECT i.*, u.name as created_by_name 
      FROM inventory i
      LEFT JOIN users u ON i.created_by = u.id
      WHERE i.id = ?
    `).get(id);

    item.reorder_alert = item.quantity <= item.reorder_threshold;

    res.status(201).json({ message: 'Inventory item created successfully.', item });
  } catch (err) {
    console.error('Create inventory item error:', err);
    res.status(500).json({ error: 'Internal server error.' });
  }
});

/**
 * PUT /api/inventory/:id
 * Update an inventory item. Managers and owners only.
 */
router.put('/:id', requireRole(75), (req, res) => {
  try {
    const db = getDb();
    const item = db.prepare('SELECT * FROM inventory WHERE id = ?').get(req.params.id);

    if (!item) {
      return res.status(404).json({ error: 'Inventory item not found.' });
    }

    const { item_name, sku, quantity, unit_price, supplier, reorder_threshold } = req.body;

    const updates = [];
    const params = [];

    if (item_name !== undefined) {
      updates.push('item_name = ?');
      params.push(item_name.trim());
    }

    if (sku !== undefined) {
      if (sku) {
        const existing = db.prepare('SELECT id FROM inventory WHERE sku = ? AND id != ?').get(sku.trim(), req.params.id);
        if (existing) {
          return res.status(409).json({ error: 'An item with this SKU already exists.' });
        }
      }
      updates.push('sku = ?');
      params.push(sku ? sku.trim() : null);
    }

    if (quantity !== undefined) {
      updates.push('quantity = ?');
      params.push(quantity);
    }

    if (unit_price !== undefined) {
      updates.push('unit_price = ?');
      params.push(unit_price);
    }

    if (supplier !== undefined) {
      updates.push('supplier = ?');
      params.push(supplier || null);
    }

    if (reorder_threshold !== undefined) {
      updates.push('reorder_threshold = ?');
      params.push(reorder_threshold);
    }

    if (updates.length === 0) {
      return res.status(400).json({ error: 'No fields to update.' });
    }

    updates.push("updated_at = datetime('now')");
    params.push(req.params.id);
    db.prepare(`UPDATE inventory SET ${updates.join(', ')} WHERE id = ?`).run(...params);

    logActivity(req.user.id, 'UPDATE_INVENTORY', `Updated inventory item: ${item.item_name}`);

    const updatedItem = db.prepare(`
      SELECT i.*, u.name as created_by_name 
      FROM inventory i
      LEFT JOIN users u ON i.created_by = u.id
      WHERE i.id = ?
    `).get(req.params.id);

    updatedItem.reorder_alert = updatedItem.quantity <= updatedItem.reorder_threshold;

    // Check for low stock and create notification
    if (updatedItem.quantity <= updatedItem.reorder_threshold) {
      // Notify all owners and managers
      const managers = db.prepare("SELECT id FROM users WHERE power_level >= 75 AND status = 'active'").all();
      for (const manager of managers) {
        db.prepare(`
          INSERT INTO notifications (id, user_id, message, type, read, created_at)
          VALUES (?, ?, ?, 'inventory', 0, datetime('now'))
        `).run(
          uuidv4(),
          manager.id,
          `Low stock alert: "${updatedItem.item_name}" has ${updatedItem.quantity} units (threshold: ${updatedItem.reorder_threshold})`
        );
      }
    }

    res.json({ message: 'Inventory item updated successfully.', item: updatedItem });
  } catch (err) {
    console.error('Update inventory item error:', err);
    res.status(500).json({ error: 'Internal server error.' });
  }
});

/**
 * DELETE /api/inventory/:id
 * Delete an inventory item. Managers and owners only.
 */
router.delete('/:id', requireRole(75), (req, res) => {
  try {
    const db = getDb();
    const item = db.prepare('SELECT * FROM inventory WHERE id = ?').get(req.params.id);

    if (!item) {
      return res.status(404).json({ error: 'Inventory item not found.' });
    }

    db.prepare('DELETE FROM inventory WHERE id = ?').run(req.params.id);

    logActivity(req.user.id, 'DELETE_INVENTORY', `Deleted inventory item: ${item.item_name}`);

    res.json({ message: 'Inventory item deleted successfully.' });
  } catch (err) {
    console.error('Delete inventory item error:', err);
    res.status(500).json({ error: 'Internal server error.' });
  }
});

module.exports = router;
