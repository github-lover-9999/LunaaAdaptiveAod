#!/usr/bin/env python3
import pathlib
import re
import subprocess
import tempfile

ROOT = pathlib.Path(__file__).resolve().parents[1]
SRC = ROOT / "xposed-stubs" / "src" / "main" / "java"

java_files = [str(p) for p in SRC.rglob("*.java")]
if not java_files:
    raise SystemExit("No Xposed stub sources found")

EXPECTED = {
    ("de.robv.android.xposed.XposedHelpers", "findClass"):
        "(Ljava/lang/String;Ljava/lang/ClassLoader;)Ljava/lang/Class;",
    ("de.robv.android.xposed.XposedHelpers", "findAndHookMethod"):
        "(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Object;)Lde/robv/android/xposed/XC_MethodHook$Unhook;",
    ("de.robv.android.xposed.XposedHelpers", "getObjectField"):
        "(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;",
    ("de.robv.android.xposed.XposedHelpers", "callMethod"):
        "(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;",
    ("de.robv.android.xposed.XposedBridge", "hookAllConstructors"):
        "(Ljava/lang/Class;Lde/robv/android/xposed/XC_MethodHook;)Ljava/util/Set;",
    ("de.robv.android.xposed.XposedBridge", "log(java.lang.String)"):
        "(Ljava/lang/String;)V",
    ("de.robv.android.xposed.XposedBridge", "log(java.lang.Throwable)"):
        "(Ljava/lang/Throwable;)V",
    ("de.robv.android.xposed.IXposedHookLoadPackage", "handleLoadPackage"):
        "(Lde/robv/android/xposed/callbacks/XC_LoadPackage$LoadPackageParam;)V",
    ("de.robv.android.xposed.XSharedPreferences", "reload"):
        "()V",
    ("de.robv.android.xposed.XSharedPreferences", "getBoolean"):
        "(Ljava/lang/String;Z)Z",
    ("de.robv.android.xposed.XSharedPreferences", "getInt"):
        "(Ljava/lang/String;I)I",
    ("de.robv.android.xposed.XSharedPreferences", "getFloat"):
        "(Ljava/lang/String;F)F",
}


def descriptors(javap_text: str):
    out = []
    lines = javap_text.splitlines()
    for i, line in enumerate(lines):
        stripped = line.strip()
        if stripped.startswith("public") and stripped.endswith(";"):
            desc = None
            for next_line in lines[i + 1:i + 4]:
                if "descriptor:" in next_line:
                    desc = next_line.split("descriptor:", 1)[1].strip()
                    break
            if desc:
                out.append((stripped, desc))
    return out


with tempfile.TemporaryDirectory() as out_dir:
    subprocess.run(["javac", "-d", out_dir, *java_files], check=True)

    classes = sorted({class_name for class_name, _ in EXPECTED})
    dumped = {}
    for class_name in classes:
        proc = subprocess.run(
            ["javap", "-classpath", out_dir, "-s", class_name],
            check=True,
            text=True,
            capture_output=True,
        )
        dumped[class_name] = descriptors(proc.stdout)

    failures = []
    for (class_name, method_key), expected in EXPECTED.items():
        method_name = method_key.split("(", 1)[0]
        candidates = [(sig, desc) for sig, desc in dumped[class_name]
                      if re.search(rf"\b{re.escape(method_name)}\(", sig)]
        if "(" in method_key and method_key != "findAndHookMethod":
            arg_hint = method_key[method_key.find("(") + 1:-1]
            candidates = [(sig, desc) for sig, desc in candidates if arg_hint in sig]
        if not candidates:
            failures.append(f"{class_name}.{method_key}: method not found")
            continue
        if expected not in [desc for _, desc in candidates]:
            actual = ", ".join(desc for _, desc in candidates)
            failures.append(
                f"{class_name}.{method_key}:\n  expected {expected}\n  actual   {actual}"
            )

    if failures:
        raise SystemExit("Xposed stub ABI mismatch:\n" + "\n".join(failures))

print(f"Xposed stub ABI contract OK ({len(EXPECTED)} descriptors)")
