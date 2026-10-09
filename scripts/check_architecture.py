"""Keep dependency direction simple without a framework or extra Gradle modules."""
from pathlib import Path
import re

root = Path("app/src/main/java/com/gymtracker/android")
violations = []
for path in (root / "domain").rglob("*.kt"):
    source = path.read_text()
    if re.search(r"^import (android\.|androidx\.|com\.gymtracker\.android\.(data|presentation)\.)", source, re.M):
        violations.append(str(path) + ": domain must only depend on Kotlin/JVM and repository interfaces")
for path in (root / "presentation").rglob("*.kt"):
    if re.search(r"^import com\.gymtracker\.android\.data\.", path.read_text(), re.M):
        violations.append(str(path) + ": presentation must depend on domain")
for path in root.rglob("*.kt"):
    if "@Composable" in path.read_text() and "composables" not in path.parts:
        violations.append(str(path) + ": composables belong in presentation/composables")
if violations:
    raise SystemExit("\n".join(violations))
print("Architecture boundaries passed")
