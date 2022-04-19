
M.AutoInit();
var elem = document.querySelector('.collapsible.expandable');
var instance = M.Collapsible.init(elem, {
    accordion: false
});
const ALLOW_MODS = [];

function getVersionCode(str) {
    const version = str.split('.');
    if (version.length > 1) {
        return +version[1];
    }
    return 0;
}

function initPage() {
    const params = new URLSearchParams(window.location.search);
    activeVersion(params.has('version') ? getVersionCode(params.get('version')) : 16, true);
}

$(document).ready(() => {
    fetch('https://redlime.github.io/MCSRMods/mods.json')
        .then(response => response.json())
        .then(json => {
            console.log(json);
            for (const mod of json) {
                ALLOW_MODS.push(mod);
            }
            initPage();
        });
})

$(window).on("popstate", function () {
    initPage();
});

$('[id^="mod-version-"]').click(function () {
    activeVersion(+($(this)[0].id.replace("mod-version-", '')), false);
});

function activeVersion(ver, clickable) {
    const params = new URLSearchParams(window.location.search);
    if (clickable) {
        document.getElementById('mod-version-' + ver)?.click();
        return;
    }
    if (!(!params.has('version') && ver == 16) && !(params.has('version') && getVersionCode(params.get('version')) == ver)) {
        history.pushState(null, null, window.location.origin + window.location.pathname + "?version=1." + ver);
    }
    if (ALLOW_MODS.length) {
        $('#mods-tab').html(ALLOW_MODS.filter(mod => mod.downloads.find(asset => isContainsVersion(ver, asset.versions))).map(mod => getElementFromModInfo(mod, ver)).join(''));
    }
}


// Return Mod HTML String
function getElementFromModInfo(obj, version) {
    const downloadBuild = obj.downloads.find(versionInfo => isContainsVersion(version, versionInfo.versions) && versionInfo.build)?.build;
    const advancedBuild = obj.advanced.filter(versionInfo => isContainsVersion(version, versionInfo.versions))
        .map(versionInfo => `<div><a class="light-font waves-effect waves-light btn" href="${versionInfo.url}" target="_blank">Download (${versionInfo.summary})</a></div>`).join('');
    const categoryIcons = obj.category.map(category => `<img title="${category}" src="${getCategoryIconURL(category)}" class="text-img circle"/>`).join(" ");

    return `<li>` +
                //Mod Name & Version & Mod Loader
                `<div class="collapsible-header light-font"><b style="opacity: 0.5">${obj.name}</b>${downloadBuild ? `<small style="padding-left: 0.5em;">(v${downloadBuild})</small>` : ''} ${categoryIcons}</div>` +

                `<div class="collapsible-body">` +
                    //Allowed MC Versions
                    `<div>Allowed & Available MC Versions<br><big class="light-font description">${obj.downloads.map(asset => asset.versions.join(", ")).join(", ")}</big></div>` +

                    //Mod Description
                    (obj.description ? `<div>Description<br><small class="light-font description">${obj.description.replaceAll('\n', '<br>')}</small></div>` : '') +

                    //Mod Warning
                    (obj.warn ? `<div><b>WARNING!</b><br><small class="light-font description">${obj.warn.replaceAll('\n', '<br>')}<br>If you didn't follow this warning, your run being may rejected.</small></div>` : '') +

                    //Incompatible mod list
                    (obj.incompatible?.length ? `<div>Incompatible Mods<br><small class="light-font description">You must be only use one of these : <b>${obj.name}</b>, ${obj.incompatible.join(', ')}</small></div>` : '') +

                    //Download Button
                    getDownloadButtonHTML(obj, version) +

                    //Advanced Downloads
                    (advancedBuild ? `<div><details><summary style="cursor: pointer;">Advanced downloads</summary>${advancedBuild}</details></div>` : ``) +
                `</div>` +
            `</li>`;
}


// Return Category Icon Image URL
function getCategoryIconURL(loader) {
    switch (loader) {
        case 'fabric':
            return 'https://fabricmc.net/assets/logo.png';
        case 'optifine':
            return 'https://i.imgur.com/eKIfz6R.png';
        case 'legacy-fabric':
            return 'https://avatars.githubusercontent.com/u/62736781?s=200&v=4';
        default:
            return '';
    }
}

// Return download buttons
function getDownloadButtonHTML(obj, version) {
    const availableBuilds = obj.downloads.filter(versionInfo => isContainsVersion(version, versionInfo.versions));
    return availableBuilds.map(versionInfo => `<div><a class="light-font waves-effect waves-light btn" href="${versionInfo.url}" target="_blank">Download (for ${versionInfo.versions.filter(v => isContainsVersion(version, [v])).join(", ")}${versionInfo.summary ? ` / ${versionInfo.summary}` : ""})</a></div>`).join('');
}

// Check version compare
// `versionNum` is number (middle of version name, ex] 0."2".0)
// `versionArray` is comparable version array. (["0.1", "0.3+"])
function isContainsVersion(versionNum, versionArray) {
    for (const versionName of versionArray) {
        if (versionName.split('.').length <= 1) continue;

        const targetVersion = +(versionName.split('.')[1].replace(/[^0-9]/g, ''));
        if (versionName.includes('+') && versionNum >= targetVersion) {
            return true;
        }
        if (versionName.includes('~') && versionNum <= targetVersion) {
            return true;
        }
        if (versionNum == targetVersion) {
        return true;
        }
    }

    return false;
}