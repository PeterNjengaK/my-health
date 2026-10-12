$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent $PSScriptRoot
$javaDirectory='C:\Program Files\Android\Android Studio\jbr\bin'
$outputDirectory=Join-Path $projectRoot 'artifacts\billing-tests'
New-Item -ItemType Directory -Force $outputDirectory | Out-Null
& "$javaDirectory\javac.exe" -d $outputDirectory "$projectRoot\app\src\main\java\com\peter\myhealth\PurchaseVerifier.java" "$projectRoot\tests\PurchaseVerifierTest.java"
if($LASTEXITCODE -ne 0){throw 'Compilation failed'}
& "$javaDirectory\java.exe" -cp $outputDirectory com.peter.myhealth.PurchaseVerifierTest
if($LASTEXITCODE -ne 0){throw 'Receipt tests failed'}
