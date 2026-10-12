param([Parameter(Mandatory=$true)][string]$Destination)
$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Security
$encrypted=[IO.File]::ReadAllBytes((Join-Path (Split-Path -Parent $PSScriptRoot) '.release\password.dpapi'))
$bytes=[Security.Cryptography.ProtectedData]::Unprotect($encrypted,$null,[Security.Cryptography.DataProtectionScope]::CurrentUser)
try{[IO.File]::WriteAllBytes([IO.Path]::GetFullPath($Destination),$bytes);Write-Output 'Password exported to the destination you chose. Store it with the upload key in a secure vault; never commit it.'}finally{[Array]::Clear($bytes,0,$bytes.Length)}
