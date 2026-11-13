const express = require('express');
const { getDb } = require('../database');
const { authenticate, requireRole } = require('../middleware/auth');

const router = express.Router();

// All dashboard routes require authentication
router.use(authenticate);

/**
 * GET /api/dashboard
 * Get aggregated dashboard stats. Data is filtered based on user role.
 * Returns: total sales today, total expenses today, profit, pending tasks, low stock count, recent activity.
 */
router.get('/', requireRole(25), (req, res) => {
  try {
    const db = getDb();
    const today = new Date().toISOString().split('T')[0];
    const isOwnerOrManager = req.user.power_level >= 75;

    let totalSalesToday, totalExpensesToday, totalSalesMonth, totalExpensesMonth;
    let pendingTasks, totalTasks;

    // Get first day of current month
    const now = new Date();
    const monthStart = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-01`;

    if (isOwnerOrManager) {
      // Owners/managers see all data
      totalSalesToday = db.prepare(
        "SELECT COALESCE(SUM(total), 0) as total FROM sales WHERE date = ?"
      ).get(today).total;

      totalExpensesToday = db.prepare(
        "SELECT COALESCE(SUM(amount), 0) as total FROM expenses WHERE date = ?"
      ).get(today).total;

      totalSalesMonth = db.prepare(
        "SELECT COALESCE(SUM(total), 0) as total FROM sales WHERE date >= ?"
      ).get(monthStart).total;

      totalExpensesMonth = db.prepare(
        "SELECT COALESCE(SUM(amount), 0) as total FROM expenses WHERE date >= ?"
      ).get(monthStart).total;

      pendingTasks = db.prepare(
        "SELECT COUNT(*) as count FROM tasks WHERE status != 'completed'"
      ).get().count;

      totalTasks = db.prepare(
        "SELECT COUNT(*) as count FROM tasks"
      ).get().count;
    } else if (req.user.power_level >= 50) {
      // Employees see only their own data
      totalSalesToday = db.prepare(
        "SELECT COALESCE(SUM(total), 0) as total FROM sales WHERE date = ? AND created_by = ?"
      ).get(today, req.user.id).total;

      totalExpensesToday = db.prepare(
        "SELECT COALESCE(SUM(amount), 0) as total FROM expenses WHERE date = ? AND created_by = ?"
      ).get(today, req.user.id).total;

      totalSalesMonth = db.prepare(
        "SELECT COALESCE(SUM(total), 0) as total FROM sales WHERE date >= ? AND created_by = ?"
      ).get(monthStart, req.user.id).total;

      totalExpensesMonth = db.prepare(
        "SELECT COALESCE(SUM(amount), 0) as total FROM expenses WHERE date >= ? AND created_by = ?"
      ).get(monthStart, req.user.id).total;

      pendingTasks = db.prepare(
        "SELECT COUNT(*) as count FROM tasks WHERE assigned_to = ? AND status != 'completed'"
      ).get(req.user.id).count;

      totalTasks = db.prepare(
        "SELECT COUNT(*) as count FROM tasks WHERE assigned_to = ?"
      ).get(req.user.id).count;
    } else {
      // Guests see aggregated read-only data
      totalSalesToday = db.prepare(
        "SELECT COALESCE(SUM(total), 0) as total FROM sales WHERE date = ?"
      ).get(today).total;

      totalExpensesToday = db.prepare(
        "SELECT COALESCE(SUM(amount), 0) as total FROM expenses WHERE date = ?"
      ).get(today).total;

      totalSalesMonth = db.prepare(
        "SELECT COALESCE(SUM(total), 0) as total FROM sales WHERE date >= ?"
      ).get(monthStart).total;

      totalExpensesMonth = db.prepare(
        "SELECT COALESCE(SUM(amount), 0) as total FROM expenses WHERE date >= ?"
      ).get(monthStart).total;

      pendingTasks = 0;
      totalTasks = 0;
    }

    // Low stock alerts count
    const lowStockCount = db.prepare(
      "SELECT COUNT(*) as count FROM inventory WHERE quantity <= reorder_threshold"
    ).get().count;

    // Recent activity (last 10 entries)
    let recentActivity;
    if (isOwnerOrManager) {
      recentActivity = db.prepare(`
        SELECT al.*, u.name as user_name
        FROM activity_log al
        LEFT JOIN users u ON al.user_id = u.id
        ORDER BY al.timestamp DESC
        LIMIT 10
      `).all();
    } else if (req.user.power_level >= 50) {
      recentActivity = db.prepare(`
        SELECT al.*, u.name as user_name
        FROM activity_log al
        LEFT JOIN users u ON al.user_id = u.id
        WHERE al.user_id = ?
        ORDER BY al.timestamp DESC
        LIMIT 10
      `).all(req.user.id);
    } else {
      recentActivity = [];
    }

    // User counts (for owners/managers)
    let userStats = null;
    if (isOwnerOrManager) {
      userStats = {
        totalUsers: db.prepare("SELECT COUNT(*) as count FROM users WHERE status = 'active'").get().count,
        owners: db.prepare("SELECT COUNT(*) as count FROM users WHERE role = 'owner' AND status = 'active'").get().count,
        managers: db.prepare("SELECT COUNT(*) as count FROM users WHERE role = 'manager' AND status = 'active'").get().count,
        employees: db.prepare("SELECT COUNT(*) as count FROM users WHERE role = 'employee' AND status = 'active'").get().count,
        guests: db.prepare("SELECT COUNT(*) as count FROM users WHERE role = 'guest' AND status = 'active'").get().count
      };
    }

    // Recent sales (last 5)
    let recentSales;
    if (isOwnerOrManager) {
      recentSales = db.prepare(`
        SELECT s.*, u.name as created_by_name
        FROM sales s
        LEFT JOIN users u ON s.created_by = u.id
        ORDER BY s.created_at DESC
        LIMIT 5
      `).all();
    } else if (req.user.power_level >= 50) {
      recentSales = db.prepare(`
        SELECT s.*, u.name as created_by_name
        FROM sales s
        LEFT JOIN users u ON s.created_by = u.id
        WHERE s.created_by = ?
        ORDER BY s.created_at DESC
        LIMIT 5
      `).all(req.user.id);
    } else {
      recentSales = [];
    }

    // Last 7 days sales trend
    const trendLabels = [];
    const trendValues = [];
    for (let i = 6; i >= 0; i--) {
      const d = new Date();
      d.setDate(d.getDate() - i);
      const dateStr = d.toISOString().split('T')[0];
      trendLabels.push(d.toLocaleDateString(undefined, { weekday: 'short', month: 'numeric', day: 'numeric' }));
      const daySales = db.prepare(
        "SELECT COALESCE(SUM(total), 0) as total FROM sales WHERE date = ?"
      ).get(dateStr).total;
      trendValues.push(daySales);
    }

    res.json({
      stats: {
        salesToday: totalSalesToday,
        expensesToday: totalExpensesToday,
        profit: totalSalesToday - totalExpensesToday,
        pendingTasks: pendingTasks,
        totalTasks: totalTasks
      },
      salesTrend: {
        labels: trendLabels,
        values: trendValues
      },
      today: {
        sales: totalSalesToday,
        expenses: totalExpensesToday,
        profit: totalSalesToday - totalExpensesToday
      },
      month: {
        sales: totalSalesMonth,
        expenses: totalExpensesMonth,
        profit: totalSalesMonth - totalExpensesMonth
      },
      tasks: {
        pending: pendingTasks,
        total: totalTasks
      },
      lowStockCount,
      recentActivity,
      recentSales,
      userStats
    });
  } catch (err) {
    console.error('Dashboard error:', err);
    res.status(500).json({ error: 'Internal server error.' });
  }
});

module.exports = router;
