// LeoSync-Import: Hilfsfunktionen fuer tools\import\LeoSync-Import.ps1
// (gleiche Regeln wie die App: Datum, Laenderbestimmung, Ordnernamen, Teil-Hash)
using System;
using System.Collections.Generic;
using System.Drawing;
using System.Globalization;
using System.IO;
using System.Security.Cryptography;
using System.Text;
using System.Text.RegularExpressions;
using System.Web.Script.Serialization;

public class LeoGeo {
    class Shape { public string Iso; public string Name; public List<double[]> Rings = new List<double[]>();
                  public double MinX = double.MaxValue, MinY = double.MaxValue, MaxX = double.MinValue, MaxY = double.MinValue; }
    readonly List<Shape> shapes = new List<Shape>();
    public readonly HashSet<string> Names = new HashSet<string>(StringComparer.OrdinalIgnoreCase);
    public bool Available { get { return shapes.Count > 0; } }

    public LeoGeo(string path) {
        if (string.IsNullOrEmpty(path) || !File.Exists(path)) return;
        var ser = new JavaScriptSerializer(); ser.MaxJsonLength = int.MaxValue; ser.RecursionLimit = 1000;
        var root = (Dictionary<string, object>)ser.DeserializeObject(File.ReadAllText(path, Encoding.UTF8));
        foreach (var fo in (object[])root["features"]) {
            var f = fo as Dictionary<string, object>; if (f == null) continue;
            object po, go;
            if (!f.TryGetValue("properties", out po) || !f.TryGetValue("geometry", out go)) continue;
            var props = po as Dictionary<string, object>; var geom = go as Dictionary<string, object>;
            if (props == null || geom == null) continue;
            string iso = Prop(props, "ISO_A2_EH"); if (iso == null || iso.Length != 2) iso = Prop(props, "ISO_A2");
            if (iso == null || iso.Length != 2) iso = "";
            string name = Prop(props, "NAME_DE"); if (string.IsNullOrEmpty(name)) name = Prop(props, "NAME");
            if (string.IsNullOrEmpty(name)) continue;
            Names.Add(name);
            var type = geom["type"] as string; var coords = geom["coordinates"] as object[];
            if (coords == null) continue;
            if (type == "Polygon") AddPoly(iso, name, coords);
            else if (type == "MultiPolygon") foreach (var p in coords) AddPoly(iso, name, (object[])p);
        }
    }

    static string Prop(Dictionary<string, object> p, string k) {
        foreach (var kv in p) if (string.Equals(kv.Key, k, StringComparison.OrdinalIgnoreCase)) return kv.Value as string;
        return null;
    }

    void AddPoly(string iso, string name, object[] rings) {
        var s = new Shape { Iso = iso.ToUpperInvariant(), Name = name };
        bool outer = true;
        foreach (var ro in rings) {
            var pts = (object[])ro; var arr = new double[pts.Length * 2];
            for (int i = 0; i < pts.Length; i++) {
                var pt = (object[])pts[i];
                double x = Convert.ToDouble(pt[0], CultureInfo.InvariantCulture), y = Convert.ToDouble(pt[1], CultureInfo.InvariantCulture);
                arr[2 * i] = x; arr[2 * i + 1] = y;
                if (outer) { s.MinX = Math.Min(s.MinX, x); s.MaxX = Math.Max(s.MaxX, x); s.MinY = Math.Min(s.MinY, y); s.MaxY = Math.Max(s.MaxY, y); }
            }
            s.Rings.Add(arr); outer = false;
        }
        shapes.Add(s);
    }

    /** Liefert {ISO, Name} oder null */
    public string[] Lookup(double lat, double lon) {
        foreach (var s in shapes) {
            if (lon < s.MinX || lon > s.MaxX || lat < s.MinY || lat > s.MaxY) continue;
            bool inside = false;
            foreach (var r in s.Rings) if (RingContains(r, lon, lat)) inside = !inside;
            if (inside) return new[] { s.Iso, s.Name };
        }
        // knapp ausserhalb (Strand, Boot): naechstes Land innerhalb ~25 km
        double kx = Math.Cos(lat * Math.PI / 180), best = 0.25; Shape bestS = null;
        foreach (var s in shapes) {
            if (lon < s.MinX - best / kx || lon > s.MaxX + best / kx || lat < s.MinY - best || lat > s.MaxY + best) continue;
            foreach (var r in s.Rings) for (int i = 0; i < r.Length / 2 - 1; i++) {
                double d = SegDist(lon * kx, lat, r[2 * i] * kx, r[2 * i + 1], r[2 * i + 2] * kx, r[2 * i + 3]);
                if (d < best) { best = d; bestS = s; }
            }
        }
        return bestS == null ? null : new[] { bestS.Iso, bestS.Name };
    }

    static bool RingContains(double[] r, double x, double y) {
        bool c = false; int n = r.Length / 2, j = n - 1;
        for (int i = 0; i < n; i++) {
            double xi = r[2 * i], yi = r[2 * i + 1], xj = r[2 * j], yj = r[2 * j + 1];
            if ((yi > y) != (yj > y) && x < (xj - xi) * (y - yi) / (yj - yi) + xi) c = !c;
            j = i;
        }
        return c;
    }

    static double SegDist(double px, double py, double ax, double ay, double bx, double by) {
        double dx = bx - ax, dy = by - ay, len2 = dx * dx + dy * dy;
        double t = len2 == 0 ? 0 : ((px - ax) * dx + (py - ay) * dy) / len2;
        t = Math.Max(0, Math.Min(1, t));
        double cx = ax + t * dx - px, cy = ay + t * dy - py;
        return Math.Sqrt(cx * cx + cy * cy);
    }
}

public static class LeoMedia {
    const int CHUNK = 64 * 1024;

    /** EXIF-Aufnahmedatum + GPS aus JPEG/TIFF/PNG. Datum als "yyyy-MM" oder null. */
    public static string ReadExif(string path, out double lat, out double lon) {
        lat = double.NaN; lon = double.NaN;
        try {
            using (var fs = new FileStream(path, FileMode.Open, FileAccess.Read, FileShare.ReadWrite))
            using (var img = Image.FromStream(fs, false, false)) {
                var ids = new HashSet<int>(img.PropertyIdList);
                string ym = null;
                foreach (int tag in new[] { 0x9003, 0x9004, 0x0132 }) {
                    if (!ids.Contains(tag)) continue;
                    var s = Encoding.ASCII.GetString(img.GetPropertyItem(tag).Value).TrimEnd('\0', ' ');
                    var m = Regex.Match(s, @"^(\d{4})[:\-](\d{2})");
                    if (m.Success) {
                        int y = int.Parse(m.Groups[1].Value), mo = int.Parse(m.Groups[2].Value);
                        if (y >= 1980 && y <= 2100 && mo >= 1 && mo <= 12) { ym = y.ToString("0000") + "-" + mo.ToString("00"); break; }
                    }
                }
                if (ids.Contains(0x0002) && ids.Contains(0x0004)) {
                    double la = Dms(img.GetPropertyItem(0x0002).Value), lo = Dms(img.GetPropertyItem(0x0004).Value);
                    string latRef = ids.Contains(0x0001) ? Encoding.ASCII.GetString(img.GetPropertyItem(0x0001).Value).Trim('\0', ' ') : "N";
                    string lonRef = ids.Contains(0x0003) ? Encoding.ASCII.GetString(img.GetPropertyItem(0x0003).Value).Trim('\0', ' ') : "E";
                    if (latRef.StartsWith("S")) la = -la;
                    if (lonRef.StartsWith("W")) lo = -lo;
                    if (!double.IsNaN(la) && !double.IsNaN(lo) && !(la == 0 && lo == 0)) { lat = la; lon = lo; }
                }
                return ym;
            }
        } catch { return null; }
    }

    static double Dms(byte[] v) {
        if (v == null || v.Length < 24) return double.NaN;
        double r = 0, div = 1;
        for (int i = 0; i < 3; i++) {
            uint num = BitConverter.ToUInt32(v, i * 8), den = BitConverter.ToUInt32(v, i * 8 + 4);
            if (den == 0) return double.NaN;
            r += (double)num / den / div; div *= 60;
        }
        return r;
    }

    /** Datum im Dateinamen (IMG_20260614_..., WhatsApp Image 2026-06-14 ..., Unix-ms) -> "yyyy-MM" oder null */
    public static string DateFromName(string name) {
        int maxYear = DateTime.Now.Year + 1;
        foreach (Match m in Regex.Matches(name, @"(?<!\d)((?:19|20)\d{2})[-_.]?(0[1-9]|1[0-2])[-_.]?(0[1-9]|[12]\d|3[01])")) {
            int y = int.Parse(m.Groups[1].Value);
            if (y >= 1990 && y <= maxYear) return y.ToString("0000") + "-" + m.Groups[2].Value;
        }
        var e = Regex.Match(name, @"(?<!\d)(1[3-9]\d{11})(?!\d)");
        if (e.Success) {
            var dt = DateTimeOffset.FromUnixTimeMilliseconds(long.Parse(e.Groups[1].Value)).LocalDateTime;
            if (dt.Year >= 2011 && dt.Year <= maxYear) return dt.ToString("yyyy-MM");
        }
        return null;
    }

    /** Gleicher Teil-Hash wie die App: SHA-256 ueber Groesse (big endian) + 64 KB Anfang/Mitte/Ende */
    public static string PartialHash(string path, long size) {
        using (var fs = new FileStream(path, FileMode.Open, FileAccess.Read, FileShare.ReadWrite, 1 << 16))
        using (var sha = SHA256.Create()) {
            var sb = BitConverter.GetBytes(size); if (BitConverter.IsLittleEndian) Array.Reverse(sb);
            sha.TransformBlock(sb, 0, 8, null, 0);
            var regions = new List<long[]>();
            if (size <= 3L * CHUNK) regions.Add(new long[] { 0, size });
            else { regions.Add(new long[] { 0, CHUNK }); regions.Add(new long[] { size / 2 - CHUNK / 2, CHUNK }); regions.Add(new long[] { size - CHUNK, CHUNK }); }
            foreach (var r in regions) {
                var buf = new byte[r[1]]; fs.Position = r[0];
                int read = 0; while (read < buf.Length) { int n = fs.Read(buf, read, buf.Length - read); if (n <= 0) break; read += n; }
                sha.TransformBlock(buf, 0, read, null, 0);
            }
            sha.TransformFinalBlock(new byte[0], 0, 0);
            var h = new StringBuilder(); foreach (var b in sha.Hash) h.Append(b.ToString("x2")); return h.ToString();
        }
    }
}

public static class LeoNames {
    static readonly Regex MonthRx = new Regex(@"(?<!\d)(0[1-9]|1[0-2])(?!\d)");
    static readonly Regex Invalid = new Regex("[\\\\/:*?\"<>|\\x00-\\x1F]");

    public static int MonthOf(string folder) { var m = MonthRx.Match(folder); return m.Success ? int.Parse(m.Groups[1].Value) : 0; }

    public static string Sanitize(string n) { var s = Invalid.Replace(n, "_").Trim().TrimEnd('.', ' '); return s.Length == 0 ? "_" : s; }

    public static string DefaultName(int month, string country) {
        var m = month.ToString("00"); return string.IsNullOrEmpty(country) ? m : Sanitize(m + " - " + country);
    }

    /** "06" oder "06 - <Land>" = unveraendert von der App erzeugt (kein Kandidat fuer Rueckfragen) */
    public static bool IsGenerated(string folder, HashSet<string> countries) {
        if (folder.Length == 2 && MonthOf(folder) > 0) return true;
        var m = Regex.Match(folder, @"^(0[1-9]|1[0-2]) - (.+)$"); if (!m.Success) return false;
        foreach (var c in countries) if (string.Equals(Sanitize(c), m.Groups[2].Value, StringComparison.OrdinalIgnoreCase)) return true;
        return false;
    }

    public static string ConflictName(string file, int n) {
        int dot = file.LastIndexOf('.');
        return dot <= 0 ? file + "_" + n : file.Substring(0, dot) + "_" + n + file.Substring(dot);
    }

    /** bild.jpg passt zu bild.jpg, BILD.JPG, bild_2.jpg ... */
    public static bool SameBaseName(string existing, string original) {
        if (string.Equals(existing, original, StringComparison.OrdinalIgnoreCase)) return true;
        int dot = original.LastIndexOf('.');
        string b = dot > 0 ? original.Substring(0, dot) : original, e = dot > 0 ? original.Substring(dot) : "";
        return Regex.IsMatch(existing, "^" + Regex.Escape(b) + @"_\d+" + Regex.Escape(e) + "$", RegexOptions.IgnoreCase);
    }
}
