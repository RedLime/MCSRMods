
/**
 * Mod Loader[loader] naming
 * optifine    = Optifine
 * fabric      = Fabric Loader
 * 
 * Version[version] naming
 * (1.16)      = 1.16 only
 * (1.15+)     = 1.15 and above
 * (~1.12)     = 1.12 and below
 * (1.7-1.12)  = From 1.7 to 1.12
 * (*)         = All versions
 */

const ALLOW_MODS = [
    {
        "name": "Fabric Loader",
        "description": "You can use Fabric Loader, but DO NOT USE Fabric API!",
        "loader": "fabric",
        "version": "*",
        "downloads": [
            {
                "version": "1.14+",
                "url": "https://fabricmc.net/use/installer/",
            },
            {
                "version": "1.7-1.13",
                "url": "https://jitpack.io/com/github/Legacy-Fabric/fabric-installer/-SNAPSHOT/fabric-installer--SNAPSHOT.jar",
                "direct": true
            }
        ]
    },
    {
        "name": "Optifine",
        "description": "Check the Optifine rules",
        "loader": "optifine", 
        "version": "~1.14",
        "downloads": [
            {
                "version": "~1.14",
                "url": "https://optifine.net/downloads",
            }
        ]
    },
    {
        "name": "Sodium",
        "description": "Minecraft Performance Mod",
        "loader": "fabric", 
        "version": "1.15-1.17",
        "downloads": [
            {
                "version": "1.15",
                "build": "0.1.1",
                "url": "https://github.com/mrmangohands/sodium-fabric/releases/tag/mc1.15.2-0.1.1-SNAPSHOT%2B2020-12-10",
            },
            {
                "version": "1.16",
                "build": "0.2.0",
                "url": "https://github.com/mrmangohands/sodium-fabric/releases/tag/mc1.16.1-0.2.0%2Bbuild.17",
            },
            {
                "version": "1.17",
                "build": "0.3.4",
                "url": "https://modrinth.com/mod/sodium/version/Fz37KqRh",
            }
        ]
    }
];