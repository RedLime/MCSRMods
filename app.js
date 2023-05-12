const core = require('@actions/core');
const fetch = require('node-fetch');
const { Octokit } = require('octokit');

const fs = require('fs');
const path = require('path');

const VERSION_PARSING = /((\d+\.)+(\d+\.)?(\d+|x)|(v(\d+\.)?(\d+\.)?(\d+|x)))/g;
const SNAPSHOT_PARSING = /\d+w+\d+\w*/;

async function sleep(ms) {
    return new Promise((r) => setTimeout(r, ms));
}
const refreshMods = async () => {
    const githubToken = core.getInput('github_token');
    const octokit = new Octokit({ 
        auth: githubToken,
    });

    const newResourceList = [];

    const modDataList = JSON.parse(fs.readFileSync(path.resolve('./mods.json'), 'utf8'));
    if (!modDataList) return;

    const urlCaches = {};
    for await (const modData of modDataList) {
        if (modData.type != 'fabric_mod') continue;

        const oriRes = newResourceList.find(res => res.name == modData.name);
        const modResources = newResourceList.find(res => res.name == modData.name) || {
            name: modData.name,
            description: modData.description,
            warn: modData.warn,
            incompatible: modData.incompatible,
            versions: []
        };

        for await (const downloadData of modData.downloads) {
            core.info(`Requesting to '${modData.name} (${downloadData.url})!`);

            const resourceData = downloadData.resource;
            const targetHeader = {};
            let githubData = null;
            if (resourceData.type == 'github_releases') {
                const rawUrl = resourceData.url.replace('https://api.github.com/', '').split('/');

                githubData = await octokit.request('GET /repos/{owner}/{repo}/releases', {
                    owner: rawUrl[1],
                    repo: rawUrl[2],
                    per_page: 30
                });
                core.error(githubData);
            }
            if (resourceData.type == 'curseforge_files') continue;
            if (resourceData.type == 'direct') {
                for (const downloadVer of downloadData.versions) {
                    modResources.versions.push({
                        buildVersion: downloadData.build,
                        targetVersion: downloadVer,
                        downloadUrl: resourceData.url,
                        filename: resourceData.url.split('/').slice(-1)
                    })
                }
                continue;
            }

            try {
                const result = githubData || urlCaches[resourceData.url] || await (await fetch(resourceData.url, { headers: targetHeader })).json();
                urlCaches[resourceData.url] = result;
    
                const resources = convertResourceInfo(result, downloadData); 
                if (resources.length) {
                    resourceCheck: for (const resource of resources) {
                        if (!resource.buildVersion && downloadData.build) resource.buildVersion = downloadData.build;
                        const oldResource = modResources.versions.findIndex(res => resource.modName == res.modName && new ModVersion(resource.targetVersion).compare(new ModVersion(res.targetVersion)) == 0);
                        if (oldResource != -1) {
                            const old = modResources.versions[oldResource];
                            if (new ModVersion(resource.buildVersion).compare(new ModVersion(old.buildVersion)) > 0) {
                                modResources.versions.splice(oldResource, 1);
                            } else {
                                continue resourceCheck;
                            }
                        }
                        modResources.versions.push(resource);
                    }
                }
            } catch (e) {
                core.error(e);
                core.info("[ModCheck] Failed a load "+resourceData.url);
            }
            sleep(2000);
        }
        if (!oriRes) newResourceList.push(modResources);
    }

    fs.writeFileSync(path.resolve('./meta/v3/mods.json'), JSON.stringify(newResourceList, null, 4), 'utf8');

    core.info(`complete!`);
};
refreshMods();




const convertResourceInfo = (result, downloadData) => {
    let resultData = [];

    try {
        const resourceData = downloadData.resource;
        if (resourceData.type == 'github_releases') {
            resultData = resultData.concat(convertGithubResource(result, downloadData));
        }
        if (resourceData.type == 'modrinth_releases') {
            resultData = resultData.concat(convertModrinthResource(result, downloadData));
        }
    } catch (e) {
        core.info(e);
        return [];
    }

    return resultData;
}



const convertGithubResource = (result, downloadData) => {
    const resources = [];
    for (const release of result) {
        if (result.find(rel => !rel.prerelease) && release.prerelease && !downloadData.resource.format) continue;

        for (const asset of release.assets) {
            const buildVersion = getVersionsFromName(downloadData.resource.values, asset.name);
            if (downloadData.resource.format) {
                if (!new RegExp(downloadData.resource.format).test(asset.name)) {
                    continue;
                }
                var skipVersionCheck = true;
            }

            if (!skipVersionCheck) {
                let count = 0;
                if (buildVersion.mcVersion) count++;
                if (buildVersion.modVersion) count++;
                if (count < downloadData.resource.values.length) {
                    continue;
                }
            }

    
            if (buildVersion.mcVersion) {
                resources.push({
                    buildVersion: buildVersion.modVersion,
                    targetVersion: downloadData.versions.find(v => new ModVersion(v).compare(new ModVersion(buildVersion.mcVersion)) == 0) || buildVersion.mcVersion,
                    downloadUrl: asset.browser_download_url,
                    filename: asset.name
                });
            } else {
                for (const version of downloadData.versions) {
                    resources.push({
                        buildVersion: buildVersion.modVersion,
                        targetVersion: version,
                        downloadUrl: asset.browser_download_url,
                        filename: asset.name
                    });
                }
            }
        }
    }
    return resources;
}

const convertModrinthResource = (result, downloadData) => {
    const resources = [];
    for (const release of result) {
        for (const file of release.files) {
            const buildVersion = getVersionsFromName(downloadData.resource.values, file.filename);
            if (downloadData.resource.format) {
                if (!new RegExp(downloadData.resource.format).test(file.filename)) {
                    continue;
                }
                var skipVersionCheck = true;
            }

            if (!skipVersionCheck) {
                let count = 0;
                if (buildVersion.mcVersion) count++;
                if (buildVersion.modVersion) count++;
                if (count < downloadData.resource.values.length) {
                    continue;
                }
            }
    
            if (buildVersion.mcVersion) {
                resources.push({
                    buildVersion: buildVersion.modVersion,
                    targetVersion: downloadData.versions.find(v => new ModVersion(v).compare(new ModVersion(buildVersion.mcVersion)) == 0) || buildVersion.mcVersion,
                    downloadUrl: file.url,
                    filename: file.filename
                });
            } else if (release.game_versions.length) {
                for (const version of release.game_versions) {
                    if (downloadData.versions.find(v => new ModVersion(v).compare(new ModVersion(version)) == 0)) {
                        resources.push({
                            buildVersion: buildVersion.modVersion,
                            targetVersion: version,
                            downloadUrl: file.url,
                            filename: file.filename
                        });
                    }
                }
            } else {
                for (const version of downloadData.versions) {
                    resources.push({
                        buildVersion: buildVersion.modVersion,
                        targetVersion: version,
                        downloadUrl: file.url,
                        filename: file.filename
                    });
                }
            }
        }
    }
    return resources;
}



const compareVersion = (ver1, ver2) => {
    let count = 0;
    const verArr1 = ver1.split('.');
    const verArr2 = ver2.split('.');
    while (true) {
        const v1 = verArr1.length > count ? (+verArr1[count] || 0) : 0;
        const v2 = verArr2.length > count ? (+verArr2[count] || 0) : 0;
        if (verArr1[count] == 'x' || verArr2[count] == 'x') return 0;
        if (verArr1[count] == '*' || verArr2[count] == '*') return 0;
        if (v1 > v2) {
            return 1;
        }
        else if (v1 < v2) {
            return -1;
        }
        if (verArr1.length <= count && verArr2.length <= count) {
            return 0;
        }
        count++;
    }
}



const getVersionsFromName = (verArr, name) => {
    const versionObj = {};
    const skipIndexes = [];
    for (const verType of verArr) {
        const snapshotVersion = verType != 'version' ? name.match(SNAPSHOT_PARSING) : null;
        if (snapshotVersion && snapshotVersion.length) {
            versionObj.mcVersion = snapshotVersion[0];
            continue;
        }

        const normalVersion = name.matchAll(VERSION_PARSING);
        regex: for (let match of normalVersion) {
            let vs = match[0];
            const index = name.indexOf(vs);
            if (skipIndexes.includes(index)) continue;

            skipIndexes.push(index);
            if (verType == 'mc_major_version') {
                const versionCode = vs.split('.');
                if (versionCode.length == 2) vs = vs + '.x';
                else if (versionCode.length == 3) {
                    let vi = 0;
                    vs = versionCode.map(vc => vi++ == 2 ? 'x' : vc).join('.');
                }
            }
            if (verType == 'version') versionObj.modVersion = vs;
            else versionObj.mcVersion = vs;

            break regex;
        }
    }

    return versionObj;
}


class ModVersion {
    constructor(versionString) {
        this.versionString = versionString;
        this.versionName = this.versionString.replace(/[~\+\*]/g, '');

        if (SNAPSHOT_PARSING.test(this.versionString)) {
            this.isSnapshot = true;
            this.versionRange = 0;
            return;
        } 
        
        if (this.versionString.startsWith("~")) {
            this.versionRange = -1;
        } else if (this.versionString.endsWith("+")) {
            this.versionRange = 1;
        } else {
            this.versionRange = 0;
        }
        this.isSnapshot = false;
    }

    getVersionNames(target) {
        const ver1IsMaster = this.versionName.endsWith('x') || this.versionName.endsWith('*') || this.versionName.endsWith('-');
        const ver2IsMaster = target.versionName.endsWith('x') || target.versionName.endsWith('*') || target.versionName.endsWith('-');

        if (ver1IsMaster || ver2IsMaster) {
            const v1split = this.versionName.split('.');
            const v2split = target.versionName.split('.');
            if (v1split.length == v2split.length) {
                return [ v1split.slice(0, -1).join('.'), v2split.slice(0, -1).join('.') ];
            } else {
                return [ ver1IsMaster ? v1split.slice(0, -1).join('.') : v1split.join('.'), ver2IsMaster ? v2split.slice(0, -1).join('.') : v2split.join('.') ];
            }
        } else {
            return [ this.versionName, target.versionName ];
        }
    }

    /**
     * @param {ModVersion} target 
     */
    compare(target) {
        const versions = this.getVersionNames(target);
        let result = 0;
        if (this.isSnapshot) {
            result = versions[0].localeCompare(versions[1], undefined, { numberic: true });
        } else {
            result = compareVersion(versions[0], versions[1]);
        }
        
        if (this.versionRange != 0 || target.versionRange != 0) {
            if (this.versionRange == -result || target.versionRange == result) {
                return 0;
            }
        }
        return result;
    }
}