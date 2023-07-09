# MCSRMods
All of mod list for Minecraft speedrunning

## Can I use & modify this?
Sure, but need to include a link to this repository

## Main files
- [mod_versions.json](./mod_versions.json) - Available Minecraft Versions for ModCheck
- [mods.json](./mods.json) - All data of legalized mods
- [meta/v3/mods.json](./meta/v4/mods.json) - All mod versions json data for ModCheck
- [app.js](./app.js) - `meta/v3/mods.json` generator

## Used libraries
- [Jquery](https://jquery.com/)
- [Matarialize](https://materializecss.com/about.html)

## Modpacks URL format
Basic format: `https://redlime.github.io/MCSRMods/modpacks/v4/MCSR-{VERSION}-{OS}-{TYPE}.mrpack`
- `VERSION`: should be minecraft version like `1.16.1`, `1.15.2`
- `OS`: should be `Windows` or `OSX`(for Mac OS) or `Linux`
- `TYPE`: should be `RSG` or `SSG`

Example: 1.15.2 with Mac OS(OS X) for SSG\
`https://redlime.github.io/MCSRMods/modpacks/v4/MCSR-1.15.2-OSX-SSG.mrpack`

for MCSR Ranked: `https://redlime.github.io/MCSRMods/modpacks/v4/MCSRRanked-Windows-1.16.1.mrpack`
