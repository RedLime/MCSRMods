const ALLOW_VERSIONS = [];
const ALLOW_MODS = [];

const { compareVersions, compare, satisfies, validate } = window.compareVersions
let needRefresh = false;

const typeOptions = {
    version: null,
    type: 'mods',
    run: 'rsg',
    os: null,
    medical_issue: false,
    modpack_flag: 0
}

function initVersions() {
    const params = new URLSearchParams(window.location.search);
    $('#game-versions').html(`<select id="game-versions-select" class="browser-default">${ALLOW_VERSIONS.map(version => {
        const selected = (params.has('version') ? version.value == params.get('version') : typeOptions.version == null);
        if (selected) {
            typeOptions.version = version.value;
        }
        return `<option value="${version.value}" ${selected ? 'selected' : ''}>${version.name}</option>`;
    }).join('')}</select>`);
    M.FormSelect.init($('#game-versions-select'));

    if (params.has('type')) $("input[name='resource-type'][value='" + params.get('type') + "']").prop("checked", true);
    if (params.has('run')) $("input[name='run-type'][value='" + params.get('run') + "']").prop("checked", true);

    if (params.has('os')) typeOptions.os = params.get('os');
    else {
        const platform = navigator.platform.toLocaleLowerCase();
        if (platform.includes('mac') || platform.includes('osx')) typeOptions.os = 'osx';
        else if (platform.includes('linux')) typeOptions.os = 'linux';
        else typeOptions.os = 'windows';
    }
    $("input[name='os-type'][value='" + typeOptions.os + "']").prop("checked", true);

    if (params.has('modpack_flag')) typeOptions.modpack_flag = +params.get('modpack_flag');
}

function initResources() {
    if (typeOptions.type == 'mods') {
        $('#resources-tab').addClass('collapsible');
        $('#resources-tab').html(ALLOW_MODS.filter(mod => mod.files.find(file => file.game_versions.find(gv => satisfiesVersion(gv)) && rulesCheck(file))).map(mod => getElementFromModInfo(mod)).join(''));   
    } else {
        initModPack();
        $('#resources-tab').removeClass('collapsible');
        $('#resources-tab').html(ALLOW_MODS.filter(mod => mod.type == 'fabric_mod' && mod.files.find(file => file.game_versions.find(gv => satisfiesVersion(gv)) && rulesCheck(file))).map((mod, i) => getElementFromModPackInfo(mod, i)).join('') + 
                                    `<div style="text-align: center;width: 100%;"><a class="waves-effect waves-light btn-large" onclick="generateMRPack()"><i class="material-icons left">archive</i>Generate Modpack</a></div>`);
        typeOptions.modpack_flag = 0;
        modPackCheckboxValidate();
    }
}

$(document).ready(() => {
    fetch('./meta/v4/mc_versions.json')
    .then(response => response.json())
    .then(json => {
        for (const version of json) {
            if (validate(version.value)) ALLOW_VERSIONS.push(version);
        }
        initVersions();

        fetch('./meta/v4/files.json')
            .then(response => response.json())
            .then(json => {
                for (const mod of json) {
                    ALLOW_MODS.push(mod);
                }

                initResources();

                setInterval(() => {
                    const currentVersion = $('#game-versions-select').val();
                    if (currentVersion != typeOptions.version && typeOptions.version != null) {
                        typeOptions.version = currentVersion;
                        needRefresh = true;
                    }

                    const currentType = $('input[name="resource-type"]:checked').val();
                    if (currentType != typeOptions.type) {
                        typeOptions.type = currentType;
                        needRefresh = true;
                    }

                    const currentRun = $('input[name="run-type"]:checked').val();
                    if (currentRun != typeOptions.run) {
                        typeOptions.run = currentRun;
                        needRefresh = true;
                    }

                    const medicalIssue = $('input:checkbox[name="medical-issue"]').is(":checked") == true;
                    if (medicalIssue != typeOptions.medical_issue) {
                        typeOptions.medical_issue = medicalIssue;
                        needRefresh = true;
                    }

                    const currentOS = $('input[name="os-type"]:checked').val();
                    if (currentOS != typeOptions.os) {
                        typeOptions.os = currentOS;
                        needRefresh = true;
                    }

                    if (needRefresh) {
                        history.replaceState(null, null, window.location.origin + window.location.pathname + "?version=" + typeOptions.version + "&type=" + typeOptions.type + "&run=" + typeOptions.run + "&os=" + typeOptions.os + (typeOptions.modpack_flag ? ('&modpack_flag=' + typeOptions.modpack_flag) : ''));
                        initResources();
                        needRefresh = false;
                    }
                }, 50);
            });
    });
});

$('[id^="mod-version-"]').click(function () {
    activeVersion(+($(this)[0].id.replace("mod-version-", '')), false);
});


// Return Mod HTML String
function getElementFromModInfo(modInfo) {
    const build = modInfo.files.find(file => file.game_versions.find(gv => satisfiesVersion(gv)) && rulesCheck(file));
    return `<li>` +
                //Mod Name & Version & Mod Loader
                `<div class="collapsible-header light-font"><b>${modInfo.name}</b>${`<small style="padding-left: 0.5em;">(v${build.version.replace('v', '')})</small>`}</div>` +

                `<div class="collapsible-body">` +
                    //Mod Description
                    (modInfo.description ? `<div>Description<br><small class="light-font description">${modInfo.description.replaceAll('\n', '<br>')}</small></div>` : '') +

                    //Incompatible mod list
                    (modInfo.incompatible?.length ? `<div>Incompatible Mods<br><small class="light-font description">You can't use it with one of these : ${modInfo.incompatible.join(', ')}</small></div>` : '') +

                    //Download Button
                    (build.url ? `<div><a class="light-font waves-effect waves-light btn" href="${build.url}" target="_blank">Download</a></div>` : '') +

                    //Page Button
                    (build.page ? `<div><a class="light-font waves-effect waves-light btn" href="${build.page}" target="_blank">Open Page</a></div>` : '') +
                `</div>` +
            `</li>`;
}

function satisfiesVersion(gv) {
    const vp = gv.split(' ');
    if (vp.length == 1) {
        const va = vp[0].split('.');
        if (va.length < 2) return false;
        if (va[1].endsWith('-')) {
            va[1] = va[1].substring(0, va[1].length - 1) + '.x';
        }
        if (va[1].includes('-alpha')) {
            if (typeOptions.version == va[0].replace('=', '')) return true;
            va[0] = va[0].replace('=', '');
            va[1] = va[1].split('-alpha')[0] + '.0';
            va.splice(2, 10);
        }
    
        const finalVersion = va.join('.');
        try {
            return satisfies(typeOptions.version, finalVersion);
        } catch (e) {}
    } else {
        return vp.every(vpp => satisfiesVersion(vpp));
    }
}

function rulesCheck(file) {
    if (!file.rules) return true;
    for (const rule of file.rules) {
        const allow = rule.action == 'allow';
        if (rule.properties['category']) return (rule.properties['category'] == typeOptions.run) == allow || typeOptions.run == 'all'
        if (rule.properties['os']) return (typeOptions.os == rule.properties['os']) == allow
        if (rule.properties['condition']) return (typeOptions[rule.properties['condition']]) == allow
    }
    return false;
}

function modPackCheckboxValidate() {
    $('.modpack-checkbox').each(function(i, obj) {
        const objID = $(obj).attr('id').replaceAll('mod-', '');
        const modInfo = ALLOW_MODS.find(mod => mod.name.toLowerCase().replaceAll(' ', '-') == objID);
        if (modInfo.incompatible && 
            modInfo.incompatible.map(inc => $('#mod-' + inc.toLowerCase().replaceAll(' ', '-'))).find(inc => inc.is(":checked"))) {
            $(obj).prop("disabled", true);
        } else {
            $(obj).prop("disabled", false);
        }
    });
}

function initModPack() {
    $(document).on("change", ".modpack-checkbox", () => {
        modPackCheckboxValidate();
    });
}

// Return Mod HTML String
function getElementFromModPackInfo(modInfo, index) {
    const build = modInfo.files.find(file => file.game_versions.find(gv => satisfiesVersion(gv)) && rulesCheck(file));
    return `<li>` +
                `<p class="light-font"><label><input type="checkbox" class="filled-in modpack-checkbox" id="mod-${modInfo.name.toLowerCase().replaceAll(' ', '-')}" ${typeOptions.modpack_flag & (1 << index) ? 'checked': ''}/><span><b>${modInfo.name}</b>${`<small style="padding-left: 0.5em;">(v${build.version.replace('v', '')})</small>`}</label></span></p>` +
            `</li>`;
}

function generateMRPack() {
    let flagValue = 0;
    const packData = {
        formatVersion: 1,
        game: 'minecraft',
        versionId: `${typeOptions.version}-${typeOptions.os}-${typeOptions.run}-custom`,
        name: `MCSR Custom Pack`,
        dependencies: {
            "fabric-loader": ALLOW_MODS.find(mod => mod.name == 'Fabric Loader').files[0].version,
            "minecraft": typeOptions.version
        },
        files: []
    };
    $('.modpack-checkbox').each(function(i, obj) {
        if (!$(obj).is(":checked")) return;
        const objID = $(obj).attr('id').replaceAll('mod-', '');
        const modInfo = ALLOW_MODS.find(mod => mod.name.toLowerCase().replaceAll(' ', '-') == objID);
        const build = modInfo.files.find(file => file.game_versions.find(gv => satisfiesVersion(gv)) && rulesCheck(file));
        packData.files.push({
            "path": "mods/" + build.name,
            "hashes": {
                "sha1": build.sha1,
                "sha512": build.sha512
            },
            "env": {
                "client": "required",
                "server": "unsupported"
            },
            "downloads": [build.url],
            "fileSize": build.size
        });
        flagValue += (1 << i);
    });
    typeOptions.modpack_flag = flagValue;
    needRefresh = true;

    // Generate the ZIP file
    const zip = new JSZip();
    zip.file("modrinth.index.json", JSON.stringify(packData, null, 4));

    zip.generateAsync({ type: "blob" }).then((content) => {
        // Create a download link
        var link = document.createElement("a");
        link.href = URL.createObjectURL(content);
        link.download = `MCSR-${packData.version}.mrpack`;
        link.click();
    });
}
