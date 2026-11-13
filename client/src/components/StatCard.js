import React from 'react';
import './StatCard.css';

const StatCard = ({ icon, label, value, color }) => {
  return (
    <div className="stat-card">
      <div className="stat-card-icon" style={{ backgroundColor: color ? `${color}15` : '#1a237e15', color: color || 'var(--primary)' }}>
        {icon}
      </div>
      <div className="stat-card-info">
        <span className="stat-card-label">{label}</span>
        <span className="stat-card-value">{value}</span>
      </div>
    </div>
  );
};

export default StatCard;
