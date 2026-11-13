import React, { useState, useEffect } from 'react';
import API from '../api/axios';
import { useAuth } from '../context/AuthContext';
import Modal from '../components/Modal';
import StatCard from '../components/StatCard';
import { FiDollarSign, FiPlus, FiShoppingBag, FiCalendar, FiUser } from 'react-icons/fi';
import { toast } from 'react-toastify';
import './Sales.css';

const Sales = () => {
  const { user } = useAuth();
  const [sales, setSales] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showAddModal, setShowAddModal] = useState(false);
  const [form, setForm] = useState({
    product_service: '',
    quantity: 1,
    price: '',
    client: '',
    date: new Date().toISOString().split('T')[0],
    notes: '',
  });

  const powerLevel = user?.power_level ?? user?.powerLevel ?? 0;

  useEffect(() => {
    fetchSales();
  }, []);

  const fetchSales = async () => {
    try {
      const res = await API.get('/sales');
      setSales(res.data.sales || res.data || []);
    } catch (err) {
      toast.error('Failed to load sales');
    } finally {
      setLoading(false);
    }
  };

  const handleFormChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleAddSale = async (e) => {
    e.preventDefault();
    try {
      await API.post('/sales', {
        product_service: form.product_service,
        quantity: parseInt(form.quantity, 10) || 1,
        price: parseFloat(form.price) || 0,
        client: form.client,
        date: form.date,
        notes: form.notes,
      });
      toast.success('Sale recorded successfully!');
      setShowAddModal(false);
      setForm({
        product_service: '',
        quantity: 1,
        price: '',
        client: '',
        date: new Date().toISOString().split('T')[0],
        notes: '',
      });
      fetchSales();
    } catch (err) {
      toast.error(err.response?.data?.error || 'Failed to record sale');
    }
  };

  const totalRevenue = sales.reduce((sum, item) => sum + (Number(item.total) || 0), 0);
  const averageSale = sales.length > 0 ? (totalRevenue / sales.length).toFixed(2) : 0;

  if (loading) {
    return (
      <div className="loading-container">
        <div className="spinner"></div>
      </div>
    );
  }

  return (
    <div className="sales-page">
      <div className="page-header">
        <div>
          <h1>Sales Management</h1>
          <p className="text-light" style={{ fontSize: '0.9rem', marginTop: '4px' }}>
            Track client transactions, revenues, and services delivered
          </p>
        </div>
        {powerLevel >= 50 && (
          <button className="btn btn-primary" onClick={() => setShowAddModal(true)}>
            <FiPlus size={16} /> Record New Sale
          </button>
        )}
      </div>

      <div className="sales-summary-grid">
        <StatCard
          icon={<FiDollarSign size={22} />}
          label="Total Revenue"
          value={`₹${totalRevenue.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`}
          color="#1a237e"
        />
        <StatCard
          icon={<FiShoppingBag size={22} />}
          label="Transactions"
          value={sales.length}
          color="#4caf50"
        />
        <StatCard
          icon={<FiCalendar size={22} />}
          label="Average Ticket"
          value={`₹${Number(averageSale).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`}
          color="#ff9800"
        />
      </div>

      <div className="table-container">
        {sales.length === 0 ? (
          <div className="empty-state">
            <p>No sales records found. Click "Record New Sale" to get started.</p>
          </div>
        ) : (
          <table className="data-table">
            <thead>
              <tr>
                <th>Date</th>
                <th>Product / Service</th>
                <th>Client</th>
                <th>Qty</th>
                <th>Unit Price</th>
                <th>Total</th>
                <th>Recorded By</th>
              </tr>
            </thead>
            <tbody>
              {sales.map((sale) => (
                <tr key={sale.id}>
                  <td>{sale.date}</td>
                  <td style={{ fontWeight: 600 }}>{sale.product_service}</td>
                  <td>{sale.client || '—'}</td>
                  <td>{sale.quantity}</td>
                  <td>₹{Number(sale.price).toFixed(2)}</td>
                  <td className="amount-col">₹{Number(sale.total).toFixed(2)}</td>
                  <td>
                    <span style={{ fontSize: '0.85rem', color: '#616161' }}>
                      <FiUser size={12} style={{ marginRight: '4px', verticalAlign: '-1px' }} />
                      {sale.created_by_name || 'Staff'}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* Add Sale Modal */}
      <Modal isOpen={showAddModal} onClose={() => setShowAddModal(false)} title="Record New Sale">
        <form onSubmit={handleAddSale}>
          <div className="form-group">
            <label>Product or Service Name *</label>
            <input
              name="product_service"
              value={form.product_service}
              onChange={handleFormChange}
              placeholder="e.g. Enterprise License / Maintenance"
              required
            />
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
            <div className="form-group">
              <label>Quantity *</label>
              <input
                type="number"
                min="1"
                name="quantity"
                value={form.quantity}
                onChange={handleFormChange}
                required
              />
            </div>
            <div className="form-group">
              <label>Unit Price (₹) *</label>
              <input
                type="number"
                step="0.01"
                min="0"
                name="price"
                value={form.price}
                onChange={handleFormChange}
                placeholder="0.00"
                required
              />
            </div>
          </div>

          <div className="form-group">
            <label>Client / Customer Name</label>
            <input
              name="client"
              value={form.client}
              onChange={handleFormChange}
              placeholder="e.g. Acme Corp"
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
            <label>Notes / Memo</label>
            <textarea
              name="notes"
              value={form.notes}
              onChange={handleFormChange}
              placeholder="Optional notes or invoice ref..."
              rows={3}
            />
          </div>

          <button type="submit" className="btn btn-primary" style={{ width: '100%', justifyContent: 'center' }}>
            Save Sale Record
          </button>
        </form>
      </Modal>
    </div>
  );
};

export default Sales;

