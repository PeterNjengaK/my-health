$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$privateDirectory = Join-Path $projectRoot '.release'
$keyFile = Join-Path $privateDirectory 'upload-key.p12'
if (Test-Path -LiteralPath $keyFile) { Write-Output 'Upload key already exists; preserved.'; exit 0 }
New-Item -ItemType Directory -Force $privateDirectory | Out-Null
Add-Type -AssemblyName System.Security
$randomBytes = New-Object byte[] 32
$generator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$generator.GetBytes($randomBytes)
$uploadPassword = [Convert]::ToBase64String($randomBytes)
$passwordBytes = [Text.Encoding]::UTF8.GetBytes($uploadPassword)
$encrypted = [System.Security.Cryptography.ProtectedData]::Protect($passwordBytes, $null, [System.Security.Cryptography.DataProtectionScope]::CurrentUser)
[IO.File]::WriteAllBytes((Join-Path $privateDirectory 'password.dpapi'), $encrypted)
$env:MY_HEALTH_STORE_PASSWORD = $uploadPassword
try {
  & 'C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe' -genkeypair -keystore $keyFile -storetype PKCS12 -alias my-health-upload -keyalg RSA -keysize 4096 -validity 10000 -dname 'CN=My Health Upload' -storepass:env MY_HEALTH_STORE_PASSWORD -keypass:env MY_HEALTH_STORE_PASSWORD
  if ($LASTEXITCODE -ne 0) { throw 'Key generation failed' }
  [IO.File]::WriteAllText((Join-Path $privateDirectory 'signing.properties'), "storeFile=.release/upload-key.p12`nkeyAlias=my-health-upload`n", [Text.UTF8Encoding]::new($false))
  & 'C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe' -exportcert -rfc -keystore $keyFile -alias my-health-upload -storepass:env MY_HEALTH_STORE_PASSWORD -file (Join-Path $privateDirectory 'upload-certificate.pem')
  Write-Output 'Upload key created. Its password is encrypted for this Windows account; no signing secret is committed to Git.'
} finally { Remove-Item Env:\MY_HEALTH_STORE_PASSWORD; [Array]::Clear($passwordBytes,0,$passwordBytes.Length); $generator.Dispose() }
