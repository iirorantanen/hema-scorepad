#!/usr/bin/env python3
"""Writes every file that holds a copy of Fiore's shapes, from the master docs/brand/fiore.svg.

    python3 scripts/brand/derive.py           writes the files
    python3 scripts/brand/derive.py --check   fails when a file differs from what would be written

social-preview.png is not written here: its text takes the font of the machine that renders it (README, Development).
"""
import re
import subprocess
import sys
import textwrap
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
BRAND = 'docs/brand'
DRAWABLE = 'app/src/main/res/drawable'
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
    android:viewportWidth="436"
    android:viewportHeight="436">
    <!-- Moves the centre of the face, (150,140) in the master, to the centre of the icon. -->
    <group android:translateX="68" android:translateY="78">
{body}
    </group>
</vector>
'''


files = {
    f'{BRAND}/fiore.svg': master,
    f'{BRAND}/icon.svg': f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="6 -4 288 288">
  <title>HEMA Scorepad icon: the head of Fiore the Boar on black</title>
  <!-- The icon: the head of fiore.svg on the black of the display. A launcher or a browser cuts the square to its own mask. -->
  <rect x="6" y="-4" width="288" height="288" fill="#000000"/>
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
        '    The feather reaches 132.6 units from the centre of the face. In a viewport of 436 units that is 32.8 dp,\n'
        '    inside the circle of radius 33 dp that no launcher mask cuts.',
        '\n'.join(drawable_path(el) for el in ET.fromstring(head)),
    ),
    f'{DRAWABLE}/ic_launcher_monochrome.xml': vector(
        '    The one-colour layer of the launcher icon, for themed icons: the path fiore-mono of docs/brand/fiore.svg.\n'
        '    Android draws it in one colour of its own and reads only where the path is filled. The viewport is that of the foreground.',
        f'        <path android:fillColor="#FFFFFF" android:fillType="evenOdd" android:pathData="{mono}" />',
    ),
}

if '--check' in sys.argv[1:]:
    stale = [name for name, text in files.items() if (ROOT / name).read_text() != text]
    for name in stale:
        print(f'{name} differs from what scripts/brand/derive.py writes', file=sys.stderr)
    if stale:
        sys.exit('Run: python3 scripts/brand/derive.py')
else:
    for name, text in files.items():
        (ROOT / name).write_text(text)
