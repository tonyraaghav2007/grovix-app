import React from 'react';
import './RoleBadge.css';

const roleColors = {
  owner: '#f44336',
  manager: '#1a237e',
  employee: '#4caf50',
  guest: '#9e9e9e',
};

const RoleBadge = ({ role }) => {
  const roleName = (role || 'guest').toLowerCase();
  const color = roleColors[roleName] || '#9e9e9e';

  return (
    <span className="role-badge" style={{ backgroundColor: `${color}18`, color }}>
      {roleName.charAt(0).toUpperCase() + roleName.slice(1)}
    </span>
  );
};

export default RoleBadge;
