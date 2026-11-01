# Erzeugt die Launcher-Icons (adaptive icon foreground) aus .claude/icon.png
# Aufruf: powershell -ExecutionPolicy Bypass -File tools\make-icons.ps1
param([string]$Root = (Split-Path -Parent $PSScriptRoot))

Add-Type -AssemblyName System.Drawing
$src = Join-Path $Root ".claude\icon.png"
if (-not (Test-Path $src)) { throw "Icon nicht gefunden: $src" }
$res = Join-Path $Root "app\src\main\res"

# Adaptive Icon: 108dp Flaeche, sichtbarer Bereich ~72dp (66%). Motiv auf 64dp skalieren.
$densities = @{ "mdpi" = 1.0; "hdpi" = 1.5; "xhdpi" = 2.0; "xxhdpi" = 3.0; "xxxhdpi" = 4.0 }
$img = [System.Drawing.Image]::FromFile($src)
try {
    foreach ($d in $densities.Keys) {
        $f = $densities[$d]
        $canvas = [int](108 * $f)
        $motif = [int](66 * $f)
        $dir = Join-Path $res "mipmap-$d"
        New-Item -ItemType Directory -Force $dir | Out-Null
        $bmp = New-Object System.Drawing.Bitmap $canvas, $canvas
        $g = [System.Drawing.Graphics]::FromImage($bmp)
        $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
        $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
        $g.Clear([System.Drawing.Color]::Transparent)
        $off = [int](($canvas - $motif) / 2)
        $g.DrawImage($img, $off, $off, $motif, $motif)
        $g.Dispose()
        $bmp.Save((Join-Path $dir "ic_launcher_foreground.png"), [System.Drawing.Imaging.ImageFormat]::Png)
        $bmp.Dispose()
    }
} finally { $img.Dispose() }

# ---------------------------------------------------------------------------
# Benachrichtigungs-/Statusleisten-Icon: Android erlaubt nur einfarbig (weiss + Transparenz).
# Helles Fell -> weiss, dunkle Konturen (Umriss, Augen, Nase, Halsband) -> ausgespart.
Add-Type -ReferencedAssemblies System.Drawing -TypeDefinition @"
using System;
using System.Drawing;
using System.Drawing.Imaging;
using System.Runtime.InteropServices;
public static class Sync2PcIcon {
    public static Bitmap Silhouette(Bitmap src, int threshold) {
        var rect = new Rectangle(0, 0, src.Width, src.Height);
        var dst = new Bitmap(src.Width, src.Height, PixelFormat.Format32bppArgb);
        var s = src.LockBits(rect, ImageLockMode.ReadOnly, PixelFormat.Format32bppArgb);
        var d = dst.LockBits(rect, ImageLockMode.WriteOnly, PixelFormat.Format32bppArgb);
        int n = s.Stride * src.Height;
        var buf = new byte[n];
        Marshal.Copy(s.Scan0, buf, 0, n);
        for (int i = 0; i < n; i += 4) {
            int b = buf[i], g = buf[i + 1], r = buf[i + 2], a = buf[i + 3];
            double lum = 0.299 * r + 0.587 * g + 0.114 * b;
            // weicher Uebergang um die Schwelle (Kantenglaettung)
            double k = Math.Max(0, Math.Min(1, (lum - threshold) / 40.0));
            int na = (int)(a * k);
            buf[i] = 255; buf[i + 1] = 255; buf[i + 2] = 255; buf[i + 3] = (byte)na;
        }
        Marshal.Copy(buf, 0, d.Scan0, n);
        src.UnlockBits(s); dst.UnlockBits(d);
        return dst;
    }
}
"@

$notifSizes = @{ "mdpi" = 24; "hdpi" = 36; "xhdpi" = 48; "xxhdpi" = 72; "xxxhdpi" = 96 }
$orig = New-Object System.Drawing.Bitmap $src
try {
    # Erst auf Arbeitsgroesse bringen (feine Fellstruktur glaetten), dann Silhouette
    $work = New-Object System.Drawing.Bitmap 384, 384
    $g = [System.Drawing.Graphics]::FromImage($work)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.Clear([System.Drawing.Color]::Transparent)
    $g.DrawImage($orig, 0, 0, 384, 384)
    $g.Dispose()
    $sil = [Sync2PcIcon]::Silhouette($work, 70)
    $work.Dispose()
    foreach ($d in $notifSizes.Keys) {
        $px = $notifSizes[$d]
        $dir = Join-Path $res "drawable-$d"
        New-Item -ItemType Directory -Force $dir | Out-Null
        $bmp = New-Object System.Drawing.Bitmap $px, $px
        $g = [System.Drawing.Graphics]::FromImage($bmp)
        $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
        $g.Clear([System.Drawing.Color]::Transparent)
        $g.DrawImage($sil, 0, 0, $px, $px)
        $g.Dispose()
        $bmp.Save((Join-Path $dir "ic_notification.png"), [System.Drawing.Imaging.ImageFormat]::Png)
        $bmp.Dispose()
    }
    # Vorschau in gross (nur zur Kontrolle, nicht Teil der App)
    $preview = Join-Path $Root ".build\ic_notification_preview.png"
    New-Item -ItemType Directory -Force (Split-Path $preview) | Out-Null
    $sil.Save($preview, [System.Drawing.Imaging.ImageFormat]::Png)
    $sil.Dispose()
} finally { $orig.Dispose() }

# Alte Vektor-Version entfernen (gleicher Ressourcenname)
$oldVector = Join-Path $res "drawable\ic_notification.xml"
if (Test-Path $oldVector) { Remove-Item -Force $oldVector }

Write-Host "Icons erzeugt in $res"
