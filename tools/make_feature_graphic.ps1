Add-Type -AssemblyName System.Drawing

function Add-RoundedRect(
    [System.Drawing.Drawing2D.GraphicsPath]$path,
    [int]$x, [int]$y, [int]$w, [int]$h, [int]$r
) {
    $d = [Math]::Min($r * 2, [Math]::Min($w, $h))
    $path.AddArc($x, $y, $d, $d, 180, 90)
    $path.AddArc($x + $w - $d, $y, $d, $d, 270, 90)
    $path.AddArc($x + $w - $d, $y + $h - $d, $d, $d, 0, 90)
    $path.AddArc($x, $y + $h - $d, $d, $d, 90, 90)
    $path.CloseFigure()
}

$out = Join-Path $PSScriptRoot "..\docs\feature-graphic.jpg"
$shotPath = Join-Path $PSScriptRoot "..\docs\phase4-screens\01-home-top.png"
$logoPath = Join-Path $PSScriptRoot "..\docs\life-metric-logo.jpg"

$bmp = New-Object System.Drawing.Bitmap 1024, 500
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
$g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::ClearTypeGridFit
$g.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality

$mint = [System.Drawing.Color]::FromArgb(255, 209, 250, 229)
$cream = [System.Drawing.Color]::FromArgb(255, 254, 243, 199)
$peach = [System.Drawing.Color]::FromArgb(255, 255, 237, 213)
$ink = [System.Drawing.Color]::FromArgb(255, 15, 23, 42)
$muted = [System.Drawing.Color]::FromArgb(255, 71, 85, 105)
$brand = [System.Drawing.Color]::FromArgb(255, 13, 148, 136)
$white = [System.Drawing.Color]::White
$shadow = [System.Drawing.Color]::FromArgb(48, 15, 23, 42)

$grad = New-Object System.Drawing.Drawing2D.LinearGradientBrush (
    (New-Object System.Drawing.Point 0, 0),
    (New-Object System.Drawing.Point 1024, 500),
    $mint,
    $peach
)
$blend = New-Object System.Drawing.Drawing2D.ColorBlend 3
$blend.Colors = @($mint, $cream, $peach)
$blend.Positions = @(0.0, 0.45, 1.0)
$grad.InterpolationColors = $blend
$g.FillRectangle($grad, 0, 0, 1024, 500)

$shot = [System.Drawing.Image]::FromFile($shotPath)
$logo = [System.Drawing.Image]::FromFile($logoPath)

$g.DrawImage($logo, 48, 78, 96, 96)

$fontTitle = New-Object System.Drawing.Font("Segoe UI", 28, [System.Drawing.FontStyle]::Bold)
$fontTag = New-Object System.Drawing.Font("Segoe UI", 13, [System.Drawing.FontStyle]::Regular)
$fontFine = New-Object System.Drawing.Font("Segoe UI", 11, [System.Drawing.FontStyle]::Bold)
$brushInk = New-Object System.Drawing.SolidBrush $ink
$brushMuted = New-Object System.Drawing.SolidBrush $muted
$brushBrand = New-Object System.Drawing.SolidBrush $brand

$g.DrawString("Life Metric", $fontTitle, $brushInk, 48, 186)
$g.DrawString("Blood pressure, weight,", $fontTag, $brushMuted, 48, 240)
$g.DrawString("and workouts on your phone.", $fontTag, $brushMuted, 48, 266)
$g.DrawString("No account. No cloud.", $fontFine, $brushBrand, 48, 314)

$phoneX = 678
$phoneY = 20
$phoneW = 300
$phoneH = 460
$radius = 38

$shadowPath = New-Object System.Drawing.Drawing2D.GraphicsPath
Add-RoundedRect $shadowPath ($phoneX + 10) ($phoneY + 12) $phoneW $phoneH $radius
$shadowBrush = New-Object System.Drawing.SolidBrush $shadow
$g.FillPath($shadowBrush, $shadowPath)

$bezelPath = New-Object System.Drawing.Drawing2D.GraphicsPath
Add-RoundedRect $bezelPath $phoneX $phoneY $phoneW $phoneH $radius
$whiteBrush = New-Object System.Drawing.SolidBrush $white
$g.FillPath($whiteBrush, $bezelPath)

$inset = 8
$screenX = $phoneX + $inset
$screenY = $phoneY + $inset
$screenW = $phoneW - (2 * $inset)
$screenH = $phoneH - (2 * $inset)
$screenPath = New-Object System.Drawing.Drawing2D.GraphicsPath
Add-RoundedRect $screenPath $screenX $screenY $screenW $screenH 30

$oldClip = $g.Clip.Clone()
$g.SetClip($screenPath)
$destH = [int]($shot.Height * ($screenW / $shot.Width))
$g.DrawImage($shot, $screenX, $screenY, $screenW, $destH)
$g.Clip = $oldClip

$pen = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(255, 226, 232, 240), 1.5)
$g.DrawPath($pen, $bezelPath)

$codec = [System.Drawing.Imaging.ImageCodecInfo]::GetImageEncoders() | Where-Object { $_.MimeType -eq "image/jpeg" }
$ep = New-Object System.Drawing.Imaging.EncoderParameters 1
$ep.Param[0] = New-Object System.Drawing.Imaging.EncoderParameter ([System.Drawing.Imaging.Encoder]::Quality, [long]95)
$bmp.Save($out, $codec, $ep)

$g.Dispose()
$bmp.Dispose()
$shot.Dispose()
$logo.Dispose()
$fontTitle.Dispose()
$fontTag.Dispose()
$fontFine.Dispose()
$brushInk.Dispose()
$brushMuted.Dispose()
$brushBrand.Dispose()
$grad.Dispose()
$whiteBrush.Dispose()
$shadowBrush.Dispose()
$pen.Dispose()
$shadowPath.Dispose()
$bezelPath.Dispose()
$screenPath.Dispose()
$oldClip.Dispose()

Write-Output "Wrote $out"
