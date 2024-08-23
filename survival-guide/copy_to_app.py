import shutil
import os

shutil.rmtree("../app/src/main/assets/survival_guide", ignore_errors=True)
shutil.copytree("output", "../app/src/main/assets/survival_guide")

# Remove unused files
try:
    os.remove("../app/src/main/assets/survival_guide/original.html")
except:
    pass

try:
    os.remove("../app/src/main/assets/survival_guide/guide.md")
except:
    pass
