const initSqlJs = require('sql.js');
const bcrypt = require('bcryptjs');
const { v4: uuidv4 } = require('uuid');
const path = require('path');
const fs = require('fs');

const DB_PATH = path.join(__dirname, 'grovix.db');

let rawDb = null;
let wrappedDb = null;

function persist() {
  if (rawDb) {
    try {
      const data = rawDb.export();
      fs.writeFileSync(DB_PATH, Buffer.from(data));
    } catch (err) {
      console.error('Failed to persist SQLite database:', err.message);
    }
  }
}

async function initDb() {
  if (wrappedDb) return wrappedDb;

  const SQL = await initSqlJs();
  let buffer = null;
  if (fs.existsSync(DB_PATH)) {
    try {
      buffer = fs.readFileSync(DB_PATH);
    } catch (e) {
      console.warn('Could not read existing DB, creating fresh.');
    }
  }

  rawDb = buffer ? new SQL.Database(buffer) : new SQL.Database();

  wrappedDb = {
    prepare(sql) {
      return {
        get(...params) {
          const stmt = rawDb.prepare(sql);
          const flatParams = params.length === 1 && Array.isArray(params[0]) ? params[0] : params;
          if (flatParams.length > 0) stmt.bind(flatParams);
          let res = undefined;
          if (stmt.step()) {
            res = stmt.getAsObject();
          }
          stmt.free();
          return res;
        },
        all(...params) {
          const stmt = rawDb.prepare(sql);
          const flatParams = params.length === 1 && Array.isArray(params[0]) ? params[0] : params;
          if (flatParams.length > 0) stmt.bind(flatParams);
          const rows = [];
          while (stmt.step()) {
            rows.push(stmt.getAsObject());
          }
          stmt.free();
          return rows;
        },
        run(...params) {
          const flatParams = params.length === 1 && Array.isArray(params[0]) ? params[0] : params;
          rawDb.run(sql, flatParams);
          persist();
          return { changes: 1 };
        }
      };
    },
    exec(sql) {
      rawDb.run(sql);
      persist();
    },
    pragma(str) {
      // no-op for wasm sqlite
    },
    transaction(fn) {
      return (...args) => {
        try {
          rawDb.run('BEGIN TRANSACTION;');
          const result = fn(...args);
          rawDb.run('COMMIT;');
          persist();
          return result;
        } catch (err) {
          rawDb.run('ROLLBACK;');
          throw err;
        }
      };
    }
  };

  initializeTables();
  return wrappedDb;
}

function getDb() {
  if (!wrappedDb) {
    throw new Error('Database not initialized. Please call initDb() first.');
  }
  return wrappedDb;
}

function initializeTables() {
  // Users table
  wrappedDb.exec(`
    CREATE TABLE IF NOT EXISTS users (
      id TEXT PRIMARY KEY,
      email TEXT UNIQUE NOT NULL,
      password_hash TEXT NOT NULL,
      name TEXT NOT NULL,
      role TEXT NOT NULL,
      power_level INTEGER NOT NULL,
      status TEXT NOT NULL DEFAULT 'active',
      phone TEXT,
      created_at TEXT NOT NULL DEFAULT (datetime('now')),
      created_by TEXT
    );
  `);

  // Tasks table
  wrappedDb.exec(`
    CREATE TABLE IF NOT EXISTS tasks (
      id TEXT PRIMARY KEY,
      title TEXT NOT NULL,
      description TEXT,
      detailed_notes TEXT,
      assigned_to TEXT,
      assigned_by TEXT NOT NULL,
      status TEXT NOT NULL DEFAULT 'todo',
      priority TEXT NOT NULL DEFAULT 'medium',
      due_date TEXT,
      created_at TEXT NOT NULL DEFAULT (datetime('now')),
      updated_at TEXT NOT NULL DEFAULT (datetime('now'))
    );
  `);

  // Auto-migration: ensure detailed_notes column exists if tasks table already existed
  try {
    wrappedDb.exec(`ALTER TABLE tasks ADD COLUMN detailed_notes TEXT;`);
  } catch (e) {
    // Column already exists, ignore
  }

  // Sales table
  wrappedDb.exec(`
    CREATE TABLE IF NOT EXISTS sales (
      id TEXT PRIMARY KEY,
      product_service TEXT NOT NULL,
      quantity INTEGER NOT NULL DEFAULT 1,
      price REAL NOT NULL,
      total REAL NOT NULL,
      client TEXT,
      date TEXT NOT NULL,
      notes TEXT,
      created_by TEXT NOT NULL,
      created_at TEXT NOT NULL DEFAULT (datetime('now'))
    );
  `);

  // Expenses table
  wrappedDb.exec(`
    CREATE TABLE IF NOT EXISTS expenses (
      id TEXT PRIMARY KEY,
      category TEXT NOT NULL,
      amount REAL NOT NULL,
      date TEXT NOT NULL,
      notes TEXT,
      receipt_path TEXT,
      created_by TEXT NOT NULL,
      created_at TEXT NOT NULL DEFAULT (datetime('now'))
    );
  `);

  // Inventory table
  wrappedDb.exec(`
    CREATE TABLE IF NOT EXISTS inventory (
      id TEXT PRIMARY KEY,
      item_name TEXT NOT NULL,
      sku TEXT UNIQUE,
      quantity INTEGER NOT NULL DEFAULT 0,
      unit_price REAL NOT NULL DEFAULT 0,
      supplier TEXT,
      reorder_threshold INTEGER NOT NULL DEFAULT 10,
      created_by TEXT NOT NULL,
      updated_at TEXT NOT NULL DEFAULT (datetime('now'))
    );
  `);

  // Activity Log table
  wrappedDb.exec(`
    CREATE TABLE IF NOT EXISTS activity_log (
      id TEXT PRIMARY KEY,
      user_id TEXT NOT NULL,
      action TEXT NOT NULL,
      details TEXT,
      timestamp TEXT NOT NULL DEFAULT (datetime('now'))
    );
  `);

  // Notifications table
  wrappedDb.exec(`
    CREATE TABLE IF NOT EXISTS notifications (
      id TEXT PRIMARY KEY,
      user_id TEXT NOT NULL,
      message TEXT NOT NULL,
      type TEXT NOT NULL DEFAULT 'info',
      read INTEGER NOT NULL DEFAULT 0,
      created_at TEXT NOT NULL DEFAULT (datetime('now'))
    );
  `);

  seedDefaults();
}

function seedDefaults() {
  const usersToSeed = [
    { email: 'shivesh@grovix.com', password: 'Grovix@2024', name: 'Shivesh Garg', role: 'owner', power_level: 100, phone: '+91-98100-11111' },
    { email: 'sushant@grovix.com', password: 'Grovix@2024', name: 'Sushant Singh', role: 'owner', power_level: 100, phone: '+91-98100-22222' },
    { email: 'yuvik@grovix.com', password: 'Grovix@2024', name: 'Yuvik', role: 'owner', power_level: 100, phone: '+91-98100-33333' },
    { email: 'director1@grovix.com', password: 'Grovix@2024', name: 'Shivesh Garg', role: 'owner', power_level: 100, phone: '+91-98100-11111' },
    { email: 'director2@grovix.com', password: 'Grovix@2024', name: 'Sushant Singh', role: 'owner', power_level: 100, phone: '+91-98100-22222' },
    { email: 'director3@grovix.com', password: 'Grovix@2024', name: 'Yuvik', role: 'owner', power_level: 100, phone: '+91-98100-33333' },
    { email: 'manager@grovix.com', password: 'Grovix@2024', name: 'Operations Manager', role: 'manager', power_level: 75, phone: '+91-98100-44444' },
    { email: 'developer@grovix.com', password: 'Grovix@2024', name: 'Full-Stack Developer', role: 'employee', power_level: 50, phone: '+91-98100-55555' },
    { email: 'employee@grovix.com', password: 'Grovix@2024', name: 'Staff Associate', role: 'employee', power_level: 50, phone: '+91-98100-66666' },
    { email: 'guest@grovix.com', password: 'Grovix@2024', name: 'Guest Auditor', role: 'guest', power_level: 25 }
  ];

  const checkStmt = wrappedDb.prepare('SELECT id FROM users WHERE email = ?');
  const insertStmt = wrappedDb.prepare(`
    INSERT INTO users (id, email, password_hash, name, role, power_level, status, phone, created_at, created_by)
    VALUES (?, ?, ?, ?, ?, ?, 'active', ?, datetime('now'), 'system')
  `);

  for (const u of usersToSeed) {
    const existing = checkStmt.get(u.email);
    if (!existing) {
      const id = uuidv4();
      const hash = bcrypt.hashSync(u.password, 10);
      insertStmt.run(id, u.email, hash, u.name, u.role, u.power_level, u.phone || null);
      console.log(`[SEED] Created default account: ${u.email} (${u.role})`);
    }
  }

  // Seed sample initial data if tasks empty
  const taskCount = wrappedDb.prepare('SELECT COUNT(*) as count FROM tasks').get().count;
  if (taskCount === 0) {
    const director = checkStmt.get('director1@grovix.com');
    const manager = checkStmt.get('manager@grovix.com');
    const employee = checkStmt.get('employee@grovix.com');

    if (director && manager) {
      const insertTask = wrappedDb.prepare(`
        INSERT INTO tasks (id, title, description, assigned_to, assigned_by, status, priority, due_date)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
      `);

      insertTask.run(uuidv4(), 'System Architecture & Deployment', 'Finalize architecture and setup production build for Grovix app', manager.id, director.id, 'in-progress', 'urgent', '2026-11-15');
      insertTask.run(uuidv4(), 'Client Onboarding Pipeline', 'Set up automated onboarding flows for new enterprise clients', manager.id, director.id, 'todo', 'high', '2026-11-20');
      if (employee) {
        insertTask.run(uuidv4(), 'Inventory Audit Q4', 'Verify stock levels and reorder thresholds across main warehouse', employee.id, manager.id, 'todo', 'medium', '2026-11-18');
        insertTask.run(uuidv4(), 'Review Monthly Invoices', 'Audit recent transactions and generate reconciliation report', employee.id, manager.id, 'completed', 'low', '2026-11-05');
      }

      // Seed sample sales
      const insertSale = wrappedDb.prepare(`
        INSERT INTO sales (id, product_service, quantity, price, total, client, date, notes, created_by)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
      `);
      insertSale.run(uuidv4(), 'Enterprise Cloud License', 2, 4500, 9000, 'Apex Solutions Ltd', '2026-11-06', 'Annual subscription license', manager.id);
      insertSale.run(uuidv4(), 'Custom Integration Service', 1, 3200, 3200, 'Nordic Logistics', '2026-11-05', 'Payment gateway & ERP link', manager.id);
      insertSale.run(uuidv4(), 'Hardware Bundle Pro', 5, 850, 4250, 'Metro Systems', '2026-11-04', 'Terminal hardware units', manager.id);

      // Seed sample expenses
      const insertExpense = wrappedDb.prepare(`
        INSERT INTO expenses (id, category, amount, date, notes, created_by)
        VALUES (?, ?, ?, ?, ?, ?)
      `);
      insertExpense.run(uuidv4(), 'Cloud & Hosting', 850, '2026-11-05', 'AWS & Cloud cluster hosting', manager.id);
      insertExpense.run(uuidv4(), 'Office & Utilities', 1200, '2026-11-01', 'Monthly workspace rent and utilities', director.id);
      insertExpense.run(uuidv4(), 'Marketing & Advertising', 450, '2026-11-03', 'Google & LinkedIn campaigns', manager.id);

      // Seed sample inventory
      const insertInventory = wrappedDb.prepare(`
        INSERT INTO inventory (id, item_name, sku, quantity, unit_price, supplier, reorder_threshold, created_by)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
      `);
      insertInventory.run(uuidv4(), 'POS Smart Terminal V3', 'SKU-TRM-001', 45, 299.99, 'TechEquip Supply Co', 15, manager.id);
      insertInventory.run(uuidv4(), 'Thermal Receipt Paper (Box of 50)', 'SKU-PPR-010', 8, 45.00, 'PaperWorks Ltd', 12, manager.id);
      insertInventory.run(uuidv4(), 'Barcode Handheld Scanner 2D', 'SKU-SCN-042', 24, 89.50, 'Global Electronics', 10, manager.id);
      insertInventory.run(uuidv4(), 'NFC Access Cards (Pack of 100)', 'SKU-CRD-100', 5, 120.00, 'SecureTech Corp', 10, manager.id);
    }
  }
}

function logActivity(userId, action, details) {
  try {
    const currentDb = getDb();
    const stmt = currentDb.prepare(`
      INSERT INTO activity_log (id, user_id, action, details, timestamp)
      VALUES (?, ?, ?, ?, datetime('now'))
    `);
    stmt.run(uuidv4(), userId, action, details || null);
  } catch (e) {
    console.error('Failed to log activity:', e.message);
  }
}

module.exports = { initDb, getDb, logActivity };
