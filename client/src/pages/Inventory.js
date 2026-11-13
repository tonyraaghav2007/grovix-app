import React, { useState, useEffect } from 'react';
import API from '../api/axios';
import { useAuth } from '../context/AuthContext';
import Modal from '../components/Modal';
import StatCard from '../components/StatCard';
import { FiPackage, FiPlus, FiAlertTriangle, FiDollarSign, FiSearch, FiEdit2 } from 'react-icons/fi';
import { toast } from 'react-toastify';
import './Inventory.css';

const Inventory = () => {
  const { user } = useAuth();
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [showAddModal, setShowAddModal] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [editingItem, setEditingItem] = useState(null);
  const [form, setForm] = useState({
    item_name: '',
    sku: '',
    quantity: 10,
    unit_price: '',
    supplier: '',
    reorder_threshold: 10,
  });

  const powerLevel = user?.power_level ?? user?.powerLevel ?? 0;
  const canManage = powerLevel >= 75;

  useEffect(() => {
    fetchInventory();
  }, []);

  const fetchInventory = async () => {
    try {
      const res = await API.get('/inventory');
      setItems(res.data.items || res.data || []);
    } catch (err) {
      toast.error('Failed to load inventory');
    } finally {
      setLoading(false);
    }
  };

  const handleFormChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleAddItem = async (e) => {
    e.preventDefault();
    try {
      await API.post('/inventory', {
        item_name: form.item_name,
        sku: form.sku || undefined,
        quantity: parseInt(form.quantity, 10) || 0,
        unit_price: parseFloat(form.unit_price) || 0,
        supplier: form.supplier,
        reorder_threshold: parseInt(form.reorder_threshold, 10) || 5,
      });
      toast.success('Inventory item added successfully!');
      setShowAddModal(false);
      setForm({
        item_name: '',
        sku: '',
        quantity: 10,
        unit_price: '',
        supplier: '',
        reorder_threshold: 10,
      });
      fetchInventory();
    } catch (err) {
      toast.error(err.response?.data?.error || 'Failed to add item');
    }
  };

  const handleOpenEdit = (item) => {
    setEditingItem(item);
    setForm({
      item_name: item.item_name,
      sku: item.sku || '',
      quantity: item.quantity,
      unit_price: item.unit_price,
      supplier: item.supplier || '',
      reorder_threshold: item.reorder_threshold,
    });
    setShowEditModal(true);
  };

  const handleUpdateItem = async (e) => {
    e.preventDefault();
    try {
      await API.put(`/inventory/${editingItem.id}`, {
        item_name: form.item_name,
        sku: form.sku,
        quantity: parseInt(form.quantity, 10) || 0,
        unit_price: parseFloat(form.unit_price) || 0,
        supplier: form.supplier,
        reorder_threshold: parseInt(form.reorder_threshold, 10) || 5,
      });
      toast.success('Inventory item updated successfully!');
      setShowEditModal(false);
      setEditingItem(null);
      fetchInventory();
    } catch (err) {
      toast.error(err.response?.data?.error || 'Failed to update item');
    }
  };

  const filteredItems = items.filter((item) => {
    const q = search.toLowerCase();
    return (
      (item.item_name && item.item_name.toLowerCase().includes(q)) ||
      (item.sku && item.sku.toLowerCase().includes(q)) ||
      (item.supplier && item.supplier.toLowerCase().includes(q))
    );
  });

  const lowStockCount = items.filter((i) => i.quantity <= i.reorder_threshold).length;
  const totalStockValue = items.reduce(
    (sum, i) => sum + (Number(i.quantity) || 0) * (Number(i.unit_price) || 0),
    0
  );

  if (loading) {
    return (
      <div className="loading-container">
        <div className="spinner"></div>
      </div>
    );
  }

  return (
    <div className="inventory-page">
      <div className="page-header">
        <div>
          <h1>Inventory & Stock</h1>
          <p className="text-light" style={{ fontSize: '0.9rem', marginTop: '4px' }}>
            Manage warehouse goods, parts, equipment, and automated replenishment alerts
          </p>
        </div>
        {canManage && (
          <button className="btn btn-primary" onClick={() => setShowAddModal(true)}>
            <FiPlus size={16} /> Add Stock Item
          </button>
        )}
      </div>

      <div className="sales-summary-grid">
        <StatCard
          icon={<FiPackage size={22} />}
          label="Total Items SKU"
          value={items.length}
          color="#1a237e"
        />
        <StatCard
          icon={<FiAlertTriangle size={22} />}
          label="Low Stock Alerts"
          value={lowStockCount}
          color={lowStockCount > 0 ? '#d32f2f' : '#4caf50'}
        />
        <StatCard
          icon={<FiDollarSign size={22} />}
          label="Total Inventory Value"
          value={`₹${totalStockValue.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`}
          color="#ff9800"
        />
      </div>

      <div className="inventory-toolbar">
        <div style={{ position: 'relative', width: '320px' }}>
          <FiSearch
            size={16}
            style={{ position: 'absolute', left: '12px', top: '12px', color: '#9e9e9e' }}
          />
          <input
            type="text"
            className="search-input"
            style={{ paddingLeft: '36px' }}
            placeholder="Search by name, SKU, or supplier..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
        {lowStockCount > 0 && (
          <span style={{ fontSize: '0.85rem', color: '#c62828', fontWeight: 600 }}>
            ⚠️ {lowStockCount} item{lowStockCount > 1 ? 's are' : ' is'} at or below reorder threshold!
          </span>
        )}
      </div>

      <div className="table-container">
        {filteredItems.length === 0 ? (
          <div className="empty-state">
            <p>No inventory items match your search.</p>
          </div>
        ) : (
          <table className="data-table">
            <thead>
              <tr>
                <th>Item Name</th>
                <th>SKU</th>
                <th>Quantity</th>
                <th>Unit Price</th>
                <th>Total Value</th>
                <th>Supplier</th>
                <th>Status</th>
                {canManage && <th>Actions</th>}
              </tr>
            </thead>
            <tbody>
              {filteredItems.map((item) => {
                const isLow = item.quantity <= item.reorder_threshold;
                const value = (Number(item.quantity) || 0) * (Number(item.unit_price) || 0);

                return (
                  <tr key={item.id} style={{ backgroundColor: isLow ? '#fffbfa' : 'transparent' }}>
                    <td style={{ fontWeight: 600 }}>{item.item_name}</td>
                    <td>
                      <code>{item.sku || '—'}</code>
                    </td>
                    <td style={{ fontWeight: 600, color: isLow ? '#c62828' : '#212121' }}>
                      {item.quantity} units
                    </td>
                    <td>₹{Number(item.unit_price).toFixed(2)}</td>
                    <td className="amount-col">₹{value.toFixed(2)}</td>
                    <td>{item.supplier || '—'}</td>
                    <td>
                      <span className={`stock-badge ${isLow ? 'stock-badge-low' : 'stock-badge-ok'}`}>
                        {isLow ? `Low Stock (Min: ${item.reorder_threshold})` : 'In Stock'}
                      </span>
                    </td>
                    {canManage && (
                      <td>
                        <button
                          className="btn btn-sm btn-outline"
                          onClick={() => handleOpenEdit(item)}
                          title="Edit Stock Item"
                        >
                          <FiEdit2 size={13} style={{ marginRight: '4px' }} /> Edit
                        </button>
                      </td>
                    )}
                  </tr>
                );
              })}
            </tbody>
          </table>
        )}
      </div>

      {/* Add Item Modal */}
      <Modal isOpen={showAddModal} onClose={() => setShowAddModal(false)} title="Add Inventory Item">
        <form onSubmit={handleAddItem}>
          <div className="form-group">
            <label>Item Name *</label>
            <input
              name="item_name"
              value={form.item_name}
              onChange={handleFormChange}
              placeholder="e.g. POS Terminal V3"
              required
            />
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
            <div className="form-group">
              <label>SKU (Stock Keeping Unit)</label>
              <input
                name="sku"
                value={form.sku}
                onChange={handleFormChange}
                placeholder="e.g. SKU-TRM-001"
              />
            </div>
            <div className="form-group">
              <label>Quantity *</label>
              <input
                type="number"
                min="0"
                name="quantity"
                value={form.quantity}
                onChange={handleFormChange}
                required
              />
            </div>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
            <div className="form-group">
              <label>Unit Cost (₹) *</label>
              <input
                type="number"
                step="0.01"
                min="0"
                name="unit_price"
                value={form.unit_price}
                onChange={handleFormChange}
                placeholder="0.00"
                required
              />
            </div>
            <div className="form-group">
              <label>Reorder Threshold *</label>
              <input
                type="number"
                min="0"
                name="reorder_threshold"
                value={form.reorder_threshold}
                onChange={handleFormChange}
                required
              />
            </div>
          </div>

          <div className="form-group">
            <label>Supplier / Vendor</label>
            <input
              name="supplier"
              value={form.supplier}
              onChange={handleFormChange}
              placeholder="e.g. TechEquip Supply Co"
            />
          </div>

          <button type="submit" className="btn btn-primary" style={{ width: '100%', justifyContent: 'center' }}>
            Save Item to Inventory
          </button>
        </form>
      </Modal>

      {/* Edit Item Modal */}
      <Modal isOpen={showEditModal} onClose={() => setShowEditModal(false)} title="Update Inventory Item">
        <form onSubmit={handleUpdateItem}>
          <div className="form-group">
            <label>Item Name *</label>
            <input
              name="item_name"
              value={form.item_name}
              onChange={handleFormChange}
              required
            />
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
            <div className="form-group">
              <label>SKU</label>
              <input
                name="sku"
                value={form.sku}
                onChange={handleFormChange}
              />
            </div>
            <div className="form-group">
              <label>Quantity In Stock *</label>
              <input
                type="number"
                min="0"
                name="quantity"
                value={form.quantity}
                onChange={handleFormChange}
                required
              />
            </div>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
            <div className="form-group">
              <label>Unit Cost (₹) *</label>
              <input
                type="number"
                step="0.01"
                min="0"
                name="unit_price"
                value={form.unit_price}
                onChange={handleFormChange}
                required
              />
            </div>
            <div className="form-group">
              <label>Reorder Threshold *</label>
              <input
                type="number"
                min="0"
                name="reorder_threshold"
                value={form.reorder_threshold}
                onChange={handleFormChange}
                required
              />
            </div>
          </div>

          <div className="form-group">
            <label>Supplier / Vendor</label>
            <input
              name="supplier"
              value={form.supplier}
              onChange={handleFormChange}
            />
          </div>

          <button type="submit" className="btn btn-primary" style={{ width: '100%', justifyContent: 'center' }}>
            Apply Inventory Updates
          </button>
        </form>
      </Modal>
    </div>
  );
};

export default Inventory;

