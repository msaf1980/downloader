package downloader;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import Utils.FileUtils;
import Utils.URLUtils;

public class URLDownloaderTest {

	@Test
	public void testRewriteURL() throws IOException {
		/*
		String rootdir = System.getProperty("user.dir");
		
		URLDownloader url_downloader = new URLDownloader();
		url_downloader.setOutdir(rootdir + File.separator + "data");
		url_downloader.mapLoad();
		
		String link_base;
		
		String link_full;
		String link_abs;
		String link_check;
		String rewrite_link;
		
		link_base = "https://ru.wikibooks.org/wiki/Реализации_алгоритмов";
		
		link_abs = "https://ru.wikibooks.org/w/load.php?debug=false&lang=ru&modules=ext.pygments,wikimediaBadges|ext.uls.interlanguage|ext.visualEditor.desktopArticleTarget.noscript|mediawiki.legacy.commonPrint,shared|mediawiki.sectionAnchor|mediawiki.skinning.interface|skins.vector.styles|wikibase.client.init&only=styles&skin=vector";
		link_full = "https://ru.wikibooks.org/w/load.php?debug=false&lang=ru&modules=ext.pygments,wikimediaBadges|ext.uls.interlanguage|ext.visualEditor.desktopArticleTarget.noscript|mediawiki.legacy.commonPrint,shared|mediawiki.sectionAnchor|mediawiki.skinning.interface|skins.vector.styles|wikibase.client.init&only=styles&skin=vector";
		link_check = URLUtils.urlSepNormalize("site\\load-2.css");
		
		rewrite_link = url_downloader.rewriteLink(link_base, link_full, "", link_abs);
		
		assertEquals(rewrite_link, link_check);
                */

	}
}
