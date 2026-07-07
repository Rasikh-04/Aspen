#!/usr/bin/env python3
"""
md_to_pdf.py  --  Reusable Markdown -> PDF converter.

Styling (fixed per project spec):
  - White background
  - Times New Roman font family (Liberation Serif used as metric-compatible
    fallback on Linux, which renders identically to Times New Roman)
  - 12pt body text
  - No dark-coloured sections: headings and table headers use light tints only,
    never dark fills. Borders are thin and grey.

Usage:
    python3 md_to_pdf.py input.md output.pdf
    python3 md_to_pdf.py input.md output.pdf "Optional Document Title"
"""

import sys
import markdown
from weasyprint import HTML

CSS = """
@page {
    size: A4;
    margin: 2cm 2cm 2cm 2cm;
    @bottom-center {
        content: counter(page);
        font-family: "Times New Roman", "Liberation Serif", serif;
        font-size: 10pt;
        color: #444444;
    }
}

* { background: transparent; }

body {
    font-family: "Times New Roman", "Liberation Serif", "Nimbus Roman", serif;
    font-size: 12pt;
    line-height: 1.45;
    color: #111111;
    background: #ffffff;
}

h1 {
    font-size: 19pt;
    color: #1a1a1a;
    border-bottom: 1px solid #c8c8c8;
    padding-bottom: 4px;
    margin-top: 18px;
    margin-bottom: 10px;
}

h2 {
    font-size: 15pt;
    color: #233044;            /* dark-ish text only, NO fill */
    background: #eef2f7;        /* very light tint */
    padding: 4px 8px;
    border-left: 4px solid #9db4d0;
    margin-top: 16px;
    margin-bottom: 8px;
}

h3 {
    font-size: 13pt;
    color: #2a2a2a;
    margin-top: 12px;
    margin-bottom: 5px;
}

h4 {
    font-size: 12pt;
    font-style: italic;
    color: #333333;
    margin-top: 10px;
    margin-bottom: 4px;
}

p { margin: 5px 0; }

ul, ol { margin: 5px 0 5px 0; padding-left: 22px; }
li { margin: 2px 0; }

strong { color: #000000; }

code {
    font-family: "DejaVu Sans Mono", monospace;
    font-size: 10.5pt;
    background: #f4f4f4;
    padding: 1px 3px;
    border-radius: 2px;
}

pre {
    background: #f7f7f7;
    border: 1px solid #dddddd;
    border-radius: 3px;
    padding: 8px 10px;
    font-size: 10pt;
    line-height: 1.3;
    white-space: pre-wrap;
    word-wrap: break-word;
}
pre code { background: transparent; padding: 0; font-size: 10pt; }

table {
    border-collapse: collapse;
    width: 100%;
    margin: 8px 0;
    font-size: 11pt;
}
th {
    background: #eef2f7;        /* light header tint, never dark */
    color: #1a1a1a;
    border: 1px solid #c8c8c8;
    padding: 5px 8px;
    text-align: left;
    font-weight: bold;
}
td {
    border: 1px solid #d6d6d6;
    padding: 5px 8px;
    vertical-align: top;
}
tr:nth-child(even) td { background: #fbfbfb; }

blockquote {
    margin: 8px 0;
    padding: 4px 12px;
    border-left: 3px solid #c8c8c8;
    color: #333333;
    background: #fafafa;
}

hr { border: none; border-top: 1px solid #d0d0d0; margin: 14px 0; }

a { color: #2a4a78; text-decoration: none; }
"""


def convert(md_path, pdf_path, title=None):
    with open(md_path, "r", encoding="utf-8") as f:
        text = f.read()

    html_body = markdown.markdown(
        text,
        extensions=["tables", "fenced_code", "toc", "sane_lists", "attr_list"],
    )

    title_html = f"<title>{title}</title>" if title else ""
    full_html = f"<!DOCTYPE html><html><head><meta charset='utf-8'>{title_html}</head><body>{html_body}</body></html>"

    HTML(string=full_html).write_pdf(pdf_path, stylesheets=[__import__("weasyprint").CSS(string=CSS)])
    print(f"Wrote {pdf_path}")


if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("Usage: python3 md_to_pdf.py input.md output.pdf [title]")
        sys.exit(1)
    convert(sys.argv[1], sys.argv[2], sys.argv[3] if len(sys.argv) > 3 else None)
