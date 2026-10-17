// Private grant is saved only under ignored .release. Never print or commit its contents.
const fs = require('node:fs'), crypto = require('node:crypto'), path = require('node:path');
const dir = path.resolve(__dirname, '../.release');
fs.mkdirSync(dir, { recursive: true });
const privateFile = path.join(dir, 'review-access-private.pem');
if (!fs.existsSync(privateFile)) {
  const pair = crypto.generateKeyPairSync('rsa', { modulusLength: 2048 });
  fs.writeFileSync(privateFile, pair.privateKey.export({ type:'pkcs8', format:'pem' }), {flag:'wx'});
}
const privateKey = crypto.createPrivateKey(fs.readFileSync(privateFile));
const publicKey = crypto.createPublicKey(privateKey).export({type:'spki',format:'der'}).toString('base64');
fs.writeFileSync(path.join(dir,'review-access-public-key.txt'),publicKey);
const code = crypto.sign('sha256',Buffer.from('my-health-review-access:v1:com.peter.myhealth'),privateKey).toString('base64url');
fs.writeFileSync(path.join(dir,'review-access-code.txt'),code);
fs.writeFileSync(path.join(dir,'reviewer-instructions.txt'),
  'Name: My Health full review access\n\nUsername/password: Not required; leave blank.\n\nInstructions:\nInstall version 2.3.2 or later. No app registration, personal login, payment or trial is needed. Tap You (bottom right), scroll down, tap My Health Pro, then Review access. Paste the complete code below and tap Activate review access. This grants the same GPS workouts, barcode lookup, extended trends and PDF features as Pro without a payment. The code does not expire and requires no separate device or one-time password.\n\n'+code+
  '\n\nActivity recognition is required for automatic steps; camera is requested for barcode scanning and foreground location for GPS workouts. Health Connect is optional and requires Android 14 or later; use manual journal entries without it. App lock is off by default on clean installs. The app does not measure vital signs itself; manual readings or compatible Health Connect source records are needed. Health records remain on the review device.\n');
console.log('Review key and non-expiring grant prepared under .release; private key and code were not printed.');
