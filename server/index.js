const express = require('express');
const cors = require('cors');
const path = require('path');
const fs = require('fs');
const { initDb } = require('./database');

const app = express();
const PORT = process.env.PORT || 5000;

// Middlewares
app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Uploads directory
const uploadsDir = path.join(__dirname, 'uploads');
if (!fs.existsSync(uploadsDir)) {
  fs.mkdirSync(uploadsDir, { recursive: true });
}
app.use('/uploads', express.static(uploadsDir));

async function startServer() {
  try {
    // Initialize WebAssembly SQLite database
    console.log('Initializing Grovix Database...');
    await initDb();
    console.log('Database initialized successfully.');

    // API Routes
    app.use('/api/auth', require('./routes/auth'));
    app.use('/api/sync', require('./routes/sync'));
    app.use('/api/users', require('./routes/users'));
    app.use('/api/employees', require('./routes/users'));
    app.use('/api/tasks', require('./routes/tasks'));
    app.use('/api/sales', require('./routes/sales'));
    app.use('/api/expenses', require('./routes/expenses'));
    app.use('/api/inventory', require('./routes/inventory'));
    app.use('/api/dashboard', require('./routes/dashboard'));
    app.use('/api/reports', require('./routes/reports'));
    app.use('/api/notifications', require('./routes/notifications'));
    app.use('/api/activity', require('./routes/activity'));

    // Health check route
    app.get('/api/health', (req, res) => {
      res.json({ status: 'ok', service: 'Grovix API Server', timestamp: new Date().toISOString() });
    });

    // Serve frontend web application (supports standalone server/public or monorepo client/build)
    const localPublicPath = path.join(__dirname, 'public');
    const clientBuildPath = path.join(__dirname, '..', 'client', 'build');
    const staticWebPath = fs.existsSync(localPublicPath) 
      ? localPublicPath 
      : (fs.existsSync(clientBuildPath) ? clientBuildPath : null);

    if (staticWebPath) {
      console.log(`Serving Web Application from: ${staticWebPath}`);
      app.use(express.static(staticWebPath));
      app.get('*', (req, res) => {
        res.sendFile(path.join(staticWebPath, 'index.html'));
      });
    } else {
      console.log('Web frontend build not found. Running in API-only mode.');
    }

    // Global error handler
    app.use((err, req, res, next) => {
      console.error('Unhandled server error:', err);
      res.status(500).json({ error: err.message || 'Internal Server Error' });
    });

    app.listen(PORT, () => {
      console.log(`================================================`);
      console.log(`  GROVIX Business Management Server`);
      console.log(`  Listening on port ${PORT}`);
      console.log(`  Access URL: http://localhost:${PORT}`);
      console.log(`================================================`);
    });
  } catch (err) {
    console.error('Fatal server startup error:', err);
    process.exit(1);
  }
}

startServer();
