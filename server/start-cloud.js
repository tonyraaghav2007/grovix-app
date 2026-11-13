const localtunnel = require('localtunnel');
const http = require('http');
const path = require('path');

// Launch the Grovix Express server
console.log('Starting Grovix Backend Server...');
require('./index.js');

const PORT = process.env.PORT || 5000;
const PREFERRED_SUBDOMAIN = process.env.SUBDOMAIN || 'grovix-app';

let activeTunnel = null;

async function startTunnel() {
  try {
    console.log(`Establishing Central Cloud Tunnel on port ${PORT}...`);
    activeTunnel = await localtunnel({
      port: PORT,
      subdomain: PREFERRED_SUBDOMAIN
    });

    console.log('\n');
    console.log('========================================================================');
    console.log('   GROVIX CENTRAL CLOUD SERVER IS NOW ACTIVE & SYNCHRONIZING!          ');
    console.log('========================================================================');
    console.log(`   Local Server URL:   http://localhost:${PORT}`);
    console.log(`   Central Cloud URL:  ${activeTunnel.url}`);
    console.log('   Database Storage:   SQLite persistent (grovix.db)');
    console.log('   Cloud Sync Engine:  REAL-TIME REST & JSON API (Min SDK 24+)');
    console.log('========================================================================');
    console.log('   All colleagues using the Grovix Android APK on any network will     ');
    console.log(`   automatically synchronize with this central cloud database!          `);
    console.log('========================================================================');
    console.log('   Press Ctrl+C at any time to stop the server.\n');

    activeTunnel.on('close', () => {
      console.warn('⚠️ Cloud tunnel was closed. Reconnecting in 5 seconds...');
      setTimeout(startTunnel, 5000);
    });

    activeTunnel.on('error', (err) => {
      console.error('⚠️ Cloud tunnel error:', err.message);
    });

  } catch (err) {
    console.error('⚠️ Could not open preferred tunnel subdomain. Retrying with random subdomain...');
    try {
      activeTunnel = await localtunnel({ port: PORT });
      console.log('\n========================================================================');
      console.log(`   GROVIX CENTRAL CLOUD URL: ${activeTunnel.url}`);
      console.log('========================================================================\n');
    } catch (e2) {
      console.error('Fatal tunnel error:', e2.message);
    }
  }
}

// Wait 1.5 seconds for express server to bind port, then establish tunnel
setTimeout(startTunnel, 1500);

// Graceful shutdown
process.on('SIGINT', () => {
  console.log('\nShutting down Grovix Central Cloud server...');
  if (activeTunnel) activeTunnel.close();
  process.exit(0);
});

