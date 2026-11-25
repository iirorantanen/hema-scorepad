#!/usr/bin/env python3
"""Writes every file that holds a copy of Fiore's shapes, from the master docs/brand/fiore.svg.

    python3 scripts/brand/derive.py           writes the files
    python3 scripts/brand/derive.py --check   fails when a file differs from what would be written

social-preview.png is not written here: its text takes the font of the machine that renders it (README, Development).
The PNG files of the icon are drawn by Png.java, which compares their pixels.
"""
import math
import re
import subprocess
import sys
import textwrap
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
BRAND = 'docs/brand'
DRAWABLE = 'app/src/main/res/drawable'
WEB = 'browser/src/jsMain/resources'
# The launcher icon and the icon SVG (#58): the head without its feather is centred and fills the circle of 33 dp that no launcher mask cuts.
# The ear tips, (70,62) and (230,62), and the chin, (150,220), lie on a circle of radius 99.25 around this centre.
CX, CY = 150, 120.75
VIEWPORT = 327  # the units across the 108 dp of a launcher layer, which puts the circle of 33 dp at 99.9 units
SIDE = VIEWPORT * 2 // 3  # the 72 dp in the middle of a layer: the square a launcher masks, and the icon SVG
FEATHER = '#f2c94c'
# The icon of the browser version: the file, its pixels, and the square of the master it shows as x, y and side.
# A home screen may cut a maskable icon down to the circle of 0.4 of its width around its centre.
# The head reaches 133 units from the centre of the face, and 0.4 of 336 is 134.
PNG = {
    'icon-192.png': (192, 6, -4, 288),
    'icon-512.png': (512, 6, -4, 288),
    'icon-maskable-512.png': (512, -18, -28, 336),
}
MONO = re.compile(r'(id="fiore-mono" fill-rule="evenodd" d=")[^"]*(")')
FONT = 'font-family="system-ui, sans-serif" font-weight="700"'

mono = subprocess.run(
    ['java', '-Djava.awt.headless=true', str(ROOT / 'scripts/brand/Mono.java')],
    check=True, capture_output=True, text=True,
).stdout.strip()
master = MONO.sub(lambda m: m.group(1) + mono + m.group(2), (ROOT / BRAND / 'fiore.svg').read_text())
head = re.search(r'  <g id="fiore-head">.*?</g>\n', master, re.S).group(0)
nested = textwrap.indent(head, '  ')


def path_data(el):
    """An SVG shape as the path data a vector drawable takes."""
    a = el.attrib
    if el.tag == 'polygon':
        return 'M' + 'L'.join(a['points'].split()) + 'Z'
    if el.tag == 'path':
        return a['d']
    cx, cy = float(a['cx']), float(a['cy'])
    rx, ry = (float(a['r']),) * 2 if el.tag == 'circle' else (float(a['rx']), float(a['ry']))
    return f'M{cx - rx:g},{cy:g}a{rx:g},{ry:g} 0 1,0 {2 * rx:g},0a{rx:g},{ry:g} 0 1,0 {-2 * rx:g},0Z'


def reach(spots):
    """How far the farthest of the points is from the centre of the icon, in dp."""
    return max(math.hypot(x - CX, y - CY) for x, y in spots) * 108 / VIEWPORT


def points(el):
    """Points on the outline of a shape of the head, with its stroke left out. A curve gives 16 of them."""
    a = el.attrib
    if el.tag in ('polygon', 'path'):
        assert not re.search(r'[A-Za-z]', re.sub('[MLQZ]', '', a.get('d', ''))), 'points() reads the path commands M, L, Q and Z only'
        found = []
        for command, numbers in re.findall(r'([MLQ]?)([^MLQZ]+)', a.get('points') or a['d']):
            n = [float(v) for v in re.findall(r'-?[\d.]+', numbers)]
            ends = list(zip(n[::2], n[1::2]))
            if command == 'Q':
                (x0, y0), (x1, y1), (x2, y2) = found[-1], *ends
                ends = [((1 - t) ** 2 * x0 + 2 * t * (1 - t) * x1 + t * t * x2, (1 - t) ** 2 * y0 + 2 * t * (1 - t) * y1 + t * t * y2)
                        for t in (i / 16 for i in range(1, 17))]
            found += ends
        return found
    cx, cy = float(a['cx']), float(a['cy'])
    rx, ry = (float(a['r']),) * 2 if el.tag == 'circle' else (float(a['rx']), float(a['ry']))
    return [(cx + rx * math.cos(math.radians(t)), cy + ry * math.sin(math.radians(t))) for t in range(360)]


shapes = list(ET.fromstring(head))
bare = [p for el in shapes if el.get('fill') != FEATHER for p in points(el)]
feather = [p for el in shapes if el.get('fill') == FEATHER for p in points(el)]
# The head without its feather touches the circle left of the centre, right of it and below it: it fills the circle and is centred in it.
for name, half in ('left', lambda x, y: x < CX), ('right', lambda x, y: x > CX), ('bottom', lambda x, y: y > CY):
    r = reach(p for p in bare if half(*p))
    assert 32 <= r <= 33, f'the {name} of the head without its feather reaches {r:.1f} dp, not 32 to 33 dp'
assert reach(feather) > 33, 'the feather stays inside the circle of 33 dp'


def drawable_path(el):
    a = el.attrib
    attrs = []
    if a.get('fill', 'none') != 'none':
        attrs.append(f'android:fillColor="{a["fill"].upper()}"')
    if 'stroke' in a:
        attrs.append(f'android:strokeColor="{a["stroke"].upper()}"')
        attrs.append(f'android:strokeWidth="{a["stroke-width"]}"')
    if 'stroke-linecap' in a:
        attrs.append(f'android:strokeLineCap="{a["stroke-linecap"]}"')
    attrs.append(f'android:pathData="{path_data(el)}"')
    return '        <path ' + ' '.join(attrs) + ' />'


def vector(comment, body):
    return f'''<?xml version="1.0" encoding="utf-8"?>
<!--
{comment}
    Written by scripts/brand/derive.py.
-->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="{VIEWPORT}"
    android:viewportHeight="{VIEWPORT}">
    <!-- Moves ({CX:g},{CY:g}) in the master, the centre of the head without its feather, to the centre of the icon. -->
    <group android:translateX="{VIEWPORT / 2 - CX:g}" android:translateY="{VIEWPORT / 2 - CY:g}">
{body}
    </group>
</vector>
'''


files = {
    f'{BRAND}/fiore.svg': master,
    f'{BRAND}/icon.svg': f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="{CX - SIDE / 2:g} {CY - SIDE / 2:g} {SIDE} {SIDE}">
  <title>HEMA Scorepad icon: the head of Fiore the Boar on black</title>
  <!-- The icon: the head of fiore.svg on the black of the display, in the square a launcher masks. The square cuts the tip of the feather, and a round mask cuts more of it. -->
  <rect x="{CX - SIDE / 2:g}" y="{CY - SIDE / 2:g}" width="{SIDE}" height="{SIDE}" fill="#000000"/>
{head}</svg>
''',
    f'{BRAND}/readme-header.svg': f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1000 260">
  <title>HEMA Scorepad: the head of Fiore the Boar between a Red score of 3 and a Blue score of 2, drawn as on the display</title>
  <!-- A picture of the display, not a screenshot. The head is a copy of fiore.svg. Fiore is not on the display of the scoreboard. -->
  <rect width="1000" height="260" rx="12" fill="#000000"/>
  <rect x="499" width="2" height="260" fill="#444444"/>
  <text x="250" y="195" text-anchor="middle" {FONT} font-size="190" fill="#FF5252">3</text>
  <text x="750" y="195" text-anchor="middle" {FONT} font-size="190" fill="#4DA3FF">2</text>
  <circle cx="500" cy="130" r="112" fill="#000000"/>
  <svg x="390" y="20" width="220" height="220" viewBox="6 -4 288 288">
{nested}  </svg>
</svg>
''',
    f'{BRAND}/social-preview.svg': f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1280 640">
  <title>HEMA Scorepad: a Red score of 3 and a Blue score of 2 on black, with the head of Fiore the Boar on the divider between them, and a bout timer reading of 1:24</title>
  <!-- The source of social-preview.png, the picture GitHub shows with a link to the repository. The head is a copy of fiore.svg. -->
  <rect width="1280" height="640" fill="#000000"/>
  <rect x="639" width="2" height="640" fill="#444444"/>
  <text x="300" y="470" text-anchor="middle" {FONT} font-size="400" fill="#FF5252">3</text>
  <text x="980" y="470" text-anchor="middle" {FONT} font-size="400" fill="#4DA3FF">2</text>
  <circle cx="640" cy="330" r="180" fill="#000000"/>
  <svg x="470" y="160" width="340" height="340" viewBox="6 -4 288 288">
{nested}  </svg>
  <rect x="380" y="26" width="520" height="92" fill="#000000"/>
  <text x="640" y="92" text-anchor="middle" {FONT} font-size="60" letter-spacing="3" fill="#FFFFFF">HEMA SCOREPAD</text>
  <rect x="560" y="548" width="160" height="80" fill="#000000"/>
  <text x="640" y="610" text-anchor="middle" {FONT} font-size="64" fill="#FFFFFF">1:24</text>
</svg>
''',
    f'{DRAWABLE}/ic_launcher_foreground.xml': vector(
        '    The foreground of the launcher icon: the head of Fiore the Boar, the shapes of docs/brand/fiore.svg as paths.\n'
        f'    The head without its feather reaches {reach(bare):.1f} dp from the centre of the icon,\n'
        f'    inside the circle of radius 33 dp that no launcher mask cuts. The feather reaches {reach(feather):.1f} dp, and a mask cuts its tip.',
        '\n'.join(drawable_path(el) for el in shapes),
    ),
    f'{DRAWABLE}/ic_launcher_monochrome.xml': vector(
        '    The one-colour layer of the launcher icon, for themed icons: the path fiore-mono of docs/brand/fiore.svg.\n'
        '    Android draws it in one colour of its own and reads only where the path is filled. The viewport is that of the foreground.',
        f'        <path android:fillColor="#FFFFFF" android:fillType="evenOdd" android:pathData="{mono}" />',
    ),
}


def png(*mode):
    """Writes the PNG files of the icon, or with --check names the ones that differ. False when one differs."""
    squares = [str(v) for name, square in PNG.items() for v in (f'{WEB}/{name}', *square)]
    java = ['java', '-Djava.awt.headless=true', 'scripts/brand/Png.java', *mode, f'{BRAND}/fiore.svg']
    return subprocess.run(java + squares, cwd=ROOT).returncode == 0


if '--check' in sys.argv[1:]:
    stale = [name for name, text in files.items() if (ROOT / name).read_text() != text]
    for name in stale:
        print(f'{name} differs from what scripts/brand/derive.py writes', file=sys.stderr)
    if not png('--check') or stale:
        sys.exit('Run: python3 scripts/brand/derive.py')
else:
    for name, text in files.items():
        (ROOT / name).write_text(text)
    if not png():
        sys.exit(1)
