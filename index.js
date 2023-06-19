const ALLOW_VERSIONS = [];
const ALLOW_MODS = [];

const { compareVersions, compare, satisfies, validate } = window.compareVersions

const typeOptions = {
    version: null,
    type: 'mods',
    run: 'rsg',
    isMac: navigator.platform.toLocaleLowerCase().includes('mac'),
    medical_issue: false
}

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

function initVersions() {
    const params = new URLSearchParams(window.location.search);
    let hasFirst = false;
    $('#game-versions').html(`<select id="game-versions-select" class="browser-default">${ALLOW_VERSIONS.map(version => {
        const selected = (params.has('version') ? version.value == params.get('version') : !hasFirst);
        if (!hasFirst) {
            hasFirst = true;
            typeOptions.version = version.value;
        }
        return `<option value="${version.value}" ${selected ? 'selected' : ''}>${version.name}</option>`;
    }).join('')}</select>`);
    M.FormSelect.init($('#game-versions-select'));

    if (params.has('type')) $("input[name='resource-type'][value='" + params.get('type') + "']").prop("checked", true);
    if (params.has('run')) $("input[name='run-type'][value='" + params.get('run') + "']").prop("checked", true);
}

function initResources() {
    $('#resources-tab').html(ALLOW_MODS.filter(mod => mod.files.find(file => file.game_versions.find(gv => satisfiesVersion(gv))) && rulesCheck(mod)).map(mod => getElementFromModInfo(mod)).join(''));    
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

                setInterval(() => {
                    let needRefresh = false;

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

                    if (needRefresh) {
                        history.replaceState(null, null, window.location.origin + window.location.pathname + "?version=" + typeOptions.version + "&type=" + typeOptions.type + "&run=" + typeOptions.run);
                        initResources();
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
    const build = modInfo.files.find(file => file.game_versions.find(gv => satisfiesVersion(gv) && rulesCheck(modInfo)));
    return `<li>` +
                //Mod Name & Version & Mod Loader
                `<div class="collapsible-header light-font"><b>${modInfo.name}</b>${`<small style="padding-left: 0.5em;">(v${build.version.replace('v', '')})</small>`}</div>` +

                `<div class="collapsible-body">` +
                    //Mod Description
                    (modInfo.description ? `<div>Description<br><small class="light-font description">${modInfo.description.replaceAll('\n', '<br>')}</small></div>` : '') +

                    //Incompatible mod list
                    (modInfo.incompatible?.length ? `<div>Incompatible Mods<br><small class="light-font description">You must be only use one of these : <b>${modInfo.name}</b>, ${modInfo.incompatible.join(', ')}</small></div>` : '') +

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
            va[1] = va[1].substring(0, va[1].length - 1) + '.0';
        }
        if (va[1].includes('-alpha')) {
            return typeOptions.version == vp[0].replace('=');
        }
    
        const finalVersion = va.join('.');
        try {
            return satisfies(typeOptions.version, finalVersion);
        } catch (e) {}
    } else {
        return vp.every(vpp => satisfiesVersion(vpp));
    }
}

function rulesCheck(mod) {
    return mod.files.every(file => {
        if (!file.rules) return true;
        for (const rule of file.rules) {
            const allow = rule.action == 'allow';
            if (rule.properties['category']) return (rule.properties['category'] == typeOptions.run) == allow || typeOptions.run == 'all'
            if (rule.properties['os'] == 'osx') return typeOptions.isMac == allow
            if (rule.properties['condition']) return (typeOptions[rule.properties['condition']]) == allow
        }
        return false;
    })
}