const express = require('express');
const { getDb } = require('../database');
const { authenticate, requireRole } = require('../middleware/auth');

const router = express.Router();

// All report routes require authentication
router.use(authenticate);

/**
 * GET /api/reports/summary
 * Financial summary with date range filters.
 * Query params: date_from, date_to (YYYY-MM-DD format)
 * Owners/managers see all data, employees see their own.
 */
router.get('/summary', requireRole(50), (req, res) => {
  try {
    const db = getDb();
    const { date_from, date_to } = req.query;

    // Default to current month if no dates specified
    const now = new Date();
    const defaultFrom = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-01`;
    const defaultTo = now.toISOString().split('T')[0];

    const from = date_from || defaultFrom;
    const to = date_to || defaultTo;

    const isOwnerOrManager = req.user.power_level >= 75;

    let salesData, expensesData, salesByProduct, expensesByCategory, dailySales, dailyExpenses;

    if (isOwnerOrManager) {
      // Total sales in range
      salesData = db.prepare(
        "SELECT COALESCE(SUM(total), 0) as total, COUNT(*) as count FROM sales WHERE date >= ? AND date <= ?"
      ).get(from, to);

      // Total expenses in range
      expensesData = db.prepare(
        "SELECT COALESCE(SUM(amount), 0) as total, COUNT(*) as count FROM expenses WHERE date >= ? AND date <= ?"
      ).get(from, to);

      // Sales grouped by product/service
      salesByProduct = db.prepare(`
        SELECT product_service, SUM(quantity) as total_quantity, SUM(total) as total_revenue, COUNT(*) as transaction_count
        FROM sales
        WHERE date >= ? AND date <= ?
        GROUP BY product_service
        ORDER BY total_revenue DESC
      `).all(from, to);

      // Expenses grouped by category
      expensesByCategory = db.prepare(`
        SELECT category, SUM(amount) as total_amount, COUNT(*) as transaction_count
        FROM expenses
        WHERE date >= ? AND date <= ?
        GROUP BY category
        ORDER BY total_amount DESC
      `).all(from, to);

      // Daily sales breakdown
      dailySales = db.prepare(`
        SELECT date, SUM(total) as total
        FROM sales
        WHERE date >= ? AND date <= ?
        GROUP BY date
        ORDER BY date ASC
      `).all(from, to);

      // Daily expenses breakdown
      dailyExpenses = db.prepare(`
        SELECT date, SUM(amount) as total
        FROM expenses
        WHERE date >= ? AND date <= ?
        GROUP BY date
        ORDER BY date ASC
      `).all(from, to);
    } else {
      // Employee view: their own data only
      salesData = db.prepare(
        "SELECT COALESCE(SUM(total), 0) as total, COUNT(*) as count FROM sales WHERE date >= ? AND date <= ? AND created_by = ?"
      ).get(from, to, req.user.id);

      expensesData = db.prepare(
        "SELECT COALESCE(SUM(amount), 0) as total, COUNT(*) as count FROM expenses WHERE date >= ? AND date <= ? AND created_by = ?"
      ).get(from, to, req.user.id);

      salesByProduct = db.prepare(`
        SELECT product_service, SUM(quantity) as total_quantity, SUM(total) as total_revenue, COUNT(*) as transaction_count
        FROM sales
        WHERE date >= ? AND date <= ? AND created_by = ?
        GROUP BY product_service
        ORDER BY total_revenue DESC
      `).all(from, to, req.user.id);

      expensesByCategory = db.prepare(`
        SELECT category, SUM(amount) as total_amount, COUNT(*) as transaction_count
        FROM expenses
        WHERE date >= ? AND date <= ? AND created_by = ?
        GROUP BY category
        ORDER BY total_amount DESC
      `).all(from, to, req.user.id);

      dailySales = db.prepare(`
        SELECT date, SUM(total) as total
        FROM sales
        WHERE date >= ? AND date <= ? AND created_by = ?
        GROUP BY date
        ORDER BY date ASC
      `).all(from, to, req.user.id);

      dailyExpenses = db.prepare(`
        SELECT date, SUM(amount) as total
        FROM expenses
        WHERE date >= ? AND date <= ? AND created_by = ?
        GROUP BY date
        ORDER BY date ASC
      `).all(from, to, req.user.id);
    }

    // Top sellers (users who made the most sales) - managers/owners only
    let topSellers = null;
    if (isOwnerOrManager) {
      topSellers = db.prepare(`
        SELECT u.name, u.email, SUM(s.total) as total_sales, COUNT(*) as transaction_count
        FROM sales s
        JOIN users u ON s.created_by = u.id
        WHERE s.date >= ? AND s.date <= ?
        GROUP BY s.created_by
        ORDER BY total_sales DESC
        LIMIT 10
      `).all(from, to);
    }

    res.json({
      period: { from, to },
      sales: {
        total: salesData.total,
        count: salesData.count,
        byProduct: salesByProduct,
        daily: dailySales
      },
      expenses: {
        total: expensesData.total,
        count: expensesData.count,
        byCategory: expensesByCategory,
        daily: dailyExpenses
      },
      profit: salesData.total - expensesData.total,
      topSellers
    });
  } catch (err) {
    console.error('Report summary error:', err);
    res.status(500).json({ error: 'Internal server error.' });
  }
});

module.exports = router;
