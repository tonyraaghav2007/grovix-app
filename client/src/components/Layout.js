import React, { useState } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import RoleBadge from './RoleBadge';
import './Layout.css';
import {
  FiHome,
  FiCheckSquare,
  FiDollarSign,
  FiTrendingDown,
  FiPackage,
  FiUsers,
  FiSettings,
  FiBarChart2,
  FiLogOut,
  FiMenu,
  FiX,
} from 'react-icons/fi';

const Layout = ({ children }) => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [sidebarOpen, setSidebarOpen] = useState(false);

  const powerLevel = user?.power_level ?? user?.powerLevel ?? 0;

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const closeSidebar = () => setSidebarOpen(false);

  const navItems = [
    { path: '/dashboard', icon: <FiHome size={18} />, label: 'Dashboard', minPower: 0 },
    { path: '/tasks', icon: <FiCheckSquare size={18} />, label: 'Tasks', minPower: 0 },
    { path: '/sales', icon: <FiDollarSign size={18} />, label: 'Sales', minPower: 0 },
    { path: '/expenses', icon: <FiTrendingDown size={18} />, label: 'Expenses', minPower: 0 },
    { path: '/inventory', icon: <FiPackage size={18} />, label: 'Inventory', minPower: 50 },
    { path: '/employees', icon: <FiUsers size={18} />, label: 'Employees', minPower: 75 },
    { path: '/reports', icon: <FiBarChart2 size={18} />, label: 'Reports', minPower: 0 },
    { path: '/settings', icon: <FiSettings size={18} />, label: 'Settings', minPower: 100 },
  ];

  return (
    <div className="layout">
      {sidebarOpen && <div className="sidebar-overlay" onClick={closeSidebar}></div>}

      <aside className={`sidebar ${sidebarOpen ? 'sidebar-open' : ''}`}>
        <div className="sidebar-brand">
          <span className="sidebar-logo">G</span>
          <span className="sidebar-brand-text">Grovix</span>
          <button className="sidebar-close-btn" onClick={closeSidebar}>
            <FiX size={20} />
          </button>
        </div>

        <nav className="sidebar-nav">
          {navItems
            .filter((item) => powerLevel >= item.minPower)
            .map((item) => (
              <NavLink
                key={item.path}
                to={item.path}
                className={({ isActive }) => `sidebar-link ${isActive ? 'sidebar-link-active' : ''}`}
                onClick={closeSidebar}
              >
                {item.icon}
                <span>{item.label}</span>
              </NavLink>
            ))}
        </nav>

        <div className="sidebar-footer">
          <button className="sidebar-link sidebar-logout" onClick={handleLogout}>
            <FiLogOut size={18} />
            <span>Logout</span>
          </button>
        </div>
      </aside>

      <div className="main-area">
        <header className="top-bar">
          <button className="hamburger" onClick={() => setSidebarOpen(true)}>
            <FiMenu size={22} />
          </button>
          <div className="top-bar-right">
            <RoleBadge role={user?.role} />
            <span className="top-bar-name">{user?.name || 'User'}</span>
            <button className="btn-icon" onClick={handleLogout} title="Logout">
              <FiLogOut size={18} />
            </button>
          </div>
        </header>

        <main className="main-content">{children}</main>
      </div>
    </div>
  );
};

export default Layout;
