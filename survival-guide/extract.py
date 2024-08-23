import pymupdf
from markdownify import markdownify as md
import re
import base64
from PIL import Image
import os

# https://armypubs.army.mil/epubs/DR_pubs/DR_a/pdf/web/ARN12086_ATP%203-50x21%20FINAL%20WEB%202.pdf
doc = pymupdf.open('ARN12086_ATP 3-50x21 FINAL WEB 2.pdf')
if not os.path.exists('output'):
    os.mkdir('output')

html = ""

for page in doc:
    # TODO: Image positions aren't retained - they are all at the bottom of the page
    # TODO: Tables aren't properly found
    content = page.get_text('xhtml')
    # tables = page.find_tables().tables
    # print([table.to_markdown() for table in tables])
    html += content

# Remove all img tags where height="8"
# Replace this with the section number (this only happens in chapter 2)
section_images = re.findall(r'<p><img[^>]*height="8"[^>]*></p>\n<p>', html)
for i, image in enumerate(section_images):
    section = f'2-{i + 1}. '
    html = html.replace(image, f'<p>{section}')

# Replace all images with webp images
i = 0
total_size = 0
images = re.findall(r'(<img[^>]*src="[^"]*"[^>]*>)', html)
for image in images:
    srcBase64 = re.search(r'data:image/png;base64,([^"]*)', image).group(1)
    # Base64 decode
    srcBytes = base64.b64decode(srcBase64)
    # Convert to webp
    with open(f'output/{i}.png', 'wb') as f:
        f.write(srcBytes)
    img = Image.open(f'output/{i}.png')
    # TODO: Most images can be smaller / lower quality
    img.thumbnail((1000, 400))
    img.save(f'output/{i}.webp', 'WEBP', quality=20)
    img.close()
    total_size += os.stat(f'output/{i}.webp').st_size
    # Delete the png
    os.remove(f'output/{i}.png')
    html = html.replace(image, f'<img style="height: auto; width: 100%;" src="{i}.webp" />')
    i += 1

print(f'Total size of images: {total_size / 1024} KB')

with open('output/original.html', 'w') as f:
    f.write(html)

# TODO: Fix tables

# Remove the footer
html = html.replace('&#x14;&#x1b;&#x3;6HSWHPEHU&#x3;&#x15;&#x13;&#x14;&#x1b;', '')

# Set up the lists
html = re.sub('<p>z (.*)</p>', r'<li>\1</li>', html)
html = re.sub('(&#x83;.*?)(?=<)', r'<ul>\1</ul>', html)
html = re.sub('&#x83;(.*?)(?=&#x83;|<)', r'<li>\1</li>', html)

# Replace unsupported characters
html = html.replace('&#x2019;', "'")
html = html.replace('&#xbd;', '1/2')
html = html.replace('&#x2013;', '-')
html = html.replace('&#x2014;', '-')
html = html.replace('&#xbc;', '1/4')
html = html.replace('&#x201c;', '"')
html = html.replace('&#x201d;', '"')
# TODO: This isn't working in the markdown
html = html.replace('&#xb0;', '&deg;')
html = html.replace('&#xe8;', 'è')
html = html.replace('&#x3;', '')
html = html.replace('This page intentionally left blank.', '')

# Remove the last 2 pages
html = html.split('<div id="page0">')[:-2]
html = '<div id="page0">'.join(html)

# Always remove the first 2 lines after a div line (they are page headers), except if they are a title
lines = html.split('\n')
for i in range(len(lines) - 2):
    if lines[i].startswith('<div'):
        if lines[i + 1].startswith('<p>'):
            lines[i + 1] = ''
        if lines[i + 2].startswith('<p>'):
            lines[i + 2] = ''
    
html = '\n'.join([line for line in lines if line.strip() != ''])

# Remove the introduction
html = html[html.find('<div id="page0">\n<h3><b>Chapter 1</b></h3>'):]

# Remove everything below references
html = html[:html.find('<div id="page0">\n<h2><b>References </b></h2>')]

# Make chapter titles h1
html = re.sub(r'<h3><b>(Chapter .+)</b></h3><h2><b>(.*)</b></h2>', r'<h1>\1: \2</h1>', html)
html = re.sub(r'<h3><b>(Appendix .+)</b></h3><h2><b>(.*)</b></h2>', r'<h1>\1: \2</h1>', html)

# Remove unneeded bold tags and upgrade h2 to h1
html = re.sub(r'<h2><b>(.*)</b></h2>', r'<h1>\1</h1>', html)
html = re.sub(r'<h3><b>(.*)</b></h3>', r'<h3>\1</h3>', html)

# Restore broken paragraphs
# TODO: Anything that isn't a <b>, - , or in the form ##-## should be part of the same paragraph. An image may also break a paragraph, so it should move up the sentence to be above the image.
html = re.sub(r'</p>\n</div>\n<div id="page0">\n<p>([a-z])', r' \1', html)

# TODO: Remove empty paragraphs
# TODO: Remove divs
# TODO: Generate a table of contents with links to each section
# TODO: Styling
# TODO: Some list items are not formatted correctly (linebreaks)

with open('output/guide.html', 'w') as f:
    f.write(html)

total_size += os.stat('output/guide.html').st_size

# markdown = md(html)

# # Replace more than 2 newlines with 2 newlines
# markdown = '\n'.join([line.strip() if len(line.strip()) == 0 else line for line in markdown.splitlines()])
# markdown = re.sub(r'\n{3,}', '\n\n', markdown)

# with open('output/guide.md', 'w') as f:
#     f.write(markdown)

print(f'Total size: {total_size / 1024} KB')