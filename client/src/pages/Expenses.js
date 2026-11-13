import React, { useState, useEffect } from 'react';
import API from '../api/axios';
import { useAuth } from '../context/AuthContext';
import Modal from '../components/Modal';
import StatCard from '../components/StatCard';
import { Doughnut } from 'react-chartjs-2';
import {
  Chart as ChartJS,
  ArcElement,
  Tooltip as ChartTooltip,
  Legend as ChartLegend,
} from 'chart.js';
import { FiTrendingDown, FiPlus, FiTag, FiDollarSign } from 'react-icons/fi';
import { toast } from 'react-toastify';
import './Expenses.css';

ChartJS.register(ArcElement, ChartTooltip, ChartLegend);

const CATEGORIES = [
  'Office & Utilities',
  'Cloud & Hosting',
  'Hardware & Equipment',
  'Software & SaaS',
  'Marketing & Advertising',
  'Travel & Meals',
  'Legal & Consulting',
  'Supplies',
  'Other',
];

const CATEGORY_COLORS = [
  '#1a237e',
  '#0288d1',
  '#00897b',
  '#7b1fa2',
  '#f57c00',
  '#d32f2f',
  '#5d4037',
  '#689f38',
  '#616161',
];

const Expenses = () => {
  const { user } = useAuth();
  const [expenses, setExpenses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showAddModal, setShowAddModal] = useState(false);
  const [form, setForm] = useState({
    category: 'Office & Utilities',
    amount: '',
    date: new Date().toISOString().split('T')[0],
    notes: '',
  });

  const powerLevel = user?.power_level ?? user?.powerLevel ?? 0;

  useEffect(() => {
    fetchExpenses();
  }, []);

  const fetchExpenses = async () => {
    try {
      const res = await API.get('/expenses');
      setExpenses(res.data.expenses || res.data || []);
    } catch (err) {
      toast.error('Failed to load expenses');
    } finally {
      setLoading(false);
    }
  };

  const handleFormChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleAddExpense = async (e) => {
    e.preventDefault();
    try {
      await API.post('/expenses', {
        category: form.category,
        amount: parseFloat(form.amount) || 0,
        date: form.date,
        notes: form.notes,
      });
      toast.success('Expense recorded successfully!');
      setShowAddModal(false);
      setForm({
        category: 'Office & Utilities',
        amount: '',
        date: new Date().toISOString().split('T')[0],
        notes: '',
      });
      fetchExpenses();
    } catch (err) {
      toast.error(err.response?.data?.error || 'Failed to record expense');
    }
  };

  const totalExpenses = expenses.reduce((sum, item) => sum + (Number(item.amount) || 0), 0);

  // Calculate category aggregates for chart
  const categoryMap = {};
  expenses.forEach((e) => {
    const cat = e.category || 'Other';
    categoryMap[cat] = (categoryMap[cat] || 0) + (Number(e.amount) || 0);
  });

  const chartLabels = Object.keys(categoryMap);
  const chartValues = Object.values(categoryMap);

  const chartData = {
    labels: chartLabels.length > 0 ? chartLabels : ['None'],
    datasets: [
      {
        data: chartValues.length > 0 ? chartValues : [1],
        backgroundColor: CATEGORY_COLORS.slice(0, Math.max(chartLabels.length, 1)),
        borderWidth: 1,
      },
    ],
  };

  if (loading) {
    return (
      <div className="loading-container">
        <div className="spinner"></div>
      </div>
    );
  }

  return (
    <div className="expenses-page">
      <div className="page-header">
        <div>
          <h1>Expense Tracking</h1>
          <p className="text-light" style={{ fontSize: '0.9rem', marginTop: '4px' }}>
            Monitor corporate costs, vendor disbursements, and departmental spending
          </p>
        </div>
        {powerLevel >= 50 && (
          <button className="btn btn-primary" onClick={() => setShowAddModal(true)}>
            <FiPlus size={16} /> Record Expense
          </button>
        )}
      </div>

      <div className="sales-summary-grid">
        <StatCard
          icon={<FiTrendingDown size={22} />}
          label="Total Expenses"
          value={`₹${totalExpenses.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`}
          color="#f44336"
        />
        <StatCard
          icon={<FiTag size={22} />}
          label="Expense Records"
          value={expenses.length}
          color="#1a237e"
        />
        <StatCard
          icon={<FiDollarSign size={22} />}
          label="Average Expense"
          value={`₹${(expenses.length > 0 ? totalExpenses / expenses.length : 0).toFixed(2)}`}
          color="#ff9800"
        />
      </div>

      <div className="expenses-overview-grid">
        <div className="table-container">
          {expenses.length === 0 ? (
            <div className="empty-state">
              <p>No expenses recorded yet. Click "Record Expense" to add one.</p>
            </div>
          ) : (
            <table className="data-table">
              <thead>
                <tr>
                  <th>Date</th>
                  <th>Category</th>
                  <th>Amount</th>
                  <th>Notes</th>
                  <th>Reported By</th>
                </tr>
              </thead>
              <tbody>
                {expenses.map((item) => (
                  <tr key={item.id}>
                    <td>{item.date}</td>
                    <td>
                      <span className="category-tag">{item.category}</span>
                    </td>
                    <td className="expense-amount-col">₹{Number(item.amount).toFixed(2)}</td>
                    <td>{item.notes || '—'}</td>
                    <td style={{ fontSize: '0.85rem', color: '#616161' }}>
                      {item.created_by_name || 'Staff'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        <div className="chart-card">
          <h3>Expense Distribution</h3>
          {chartValues.length > 0 ? (
            <div style={{ height: '260px', position: 'relative' }}>
              <Doughnut
                data={chartData}
                options={{
                  responsive: true,
                  maintainAspectRatio: false,
                  plugins: {
                    legend: { position: 'bottom', labels: { boxWidth: 12, font: { size: 11 } } },
                  },
                }}
              />
            </div>
          ) : (
            <div className="empty-state">
              <p>No chart data available</p>
            </div>
          )}
        </div>
      </div>

      {/* Add Expense Modal */}
      <Modal isOpen={showAddModal} onClose={() => setShowAddModal(false)} title="Record New Expense">
        <form onSubmit={handleAddExpense}>
          <div className="form-group">
            <label>Category *</label>
            <select name="category" value={form.category} onChange={handleFormChange} required>
              {CATEGORIES.map((cat) => (
                <option key={cat} value={cat}>
                  {cat}
                </option>
              ))}
            </select>
          </div>

          <div className="form-group">
            <label>Amount (₹) *</label>
            <input
              type="number"
              step="0.01"
              min="0"
              name="amount"
              value={form.amount}
              onChange={handleFormChange}
              placeholder="0.00"
              required
            />
          </div>

          <div className="form-group">
            <label>Date *</label>
            <input
              type="date"
              name="date"
              value={form.date}
              onChange={handleFormChange}
              required
            />
          </div>

          <div className="form-group">
            <label>Description / Notes</label>
            <textarea
              name="notes"
              value={form.notes}
              onChange={handleFormChange}
              placeholder="Details or justification..."
              rows={3}
            />
          </div>

          <button type="submit" className="btn btn-primary" style={{ width: '100%', justifyContent: 'center' }}>
            Save Expense Record
          </button>
        </form>
      </Modal>
    </div>
  );
};

export default Expenses;

