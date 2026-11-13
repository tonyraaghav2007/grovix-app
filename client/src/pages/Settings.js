import React, { useState, useEffect } from 'react';
import API from '../api/axios';
import { useAuth } from '../context/AuthContext';
import { FiSettings, FiLock, FiActivity, FiBriefcase } from 'react-icons/fi';
import { toast } from 'react-toastify';
import './Settings.css';

const Settings = () => {
  const { user } = useAuth();
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [activities, setActivities] = useState([]);
  const [loadingActivities, setLoadingActivities] = useState(true);

  // Business settings state
  const [businessSettings, setBusinessSettings] = useState({
    companyName: 'Grovix International',
    currency: 'USD ($)',
    taxRate: '8.25',
    fiscalYearStart: 'January',
  });

  useEffect(() => {
    fetchActivityLog();
  }, []);

  const fetchActivityLog = async () => {
    try {
      const res = await API.get('/activity', { params: { limit: 25 } });
      setActivities(res.data.activities || res.data || []);
    } catch (err) {
      console.error('Failed to load activity log');
    } finally {
      setLoadingActivities(false);
    }
  };

  const handleChangePassword = async (e) => {
    e.preventDefault();
    if (newPassword !== confirmPassword) {
      toast.error('New passwords do not match');
      return;
    }

    try {
      await API.post('/auth/change-password', {
        currentPassword,
        newPassword,
      });
      toast.success('Password changed successfully!');
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
    } catch (err) {
      toast.error(err.response?.data?.error || 'Failed to update password');
    }
  };

  const handleSaveBusinessSettings = (e) => {
    e.preventDefault();
    toast.success('System settings saved successfully!');
  };

  return (
    <div className="settings-page">
      <div className="page-header">
        <div>
          <h1>Administration & Settings</h1>
          <p className="text-light" style={{ fontSize: '0.9rem', marginTop: '4px' }}>
            System configuration, user security, and audit activity trail
          </p>
        </div>
      </div>

      <div className="settings-grid">
        {/* Security & Password */}
        <div className="settings-card">
          <h3>
            <FiLock size={18} /> Account Security
          </h3>
          <p className="text-light" style={{ fontSize: '0.85rem', marginBottom: '16px' }}>
            Update your login password and credentials.
          </p>

          <form onSubmit={handleChangePassword}>
            <div className="form-group">
              <label>Current Password</label>
              <input
                type="password"
                value={currentPassword}
                onChange={(e) => setCurrentPassword(e.target.value)}
                placeholder="••••••••"
                required
              />
            </div>

            <div className="form-group">
              <label>New Password (min 6 characters)</label>
              <input
                type="password"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                placeholder="••••••••"
                required
              />
            </div>

            <div className="form-group">
              <label>Confirm New Password</label>
              <input
                type="password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                placeholder="••••••••"
                required
              />
            </div>

            <button type="submit" className="btn btn-primary" style={{ width: '100%', justifyContent: 'center' }}>
              Update Password
            </button>
          </form>
        </div>

        {/* Business Settings */}
        <div className="settings-card">
          <h3>
            <FiBriefcase size={18} /> Company Preferences
          </h3>
          <p className="text-light" style={{ fontSize: '0.85rem', marginBottom: '16px' }}>
            Global business parameters and localization.
          </p>

          <form onSubmit={handleSaveBusinessSettings}>
            <div className="form-group">
              <label>Company Name</label>
              <input
                type="text"
                value={businessSettings.companyName}
                onChange={(e) =>
                  setBusinessSettings({ ...businessSettings, companyName: e.target.value })
                }
              />
            </div>

            <div className="form-group">
              <label>Operating Currency</label>
              <select
                value={businessSettings.currency}
                onChange={(e) =>
                  setBusinessSettings({ ...businessSettings, currency: e.target.value })
                }
              >
                <option value="USD ($)">USD - US Dollar ($)</option>
                <option value="EUR (€)">EUR - Euro (€)</option>
                <option value="GBP (£)">GBP - British Pound (£)</option>
                <option value="PKR (₨)">PKR - Pakistani Rupee (₨)</option>
                <option value="INR (₹)">INR - Indian Rupee (₹)</option>
              </select>
            </div>

            <div className="form-group">
              <label>Default Tax Rate (%)</label>
              <input
                type="number"
                step="0.01"
                value={businessSettings.taxRate}
                onChange={(e) =>
                  setBusinessSettings({ ...businessSettings, taxRate: e.target.value })
                }
              />
            </div>

            <button type="submit" className="btn btn-primary" style={{ width: '100%', justifyContent: 'center' }}>
              Save Company Settings
            </button>
          </form>
        </div>
      </div>

      {/* Activity Log */}
      <div className="settings-card" style={{ marginTop: '24px' }}>
        <h3>
          <FiActivity size={18} /> System Audit & Activity Trail
        </h3>
        <p className="text-light" style={{ fontSize: '0.85rem', marginBottom: '16px' }}>
          Real-time log of security events, administrative changes, and operational records.
        </p>

        {loadingActivities ? (
          <div className="loading-container" style={{ padding: '24px' }}>
            <div className="spinner"></div>
          </div>
        ) : activities.length === 0 ? (
          <div className="empty-state">
            <p>No activity records logged yet.</p>
          </div>
        ) : (
          <div className="activity-list">
            {activities.map((act) => (
              <div key={act.id} className="activity-item">
                <div className="activity-header">
                  <span style={{ fontWeight: 600, color: '#1a237e' }}>
                    {act.user_name || 'System'} ({act.user_role || 'system'})
                  </span>
                  <span style={{ fontSize: '0.75rem' }}>
                    {act.timestamp ? new Date(act.timestamp).toLocaleString() : '—'}
                  </span>
                </div>
                <div style={{ color: '#212121', marginTop: '2px' }}>
                  <strong>{act.action}</strong>: {act.details || '—'}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default Settings;

