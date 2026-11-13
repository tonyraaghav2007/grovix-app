import React from 'react';
import './TaskCard.css';
import { FiClock, FiUser } from 'react-icons/fi';

const priorityColors = {
  urgent: '#d32f2f',
  high: '#f44336',
  medium: '#ff9800',
  low: '#4caf50',
};

const TaskCard = ({ task, onClick }) => {
  const priority = (task.priority || 'medium').toLowerCase();
  const rawDate = task.dueDate || task.due_date;
  const dueDate = rawDate ? new Date(rawDate).toLocaleDateString() : null;
  const assigneeName = task.assigneeName || task.assigned_to_name;

  return (
    <div className="task-card" onClick={() => onClick && onClick(task)}>
      <div className="task-card-header">
        <h4 className="task-card-title">{task.title}</h4>
        <span
          className="task-priority-badge"
          style={{ backgroundColor: priorityColors[priority] || '#ff9800' }}
        >
          {priority}
        </span>
      </div>
      {task.description && (
        <p className="task-card-desc">{task.description.substring(0, 80)}{task.description.length > 80 ? '...' : ''}</p>
      )}
      <div className="task-card-footer">
        {assigneeName && (
          <span className="task-card-meta">
            <FiUser size={12} />
            {assigneeName}
          </span>
        )}
        {dueDate && (
          <span className="task-card-meta">
            <FiClock size={12} />
            {dueDate}
          </span>
        )}
      </div>
    </div>
  );
};

export default TaskCard;
