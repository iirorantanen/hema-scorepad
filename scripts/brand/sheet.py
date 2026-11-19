#!/usr/bin/env python3
"""Writes an HTML page that shows the icon and the themed icon at 192 px and 48 px, in a round and a rounded-square mask.

    python3 scripts/brand/sheet.py build/icon-sheet.html

The page is the picture a ticket asks for when the icon changes. It is opened in a browser or rendered with headless Chrome.
"""
import re
import sys
from pathlib import Path

BRAND = Path(__file__).resolve().parents[2] / 'docs/brand'
icon = (BRAND / 'icon.svg').read_text()
mono = re.search(r'id="fiore-mono" fill-rule="evenodd" d="([^"]+)"', (BRAND / 'fiore.svg').read_text()).group(1)
# The colours a launcher picks are its own; these two stand in for them.
themed = ('<svg xmlns="http://www.w3.org/2000/svg" viewBox="6 -4 288 288"><rect x="6" y="-4" width="288" height="288" fill="#2d3b4e"/>'
          f'<path fill="#c9daf5" fill-rule="evenodd" d="{mono}"/></svg>')


def cell(svg, px, is_round):
    sized = svg.replace('<svg ', f'<svg width="{px}" height="{px}" ', 1)
    return (f'<div class="cell"><div class="mask" style="width:{px}px;height:{px}px;border-radius:{"50%" if is_round else "22%"}">'
            f'{sized}</div>{px} px, {"round" if is_round else "rounded square"}</div>')


rows = ''.join(
    f'<h2>{name}</h2><div class="row">' + ''.join(cell(svg, px, r) for px in (192, 48) for r in (True, False)) + '</div>'
    for name, svg in (('The icon', icon), ("The themed icon, in one colour of the launcher's own", themed))
)
Path(sys.argv[1]).write_text(f'''<!DOCTYPE html><meta charset="utf-8"><title>HEMA Scorepad icon</title><style>
body {{ margin: 0; padding: 20px 24px; background: #e9e9ec; color: #1b1b1f; font-family: system-ui, sans-serif; width: 760px; }}
h2 {{ font-size: 15px; margin: 8px 0 12px; }}
.row {{ display: flex; align-items: flex-end; gap: 32px; margin-bottom: 20px; }}
.cell {{ text-align: center; font-size: 12px; color: #55555c; }}
.mask {{ overflow: hidden; margin: 0 auto 6px; }} .mask svg {{ display: block; }}
</style>{rows}''')
