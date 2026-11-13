import React, { useState, useEffect } from 'react';
import API from '../api/axios';
import { useAuth } from '../context/AuthContext';
import Modal from '../components/Modal';
import RoleBadge from '../components/RoleBadge';
import StatCard from '../components/StatCard';
import { FiUsers, FiUserPlus, FiShield, FiUserCheck, FiEdit2, FiPhone, FiMail } from 'react-icons/fi';
import { toast } from 'react-toastify';
import './Employees.css';

const Employees = () => {
  const { user } = useAuth();
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showAddModal, setShowAddModal] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [editingUser, setEditingUser] = useState(null);

  const [form, setForm] = useState({
    name: '',
    email: '',
    password: '',
    role: 'employee',
    phone: '',
  });

  const [editForm, setEditForm] = useState({
    name: '',
    email: '',
    role: 'employee',
    phone: '',
    status: 'active',
  });

  const powerLevel = user?.power_level ?? user?.powerLevel ?? 0;
  const isOwner = powerLevel >= 100;
  const isManager = powerLevel >= 75;

  useEffect(() => {
    fetchUsers();
  }, []);

  const fetchUsers = async () => {
    try {
      const res = await API.get('/users');
      setUsers(res.data.users || res.data || []);
    } catch (err) {
      toast.error('Failed to load team members');
    } finally {
      setLoading(false);
    }
  };

  const handleFormChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleEditFormChange = (e) => {
    setEditForm({ ...editForm, [e.target.name]: e.target.value });
  };

  const handleAddUser = async (e) => {
    e.preventDefault();
    try {
      await API.post('/users', {
        name: form.name.trim(),
        email: form.email.toLowerCase().trim(),
        password: form.password,
        role: form.role,
        phone: form.phone.trim() || undefined,
      });
      toast.success(`Account created for ${form.name} (${form.role})`);
      setShowAddModal(false);
      setForm({
        name: '',
        email: '',
        password: '',
        role: isOwner ? 'manager' : 'employee',
        phone: '',
      });
      fetchUsers();
    } catch (err) {
      toast.error(err.response?.data?.error || 'Failed to create user');
    }
  };

  const handleOpenEdit = (target) => {
    setEditingUser(target);
    setEditForm({
      name: target.name,
      email: target.email,
      role: target.role,
      phone: target.phone || '',
      status: target.status,
    });
    setShowEditModal(true);
  };

  const handleUpdateUser = async (e) => {
    e.preventDefault();
    try {
      await API.put(`/users/${editingUser.id}`, {
        name: editForm.name.trim(),
        email: editForm.email.toLowerCase().trim(),
        role: editForm.role,
        phone: editForm.phone.trim() || null,
        status: editForm.status,
      });
      toast.success('Account updated successfully!');
      setShowEditModal(false);
      setEditingUser(null);
      fetchUsers();
    } catch (err) {
      toast.error(err.response?.data?.error || 'Failed to update account');
    }
  };

  const handleToggleStatus = async (target) => {
    const newStatus = target.status === 'active' ? 'inactive' : 'active';
    try {
      await API.put(`/users/${target.id}`, { status: newStatus });
      toast.success(`User marked as ${newStatus}`);
      fetchUsers();
    } catch (err) {
      toast.error(err.response?.data?.error || 'Failed to change status');
    }
  };

  if (loading) {
    return (
      <div className="loading-container">
        <div className="spinner"></div>
      </div>
    );
  }

  const activeStaff = users.filter((u) => u.status === 'active').length;
  const directorsCount = users.filter((u) => u.role === 'owner').length;
  const managersCount = users.filter((u) => u.role === 'manager').length;

  return (
    <div className="employees-page">
      <div className="page-header">
        <div>
          <h1>Team & Personnel Management</h1>
          <p className="text-light" style={{ fontSize: '0.9rem', marginTop: '4px' }}>
            {isOwner
              ? 'Full control over Directors, Managers, and Staff roles & accounts'
              : 'Manage and coordinate assigned operational staff and employees'}
          </p>
        </div>
        {isManager && (
          <button className="btn btn-primary" onClick={() => setShowAddModal(true)}>
            <FiUserPlus size={16} /> Add Team Member
          </button>
        )}
      </div>

      <div className="sales-summary-grid">
        <StatCard
          icon={<FiUsers size={22} />}
          label="Total Team Members"
          value={users.length}
          color="#1a237e"
        />
        <StatCard
          icon={<FiShield size={22} />}
          label="Directors / Owners"
          value={directorsCount}
          color="#d32f2f"
        />
        <StatCard
          icon={<FiUserCheck size={22} />}
          label="Active Staff"
          value={activeStaff}
          color="#4caf50"
        />
      </div>

      <div className="table-container">
        <table className="data-table">
          <thead>
            <tr>
              <th>Member Name</th>
              <th>Contact</th>
              <th>Role & Power</th>
              <th>Status</th>
              <th>Joined Date</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {users.map((member) => {
              const canEditThisUser =
                isOwner || (isManager && member.power_level < powerLevel && member.id !== user.id);

              return (
                <tr key={member.id}>
                  <td>
                    <div style={{ fontWeight: 600 }}>{member.name}</div>
                    {member.id === user?.id && (
                      <span style={{ fontSize: '0.75rem', color: '#1a237e', fontWeight: 600 }}>
                        (You)
                      </span>
                    )}
                  </td>
                  <td>
                    <div style={{ fontSize: '0.85rem' }}>
                      <FiMail size={12} style={{ marginRight: '4px' }} />
                      {member.email}
                    </div>
                    {member.phone && (
                      <div style={{ fontSize: '0.8rem', color: '#757575', marginTop: '2px' }}>
                        <FiPhone size={11} style={{ marginRight: '4px' }} />
                        {member.phone}
                      </div>
                    )}
                  </td>
                  <td>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                      <RoleBadge role={member.role} />
                      <span style={{ fontSize: '0.75rem', color: '#757575' }}>
                        Lvl {member.power_level}
                      </span>
                    </div>
                  </td>
                  <td>
                    <span
                      className={`status-badge ${
                        member.status === 'active' ? 'status-badge-active' : 'status-badge-inactive'
                      }`}
                    >
                      {member.status}
                    </span>
                  </td>
                  <td style={{ fontSize: '0.85rem', color: '#757575' }}>
                    {member.created_at ? new Date(member.created_at).toLocaleDateString() : '—'}
                  </td>
                  <td>
                    {canEditThisUser ? (
                      <div className="action-btn-group">
                        <button
                          className="btn btn-sm btn-outline"
                          onClick={() => handleOpenEdit(member)}
                          title="Edit user details"
                        >
                          <FiEdit2 size={12} style={{ marginRight: '4px' }} /> Edit
                        </button>
                        {member.id !== user?.id && (
                          <button
                            className={`btn btn-sm ${
                              member.status === 'active' ? 'btn-outline' : 'btn-success'
                            }`}
                            onClick={() => handleToggleStatus(member)}
                          >
                            {member.status === 'active' ? 'Deactivate' : 'Activate'}
                          </button>
                        )}
                      </div>
                    ) : (
                      <span style={{ fontSize: '0.8rem', color: '#9e9e9e' }}>Protected</span>
                    )}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      {/* Add User Modal */}
      <Modal isOpen={showAddModal} onClose={() => setShowAddModal(false)} title="Create New Account">
        <form onSubmit={handleAddUser}>
          <div className="form-group">
            <label>Full Name *</label>
            <input
              name="name"
              value={form.name}
              onChange={handleFormChange}
              placeholder="e.g. Sarah Jenkins"
              required
            />
          </div>

          <div className="form-group">
            <label>Email Address *</label>
            <input
              type="email"
              name="email"
              value={form.email}
              onChange={handleFormChange}
              placeholder="e.g. sarah@grovix.com"
              required
            />
          </div>

          <div className="form-group">
            <label>Initial Password * (min 6 characters)</label>
            <input
              type="password"
              name="password"
              value={form.password}
              onChange={handleFormChange}
              placeholder="Temporary password"
              required
            />
          </div>

          <div className="form-group">
            <label>Assigned Role & Power Level *</label>
            <select name="role" value={form.role} onChange={handleFormChange} required>
              {isOwner && <option value="owner">Owner / Director (Power: 100 - Full Access)</option>}
              {isOwner && <option value="manager">Manager / Dev (Power: 75 - Manage Staff & Operations)</option>}
              <option value="employee">Employee / Staff (Power: 50 - Assigned Tasks & Records)</option>
              <option value="guest">Guest / Viewer (Power: 25 - Read-only Overview)</option>
            </select>
          </div>

          <div className="form-group">
            <label>Phone Number (Optional)</label>
            <input
              name="phone"
              value={form.phone}
              onChange={handleFormChange}
              placeholder="e.g. +1 555-0123"
            />
          </div>

          <button type="submit" className="btn btn-primary" style={{ width: '100%', justifyContent: 'center' }}>
            Create Account
          </button>
        </form>
      </Modal>

      {/* Edit User Modal */}
      <Modal isOpen={showEditModal} onClose={() => setShowEditModal(false)} title="Edit Member Account">
        <form onSubmit={handleUpdateUser}>
          <div className="form-group">
            <label>Full Name *</label>
            <input
              name="name"
              value={editForm.name}
              onChange={handleEditFormChange}
              required
            />
          </div>

          <div className="form-group">
            <label>Email Address *</label>
            <input
              type="email"
              name="email"
              value={editForm.email}
              onChange={handleEditFormChange}
              required
            />
          </div>

          <div className="form-group">
            <label>Role</label>
            <select name="role" value={editForm.role} onChange={handleEditFormChange}>
              {isOwner && <option value="owner">Owner / Director (Power 100)</option>}
              {isOwner && <option value="manager">Manager / Dev (Power 75)</option>}
              <option value="employee">Employee / Staff (Power 50)</option>
              <option value="guest">Guest (Power 25)</option>
            </select>
          </div>

          <div className="form-group">
            <label>Account Status</label>
            <select name="status" value={editForm.status} onChange={handleEditFormChange}>
              <option value="active">Active</option>
              <option value="inactive">Inactive</option>
            </select>
          </div>

          <div className="form-group">
            <label>Phone Number</label>
            <input
              name="phone"
              value={editForm.phone}
              onChange={handleEditFormChange}
            />
          </div>

          <button type="submit" className="btn btn-primary" style={{ width: '100%', justifyContent: 'center' }}>
            Save Changes
          </button>
        </form>
      </Modal>
    </div>
  );
};

export default Employees;

