param([switch]$Submission)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Add-Type -AssemblyName System.Security
$privateDirectory = Join-Path $projectRoot '.release'
$originalBillingKey = $env:PLAY_BILLING_PUBLIC_KEY
$billingKeyFile = Join-Path $privateDirectory 'play-billing-public-key.txt'
if ([string]::IsNullOrWhiteSpace($env:PLAY_BILLING_PUBLIC_KEY) -and (Test-Path -LiteralPath $billingKeyFile)) {
  $env:PLAY_BILLING_PUBLIC_KEY = ([IO.File]::ReadAllText($billingKeyFile) -replace '\s','')
}
if (-not [string]::IsNullOrWhiteSpace($env:PLAY_BILLING_PUBLIC_KEY)) {
  try { $keyBytes = [Convert]::FromBase64String($env:PLAY_BILLING_PUBLIC_KEY) } catch { throw 'The Play public licensing key is not valid Base64.' }
  if ($keyBytes.Length -lt 256 -or $keyBytes[0] -ne 48) { throw 'The Play public licensing key does not look like a DER RSA public key.' }
}
$encrypted = [IO.File]::ReadAllBytes((Join-Path $privateDirectory 'password.dpapi'))
$secretBytes = [Security.Cryptography.ProtectedData]::Unprotect($encrypted,$null,[Security.Cryptography.DataProtectionScope]::CurrentUser)
$env:MY_HEALTH_STORE_PASSWORD = [Text.Encoding]::UTF8.GetString($secretBytes)
$env:MY_HEALTH_KEY_PASSWORD = $env:MY_HEALTH_STORE_PASSWORD
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
try {
  if ($Submission) {
    $missing = @('PLAY_BILLING_PUBLIC_KEY','PRIVACY_POLICY_URL','SUPPORT_EMAIL','PUBLIC_DEVELOPER_NAME') | Where-Object { [string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($_)) }
    if ($missing.Count -gt 0) { throw "Submission settings missing: $($missing -join ', '). Set these after the Play Console account is created." }
    if ($env:PRIVACY_POLICY_URL -notmatch '^https://') { throw 'Privacy policy must have a public HTTPS URL.' }
  }
  Push-Location $projectRoot
  & .\gradlew.bat :app:bundleRelease :app:lintRelease --console=plain
  if ($LASTEXITCODE -ne 0) { throw 'Release build failed' }
  Write-Output 'Signed AAB: app/build/outputs/bundle/release/app-release.aab'
  if (-not $Submission) { Write-Output 'Release candidate only. Play Console, billing product/key, public policy, support details and purchase testing must be completed before submission.' }
} finally {
  if ((Get-Location).Path -eq $projectRoot) { Pop-Location }
  Remove-Item Env:\MY_HEALTH_STORE_PASSWORD,Env:\MY_HEALTH_KEY_PASSWORD -ErrorAction SilentlyContinue
  $env:PLAY_BILLING_PUBLIC_KEY = $originalBillingKey
  [Array]::Clear($secretBytes,0,$secretBytes.Length)
}
