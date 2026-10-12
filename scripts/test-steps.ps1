$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$javaRoot = 'C:\Program Files\Android\Android Studio\jbr\bin'
$testOutput = Join-Path $projectRoot 'artifacts\step-tests'
New-Item -ItemType Directory -Force $testOutput | Out-Null
& "$javaRoot\javac.exe" -d $testOutput "$projectRoot\app\src\main\java\com\peter\myhealth\StepMath.java" "$projectRoot\tests\StepMathTest.java"
if ($LASTEXITCODE -ne 0) { throw 'Compilation failed' }
& "$javaRoot\java.exe" -cp $testOutput com.peter.myhealth.StepMathTest
if ($LASTEXITCODE -ne 0) { throw 'Step checks failed' }
