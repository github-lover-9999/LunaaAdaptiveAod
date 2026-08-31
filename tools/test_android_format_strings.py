from pathlib import Path
import re
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / 'app/src/main/java'
STRINGS = ROOT / 'app/src/main/res/values/strings.xml'

# Java Formatter syntax accepted by Android Resources#getString(id, formatArgs...).
FORMAT_SPEC = re.compile(
    r'%(?:\d+\$)?[-#+ 0,(<]*\d*(?:\.\d+)?[tT]?[bBhHsScCdoxXeEfgGaA%n]'
)

java = '\n'.join(p.read_text(encoding='utf-8') for p in SRC.rglob('*.java'))
formatted_names = set(re.findall(r'getString\(\s*R\.string\.([A-Za-z0-9_]+)\s*,', java))

root = ET.parse(STRINGS).getroot()
strings = {node.attrib['name']: ''.join(node.itertext()) for node in root.findall('string')}

errors = []
for name in sorted(formatted_names):
    value = strings.get(name)
    if value is None:
        errors.append(f'{name}: resource missing')
        continue

    i = 0
    while i < len(value):
        if value[i] != '%':
            i += 1
            continue
        if value.startswith('%%', i):
            i += 2
            continue
        match = FORMAT_SPEC.match(value, i)
        if match is None:
            errors.append(f'{name}: stray/invalid % at offset {i}: {value!r}')
            break
        i = match.end()

if errors:
    raise AssertionError('\n'.join(errors))

print(f'PASS Android format strings ({len(formatted_names)} formatted resources)')
