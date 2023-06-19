package com.redlimerl.mcsr.mod;

import com.redlimerl.mcsr.helper.VersionPredicateHelper;
import com.redlimerl.mcsr.mod.abst.ModAsset;
import com.redlimerl.mcsr.mod.abst.ModDownload;
import com.redlimerl.mcsr.mod.abst.ModInfo;
import com.redlimerl.mcsr.helper.HttpRequestHelper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.List;
import java.util.SortedSet;

public class Optifine extends ModInfo {
    public Optifine(String name, String description, List<ModDownload> downloads, boolean recommended) {
        super(name, description, "optifine", downloads, recommended);
    }

    @Override
    protected void init(SortedSet<ModAsset> treeSet) throws Throwable {
        ModDownload optifineInfo = getDownloads().get(0);

        Document pageHtml = Jsoup.parse(HttpRequestHelper.getStringFromUrl(optifineInfo.getPageUrl()));
        Elements downloadTables = pageHtml.getElementsByClass("downloadTable mainTable");

        for (Element downloadTable : downloadTables) {
            Elements downloadElement = downloadTable.select(".colMirror a");
            String downloadUrl = downloadElement.attr("href");
            if (!downloadUrl.isBlank()) {
                String fileName = downloadUrl.replace("http://optifine.net/adloadx?f=OptiFine_", "");
                String[] modName = fileName.replace(".jar", "").split("_");

                ModAsset modAsset = new ModAsset(
                        String.join("_", modName).replace(modName[0] + "_", ""),
                        VersionPredicateHelper.getFromStringArray(new String[] { modName[0] }),
                        fileName, null, downloadUrl
                );
                treeSet.add(modAsset);
            }
        }
    }
}
