import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import API from '../api/axios';
import StatCard from '../components/StatCard';
import { Line } from 'react-chartjs-2';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
  Filler,
} from 'chart.js';
import { FiDollarSign, FiTrendingDown, FiTrendingUp, FiCheckSquare, FiPlus } from 'react-icons/fi';
import { toast } from 'react-toastify';
import './Dashboard.css';

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Title, Tooltip, Legend, Filler);

const Dashboard = () => {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    fetchDashboard();
  }, []);

  const fetchDashboard = async () => {
    try {
      const res = await API.get('/dashboard');
      setData(res.data);
    } catch (err) {
      toast.error('Failed to load dashboard data');
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="loading-container">
        <div className="spinner"></div>
      </div>
    );
  }

  const stats = data?.stats || {};
  const salesTrend = data?.salesTrend || { labels: [], values: [] };
  const recentActivity = data?.recentActivity || [];

  const chartData = {
    labels: salesTrend.labels || [],
    datasets: [
      {
        label: 'Sales',
        data: salesTrend.values || [],
        borderColor: '#1a237e',
        backgroundColor: 'rgba(26, 35, 126, 0.08)',
        tension: 0.4,
        fill: true,
        pointBackgroundColor: '#1a237e',
        pointRadius: 4,
        pointHoverRadius: 6,
      },
    ],
  };

  const chartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: false },
      tooltip: {
        backgroundColor: '#1a237e',
        padding: 10,
        cornerRadius: 8,
      },
    },
    scales: {
      x: {
        grid: { display: false },
        ticks: { font: { size: 12 }, color: '#757575' },
      },
      y: {
        grid: { color: '#f0f0f0' },
        ticks: { font: { size: 12 }, color: '#757575' },
        beginAtZero: true,
      },
    },
  };

  return (
    <div className="dashboard-page">
      <div className="page-header">
        <h1>Dashboard</h1>
      </div>

      <div className="stat-grid">
        <StatCard
          icon={<FiDollarSign size={22} />}
          label="Sales Today"
          value={`₹${(stats.salesToday || 0).toLocaleString()}`}
          color="#1a237e"
        />
        <StatCard
          icon={<FiTrendingDown size={22} />}
          label="Expenses Today"
          value={`₹${(stats.expensesToday || 0).toLocaleString()}`}
          color="#f44336"
        />
        <StatCard
          icon={<FiTrendingUp size={22} />}
          label="Profit"
          value={`₹${(stats.profit || 0).toLocaleString()}`}
          color="#4caf50"
        />
        <StatCard
          icon={<FiCheckSquare size={22} />}
          label="Pending Tasks"
          value={stats.pendingTasks || 0}
          color="#ff9800"
        />
      </div>

      <div className="dashboard-grid">
        <div className="card dashboard-chart-card">
          <h3 className="card-title">Sales Trend (Last 7 Days)</h3>
          <div className="chart-container">
            <Line data={chartData} options={chartOptions} />
          </div>
        </div>

        <div className="dashboard-side">
          <div className="card dashboard-activity-card">
            <h3 className="card-title">Recent Activity</h3>
            <div className="activity-list">
              {recentActivity.length === 0 ? (
                <p className="text-light" style={{ fontSize: '0.85rem', padding: '8px 0' }}>No recent activity</p>
              ) : (
                recentActivity.map((item, idx) => (
                  <div key={idx} className="activity-item">
                    <div className="activity-dot"></div>
                    <div className="activity-content">
                      <p className="activity-text">
                        <strong>{item.action ? `${item.action}: ` : ''}</strong>
                        {item.details || item.message || item.description || 'System event'}
                        {item.user_name && <span style={{ fontSize: '0.75rem', color: '#757575', display: 'block' }}>By: {item.user_name}</span>}
                      </p>
                      <span className="activity-time">
                        {item.timestamp ? new Date(item.timestamp).toLocaleString() : ''}
                      </span>
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>

          <div className="quick-actions">
            <h3 className="card-title mb-16">Quick Actions</h3>
            <div className="quick-actions-grid">
              <button className="btn btn-primary" onClick={() => navigate('/sales')}>
                <FiPlus size={16} /> Add Sale
              </button>
              <button className="btn btn-accent" onClick={() => navigate('/tasks')}>
                <FiPlus size={16} /> Add Task
              </button>
              <button className="btn btn-outline" onClick={() => navigate('/expenses')}>
                <FiPlus size={16} /> Add Expense
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
