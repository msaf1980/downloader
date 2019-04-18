package downloader;

import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import Utils.CustomException;
import Utils.CustomRuntimeException;
import Utils.FileSysException;
import Utils.FileUtils;
import Utils.NumUtils;
import Utils.RedirectException;
import Utils.StrUtils;
import Utils.TranslitCyrillic;
import Utils.URLUtils;
import Utils.UnsupportedException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

enum DirStruct { 
	/**
	 * FLAT - only filename
	 * FLATDIRS - only filename for html, other files saved in 'site' dir
	 * DIRS - save files with full path 
	 * SITEDIRS - save files with sitename + full path
	 */
	FLAT, FLATDIRS, SITEDIRS, DIRS;
	
	public static DirStruct fromString(String name) {
		if(name != null ) {
			try {
				return fromStringWithEx(name);
			} catch(IllegalArgumentException e) {
			}
		}
		return null;
	}
	
	public static DirStruct fromStringWithEx(String name) {
		 if(name != null ) {
            return DirStruct.valueOf(name.toUpperCase());
	    }
	    return null;
	}
	
	public static boolean contains(String name) {
        for (DirStruct dirStruct : DirStruct.values()) {
           if (dirStruct.name().equalsIgnoreCase(name))
               return true;
        }
        return false;
   }
}

enum Protocol { 
	HTTP, HTTPS;
	
	public static Protocol fromString(String name) {
		if(name != null ) {
			try {
				return fromStringWithEx(name);
			} catch(IllegalArgumentException e) {
			}
		}
		return null;
	}
	
	public static Protocol fromStringWithEx(String name) {
		if(name != null ) {
			return Protocol.valueOf(name.toUpperCase());
		}
		return null;
	}
	
	public static boolean contains(String name) {
        for (Protocol proto : Protocol.values()) {
           if (proto.name().equalsIgnoreCase(name))
               return true;
        }
        return false;
   }
}

enum NameGen { 
	/**
	 * AUTO - auto-generate (for url, ended with / generate index-N.html)
	 * SUBURL - for url, ended with / generate name from suburl instead index-N.html
	 */
	AUTO, SUBURL;
	
	public static NameGen fromString(String name) {
		if(name != null ) {
			try {
				return fromStringWithEx(name);
			} catch(IllegalArgumentException e) {
			}
		}
		return null;
	}
	
	public static NameGen fromStringWithEx(String name) {
		 if(name != null ) {
            return NameGen.valueOf(name.toUpperCase());
	    }
	    return null;
	}
	
	public static boolean contains(String name) {
        for (NameGen nameGen : NameGen.values()) {
           if (nameGen.name().equalsIgnoreCase(name))
               return true;
        }
        return false;
   }
}

enum SkipReason {
	REJECT_SIZE, REJECT_REGEX, RESTRICT, NOT_FOUND, IO_ERROR, ERROR, NOT_SUPPORTED;
}

public class URLDownloader {
    private static final String userAgent = "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-GB; rv:1.9.2.13) Gecko/20101203 Firefox/3.6.13 (.NET CLR 3.5.30729)";
    
	private int shutdown = 0;
    
    public void setShutdown() {
        shutdown = 0;
    }

    public boolean isShutdown() {
        return shutdown > 0 ? true : false;
    }
        
	private int threads = 1;

    private int retry = 2;
        
	private static final String site_prefix = "site" + File.separator;
	//loaded urls map
	private ConcurrentHashMap<String, URLObject> urls_map = new ConcurrentHashMap<String, URLObject>(50, (float) 0.75, 1); 
    private ConcurrentHashMap<String, URLObject> redirect_map = new ConcurrentHashMap<String, URLObject>(50, (float) 0.75, 1); 
    
	private ConcurrentHashMap<String, URLObject> paths_map = new ConcurrentHashMap<String, URLObject>(50, (float) 0.75, 1);
	//private HashMap<String, URL_Object> urls_map = new HashMap<String, URL_Object>(200);
	private ConcurrentLinkedQueue<String> urls_queue = new ConcurrentLinkedQueue<String>();
	
	private ConcurrentHashMap<String, SkipReason> skip_urls_map = new ConcurrentHashMap<String, SkipReason>(50, (float) 0.75, 1); 
	
	//private ConcurrentLinkedQueue<String> map_queue = null;
	
	//root urls for download
	private HashSet<String> rooturls = new HashSet<String>();
	//root sites
	private HashSet<String> rootsites = new HashSet<String>();
	//root dir - bases on restrict_site
	private ArrayList<String> rootdirs = new ArrayList<String>();
	//private HashMap<String, String> rootdirs_map = new HashMap<String, String>();
    
	public boolean addRootURL(String e) {
		return rooturls.add(e);
	}
	
    private HashSet<String> rooturlsgroup = new HashSet<String>();
	public boolean addRootURLGroup(String e) {
		return rooturlsgroup.add(e);
	}
    
    private String rooturlsgroup_range;
    public void setRootURLGroupRange(String e) {
		rooturlsgroup_range = e;
	}
    
    boolean https_equal = false;
    public void setHTTPSEqual() {
        https_equal = true;
    }
    
	//case sensitive reject regex
	private ArrayList<String> rejects = new ArrayList<String>();
	public boolean addReject(String e) {
		return rejects.add(e);
	}
	
	//case insensitive reject regex
	private ArrayList<String> rejects_i = new ArrayList<String>();
	public boolean addRejectInsensitive(String e) {
		return rejects_i.add(e);
	}

	//compiled reject regex
	private ArrayList<Pattern> rejects_p = new ArrayList<Pattern>();
	
	//case sensitive exclude from reject regex
	private ArrayList<String> excludes = new ArrayList<String>();
	public boolean addExclude(String e) {
		return excludes.add(e);
	}
	
	//case insensitive exclude from reject regex
	private ArrayList<String> excludes_i = new ArrayList<String>();
	public boolean addExcludeInsensitive(String e) {
		return excludes_i.add(e);
	}
	
	//compiled reject exclude from regex
	private ArrayList<Pattern> excludes_p = new ArrayList<Pattern>();
	
	//root dir for downloaded files
	private  String rootdir = ".";
	
	private static final String MAPFILE = "downloader.map";
	private static final String SKIPFILE = "downloader.skip";
	
	private String mapfile = MAPFILE;
	private BufferedWriter mapfile_out = null;
    
	private String skipfile = SKIPFILE;
	
	/*
	FLAT - all in one dir
	FLATDIRS - flat for for html and doc, but images, jscript, css, etc create subdir site)
	SITEDIRS - simulate site directory structure
	*/
	private DirStruct dir_struct = DirStruct.FLAT;
	
    private NameGen name_gen = NameGen.AUTO;
    
	//limit links levelup for internal and external sites
	private int levelon = 1;
	private int levelout = 0;
	
    //restrict_level on rooturl's sites
    //site = -1 
    //rootdir = 0
    //rootdir - N
	private byte restrict_site = 0;
	
	private long size_limit = -1;

	//-------------------
	public void setThreads(String s) {
		this.threads = Integer.parseInt(s);
	}
	//-------------------
	public void setDirStruct(String s) {
		dir_struct = DirStruct.fromStringWithEx(s);
	}
	//-------------------
	public void setLevelOn(String s) {
		this.levelon = Byte.parseByte(s);
	}
	//-------------------
	public void setLevelOut(String s) {
		this.levelout = Byte.parseByte(s);
	}
	//-------------------
	public void setRestrictSite(String s) {
		if (s.equals("site"))
			restrict_site = -1;
		else if (s.equals("rootdir"))
			restrict_site = 0;
		else
			restrict_site = Byte.parseByte(s);
	}
	//-------------------
	public void setOutdir(String outdir) {
        if (outdir == null || outdir.length() == 0)
            this.rootdir = ".";
        else
            this.rootdir = outdir;
		mapfile = FileUtils.pathJoin(rootdir, MAPFILE);
		skipfile = FileUtils.pathJoin(rootdir, SKIPFILE);
	}
	//-------------------
	public String getOutdir() {
		return rootdir;
	}
	//-------------------
	public void setSizeLimit(String s) {
		this.size_limit = NumUtils.ParseLong2(s);
	}
	//-------------------
    public void setNameGen(String s) {
        this.name_gen = NameGen.fromStringWithEx(s);
    }
    //-------------------        
	private ArrayList<RegexReplace> regex_path_repl = new ArrayList<RegexReplace>();
	public void putPathRewriteRegex(String s) {
		String[] values = s.split("\\|");
		Pattern p;
		switch (values.length) {
		case 1:
			regex_path_repl.add(new RegexReplace(values[0], null, null));
			break;
		case 2:
			regex_path_repl.add(new RegexReplace(values[0], values[1], null));
			break;
		case 3:
			regex_path_repl.add(new RegexReplace(values[0], values[1], values[2]));
			break;
		default:
			return;
		}
		
		//char[] attrs = values[2]
	}
	//-------------------	
	private ArrayList<RegexReplace> regex_dir_repl = new ArrayList<RegexReplace>();
	public void putDirRewriteRegex(String s) {
		String[] values = s.split("\\|");
		Pattern p;
		switch (values.length) {
		case 1:
			regex_dir_repl.add(new RegexReplace(values[0], null, null));
			break;
		case 2:
			regex_dir_repl.add(new RegexReplace(values[0], values[1], null));
			break;
		case 3:
			regex_dir_repl.add(new RegexReplace(values[0], values[1], values[2]));
			break;
		default:
			return;
		}
		
		//char[] attrs = values[2]
	}
	//-------------------
	private boolean urlmapAdd(String url, int level) {
		synchronized(urls_map) {
			if (! urls_map.containsKey(url)) {
				urls_map.put(url, new URLObject(url, level));
				return true;
			}
		}
		return urls_map.get(url).setLLevel(level);
	}
	//-------------------
	/**
	 * Check if link (from <link rel=".." href=".."/>) is skiped 
	 * @param rel
	 * @return
	 */
	public boolean checkSkipedLink(String rel) {
		switch (rel) {
		case "stylesheet":
		case "shortcut icon":
			return false;
		default:
			return true;
		}
	}
	//-------------------
	/**
	 * Check if url (from <a href=".."/>) is rejected 
	 * @param url
	 * @param site
	 * @param level
	 * @return
	 */
	private boolean isRejectedURL(String url, String site, int level) {
		if (site == null || url == null)
			return true;
		if (rootsites.contains(site)) {
			if (level > levelon) {
				skipPut(url, SkipReason.RESTRICT);
				Downloader.LOGGER.finest(url + " restrited by level on");
				return true;
			}
			if (restrict_site >= 0) {
				boolean reject = true;
				for (int i = rootdirs.size() - 1; i > -1 ; i--) {
					if (url.startsWith(rootdirs.get(i))) {
						reject = false;
						break;
					}
				}
				if (reject) {
					skipPut(url, SkipReason.RESTRICT);
					Downloader.LOGGER.finest(url + " restrited by root site");
					return true;
				}
			}
		} else if (level > levelout) {
			//skipPut(url, SkipReason.RESTRICT);
			Downloader.LOGGER.finest(url + " restrited by level out");
			return true;
		}

		for (int i = rejects_p.size() - 1; i > -1 ; i--) {
			if (rejects_p.get(i).matcher(url).find()) {
				for (int j = excludes_p.size() - 1; j > -1 ; j--) {
					if (excludes_p.get(j).matcher(url).find())
						return false;
				}
				Downloader.LOGGER.fine(url + " rejected by regex " + rejects_p.toString());
				skipPut(url, SkipReason.REJECT_REGEX);
				return true;
			}
		}
		
		return false;
	}
	//-------------------
	/**
	 * 
	 * @param s_url
	 * @param parent_level
	 * @param is_link
	 * @param queue
	 * @return
	 */
	public boolean putURL(String s_url, int parent_level, boolean is_link, boolean queue) {
        if (s_url == null || s_url.equals(""))
            return false;
        String striped_url = URLUtils.urlStripAnchor(s_url);
        if (striped_url == null)
                return false;
		if (skip_urls_map.containsKey(striped_url))
			return false;
		String u_url;
		try {

			u_url = URLUtils.urlDecode(striped_url, "UTF-8");
			//u_url.replaceFirst("%3A%2F%2F", "://");
		} catch (IllegalArgumentException|UnsupportedEncodingException e) {
			//throw new CustomException(s_url + " failed to decode");
            Downloader.LOGGER.warning(striped_url + " failed to decode");
            skip_urls_map.put(striped_url, SkipReason.ERROR);
			return false;
		}
		try {
			URL url = new URL(u_url);
			if (! Protocol.contains(url.getProtocol())) {
                //Downloader.LOGGER.warning(u_url + " has unsupported protocol");
				return false;
			}
			int level = parent_level;
			String site = URLUtils.urlGetSite(url);
		
			if (parent_level == 0) {
				//root url
				if (! rootsites.contains(site))
					rootsites.add(site);
                //if (! rootdirs.contains(site))
				//	rootdirs.add(site);
                if (https_equal) {
                    String site_additional = null;
                    if (url.getProtocol().equals("https"))
                        site_additional = "http://" + site.substring(8);
                    else if (url.getProtocol().equals("http"))
                        site_additional = "https://" + site.substring(7);
                    if (site_additional != null) {
                        if (! rootsites.contains(site_additional))
                            rootsites.add(site_additional);    
                        //if (! rootdirs.contains(site_additional))
                        //    rootdirs.add(site_additional);
                    }
                }
				level = 1;
				
				if (restrict_site >= 0) {
					int dir_level = restrict_site;
					String dir_restrict = url.getFile();
					int pos = dir_restrict.length() - 1;
					if (dir_restrict.endsWith("/"))
						dir_level--;
					
					while (dir_level >= 0 && pos > 0) {
						pos = dir_restrict.lastIndexOf("/", pos - 1);
						dir_level--;
					}
					if (pos <= 0) {
						if (! rootsites.contains(site))
							rootsites.add(site);
                        if (! rootdirs.contains(dir_restrict))
							rootdirs.add(site);
					} else {
						dir_restrict = dir_restrict.substring(0, pos + 1);
						if (! rootdirs.contains(dir_restrict))
							rootdirs.add(site + dir_restrict);
					}
				}
			} else if (is_link) {
				level++;
				if (isRejectedURL(u_url, site, level))
					return false;
			}
			
			//push to queue, if not exist or level changed
			if (urlmapAdd(u_url, level) && queue)
				urls_queue.add(u_url);
			return true;
			
		} catch (MalformedURLException e) {
			Downloader.LOGGER.warning(s_url + " has incorrect format");
            return false;
		}	
	}
	//-------------------
	public void initRootURLs() {
		for (String url : rooturls) {
    		putURL(url, 0, true, true);
		}		
	}
	//-------------------
    public void expandRootURLGroup() {
        if (rooturlsgroup.size() > 0) {
            if (rooturlsgroup_range == null)
                throw new CustomRuntimeException("Root URLs range not set");

            int[] split_num = new int[3];
            char[] split = new char[2];
            boolean numeric = false;
            String[] split_tmp = rooturlsgroup_range.split("-");
            if (rooturlsgroup_range.matches("^[0-9]+-[0-9]+$")) {
                numeric = true;
                split_num[0] = Integer.parseInt(split_tmp[0]);
                split_num[1] = Integer.parseInt(split_tmp[1]);
                split_num[2] = 1;
            } else if (rooturlsgroup_range.matches("^[0-9]+-[0-9]+-[1-9]+$")) {
                numeric = true;
                split_num[0] = Integer.parseInt(split_tmp[0]);
                split_num[1] = Integer.parseInt(split_tmp[1]);
                split_num[2] = Integer.parseInt(split_tmp[2]);
            } else if (rooturlsgroup_range.matches("^[a-zA-Z]-[a-zA-Z]$")) {
                split[0] = split_tmp[0].charAt(0);
                split[1] = split_tmp[1].charAt(0);
            } else
                throw new CustomRuntimeException("Root URLs range must be in format START-END (or may be START-END-STEP for numbers, default step set to 1)");
            
            Pattern p = Pattern.compile("\\$+");
            for (String urlgroup : rooturlsgroup) {
                //urls_queue.add(url);
                Matcher m = p.matcher(urlgroup);
                int pos = 0;
                if (m.find()) {
                    if (numeric) {
                        for (int i =  split_num[0]; i <= split_num[1]; i+=split_num[2]) {
                            StringBuilder sb = new StringBuilder();
                            if (m.start() > pos)
                                sb.append(urlgroup.substring(pos, m.start()));
                            int len = m.end() - m.start();
                            if (len > 1) {
                                String format = String.format("%%0%dd", len);
                                sb.append(String.format(format, i));
                            } else
                                sb.append(Integer.toString(i));
                            int end = m.end();
                            if (m.end() < urlgroup.length())
                                sb.append(urlgroup.substring(m.end()));
                            String url = sb.toString();
                            addRootURL(url);
                        }
                        
                    } else {
                        for (int i = split[0]; i <= split[1]; i++) {
                            StringBuilder sb = new StringBuilder();
                            if (m.start() > pos)
                                sb.append(urlgroup.substring(pos, m.start()));
                            sb.append(String.valueOf(i));
                            if (m.end() < urlgroup.length() - 1)
                                sb.append(urlgroup.substring(m.end() + 1));
                            String url = sb.toString();
                            addRootURL(url);
                        }
                    }
                } else
                    throw new CustomRuntimeException("Root URLs range not set correct");
            }
        }
    }
    
	public void queueRootURLs() {
		for (String url : rooturls) {
			urls_queue.add(url);
		}
	}
	//-------------------
	public void prepare() throws IOException {
		//if (threads > 1)
		//	map_queue = new ConcurrentLinkedQueue<String>();
        expandRootURLGroup();
    	initRootURLs();
		
		int i;
		for (i = 0; i < rejects.size(); i++) {
			Pattern p = Pattern.compile(rejects.get(i), Pattern.UNICODE_CHARACTER_CLASS);
			rejects_p.add(p);
		}
		for (i = 0; i < rejects_i.size(); i++) {
			Pattern p = Pattern.compile(rejects_i.get(i), Pattern.UNICODE_CHARACTER_CLASS | 
														Pattern.UNICODE_CASE | Pattern.CASE_INSENSITIVE);
			rejects_p.add(p);
		}
		
		for (i = 0; i < excludes.size(); i++) {
			Pattern p = Pattern.compile(excludes.get(i), Pattern.UNICODE_CHARACTER_CLASS);
			excludes_p.add(p);
		}
		for (i = 0; i < excludes_i.size(); i++) {
			Pattern p = Pattern.compile(excludes_i.get(i), Pattern.UNICODE_CHARACTER_CLASS | 
														Pattern.UNICODE_CASE | Pattern.CASE_INSENSITIVE);
			excludes_p.add(p);
		}
		
		mapLoad();
	}
	//-------------------
	public void mapLoad() throws IOException {
		Path mapfile_path = Paths.get(mapfile);
		boolean duplicate = false;
		try (BufferedReader br = Files.newBufferedReader(mapfile_path, StandardCharsets.UTF_8)) {
			String line;
			synchronized (urls_map) {
				while ((line = br.readLine()) != null)   {
                    if (URLObject.setRedirect(urls_map, redirect_map, line))
                        continue;
					URLObject url_obj = new URLObject(line);
					if (urls_map.contains(url_obj.url)) {
						duplicate = true;
						Downloader.LOGGER.info("duplicate found, resaving map after load");
					}
					if (FileUtils.isFileEmpthy(rootdir, url_obj.path) != 2) {
						url_obj.reset();
						if (! rooturls.contains(url_obj.url))
							urls_queue.add(url_obj.url);
					}
					urls_map.put(url_obj.url, url_obj);
					paths_map.put(url_obj.path, url_obj);
				}
			}
		} catch (NoSuchFileException e) {
		}
		if (duplicate) {
			Files.copy(mapfile_path, Paths.get(mapfile_path + ".backup"), StandardCopyOption.REPLACE_EXISTING);
			mapfile_out = Files.newBufferedWriter(mapfile_path, StandardCharsets.UTF_8);
			for (URLObject url_obj : urls_map.values()) {
				if (url_obj.path != null) 
					mapSave(url_obj);
			}
		} else
			mapfile_out = Files.newBufferedWriter(mapfile_path, StandardCharsets.UTF_8, StandardOpenOption.CREATE, 
																					StandardOpenOption.APPEND);
	}
	//-------------------
	private void mapSave(URLObject url_object) throws IOException {
		mapfile_out.write(url_object.toString());
		mapfile_out.newLine();
		mapfile_out.flush();
	}
	//-------------------
	private void skipPut(String url, SkipReason reason) {
		skip_urls_map.put(url, reason);
	}
	//-------------------
	private void skipSave() {
		try (BufferedWriter out = Files.newBufferedWriter(Paths.get(skipfile), StandardCharsets.UTF_8)) {
			for (Map.Entry<String, SkipReason> entry : skip_urls_map.entrySet()) {
				out.write(entry.getKey());
				out.write(" ");
				out.write(entry.getValue().toString());
				out.newLine();
			}
		} catch (IOException e) {
			Downloader.LOGGER.severe(skipfile + ": " + e.getMessage());
		}
	}
	//-------------------
	public void close() {
		try {
			if (mapfile_out != null)
				mapfile_out.close();
		} catch (IOException e) {
			Downloader.LOGGER.severe(mapfile + ": " + e.getMessage());;
		}
	}
	//-------------------
	/**
	 * Print html links for download
	 * @param base_url
	 * @param html
	 * @param parent_level
	 * @param dump_only
	 * @return
	 */

	public void parseHTML(String base_url, int parent_level) {
		if (base_url == null)
			return;

		Document doc;
		try {
			doc = Jsoup.connect(base_url).get();
		} catch (IOException e) {
			Downloader.LOGGER.warning(base_url + " : " + e.getMessage());
			return;
		}
		
		Elements links = doc.select("a[href]");
		Elements media = doc.select("[src]");
		Elements imports = doc.select("link[href]");
		
		System.out.println("==MEDIA");
		for (Element elem : media) {
            try {
            	//String url_short = URLUtils.urlStripAnchor(URLDecoder.decode(elem.attr("src"), "UTF-8"));
    			String url = URLUtils.urlDecode(URLUtils.urlStripAnchor(elem.attr("abs:src")), "UTF-8");
    			if (url != null)
   					System.out.println(url);
    		} catch (UnsupportedEncodingException e) {
    			Downloader.LOGGER.warning(elem.attr("abs:src") + " failed to decode!");
    		}
        }
		
		System.out.println("==IMPORT");
        for (Element elem : imports) {
            try {
            	String url_short = URLUtils.urlDecode(URLUtils.urlStripAnchor(elem.attr("href")), "UTF-8");
            	String url = URLUtils.urlDecode(URLUtils.urlStripAnchor(elem.attr("abs:href")), "UTF-8");
            	if (url_short != null && url != null && ! url.equals(base_url) && ! checkSkipedLink(elem.attr("rel")))
    				System.out.println(url);
    		} catch (UnsupportedEncodingException e) {
    			Downloader.LOGGER.warning(elem.attr("abs:href") + " failed to decode!");
    		}
        }

        System.out.println("==HREF");
        for (Element elem : links) {
            try {
            	String url_short = URLUtils.urlDecode(URLUtils.urlStripAnchor(elem.attr("href")), "UTF-8");
            	String url = URLUtils.urlDecode(URLUtils.urlStripAnchor(elem.attr("abs:href")), "UTF-8");
            	if (url_short != null && url !=null && ! url.equals(base_url) && ! isRejectedURL(url, URLUtils.urlGetSite(url), parent_level + 1))
            		System.out.println(url);
    		} catch (UnsupportedEncodingException e) {
    			Downloader.LOGGER.warning(elem.attr("abs:href") + " failed to decode!");
    		}
        }
	}
	//-------------------
    public String newFilename(String url) {
        if (! this.rootsites.contains(url) && ! this.rooturls.contains(url))
            if (this.name_gen == NameGen.SUBURL)
            {
                String baseDir = URLUtils.baseDirFileName(url)[0];
                String cmp;
                if (baseDir.endsWith("/"))
                    cmp = baseDir.substring(0, baseDir.length() - 1);
                else
                    cmp = url;
                if (! this.rootsites.contains(cmp))
                    return URLUtils.baseDirFileName(cmp)[1];
            }
        return "index";
    }
    //-------------------
	/**
	 * Generate new filename (return null if path exist and not changed or old path)
	 * @param url_obj
	 * @return oldpath or null if not changed
	 * @throws MalformedURLException
	 */
	public String generateFilename(URLObject url_obj) throws MalformedURLException {
		URL url = new URL(url_obj.url);
		//String url_path = url.ge
		String[] nameEx;
		String filename;
		
		if (dir_struct == DirStruct.FLAT || dir_struct == DirStruct.FLATDIRS) {
			filename = URLUtils.urlGetFileName(url);
			if (filename == null || filename.equals("")) {
				nameEx = new String[2];
                nameEx[0] = this.newFilename(url_obj.url);
				nameEx[1] = URLUtils.mimeGetDefaultExtension(url_obj.content_type);
			} else {
				for (int i = regex_path_repl.size() - 1; i >= 0; i--)
					filename = regex_path_repl.get(i).replace(filename);
				nameEx = FileUtils.filenameExtension(filename);
			}

			if (dir_struct == DirStruct.FLATDIRS) {
				if (url_obj.content_type != null && ! url_obj.content_type.equals("text/html"))
					nameEx[0] = site_prefix + nameEx[0];
			}

		} else {
			if (dir_struct == DirStruct.DIRS) {
                if (url.getPath() == null || url.getPath().length() == 0)
                    filename = "";
                else
                    filename = url.getPath().substring(1);
            } else {
				if (url.getPort() == -1)
					filename = url.getHost() + url.getPath();
				else
					filename = url.getHost() + "_" + Integer.toString(url.getPort()) + url.getPath();
			}

			nameEx = URLUtils.baseDirFileName(filename);
			for (int i = regex_dir_repl.size() - 1; i >= 0; i--)
				nameEx[0] = regex_dir_repl.get(i).replace(nameEx[0]);
			filename = nameEx[0] + nameEx[1];

			for (int i = regex_path_repl.size() - 1; i >= 0; i--)
				filename = regex_path_repl.get(i).replace(filename);

            if (filename == null || filename.equals("")) {
				nameEx = new String[2];
				nameEx[0] = this.newFilename(url_obj.url);
				nameEx[1] = URLUtils.mimeGetDefaultExtension(url_obj.content_type);
			} else if (filename.endsWith("/")) {
				nameEx = new String[2];
				nameEx[0] = filename + "/" + this.newFilename(url_obj.url);
				nameEx[1] = URLUtils.mimeGetDefaultExtension(url_obj.content_type);
			} else
				nameEx = URLUtils.urlExtension(filename);
		}

        if (url_obj.content_type == null)
            nameEx[1] = "";
		else if (url_obj.content_type.equals("text/html")) {
			/*if (! nameEx[1].equals(".html") && ! nameEx[1].equals(".htm"))
				nameEx[1] = ".html";
            */
			/*
            ".htm":
			".shtml":
			".shtm":
			".xhtml":
			".xhtm":
            */
            switch (nameEx[1]) {
                case ".html":
                case ".htm":
                    break;
                default:
                    nameEx[1] = ".html";
            }
		} else if (url_obj.content_type.equals("text/javascript") ||
				url_obj.content_type.equals("application/javascript") ||
					url_obj.content_type.equals("application/x-javascript")
					) {
			if (! nameEx[1].equals(".js"))
				nameEx[1] = ".js";
		} else if (url_obj.content_type.equals("text/css")) {
			if (! nameEx[1].equals(".css"))
				nameEx[1] = ".css";
		} else if ((url_obj.content_type.startsWith("application/") || 
				url_obj.content_type.startsWith("audio/") || 
				url_obj.content_type.startsWith("video/")
				) && (
						nameEx[1].equals("") |
						nameEx[1].equals(".php") || 
						nameEx[1].equals(".jsp") || 
						nameEx[1].equals(".py") || 
						nameEx[1].equals(".cgi")
						)
				)
			nameEx[1] = URLUtils.mimeGetDefaultExtension(url_obj.content_type);
		else if (url_obj.content_type.startsWith("image/") && (
				nameEx[1].equals("") ||
				nameEx[1].equals(".php") || 
				nameEx[1].equals(".jsp") || 
				nameEx[1].equals(".py") || 
				nameEx[1].equals(".cgi")
				)
						)
			nameEx[1] = URLUtils.mimeGetDefaultExtension(url_obj.content_type);
		String path = TranslitCyrillic.transliterate_filesystem(nameEx[0]) + nameEx[1];
		if (url_obj.path != null) {
			String basedir = FileUtils.baseDir(url_obj.path);
			String basedir_new = FileUtils.baseDir(path);
			if (basedir.equals(basedir_new))
				return null;
		}
		
		synchronized(paths_map) {
			int i = 1;
			// && ! paths_map.get(path).url.equals(url_obj.url)
			while (paths_map.containsKey(path))
				path = TranslitCyrillic.transliterate_filesystem(nameEx[0]) + "-" + Integer.toString(i++) + nameEx[1];
			
			if (url_obj.path == null) {
				url_obj.path = path;
                paths_map.put(path, url_obj);
            } else if (! url_obj.path.equals(path)) {
				String oldpath = url_obj.path;
                if (paths_map.contains(oldpath))
                    paths_map.remove(oldpath);
				url_obj.path = path;
                paths_map.put(path, url_obj);                
				return oldpath;
			}	
		}
		return null;
	}
	//-------------------
	/**
	 * Load text from InputStream
	 * @param inputStream
	 * @param charset
	 * @return string with text contend (end line in Unix format)
	 * @throws IOException
	 */
	private String textstreamRead(InputStream inputStream, String charset) throws IOException {
        if (inputStream == null)
            return null;
		try (InputStreamReader istream = new InputStreamReader(inputStream, charset)) {
			StringBuilder sb = new StringBuilder();
			BufferedReader br = new BufferedReader(istream);
			String line;
			while ((line =	br.readLine()) != null) {
				sb.append(line);
				sb.append("\n");
			}
			return sb.toString(); 
		} catch (IOException e) {
			throw e;
		}

	}
	//-------------------
	/**
	 * Write string to text file
	 * @param filename
	 * @param s
	 * @return true if success
	 * @throws IOException
	 * @throws FileSysException
	 */
	public boolean textfileWrite(String url, String filename, String s) throws IOException, FileSysException {
		if (new File(rootdir).getFreeSpace() <= s.length())
			throw new FileSysException(String.format("%s: Not enough space to save %s", url, filename));
		try (BufferedWriter out = Files.newBufferedWriter(Paths.get(rootdir, filename), StandardCharsets.UTF_8)) {
			out.write(s, 0, s.length());
			out.write("\n");
			return true;
		} catch (IOException e) {
			shutdown = 1;
			try {
				if (FileUtils.isFile(rootdir, filename) > 0)
					new File(rootdir, filename).delete();
			} finally { }
			if (new File(rootdir).getFreeSpace() <= s.length())
				throw new FileSysException(String.format("%s: Not enough space to save %s", url, filename));
			throw e;
		}
	}
	//-------------------
	/**
	 * Read text file to string
	 * @param filename
	 * @return string with file context
	 * @throws IOException
	 */
	public String textfileRead(String filename) throws IOException {
		try (BufferedReader in = Files.newBufferedReader(Paths.get(rootdir, filename), StandardCharsets.UTF_8)) {
			String line;
			StringBuilder sb = new StringBuilder();
			while ((line = in.readLine()) != null) {
				sb.append(line);
				sb.append("\n");
			}
			return sb.toString();
		} catch (FileNotFoundException e) {
		   	return null;
		} catch (IOException e) {
			throw e;
		}
	}
	//-------------------
	/**
	 * Save binary file from input stream
	 * @param filename
	 * @param istream - Input stream
	 * @return true if success
	 * @throws IOException 
	 * @throws FileSysException 
	 */
	public long binstreamWrite(String url, String filename, InputStream istream) throws IOException, FileSysException {
		long read = 0;
		try (InputStream in = new BufferedInputStream(istream);
			 BufferedOutputStream out = new BufferedOutputStream(Files.newOutputStream(Paths.get(rootdir, filename)))
					 ) {

			int bufsize = 16384;
			byte[] buf = new byte[bufsize];
			int n;
			while ((n = in.read(buf, 0, bufsize)) != -1) {
				if (n > 0) {
					try {
						out.write(buf, 0, n);
					} catch (IOException e) {
						shutdown = 1;
						throw e;
					}
					read += n;
				}
			}
			return read;
		} catch (IOException e) {
			try {
				if (FileUtils.isFile(rootdir, filename) > 0)
					new File(rootdir, filename).delete();
			} finally { }
			if (new File(rootdir).getFreeSpace() <= read)
				throw new FileSysException(String.format("%s: Not enough space to save %s", url, filename));
			throw e;
		}
	}
	//-------------------
    public URLConnection connectionRedirect(String link, HttpURLConnection conn, int redirect) throws IOException, RedirectException, UnsupportedException, KeyManagementException, NoSuchAlgorithmException {
        int responce = 0;
        if (conn instanceof HttpsURLConnection)
            responce = ((HttpsURLConnection) conn).getResponseCode();
        else 
            responce = conn.getResponseCode();
        switch (responce)
        {
            case HttpURLConnection.HTTP_MOVED_PERM:
            case HttpURLConnection.HTTP_MOVED_TEMP:
                if (redirect > 0) {
                    String location;
                    if (conn instanceof HttpsURLConnection)
                        location = ((HttpsURLConnection) conn).getHeaderField("Location");
                    else
                        location = conn.getHeaderField("Location");
                    URL url = new URL(location);
                    Protocol proto = Protocol.fromString(url.getProtocol());
                    URLObject.setRedirect(urls_map, redirect_map, link, location);
                    return openConnection(link, url, proto, redirect - 1);
                } else {
                    String location;
                    if (conn instanceof HttpsURLConnection)
                        location = ((HttpsURLConnection) conn).getHeaderField("Location");
                    else
                        location = conn.getHeaderField("Location");
                    throw new RedirectException();
                }
            default:
                return conn;
        }
        /*
        switch (http_conn.getResponseCode())
        {
            case HttpURLConnection.HTTP_MOVED_PERM:
            case HttpURLConnection.HTTP_MOVED_TEMP:
                if (processRedirect) {
                    String location = http_conn.getHeaderField("Location");
                    URL url = new URL(location);
                    Protocol proto = Protocol.fromString(url.getProtocol());
                    URLObject.setRedirect(urls_map, redirect_map, link, location);
                    return openConnection(link, url, proto, false);
                } else {
                    String location = http_conn.getHeaderField("Location");
                    throw new RedirectException();
                }
            default:
                return http_conn;
        } 
        */
    }
    //-------------------
    public URLConnection openConnection(String link, URL url, Protocol proto, int redirect) throws IOException, UnsupportedException, RedirectException, KeyManagementException, NoSuchAlgorithmException {
        if (proto.equals(Protocol.HTTP)) {
            HttpURLConnection http_conn = (HttpURLConnection) url.openConnection();
            http_conn.setFollowRedirects(false);
            http_conn.setRequestProperty("User-Agent", userAgent);
            http_conn.connect();
            return connectionRedirect(link, http_conn, redirect);
        } else if (proto.equals(Protocol.HTTPS)) {
            HttpsURLConnection https_conn = (HttpsURLConnection) url.openConnection();
            https_conn.setHostnameVerifier(new HostnameVerifierAll()); 
            // Don't check SSL certificate
            TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() 
                    {
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
                        public void checkClientTrusted(X509Certificate[] certs, String authType) { }
                        public void checkServerTrusted(X509Certificate[] certs, String authType) { }
                    } 
                };

            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, null);
            https_conn.setDefaultSSLSocketFactory(sc.getSocketFactory());
            https_conn.setFollowRedirects(false);
            https_conn.setRequestProperty("User-Agent", userAgent);
            https_conn.connect();
            return connectionRedirect(link, https_conn, redirect);
        } else {
            throw new UnsupportedException(url.toString() + " has unsupported protocol");
        }
    }
    //-------------------
	public boolean loadURL(String s_url, boolean parse_html) throws FileSysException {
        if (s_url == null || s_url.equals(""))
            return false;
		URLObject url_obj = urls_map.get(s_url);
		if (url_obj == null) {
			Downloader.LOGGER.warning(s_url + " not in URL list");
			skipPut(s_url, SkipReason.ERROR);
			return false;
		}
		if (url_obj.isRejected(retry))
			return false;

		if (url_obj.isDone()) {
			if (url_obj.content_type != null && url_obj.content_type.equals("text/html")) {
				if (FileUtils.isFileEmpthy(rootdir, url_obj.path) == 2) {
					if (parse_html && url_obj.isProcessed())
						return true;
					processHTML(url_obj, null);
					return true;
				}
			} else if (FileUtils.isFileEmpthy(rootdir, url_obj.path) == 2)
				return true;
			url_obj.reset();
		}
		URLConnection conn = null;
		boolean result = false;
		try {
       		if (! url_obj.setLoad())
    			return false;	
			String u_url;
			try {
				u_url = new URI(s_url).toASCIIString();
			} catch (URISyntaxException e) {
				//Downloader.LOGGER.warning(s_url + " failed to encode");
				//skip_put(s_url, SkipReason.ERROR);
				//return false;
				u_url = s_url;
			}
			
 			URL url = new URL(u_url);
			Protocol proto = Protocol.fromString(url.getProtocol());
            try {
                conn = openConnection(s_url, url, proto, 4);
            } catch (KeyManagementException | NoSuchAlgorithmException ex) {
                Downloader.LOGGER.warning(s_url + " skipped by SSL error");
				url_obj.setRejected();
				return false; 
            } catch (UnsupportedException e) {
				Downloader.LOGGER.warning(e.getMessage());
				url_obj.setRejected();
				skipPut(s_url, SkipReason.NOT_SUPPORTED);
				return false;            
            } catch (RedirectException e) {
                Downloader.LOGGER.warning(s_url + " skipped by redirect");
				url_obj.setRejected();
				return false; 
            } catch (IllegalArgumentException e) {
                Downloader.LOGGER.warning(s_url + " has invalid format");
				url_obj.setRejected();
				return false;
            }
            /*
			if (proto.equals(Protocol.HTTP)) {
				HttpURLConnection http_conn = (HttpURLConnection) url.openConnection();
				http_conn.setFollowRedirects(false);
                http_conn.setRequestProperty("User-Agent", userAgent);
				conn = http_conn;
			} else if (proto.equals(Protocol.HTTPS)) {
				HttpsURLConnection https_conn = (HttpsURLConnection) url.openConnection();
				https_conn.setFollowRedirects(false);
                https_conn.setRequestProperty("User-Agent", userAgent);
				conn = https_conn;
			} else {
				Downloader.LOGGER.warning(u_url + " has unsupported protocol");
				url_obj.setRejected();
				skipPut(s_url, SkipReason.NOT_SUPPORTED);
				return false;
			}
            */
            
			if (proto.equals(Protocol.HTTP) || proto.equals(Protocol.HTTPS)) {
                HttpURLConnection http_conn = (HttpURLConnection) conn;
                
                long length = http_conn.getContentLengthLong();
                if (length > 0 && size_limit != -1 && size_limit < length) {
                    Downloader.LOGGER.fine(String.format("%s rejected by size limit %l", u_url, length));
                    skipPut(s_url, SkipReason.REJECT_SIZE);
                    url_obj.setRejected();
                    return false;
                }

				/*
                String lengthH = conn.getHeaderField("Content-Length");
				log length = -1;
				try {
					if (lengthH != null) { 
						length = Long.parseLong(lengthH);
						if (size_limit != -1 && size_limit < length) {
							Downloader.LOGGER.fine(String.format("%s rejected by size limit %l", u_url, length));
							skipPut(s_url, SkipReason.REJECT_SIZE);
							url_obj.setRejected();
							return false;
						}
					}
				} finally { }
				*/
                
                String charset = "UTF-8";
                
				String contentTypeH = http_conn.getContentType();
                
                if (contentTypeH != null) {
                    String[] contentType = contentTypeH.split("; *");
                    if (contentType.length == 2 && contentType[1].startsWith("charset="))
                        charset = contentType[1].substring(8);
                    url_obj.setContentType(contentType[0]);
                }
				if (url_obj.path == null)
					generateFilename(url_obj);
				/*
				if (dir_struct == DirStruct.FLATDIRS) {
					File site_dir = new File(rootdir, "site");
					if (! site_dir.exists())
						site_dir.mkdir();
				}
				*/
				String dirname = FileUtils.baseDir(new File(rootdir, url_obj.path).getAbsolutePath());
				File dir = new File(dirname);
				if (! dir.isDirectory())
					dir.mkdirs();

                String errStream;
				if (url_obj.content_type != null && url_obj.content_type.equals("text/html")) {
					String html = textstreamRead(conn.getInputStream(), charset);
					result = processHTML(url_obj, html);
				} else {
					binstreamWrite(s_url, url_obj.path, http_conn.getInputStream());
                    int respCode = http_conn.getResponseCode();
                    //errStream = textstreamRead(http_conn.getErrorStream(), charset);
                    result = true;
                }
			}
		} catch (FileNotFoundException e) {
			Downloader.LOGGER.warning(s_url + " not exist");
            url_obj.setRejected();
			skipPut(s_url, SkipReason.NOT_FOUND);
			result = false;
		} catch (MalformedURLException e) {
			Downloader.LOGGER.warning(s_url + " has incorrect format!");
			url_obj.setRejected();
			skipPut(s_url, SkipReason.ERROR);
			result = false;
		} catch (IOException e) {
			Downloader.LOGGER.warning(s_url + " I/O error: " + e.getMessage());
            url_obj.incError();
			skipPut(s_url, SkipReason.IO_ERROR);
			result = false;
		}
		finally {
			url_obj.endLoad(result);
		}

		if (result) {
			try {
				mapSave(url_obj);
			} catch (IOException e) {
				url_obj.reset();
				Downloader.LOGGER.warning(s_url + " map I/O error: " + e.getMessage());
				shutdown = 1;
				//if (url_obj.path != null)
				//	new File(rootdir, url_obj.path).delete();
				result = false;
			}
		}

		return result;
	}
	//-------------------
	public void dump() {
		String url = null;
		while ((url = urls_queue.poll()) != null && shutdown == 0)
			parseHTML(url, 0);

	}
	//-------------------
	public void run() throws FileSysException {
            queueRootURLs();
            String url;
            
            if (threads > 1) {
                int n;
                ExecutorService executor = Executors.newFixedThreadPool(threads);
                ExecutorCompletionService ecs = new ExecutorCompletionService(executor);
                for (int i = 0; i <= retry && shutdown == 0; i++) {
                    n = 0;
                    while ((url = urls_queue.poll()) != null && shutdown == 0) {
                        Callable task = new URLDownloaderThread(this, url);
                        ecs.submit(task);
                        n++;
                    }
                    for (int j = 0; j < n; j++) {
                        try {
                            ecs.take();
                        } catch (InterruptedException e) {
                            Downloader.LOGGER.severe(e.getMessage());
                            setShutdown();
                            break;
                        }
            		}
                }
                
                
                n = 0;
                for (URLObject url_obj : urls_map.values()) {
                    if (url_obj.isDone() && url_obj.content_type != null && url_obj.content_type.equals("text/html")) {
                        Callable task = new HTMLProcessThread(this, url_obj);
                        executor.submit(task);
                    }
                }
                for (int j = 0; j < n; j++) {
                    try {
                        ecs.take();
                    } catch (InterruptedException e) {
                        Downloader.LOGGER.severe(e.getMessage());
                        setShutdown();
                        break;
                    }
                }

                executor.shutdown();
            } else {
                for (int i =0; i <= retry && shutdown == 0; i++) {
                    while ((url = urls_queue.poll()) != null && shutdown == 0) {
                        loadURL(url, false);
                    }
                }
                for (URLObject url_obj : urls_map.values()) {
                    if (url_obj.isDone() && url_obj.content_type != null && url_obj.content_type.equals("text/html")) {
                        processHTML(url_obj, null);
                    }
                }
            }
            skipSave();
	}
	//-------------------
	public boolean processHTML(URLObject url_obj, String html) throws FileSysException {
		Document doc = null;
        String link_html = url_obj.url;
        if (url_obj.redirect_url != null)
           link_html = url_obj.redirect_url;        
        if (html == null) {
            try {
                doc = Jsoup.parse(new File(rootdir, url_obj.path), "UTF-8", link_html);
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println(rootdir + File.pathSeparator + url_obj.path);
                throw new FileSysException(url_obj.url + " : can't load saved html");
            }
        } else {
            doc = Jsoup.parse(html, link_html);
            doc.charset(StandardCharsets.UTF_8);
        }
		
		boolean changed = false;
			
		Elements media = doc.select("[src]");
		for (Element elem : media) {
			try {
				if (elem.attr("src").equals(""))
					continue;					
				String link = URLUtils.urlDecode(URLUtils.urlStripAnchor(elem.attr("src")), "UTF-8");
				//String link_full_extract = URLDecoder.decode(elem.attr("abs:src"), "UTF-8");
				String link_full = URLUtils.urlDecode(URLUtils.urlStripAnchor(elem.attr("abs:src")), "UTF-8");
				if (link == null ||  link_full == null || url_obj.url.equals(link_full))
					continue;
				String anchor = URLUtils.urlExtractAnchor(elem.attr("abs:src"));
				String link_tppabs = link_full + anchor;
				if (html != null) {
					if (URLUtils.validateProtocolURL(link_tppabs))
						elem.attr("tppabs", link_tppabs);
				} else
					link_tppabs = elem.attr("tppabs");
				String rewrited_link = rewriteLink(url_obj, link_full, anchor, link_tppabs);
				if  (rewrited_link == null) {
					if (putURL(link_full, url_obj.getLevel(), false, false)) {
						if (loadURL(link_full, false))
							rewrited_link = rewriteLink(url_obj, link_full, anchor, link);
					}
				} else if (rewrited_link != null && ! rewrited_link.equals(link)) {
					changed = true;
					elem.attr("src", rewrited_link + anchor);
				}
			} catch (IllegalArgumentException | UnsupportedEncodingException e) {
				if (! skip_urls_map.contains(elem.attr("abs:src"))) {
					skipPut(elem.attr("abs:src"), SkipReason.ERROR);
					Downloader.LOGGER.warning(elem.attr("abs:src") + " failed to decode on " + url_obj.url + " !");
				}
			}
		}

		Elements imports = doc.select("link[href]");
		for (Element elem : imports) {
			try {
				if (elem.attr("href").equals(""))
					continue;
				String link = URLUtils.urlDecode(URLUtils.urlStripAnchor(elem.attr("href")), "UTF-8");
				String link_full = URLUtils.urlDecode(URLUtils.urlStripAnchor(elem.attr("abs:href")), "UTF-8");
				if (link == null ||  link_full == null || url_obj.url.equals(link_full))
					continue;
				String anchor = URLUtils.urlExtractAnchor(elem.attr("abs:src"));
				String link_tppabs = link_full + anchor;
				if (checkSkipedLink(elem.attr("rel")))
					continue;
				if (html != null) {
					if (URLUtils.validateProtocolURL(link_tppabs))
						elem.attr("tppabs", link_tppabs);
				} else
					link_tppabs = elem.attr("tppabs");
				String rewrited_link = rewriteLink(url_obj, link_full, "", link_tppabs);
				if  (rewrited_link == null) {
					if (putURL(link_full, url_obj.getLevel(), false, false)) {
						if (loadURL(link_full, false))
							rewrited_link = rewriteLink(url_obj, link_full, "", link);
					}
				} else if (rewrited_link != null && ! rewrited_link.equals(link)) {
					changed = true;
					elem.attr("href", rewrited_link);
				}
			} catch (IllegalArgumentException | UnsupportedEncodingException e) {
				if (! skip_urls_map.contains(elem.attr("abs:href"))) {
					skipPut(elem.attr("abs:src"), SkipReason.ERROR);
					Downloader.LOGGER.warning(elem.attr("abs:href") + " failed to decode on " + url_obj.url + " !");
				}
			}
		}
		
		Elements links = doc.select("a[href]");
		for (Element elem : links) {
			try {
				if (elem.attr("href").equals(""))
					continue;
				String link = URLUtils.urlDecode(URLUtils.urlStripAnchor(elem.attr("href")), "UTF-8");
				//String link_full_extract = URLDecoder.decode(elem.attr("abs:href"), "UTF-8");
				String link_full = URLUtils.urlDecode(URLUtils.urlStripAnchor(elem.attr("abs:href")), "UTF-8");
				if (link == null ||  link_full == null || url_obj.url.equals(link_full))
					continue;
                String anchor = URLUtils.urlExtractAnchor(elem.attr("abs:src"));
				String link_tppabs = link_full + anchor;
                
				if (html != null) {
					if (URLUtils.validateProtocolURL(link_full)) {
						elem.attr("href", link_full + anchor);
						elem.attr("tppabs", link_full + anchor);
					}
				} else
					link_tppabs = elem.attr("tppabs");
				
				String rewrited_link = rewriteLink(url_obj, link_full, anchor, URLUtils.urlStripAnchor(link_tppabs));
				if  (rewrited_link == null)
					putURL(link_full, url_obj.getLevel(), true, true);
				else if (rewrited_link != null && ! rewrited_link.equals(link)) {
					changed = true;
					elem.attr("href", rewrited_link + anchor);
				} 
			} catch (IllegalArgumentException | UnsupportedEncodingException e) {
				if (! skip_urls_map.contains(elem.attr("abs:href"))) {
					skipPut(elem.attr("abs:src"), SkipReason.ERROR);
					Downloader.LOGGER.warning(elem.attr("abs:href") + " failed to decode on " + url_obj.url + " !");
				}
			}
		}
	
		if (changed || html != null) {
			if (html == null) {
				try {
					Files.copy(Paths.get(rootdir, url_obj.path), Paths.get(rootdir, url_obj.path + ".backup"), StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException e) {
					throw new FileSysException(String.format("can't backup %s : %s", url_obj.path, e.getMessage()));
				}
				try {
					Files.write(Paths.get(rootdir, url_obj.path), doc.outerHtml().getBytes("UTF-8"));
				} catch (IOException e) {
					try {
						Files.move(Paths.get(rootdir, url_obj.path + ".backup"), Paths.get(rootdir, url_obj.path), StandardCopyOption.REPLACE_EXISTING);
						throw new FileSysException(String.format("can't save %s : %s", url_obj.path, e.getMessage()));
					} catch (IOException e1) {
						throw new FileSysException(String.format("save failed, can't restore %s : %s", url_obj.path, e.getMessage()));
					}
				}
				try {
					Files.delete(Paths.get(rootdir, url_obj.path + ".backup"));
				} catch (IOException e) {

				}
			} else {
				try {
					Files.write(Paths.get(rootdir, url_obj.path), doc.outerHtml().getBytes("UTF-8"));
				} catch (IOException e) {
					try {
						if (new File(rootdir, url_obj.path).isFile())
							Files.delete(Paths.get(rootdir, url_obj.path));
						throw new FileSysException(String.format("can't save %s : %s", url_obj.path, e.getMessage()));
					} catch (IOException e1) {
						throw new FileSysException(String.format("save failed, can't delete %s : %s", url_obj.path, e.getMessage()));
					}
				}	
			}
			url_obj.setProcessed();
		}
		
		/*
		if (changed || html != null) {
			try {
				if (html == null)
					Files.copy(Paths.get(rootdir, url_obj.path), Paths.get(rootdir, url_obj.path + ".backup"), StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e) {
				throw new CustomException(String.format("can't backup %s : %s", url_obj.path, e.getMessage()));
			}
			try {
				Files.write(Paths.get(rootdir, url_obj.path), doc.outerHtml().getBytes("UTF-8"));
			} catch (IOException e) {
				try {
					Files.move(Paths.get(rootdir, url_obj.path + ".backup"), Paths.get(rootdir, url_obj.path), StandardCopyOption.REPLACE_EXISTING);
					throw new CustomException(String.format("can't save %s : %s", url_obj.path, e.getMessage()));
				} catch (IOException e1) {
					throw new CustomException(String.format("save failed, can't restore %s : %s", url_obj.path, e.getMessage()));
				}
			}
			try {
				if (html == null)
					Files.delete(Paths.get(rootdir, url_obj.path + ".backup"));
			} catch (IOException e) {
				Downloader.LOGGER.warning(String.format("can't delete %s.backup : %s", url_obj.path, e.getMessage()));
			}
			url_obj.setProcessed();
		}
		*/
		return true;
	}
	//-------------------
	/**
	 * @param parent_link
	 * @param link_full
	 * @param link_abs
	 * @return rewriten url
	 */
	public String rewriteLink(URLObject parent_url, String link_full, String anchor, String link_abs) {
		if (parent_url ==  null)
			return null;
		String link = link_abs;
		if (link_full == null)
			link = link_full;
		 if (link == null)
			 return null;
		if (parent_url.url.equals(link))
			return "";
    
        URL url_parent_link = null;
        URL url_link = null;
		try {
            if (https_equal) {
                url_parent_link = new URL(parent_url.url);
        		url_link = new URL(link);
                if (URLUtils.equalWithoutProto(url_parent_link, url_link))
                    return "";
            }
		} catch (MalformedURLException e) {
    	}

        URLObject url_obj = urls_map.get(link);
    	if (url_obj == null)
           url_obj = redirect_map.get(link);
        if (url_obj == null && https_equal && url_link != null) {
            String link_check = null;
            if (url_link.getProtocol().equals("http")) {
                link_check = "https://" + link.substring(8);
            } else if (url_link.getProtocol().equals("https")) {
                link_check = "http://" + link.substring(7);
            }
            if (link_check != null) {
                url_obj = urls_map.get(link_check);
                if (url_obj == null)
                    url_obj = redirect_map.get(link_check);
           }
               
        }
        if (url_obj == null || url_obj.path == null)
           return null;
		String path = FileUtils.pathRelativeFile(rootdir + File.separator + parent_url.path, rootdir + File.separator + url_obj.path + anchor);
		return StrUtils.ReplaceChar(path, '\\', '/');
	}
	//-------------------
	/**
	 * for unit test
	 * @param parent_link
	 * @param link_full
	 * @param link_abs
	 * @return rewriten url
	 */
	public String rewriteLink(String parent_link, String link_full, String anchor, String link_abs) {
		return rewriteLink(urls_map.get(parent_link), link_full, anchor, link_abs);
	}
	//-------------------
	public void remap(boolean test) throws IOException {
		if (! test) {
			Files.copy(Paths.get(mapfile), Paths.get(mapfile + ".backup"), StandardCopyOption.REPLACE_EXISTING);
			mapfile_out = Files.newBufferedWriter(Paths.get(mapfile), StandardCharsets.UTF_8, StandardOpenOption.CREATE, 
																								StandardOpenOption.APPEND);					
		}
		for (URLObject url_obj : urls_map.values()) {
			String oldpath = null;
			try {
				oldpath = generateFilename(url_obj);
			} catch (MalformedURLException e) {
				Downloader.LOGGER.warning(url_obj.url + " has incorrect format");
			}
			
			if (oldpath == null)
				continue;
			System.out.println(oldpath + " -> " + url_obj.path);
			if (! test) {
				String dirname = FileUtils.baseDir(new File(rootdir, url_obj.path).getAbsolutePath());
				File dir = new File(dirname);
				if (! dir.isDirectory())
					dir.mkdirs();
				mapSave(url_obj);
				if (new File(rootdir, oldpath).isFile())
					Files.move(Paths.get(rootdir, oldpath), Paths.get(rootdir, url_obj.path), StandardCopyOption.REPLACE_EXISTING);
				else
					Downloader.LOGGER.warning(oldpath + " not exist!");
					
			}
		}
	}
	//-------------------}
}