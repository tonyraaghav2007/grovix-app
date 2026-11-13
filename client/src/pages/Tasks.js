import React, { useState, useEffect } from 'react';
import API from '../api/axios';
import { useAuth } from '../context/AuthContext';
import Modal from '../components/Modal';
import TaskCard from '../components/TaskCard';
import { FiPlus } from 'react-icons/fi';
import { toast } from 'react-toastify';
import './Tasks.css';

const Tasks = () => {
  const { user } = useAuth();
  const [tasks, setTasks] = useState([]);
  const [employees, setEmployees] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showAddModal, setShowAddModal] = useState(false);
  const [showDetailModal, setShowDetailModal] = useState(false);
  const [selectedTask, setSelectedTask] = useState(null);
  const [form, setForm] = useState({
    title: '',
    description: '',
    assignee: '',
    priority: 'medium',
    dueDate: '',
  });

  const powerLevel = user?.power_level ?? user?.powerLevel ?? 0;
  const canManage = powerLevel >= 75;

  useEffect(() => {
    fetchTasks();
    if (canManage) {
      fetchEmployees();
    }
  }, [canManage]);

  const fetchTasks = async () => {
    try {
      const res = await API.get('/tasks');
      setTasks(res.data.tasks || res.data || []);
    } catch (err) {
      toast.error('Failed to load tasks');
    } finally {
      setLoading(false);
    }
  };

  const fetchEmployees = async () => {
    try {
      const res = await API.get('/users');
      setEmployees(res.data.users || res.data || []);
    } catch (err) {
      console.error('Failed to fetch employees');
    }
  };

  const handleFormChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleAddTask = async (e) => {
    e.preventDefault();
    try {
      await API.post('/tasks', form);
      toast.success('Task created successfully');
      setShowAddModal(false);
      setForm({ title: '', description: '', assignee: '', priority: 'medium', dueDate: '' });
      fetchTasks();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to create task');
    }
  };

  const handleTaskClick = (task) => {
    setSelectedTask(task);
    setShowDetailModal(true);
  };

  const updateTaskStatus = async (taskId, status) => {
    try {
      await API.put(`/tasks/${taskId}`, { status });
      toast.success('Task status updated');
      setShowDetailModal(false);
      fetchTasks();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to update task');
    }
  };

  const getTasksByStatus = (status) => {
    return tasks.filter(
      (t) => (t.status || 'todo').toLowerCase().replace(/[\s-_]/g, '') === status
    );
  };

  if (loading) {
    return (
      <div className="loading-container">
        <div className="spinner"></div>
      </div>
    );
  }

  const columns = [
    { key: 'todo', title: 'To-Do', color: '#ff9800' },
    { key: 'inprogress', title: 'In Progress', color: '#1a237e' },
    { key: 'completed', title: 'Completed', color: '#4caf50' },
  ];

  return (
    <div className="tasks-page">
      <div className="page-header">
        <h1>Tasks</h1>
        {canManage && (
          <button className="btn btn-primary" onClick={() => setShowAddModal(true)}>
            <FiPlus size={16} /> Add Task
          </button>
        )}
      </div>

      <div className="kanban-board">
        {columns.map((col) => (
          <div key={col.key} className="kanban-column">
            <div className="kanban-column-header">
              <span className="kanban-column-dot" style={{ backgroundColor: col.color }}></span>
              <h3>{col.title}</h3>
              <span className="kanban-count">{getTasksByStatus(col.key).length}</span>
            </div>
            <div className="kanban-cards">
              {getTasksByStatus(col.key).map((task) => (
                <TaskCard key={task._id || task.id} task={task} onClick={handleTaskClick} />
              ))}
              {getTasksByStatus(col.key).length === 0 && (
                <p className="kanban-empty">No tasks</p>
              )}
            </div>
          </div>
        ))}
      </div>

      {/* Add Task Modal */}
      <Modal isOpen={showAddModal} onClose={() => setShowAddModal(false)} title="Add New Task">
        <form onSubmit={handleAddTask}>
          <div className="form-group">
            <label>Title</label>
            <input
              name="title"
              value={form.title}
              onChange={handleFormChange}
              placeholder="Task title"
              required
            />
          </div>
          <div className="form-group">
            <label>Description</label>
            <textarea
              name="description"
              value={form.description}
              onChange={handleFormChange}
              placeholder="Task description"
            />
          </div>
          <div className="form-group">
            <label>Assignee</label>
            <select name="assignee" value={form.assignee} onChange={handleFormChange}>
              <option value="">Unassigned</option>
              {employees.map((emp) => (
                <option key={emp._id || emp.id} value={emp._id || emp.id}>
                  {emp.name}
                </option>
              ))}
            </select>
          </div>
          <div className="form-group">
            <label>Priority</label>
            <select name="priority" value={form.priority} onChange={handleFormChange}>
              <option value="low">Low</option>
              <option value="medium">Medium</option>
              <option value="high">High</option>
            </select>
          </div>
          <div className="form-group">
            <label>Due Date</label>
            <input
              type="date"
              name="dueDate"
              value={form.dueDate}
              onChange={handleFormChange}
            />
          </div>
          <button type="submit" className="btn btn-primary" style={{ width: '100%', justifyContent: 'center' }}>
            Create Task
          </button>
        </form>
      </Modal>

      {/* Task Detail Modal */}
      <Modal
        isOpen={showDetailModal}
        onClose={() => setShowDetailModal(false)}
        title="Task Details"
      >
        {selectedTask && (
          <div className="task-detail">
            <h3 style={{ marginBottom: 8 }}>{selectedTask.title}</h3>
            <p className="text-light" style={{ marginBottom: 16, fontSize: '0.85rem' }}>
              {selectedTask.description || 'No description'}
            </p>
            <div className="task-detail-meta">
              <div>
                <span className="text-light" style={{ fontSize: '0.75rem' }}>Priority</span>
                <p style={{ textTransform: 'capitalize', fontWeight: 600 }}>{selectedTask.priority}</p>
              </div>
              <div>
                <span className="text-light" style={{ fontSize: '0.75rem' }}>Assignee</span>
                <p style={{ fontWeight: 600 }}>{selectedTask.assigneeName || 'Unassigned'}</p>
              </div>
              <div>
                <span className="text-light" style={{ fontSize: '0.75rem' }}>Due Date</span>
                <p style={{ fontWeight: 600 }}>
                  {selectedTask.dueDate
                    ? new Date(selectedTask.dueDate).toLocaleDateString()
                    : 'No date'}
                </p>
              </div>
              <div>
                <span className="text-light" style={{ fontSize: '0.75rem' }}>Status</span>
                <p style={{ fontWeight: 600, textTransform: 'capitalize' }}>{selectedTask.status}</p>
              </div>
            </div>
            <div className="task-detail-actions">
              <span className="text-light" style={{ fontSize: '0.8rem', fontWeight: 600 }}>Update Status:</span>
              <div className="task-status-buttons">
                <button
                  className="btn btn-sm btn-outline"
                  onClick={() => updateTaskStatus(selectedTask._id || selectedTask.id, 'todo')}
                >
                  To-Do
                </button>
                <button
                  className="btn btn-sm btn-primary"
                  onClick={() => updateTaskStatus(selectedTask._id || selectedTask.id, 'in-progress')}
                >
                  In Progress
                </button>
                <button
                  className="btn btn-sm btn-success"
                  onClick={() => updateTaskStatus(selectedTask._id || selectedTask.id, 'completed')}
                >
                  Completed
                </button>
              </div>
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
};

export default Tasks;
