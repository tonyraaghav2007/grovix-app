const fs = require('fs');
const path = require('path');

const clientBuild = path.join(__dirname, '..', 'client', 'build');
const serverPublic = path.join(__dirname, '..', 'server', 'public');

if (fs.existsSync(clientBuild)) {
  console.log(`Copying built web assets from ${clientBuild} to ${serverPublic}...`);
  if (fs.existsSync(serverPublic)) {
    fs.rmSync(serverPublic, { recursive: true, force: true });
  }
  fs.cpSync(clientBuild, serverPublic, { recursive: true });
  console.log('✅ Web frontend successfully synchronized to server/public!');
} else {
  console.warn('⚠️ client/build does not exist yet. Please run npm run build in client/ first.');
}

