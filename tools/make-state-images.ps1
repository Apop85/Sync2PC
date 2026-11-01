# Bereitet die Zustandsbilder auf: .claude\wait_cleaned.png (wartet/pausiert) und .claude\idle_cleaned.png (alles aktuell).
# Transparenten Rand wegschneiden, laengste Seite auf -Size skalieren.
# Ausgabe: app\src\main\res\drawable-nodpi\dog_wait.png und dog_idle.png
# Aufruf: powershell -ExecutionPolicy Bypass -File tools\make-state-images.ps1
param(
    [string]$Root = (Split-Path -Parent $PSScriptRoot),
    [int]$Size = 256
)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
Add-Type -ReferencedAssemblies System.Drawing -TypeDefinition @"
using System; using System.Drawing; using System.Drawing.Imaging; using System.Runtime.InteropServices;
public static class Sync2PcState {
    public static Rectangle Bounds(Bitmap b) {
        var d = b.LockBits(new Rectangle(0,0,b.Width,b.Height), ImageLockMode.ReadOnly, PixelFormat.Format32bppArgb);
        var buf = new byte[d.Stride*b.Height]; Marshal.Copy(d.Scan0, buf, 0, buf.Length); int stride = d.Stride; b.UnlockBits(d);
        int minX=b.Width,minY=b.Height,maxX=-1,maxY=-1;
        for (int y=0;y<b.Height;y++) for (int x=0;x<b.Width;x++) if (buf[y*stride+x*4+3]>8) {
            if(x<minX)minX=x; if(x>maxX)maxX=x; if(y<minY)minY=y; if(y>maxY)maxY=y; }
        return maxX<0 ? new Rectangle(0,0,b.Width,b.Height) : new Rectangle(minX,minY,maxX-minX+1,maxY-minY+1);
    }
}
"@
$outDir = Join-Path $Root "app\src\main\res\drawable-nodpi"
New-Item -ItemType Directory -Force $outDir | Out-Null
foreach ($pair in @(@("wait_cleaned.png", "dog_wait.png"), @("idle_cleaned.png", "dog_idle.png"))) {
    $src = Join-Path $Root (".claude\" + $pair[0])
    if (-not (Test-Path $src)) { Write-Warning "Fehlt: $src"; continue }
    $img = New-Object System.Drawing.Bitmap -ArgumentList $src
    $bb = [Sync2PcState]::Bounds($img)
    $scale = $Size / [math]::Max($bb.Width, $bb.Height)
    $w = [int][math]::Round($bb.Width * $scale); $h = [int][math]::Round($bb.Height * $scale)
    $out = New-Object System.Drawing.Bitmap $w, $h
    $g = [System.Drawing.Graphics]::FromImage($out)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.Clear([System.Drawing.Color]::Transparent)
    $g.DrawImage($img, (New-Object System.Drawing.Rectangle 0, 0, $w, $h), $bb, [System.Drawing.GraphicsUnit]::Pixel)
    $g.Dispose(); $img.Dispose()
    $out.Save((Join-Path $outDir $pair[1]), [System.Drawing.Imaging.ImageFormat]::Png); $out.Dispose()
    Write-Host "$($pair[1]): ${w}x${h}"
}
