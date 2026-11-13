import React, { useState, useEffect } from 'react';
import API from '../api/axios';
import StatCard from '../components/StatCard';
import { Bar } from 'react-chartjs-2';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  BarElement,
  Title,
  Tooltip,
  Legend,
} from 'chart.js';
import { FiDollarSign, FiTrendingDown, FiTrendingUp, FiPercent, FiPrinter } from 'react-icons/fi';
import { toast } from 'react-toastify';
import './Reports.css';

ChartJS.register(CategoryScale, LinearScale, BarElement, Title, Tooltip, Legend);

const Reports = () => {
  const now = new Date();
  const defaultFrom = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-01`;
  const defaultTo = now.toISOString().split('T')[0];

  const [dateFrom, setDateFrom] = useState(defaultFrom);
  const [dateTo, setDateTo] = useState(defaultTo);
  const [report, setReport] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchReport();
  }, []);

  const fetchReport = async () => {
    setLoading(true);
    try {
      const res = await API.get('/reports/summary', {
        params: { date_from: dateFrom, date_to: dateTo },
      });
      setReport(res.data);
    } catch (err) {
      toast.error('Failed to generate report');
    } finally {
      setLoading(false);
    }
  };

  const setRangeQuick = (type) => {
    const today = new Date();
    let f = defaultFrom;
    let t = defaultTo;

    if (type === 'month') {
      f = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-01`;
      t = today.toISOString().split('T')[0];
    } else if (type === 'year') {
      f = `${today.getFullYear()}-01-01`;
      t = today.toISOString().split('T')[0];
    } else if (type === 'all') {
      f = '2020-01-01';
      t = today.toISOString().split('T')[0];
    }

    setDateFrom(f);
    setDateTo(t);
  };

  const handlePrint = () => {
    window.print();
  };

  const revenue = report?.summary?.total_sales ?? report?.sales?.total ?? 0;
  const expenses = report?.summary?.total_expenses ?? report?.expenses?.total ?? 0;
  const netProfit = report?.summary?.net_profit ?? (revenue - expenses);
  const margin = revenue > 0 ? ((netProfit / revenue) * 100).toFixed(1) : 0;

  const chartData = {
    labels: ['Financial Overview'],
    datasets: [
      {
        label: 'Total Revenue (₹)',
        data: [revenue],
        backgroundColor: '#1a237e',
        borderRadius: 4,
      },
      {
        label: 'Total Expenses (₹)',
        data: [expenses],
        backgroundColor: '#f44336',
        borderRadius: 4,
      },
      {
        label: 'Net Profit (₹)',
        data: [netProfit],
        backgroundColor: '#4caf50',
        borderRadius: 4,
      },
    ],
  };

  const chartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { position: 'top' },
    },
    scales: {
      y: { beginAtZero: true },
    },
  };

  return (
    <div className="reports-page">
      <div className="page-header">
        <div>
          <h1>Financial Reports & Analytics</h1>
          <p className="text-light" style={{ fontSize: '0.9rem', marginTop: '4px' }}>
            Comprehensive P&amp;L summary, expense breakdown, and business performance metrics
          </p>
        </div>
        <button className="btn btn-outline" onClick={handlePrint}>
          <FiPrinter size={16} /> Print / Export
        </button>
      </div>

      <div className="reports-filter-bar">
        <div className="date-inputs">
          <label style={{ fontSize: '0.85rem', fontWeight: 600, color: '#424242' }}>From:</label>
          <input
            type="date"
            value={dateFrom}
            onChange={(e) => setDateFrom(e.target.value)}
          />
          <label style={{ fontSize: '0.85rem', fontWeight: 600, color: '#424242' }}>To:</label>
          <input
            type="date"
            value={dateTo}
            onChange={(e) => setDateTo(e.target.value)}
          />
          <button className="btn btn-primary btn-sm" onClick={fetchReport}>
            Filter
          </button>
        </div>

        <div style={{ display: 'flex', gap: '8px' }}>
          <button className="btn btn-sm btn-outline" onClick={() => setRangeQuick('month')}>
            This Month
          </button>
          <button className="btn btn-sm btn-outline" onClick={() => setRangeQuick('year')}>
            YTD
          </button>
          <button className="btn btn-sm btn-outline" onClick={() => setRangeQuick('all')}>
            All Time
          </button>
        </div>
      </div>

      {loading ? (
        <div className="loading-container">
          <div className="spinner"></div>
        </div>
      ) : (
        <>
          <div className="sales-summary-grid">
            <StatCard
              icon={<FiDollarSign size={22} />}
              label="Period Revenue"
              value={`₹${Number(revenue).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`}
              color="#1a237e"
            />
            <StatCard
              icon={<FiTrendingDown size={22} />}
              label="Period Expenses"
              value={`₹${Number(expenses).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`}
              color="#f44336"
            />
            <StatCard
              icon={<FiTrendingUp size={22} />}
              label="Net Operating Profit"
              value={`₹${Number(netProfit).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`}
              color={netProfit >= 0 ? '#4caf50' : '#d32f2f'}
            />
            <StatCard
              icon={<FiPercent size={22} />}
              label="Profit Margin"
              value={`${margin}%`}
              color="#ff9800"
            />
          </div>

          <div className="chart-card" style={{ marginBottom: '24px', height: '320px' }}>
            <h3>Revenue vs. Expenses vs. Profit</h3>
            <div style={{ height: '240px', position: 'relative' }}>
              <Bar data={chartData} options={chartOptions} />
            </div>
          </div>

          <div className="reports-grid">
            <div className="table-container">
              <div style={{ padding: '16px 20px', borderBottom: '1px solid #e0e0e0', fontWeight: 600 }}>
                Revenue by Product / Service
              </div>
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Product / Service</th>
                    <th>Units</th>
                    <th>Total Revenue</th>
                  </tr>
                </thead>
                <tbody>
                  {(report?.salesByProduct || []).length === 0 ? (
                    <tr>
                      <td colSpan={3} style={{ textAlign: 'center', color: '#757575' }}>
                        No sales in this date range
                      </td>
                    </tr>
                  ) : (
                    (report?.salesByProduct || []).map((p, idx) => (
                      <tr key={idx}>
                        <td style={{ fontWeight: 600 }}>{p.product_service}</td>
                        <td>{p.total_quantity}</td>
                        <td className="amount-col">₹{Number(p.total_revenue).toFixed(2)}</td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>

            <div className="table-container">
              <div style={{ padding: '16px 20px', borderBottom: '1px solid #e0e0e0', fontWeight: 600 }}>
                Expenses by Category
              </div>
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Category</th>
                    <th>Records</th>
                    <th>Total Spent</th>
                  </tr>
                </thead>
                <tbody>
                  {(report?.expensesByCategory || []).length === 0 ? (
                    <tr>
                      <td colSpan={3} style={{ textAlign: 'center', color: '#757575' }}>
                        No expenses in this date range
                      </td>
                    </tr>
                  ) : (
                    (report?.expensesByCategory || []).map((c, idx) => (
                      <tr key={idx}>
                        <td>{c.category}</td>
                        <td>{c.transaction_count}</td>
                        <td className="expense-amount-col">₹{Number(c.total_amount).toFixed(2)}</td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </>
      )}
    </div>
  );
};

export default Reports;

