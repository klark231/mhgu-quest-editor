#!/usr/bin/env python3
# Writes app/src/main/res/values*/strings.xml from i18n/*.py.
# Checks that every language has every key and the same %1$s/%1$d args as English.
import importlib.util
import re
import sys
from pathlib import Path

sys.dont_write_bytecode = True  # keeps __pycache__ out of the repo

HERE = Path(__file__).resolve().parent
RES = HERE.parent / "app/src/main/res"
NOT_TRANSLATED = {"app_name"}
ARG = re.compile(r"%\d+\$[sd]")

# file name -> values-* folder
LOCALES = {
    "ca": "ca", "da": "da", "de": "de", "el": "el", "es": "es", "fi": "fi", "fr": "fr", "hu": "hu",
    "id": "id", "it": "it", "ja": "ja", "ko": "ko", "lt": "lt", "nb": "nb", "nl": "nl", "pl": "pl",
    "pt_BR": "pt-rBR", "ro": "ro", "ru": "ru", "sv": "sv", "tr": "tr", "uk": "uk", "vi": "vi",
    "zh_CN": "zh-rCN", "zh_TW": "zh-rTW",
}


def load(name):
    spec = importlib.util.spec_from_file_location(name, HERE / f"{name}.py")
    m = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(m)
    return m


def esc(s):
    s = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    s = s.replace("\\", "\\\\").replace("'", "\\'").replace('"', '\\"').replace("\n", "\\n")
    if s[:1] in ("@", "?"):
        s = "\\" + s
    return s


def write(folder, strings, en):
    out = ['<?xml version="1.0" encoding="utf-8"?>', "<resources>"]
    for k in en:
        if folder != "values" and k in NOT_TRANSLATED:
            continue
        extra = ' translatable="false"' if k in NOT_TRANSLATED else ""
        out.append(f'    <string name="{k}"{extra}>{esc(strings[k])}</string>')
    out.append("</resources>")
    d = RES / folder
    d.mkdir(parents=True, exist_ok=True)
    (d / "strings.xml").write_text("\n".join(out) + "\n", encoding="utf-8")


def main():
    en = load("en").S
    write("values", en, en)
    bad = 0
    for name, folder in LOCALES.items():
        t = load(name).T
        for k in en:
            if k in NOT_TRANSLATED:
                continue
            if k not in t:
                print(f"{name}: missing {k}")
                bad += 1
                continue
            if sorted(ARG.findall(t[k])) != sorted(ARG.findall(en[k])):
                print(f"{name}: {k} args {ARG.findall(t[k])} != {ARG.findall(en[k])}")
                bad += 1
        for k in t:
            if k not in en:
                print(f"{name}: unknown key {k}")
                bad += 1
        merged = {**en, **t}
        write(f"values-{folder}", merged, en)
    print(f"{len(LOCALES)} languages, {len(en)} strings, {bad} problems")
    sys.exit(1 if bad else 0)


if __name__ == "__main__":
    main()
