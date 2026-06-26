const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');

console.log('Building Java application using Maven...');
try {
  const mvnCmd = process.platform === 'win32' ? 'mvnw.cmd' : './mvnw';
  
  execSync(mvnCmd + ' clean package', { stdio: 'inherit', shell: true });

  const binDir = path.join(__dirname, '..', 'bin');
  if (!fs.existsSync(binDir)) {
    fs.mkdirSync(binDir, { recursive: true });
  }

  const srcJar = path.join(__dirname, '..', 'target', 'ConC-0.0.1-SNAPSHOT.jar');
  const destJar = path.join(binDir, 'ConC.jar');
  
  if (fs.existsSync(srcJar)) {
    fs.copyFileSync(srcJar, destJar);
    console.log(`Successfully built and copied JAR to ${destJar}`);
  } else {
    console.error('Could not find built JAR file at ' + srcJar);
    process.exit(1);
  }
} catch (error) {
  console.error('Build failed:', error.message);
  process.exit(1);
}
