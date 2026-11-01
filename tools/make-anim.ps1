# Zerlegt .claude\gif_frames.png (4x4 Einzelbilder) in Animationsbilder fuer die Fortschritts-Benachrichtigung.
# Entfernt den halbtransparenten Schleier/Rauschen um die Figuren und schneidet alle Bilder einheitlich zu.
# Ausgabe: app\src\main\res\drawable-nodpi\anim_dog_01..16.png  (+ Vorschau .build\anim_preview.png)
# Aufruf: powershell -ExecutionPolicy Bypass -File tools\make-anim.ps1 [-Lo 150] [-Hi 235] [-Size 128]
param(
    [string]$Root = (Split-Path -Parent $PSScriptRoot),
    [int]$Lo = 150,     # Alpha darunter -> komplett transparent
    [int]$Hi = 235,     # Alpha darueber -> voll deckend (dazwischen weicher Uebergang)
    [int]$Size = 128,   # Kantenlaenge der Ausgabebilder in Pixel
    [int]$Cols = 4,
    [int]$Rows = 4
)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
Add-Type -ReferencedAssemblies System.Drawing -TypeDefinition @"
using System;
using System.Drawing;
using System.Drawing.Imaging;
using System.Runtime.InteropServices;
public static class Sync2PcAnim {
    // Alpha neu abbilden: < lo -> 0, > hi -> 255, dazwischen linear. Farbe bleibt.
    public static void CleanAlpha(Bitmap bmp, int lo, int hi) {
        var rect = new Rectangle(0, 0, bmp.Width, bmp.Height);
        var d = bmp.LockBits(rect, ImageLockMode.ReadWrite, PixelFormat.Format32bppArgb);
        int n = d.Stride * bmp.Height;
        var buf = new byte[n];
        Marshal.Copy(d.Scan0, buf, 0, n);
        for (int i = 0; i < n; i += 4) {
            int a = buf[i + 3];
            int na = a <= lo ? 0 : (a >= hi ? 255 : (a - lo) * 255 / (hi - lo));
            buf[i + 3] = (byte)na;
            if (na == 0) { buf[i] = 0; buf[i + 1] = 0; buf[i + 2] = 0; }
        }
        Marshal.Copy(buf, 0, d.Scan0, n);
        bmp.UnlockBits(d);
    }
    // Begrenzungsrahmen der deckenden Pixel (Alpha > 0)
    public static Rectangle Bounds(Bitmap bmp) {
        var rect = new Rectangle(0, 0, bmp.Width, bmp.Height);
        var d = bmp.LockBits(rect, ImageLockMode.ReadOnly, PixelFormat.Format32bppArgb);
        int n = d.Stride * bmp.Height;
        var buf = new byte[n];
        Marshal.Copy(d.Scan0, buf, 0, n);
        bmp.UnlockBits(d);
        int minX = bmp.Width, minY = bmp.Height, maxX = -1, maxY = -1;
        for (int y = 0; y < bmp.Height; y++) for (int x = 0; x < bmp.Width; x++) {
            if (buf[y * d.Stride + x * 4 + 3] > 0) {
                if (x < minX) minX = x; if (x > maxX) maxX = x;
                if (y < minY) minY = y; if (y > maxY) maxY = y;
            }
        }
        if (maxX < 0) return new Rectangle(0, 0, bmp.Width, bmp.Height);
        return new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }
}
"@

$src = Join-Path $Root ".claude\gif_frames.png"
if (-not (Test-Path $src)) { throw "Nicht gefunden: $src" }
$sheet = New-Object System.Drawing.Bitmap -ArgumentList $src
$cellW = $sheet.Width / $Cols
$cellH = $sheet.Height / $Rows

# 1. Zellen ausschneiden und saeubern
$cells = @()
for ($r = 0; $r -lt $Rows; $r++) {
    for ($c = 0; $c -lt $Cols; $c++) {
        $x = [int][math]::Round($c * $cellW); $y = [int][math]::Round($r * $cellH)
        $w = [int][math]::Round(($c + 1) * $cellW) - $x; $h = [int][math]::Round(($r + 1) * $cellH) - $y
        $cell = $sheet.Clone((New-Object System.Drawing.Rectangle $x, $y, $w, $h), [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
        [Sync2PcAnim]::CleanAlpha($cell, $Lo, $Hi)
        $cells += $cell
    }
}
$sheet.Dispose()

# 2. Gemeinsamer Rahmen ueber alle Bilder (damit die Figur nicht springt)
$minX = 9999; $minY = 9999; $maxX = 0; $maxY = 0
foreach ($cell in $cells) {
    $bb = [Sync2PcAnim]::Bounds($cell)
    $minX = [math]::Min($minX, $bb.X); $minY = [math]::Min($minY, $bb.Y)
    $maxX = [math]::Max($maxX, $bb.Right); $maxY = [math]::Max($maxY, $bb.Bottom)
}
$cropW = $maxX - $minX; $cropH = $maxY - $minY
$side = [math]::Max($cropW, $cropH)

# 3. Quadratisch, zentriert, auf Zielgroesse skalieren
$outDir = Join-Path $Root "app\src\main\res\drawable-nodpi"
New-Item -ItemType Directory -Force $outDir | Out-Null
Get-ChildItem $outDir -Filter "anim_dog_*.png" -ErrorAction SilentlyContinue | Remove-Item -Force
$previewDir = Join-Path $Root ".build"
New-Item -ItemType Directory -Force $previewDir | Out-Null
$preview = New-Object System.Drawing.Bitmap ($Size * $cells.Count), ($Size * 2)
$pg = [System.Drawing.Graphics]::FromImage($preview)
$pg.Clear([System.Drawing.Color]::FromArgb(255, 245, 245, 245))
$pg.FillRectangle((New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 32, 33, 36))), 0, $Size, $preview.Width, $Size)

$i = 0
foreach ($cell in $cells) {
    $i++
    $out = New-Object System.Drawing.Bitmap $Size, $Size
    $g = [System.Drawing.Graphics]::FromImage($out)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $g.Clear([System.Drawing.Color]::Transparent)
    $scale = $Size / $side
    $dw = $cropW * $scale; $dh = $cropH * $scale
    $dest = New-Object System.Drawing.RectangleF ([float](($Size - $dw) / 2)), ([float](($Size - $dh) / 2)), ([float]$dw), ([float]$dh)
    $srcRect = New-Object System.Drawing.RectangleF ([float]$minX), ([float]$minY), ([float]$cropW), ([float]$cropH)
    $g.DrawImage($cell, $dest, $srcRect, [System.Drawing.GraphicsUnit]::Pixel)
    $g.Dispose()
    $name = "anim_dog_{0:D2}.png" -f $i
    $out.Save((Join-Path $outDir $name), [System.Drawing.Imaging.ImageFormat]::Png)
    $pg.DrawImage($out, ($i - 1) * $Size, 0, $Size, $Size)
    $pg.DrawImage($out, ($i - 1) * $Size, $Size, $Size, $Size)
    $out.Dispose(); $cell.Dispose()
}
$pg.Dispose()
$previewPath = Join-Path $previewDir "anim_preview.png"
$preview.Save($previewPath, [System.Drawing.Imaging.ImageFormat]::Png)
$preview.Dispose()
Write-Host "$i Animationsbilder erzeugt in $outDir (Vorschau: $previewPath)"
