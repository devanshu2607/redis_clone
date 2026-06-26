#!/usr/bin/env node

const { spawn, execSync } = require('child_process');
const path = require('path');
const fs = require('fs');

try {
  execSync('java -version', { stdio: 'ignore' });
} catch (e) {
  console.error('Error: Java is not installed or not in your PATH. ConC cache server requires Java 21+ to run.');
  process.exit(1);
}

const jarPath = path.join(__dirname, 'ConC.jar');

if (!fs.existsSync(jarPath)) {
  console.error(`Error: ConC.jar not found at ${jarPath}. Please run "npm run build" first.`);
  process.exit(1);
}

const args = ['-jar', jarPath, ...process.argv.slice(2)];

console.log('Starting ConC Cache Server...');
const serverProcess = spawn('java', args, { stdio: 'inherit' });

serverProcess.on('close', (code) => {
  process.exit(code);
});
