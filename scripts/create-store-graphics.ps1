$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
$outputDirectory=Join-Path (Split-Path -Parent $PSScriptRoot) 'store\graphics'
New-Item -ItemType Directory -Force $outputDirectory | Out-Null
function Draw-Pulse($graphics,[float]$left,[float]$top,[float]$size) {
  $shape=@(@(10,24),@(16,24),@(20,14),@(26,34),@(31,24),@(38,24),@(38,27),@(33,27),@(26,40),@(20,23),@(18,27),@(10,27))
  [System.Drawing.PointF[]]$points=$shape | ForEach-Object {[Drawing.PointF]::new($left+$_[0]*$size/48,$top+$_[1]*$size/48)}
  $brush=[Drawing.SolidBrush]::new([Drawing.ColorTranslator]::FromHtml('#C4EF83'))
  try{$graphics.FillPolygon($brush,$points)}finally{$brush.Dispose()}
}
$icon=[Drawing.Bitmap]::new(512,512,[Drawing.Imaging.PixelFormat]::Format32bppArgb)
$g=[Drawing.Graphics]::FromImage($icon);$g.SmoothingMode=[Drawing.Drawing2D.SmoothingMode]::AntiAlias;$g.Clear([Drawing.ColorTranslator]::FromHtml('#113F36'));Draw-Pulse $g 0 0 512
$icon.Save((Join-Path $outputDirectory 'icon-512.png'),[Drawing.Imaging.ImageFormat]::Png);$g.Dispose();$icon.Dispose()
$feature=[Drawing.Bitmap]::new(1024,500,[Drawing.Imaging.PixelFormat]::Format24bppRgb)
$g=[Drawing.Graphics]::FromImage($feature);$g.SmoothingMode=[Drawing.Drawing2D.SmoothingMode]::AntiAlias;$g.TextRenderingHint=[Drawing.Text.TextRenderingHint]::AntiAliasGridFit;$g.Clear([Drawing.ColorTranslator]::FromHtml('#113F36'))
$circle=[Drawing.SolidBrush]::new([Drawing.ColorTranslator]::FromHtml('#1C5142'));$g.FillEllipse($circle,710,-70,520,520);$circle.Dispose();Draw-Pulse $g 670 20 350
$white=[Drawing.SolidBrush]::new([Drawing.ColorTranslator]::FromHtml('#F4F7EE'));$lime=[Drawing.SolidBrush]::new([Drawing.ColorTranslator]::FromHtml('#C4EF83'));$muted=[Drawing.SolidBrush]::new([Drawing.ColorTranslator]::FromHtml('#C0D5C4'))
$heading=[Drawing.Font]::new('Segoe UI',44,[Drawing.FontStyle]::Bold);$sub=[Drawing.Font]::new('Segoe UI',26);$small=[Drawing.Font]::new('Segoe UI',15)
$g.DrawString('My Health',$heading,$white,70,115);$g.DrawString('Every step. A little better.',$sub,$lime,72,205);$g.DrawString('Daily steps  |  Personal health journal',$small,$muted,74,288);$g.DrawString('Ad-free, with a one-time Pro upgrade.',$small,$muted,74,324)
$feature.Save((Join-Path $outputDirectory 'feature-1024x500.png'),[Drawing.Imaging.ImageFormat]::Png)
$g.Dispose();$feature.Dispose();$heading.Dispose();$sub.Dispose();$small.Dispose();$white.Dispose();$lime.Dispose();$muted.Dispose()
Write-Output 'Created Google Play icon and feature graphic from the existing app identity. No personal records or screenshots included.'
