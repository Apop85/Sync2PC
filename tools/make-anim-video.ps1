# Erzeugt die Benachrichtigungs-Animation aus .claude\new_animation.mp4 (benoetigt ffmpeg im PATH).
#  1. Ausschnitt (Schleife) extrahieren, quadratisch zuschneiden
#  2. Wasserzeichen unten rechts entfernen
#  3. Dunklen Hintergrund + hellen Schein um die Figur transparent machen
#     (Flutfuellung vom Rand aus; die schwarze Umrisslinie der Figur stoppt die Fuellung)
#  4. Doppelte Bilder entfernen, verkleinern, ViewFlipper-Layout neu schreiben
# Ausgabe: res\drawable-nodpi\anim_dog_XX.png, res\layout\notif_dog_flipper.xml, Vorschau .build\anim_preview.png
#
# Aufruf: powershell -ExecutionPolicy Bypass -File tools\make-anim-video.ps1
param(
    [string]$Root = (Split-Path -Parent $PSScriptRoot),
    [int]$StartFrame = 13,          # erstes Bild der Schleife (1-basiert)
    [int]$EndFrame = 49,            # letztes Bild der Schleife (Bild 50 entspricht wieder Bild 13)
    [int]$Size = 320,               # Kantenlaenge der Ausgabebilder (seit 0.1.5; scharf auch in der App-Anzeige)
    [string]$Watermark = "1210,1335,1430,1430",  # x1,y1,x2,y2 im quadratisch zugeschnittenen Bild
    [int]$FillMax = 80,             # Helligkeit bis zu der Hintergrund/Schein gefuellt wird
    [int]$OutlineMax = 26,          # dunkler als das = Umrisslinie (stoppt die Fuellung)
    [int]$RingMax = 165,            # heller Schein direkt an der Figur bis zu dieser Helligkeit entfernen
    [int]$RingSteps = 4,            # wie viele Pixel Schein abgetragen werden
    [int]$GapErode = 12,            # eingeschlossene dunkle Luecken (zwischen den Beinen) entfernen, wenn mind. so dick (0 = aus; Halsband bleibt)
    [int]$HoleMax = 3000            # umschlossene transparente Loecher kleiner als so viele Pixel wieder deckend machen (0 = aus)
)
$ErrorActionPreference = 'Stop'
$video = Join-Path $Root ".claude\images\new_animation.mp4"
if (-not (Test-Path $video)) { $video = Join-Path $Root ".claude\new_animation.mp4" }
if (-not (Test-Path $video)) { throw "Nicht gefunden: $video" }
if (-not (Get-Command ffmpeg -ErrorAction SilentlyContinue)) { throw "ffmpeg nicht gefunden (PATH)" }
Add-Type -AssemblyName System.Drawing

Add-Type -ReferencedAssemblies System.Drawing -TypeDefinition @"
using System;
using System.Collections.Generic;
using System.Drawing;
using System.Drawing.Imaging;
using System.Runtime.InteropServices;
public static class Sync2PcVideoAnim {
    static byte[] Read(Bitmap b, out int stride) {
        var d = b.LockBits(new Rectangle(0,0,b.Width,b.Height), ImageLockMode.ReadOnly, PixelFormat.Format32bppArgb);
        stride = d.Stride; var buf = new byte[d.Stride*b.Height]; Marshal.Copy(d.Scan0, buf, 0, buf.Length); b.UnlockBits(d); return buf;
    }
    static Bitmap Write(byte[] buf, int w, int h, int stride) {
        var b = new Bitmap(w, h, PixelFormat.Format32bppArgb);
        var d = b.LockBits(new Rectangle(0,0,w,h), ImageLockMode.WriteOnly, PixelFormat.Format32bppArgb);
        Marshal.Copy(buf, 0, d.Scan0, buf.Length); b.UnlockBits(d); return b;
    }
    // Hintergrund entfernen; liefert neues Bild mit Alpha
    public static Bitmap Key(Bitmap src, int wx1, int wy1, int wx2, int wy2, int fillMax, int outlineMax, int ringMax, int ringSteps, int gapErode, int holeMax) {
        int w = src.Width, h = src.Height, stride;
        var buf = Read(src, out stride);
        var lum = new int[w*h];
        for (int y=0;y<h;y++) for (int x=0;x<w;x++) {
            int i = y*stride + x*4;
            lum[y*w+x] = (int)(0.299*buf[i+2] + 0.587*buf[i+1] + 0.114*buf[i]);
        }
        // Wasserzeichen wie Hintergrund behandeln
        for (int y=Math.Max(0,wy1); y<Math.Min(h,wy2); y++) for (int x=Math.Max(0,wx1); x<Math.Min(w,wx2); x++) lum[y*w+x] = 35;
        var bg = new bool[w*h];
        var q = new Queue<int>();
        Action<int> tryAdd = (p) => { if (!bg[p] && lum[p] > outlineMax && lum[p] <= fillMax) { bg[p] = true; q.Enqueue(p); } };
        for (int x=0;x<w;x++) { tryAdd(x); tryAdd((h-1)*w+x); }
        for (int y=0;y<h;y++) { tryAdd(y*w); tryAdd(y*w+w-1); }
        while (q.Count > 0) {
            int p = q.Dequeue(); int x = p % w, y = p / w;
            if (x>0) tryAdd(p-1); if (x<w-1) tryAdd(p+1); if (y>0) tryAdd(p-w); if (y<h-1) tryAdd(p+w);
        }
        // Vom Rand aus nicht erreichte Hintergrundluecken (z. B. dunkle Flaeche zwischen den Beinen, von der
        // Umrisslinie eingeschlossen) ebenfalls entfernen. Als Hintergrund gilt nur, was mindestens gapErode Pixel
        // dick dunkel (outlineMax < Helligkeit <= fillMax) ist; dadurch bleibt das schwarze Halsband erhalten.
        var isGap = new bool[w*h];
        if (gapErode > 0) {
            var rest = new bool[w*h];
            for (int p=0;p<w*h;p++) rest[p] = !bg[p] && lum[p] > outlineMax && lum[p] <= fillMax;
            var core = (bool[])rest.Clone();
            for (int s=0; s<gapErode; s++) {
                var nxt = new bool[w*h];
                for (int y=1;y<h-1;y++) for (int x=1;x<w-1;x++) {
                    int p = y*w+x;
                    if (!core[p]) continue;
                    nxt[p] = core[p-1] && core[p+1] && core[p-w] && core[p+w]
                          && core[p-w-1] && core[p-w+1] && core[p+w-1] && core[p+w+1];
                }
                core = nxt;
            }
            var gq = new Queue<int>();
            for (int p=0;p<w*h;p++) if (core[p]) { bg[p] = true; isGap[p] = true; gq.Enqueue(p); }
            while (gq.Count > 0) {
                int p = gq.Dequeue(); int x = p % w, y = p / w;
                if (x>0 && rest[p-1] && !bg[p-1]) { bg[p-1] = true; isGap[p-1] = true; gq.Enqueue(p-1); }
                if (x<w-1 && rest[p+1] && !bg[p+1]) { bg[p+1] = true; isGap[p+1] = true; gq.Enqueue(p+1); }
                if (y>0 && rest[p-w] && !bg[p-w]) { bg[p-w] = true; isGap[p-w] = true; gq.Enqueue(p-w); }
                if (y<h-1 && rest[p+w] && !bg[p+w]) { bg[p+w] = true; isGap[p+w] = true; gq.Enqueue(p+w); }
            }
        }
        // Hellen Schein an der Figur abtragen (stoppt an der schwarzen Umrisslinie)
        for (int s=0; s<ringSteps; s++) {
            var add = new List<int>();
            for (int y=1;y<h-1;y++) for (int x=1;x<w-1;x++) {
                int p = y*w+x; if (bg[p]) continue;
                if (lum[p] <= outlineMax || lum[p] > ringMax) continue;
                if (bg[p-1] || bg[p+1] || bg[p-w] || bg[p+w]) add.Add(p);
            }
            foreach (var p in add) bg[p] = true;
        }
        // Wasserzeichen sicher entfernen
        for (int y=Math.Max(0,wy1); y<Math.Min(h,wy2); y++) for (int x=Math.Max(0,wx1); x<Math.Min(w,wx2); x++) bg[y*w+x] = true;
        // Kleine, vollstaendig umschlossene Loecher in der Figur (Fell) wieder deckend machen
        if (holeMax > 0) {
            var seen = new bool[w*h];
            var comp = new List<int>();
            var cq = new Queue<int>();
            for (int s0=0; s0<w*h; s0++) {
                if (!bg[s0] || seen[s0]) continue;
                comp.Clear(); bool border = false, hasGap = false;
                seen[s0] = true; cq.Enqueue(s0);
                while (cq.Count > 0) {
                    int p = cq.Dequeue(); comp.Add(p); int x = p % w, y = p / w; if (isGap[p]) hasGap = true;
                    if (x==0 || y==0 || x==w-1 || y==h-1) border = true;
                    if (x>0 && bg[p-1] && !seen[p-1]) { seen[p-1] = true; cq.Enqueue(p-1); }
                    if (x<w-1 && bg[p+1] && !seen[p+1]) { seen[p+1] = true; cq.Enqueue(p+1); }
                    if (y>0 && bg[p-w] && !seen[p-w]) { seen[p-w] = true; cq.Enqueue(p-w); }
                    if (y<h-1 && bg[p+w] && !seen[p+w]) { seen[p+w] = true; cq.Enqueue(p+w); }
                }
                if (!border && !hasGap && comp.Count < holeMax) foreach (var p in comp) bg[p] = false;
            }
        }
        for (int y=0;y<h;y++) for (int x=0;x<w;x++) {
            int i = y*stride + x*4;
            if (bg[y*w+x]) { buf[i]=0; buf[i+1]=0; buf[i+2]=0; buf[i+3]=0; } else buf[i+3]=255;
        }
        return Write(buf, w, h, stride);
    }
    public static Rectangle Bounds(Bitmap b) {
        int stride; var buf = Read(b, out stride);
        int minX=b.Width,minY=b.Height,maxX=-1,maxY=-1;
        for (int y=0;y<b.Height;y++) for (int x=0;x<b.Width;x++) if (buf[y*stride+x*4+3]>0) {
            if(x<minX)minX=x; if(x>maxX)maxX=x; if(y<minY)minY=y; if(y>maxY)maxY=y; }
        return maxX<0 ? new Rectangle(0,0,b.Width,b.Height) : new Rectangle(minX,minY,maxX-minX+1,maxY-minY+1);
    }
    public static double Diff(Bitmap a, Bitmap b) {
        int sa, sb; var x = Read(a, out sa); var y = Read(b, out sb); double d=0;
        for (int i=0;i<x.Length;i++) d += Math.Abs(x[i]-y[i]); return d / x.Length;
    }
}
"@

# 1. Einzelbilder extrahieren (quadratisch zugeschnitten)
$tmp = Join-Path $Root ".build\anim_src"
if (Test-Path $tmp) { Remove-Item -Recurse -Force $tmp }
New-Item -ItemType Directory -Force $tmp | Out-Null
$probe = (& ffprobe -v error -select_streams v:0 -show_entries stream=width,height -of csv=p=0 $video).Trim().Split(',')
$vw = [int]$probe[0]; $vh = [int]$probe[1]; $side = [math]::Min($vw, $vh)
$cx = [int](($vw - $side) / 2); $cy = [int](($vh - $side) / 2)
$from = $StartFrame - 1; $to = $EndFrame - 1
& ffmpeg -v error -y -i $video -vf "select='between(n\,$from\,$to)',crop=${side}:${side}:${cx}:${cy}" -fps_mode passthrough (Join-Path $tmp "f_%03d.png")
if ($LASTEXITCODE -ne 0) { throw "ffmpeg fehlgeschlagen" }
$files = Get-ChildItem $tmp -Filter "f_*.png" | Sort-Object Name
Write-Host "$($files.Count) Bilder extrahiert ($side x $side)"

# 2.+3. Wasserzeichen + Hintergrund entfernen
$wm = $Watermark.Split(',') | ForEach-Object { [int]$_ }
$keyed = @()
foreach ($f in $files) {
    $src = New-Object System.Drawing.Bitmap -ArgumentList $f.FullName
    $keyed += [Sync2PcVideoAnim]::Key($src, $wm[0], $wm[1], $wm[2], $wm[3], $FillMax, $OutlineMax, $RingMax, $RingSteps, $GapErode, $HoleMax)
    $src.Dispose()
}

# Gemeinsamer Rahmen (Figur springt nicht)
$minX = 99999; $minY = 99999; $maxX = 0; $maxY = 0
foreach ($k in $keyed) {
    $bb = [Sync2PcVideoAnim]::Bounds($k)
    $minX = [math]::Min($minX, $bb.X); $minY = [math]::Min($minY, $bb.Y)
    $maxX = [math]::Max($maxX, $bb.Right); $maxY = [math]::Max($maxY, $bb.Bottom)
}
$cropW = $maxX - $minX; $cropH = $maxY - $minY; $box = [math]::Max($cropW, $cropH)

# 4. Verkleinern, Duplikate entfernen, speichern
$outDir = Join-Path $Root "app\src\main\res\drawable-nodpi"
New-Item -ItemType Directory -Force $outDir | Out-Null
Get-ChildItem $outDir -Filter "anim_dog_*.png" -ErrorAction SilentlyContinue | Remove-Item -Force
$small = @()
foreach ($k in $keyed) {
    $out = New-Object System.Drawing.Bitmap $Size, $Size
    $g = [System.Drawing.Graphics]::FromImage($out)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.Clear([System.Drawing.Color]::Transparent)
    $scale = $Size / $box; $dw = $cropW * $scale; $dh = $cropH * $scale
    $dest = New-Object System.Drawing.RectangleF ([float](($Size - $dw) / 2)), ([float](($Size - $dh) / 2)), ([float]$dw), ([float]$dh)
    $srcR = New-Object System.Drawing.RectangleF ([float]$minX), ([float]$minY), ([float]$cropW), ([float]$cropH)
    $g.DrawImage($k, $dest, $srcR, [System.Drawing.GraphicsUnit]::Pixel)
    $g.Dispose(); $k.Dispose()
    if ($small.Count -gt 0 -and [Sync2PcVideoAnim]::Diff($small[-1], $out) -lt 0.6) { $out.Dispose(); continue }
    $small += $out
}
$n = $small.Count
for ($i = 0; $i -lt $n; $i++) {
    $small[$i].Save((Join-Path $outDir ("anim_dog_{0:D2}.png" -f ($i + 1))), [System.Drawing.Imaging.ImageFormat]::Png)
}

# ViewFlipper-Layout: Intervall so, dass die Schleife so lange dauert wie im Video (24 fps)
$loopMs = [int](($EndFrame - $StartFrame + 1) * 1000 / 24)
$interval = [int][math]::Round($loopMs / $n)
$imgs = (1..$n | ForEach-Object { '    <ImageView android:layout_width="match_parent" android:layout_height="match_parent" android:scaleType="fitCenter" android:src="@drawable/anim_dog_{0:D2}" />' -f $_ }) -join "`n"
$xml = @"
<?xml version="1.0" encoding="utf-8"?>
<!-- Laufender Hund - automatisch erzeugt von tools/make-anim-video.ps1 aus .claude/new_animation.mp4
     ($n Bilder, $interval ms je Bild). Nicht von Hand bearbeiten.
     Benachrichtigungen koennen keine Videos/GIFs abspielen; ein ViewFlipper blaettert die Bilder durch
     und pausiert automatisch bei ausgeschaltetem Bildschirm. -->
<ViewFlipper xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:autoStart="true"
    android:flipInterval="$interval">
$imgs
</ViewFlipper>
"@
[IO.File]::WriteAllText((Join-Path $Root "app\src\main\res\layout\notif_dog_flipper.xml"), $xml, (New-Object System.Text.UTF8Encoding $false))

# Vorschau: alle Bilder auf hellem und dunklem Grund
$pv = 96
$preview = New-Object System.Drawing.Bitmap ($pv * $n), ($pv * 2)
$pg = [System.Drawing.Graphics]::FromImage($preview)
$pg.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$pg.Clear([System.Drawing.Color]::FromArgb(255, 250, 250, 250))
$pg.FillRectangle((New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 32, 33, 36))), 0, $pv, $preview.Width, $pv)
for ($i = 0; $i -lt $n; $i++) { $pg.DrawImage($small[$i], $i * $pv, 0, $pv, $pv); $pg.DrawImage($small[$i], $i * $pv, $pv, $pv, $pv); $small[$i].Dispose() }
$pg.Dispose()
$previewPath = Join-Path $Root ".build\anim_preview.png"
$preview.Save($previewPath, [System.Drawing.Imaging.ImageFormat]::Png); $preview.Dispose()
Remove-Item -Recurse -Force $tmp
Write-Host "$n Animationsbilder ($Size px, $interval ms) erzeugt. Vorschau: $previewPath"
