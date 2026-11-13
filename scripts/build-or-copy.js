const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const publicIndex = path.join(__dirname, '..', 'server', 'public', 'index.html');
const copyBuild = path.join(__dirname, 'copy-build.js');

if (fs.existsSync(publicIndex)) {
  console.log('====================================================');
  console.log('✅ Production Web Platform verified in server/public!');
  console.log('====================================================');
} else {
  console.log('server/public not found. Building React Web frontend from source...');
  try {
    execSync('npm install --prefix client && npm run build --prefix client', { stdio: 'inherit' });
    if (fs.existsSync(copyBuild)) {
      require(copyBuild);
    }
  } catch (err) {
    console.error('Frontend build failed:', err.message);
    process.exit(1);
  }
}

