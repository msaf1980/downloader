package Utils;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class URLUtils {
	static final Pattern VALIDPROTOCOLURL = Pattern.compile("^[a-z]{1,6}://.*$");
	
	public static boolean validateProtocolURL(String url) {
		return VALIDPROTOCOLURL.matcher(url).matches();
	}

	static char separator = '\\';
	static char separator_rep = '/';
	
	/**
	 * Replace separators \ in url to /
	 * @param url
	 * @return
	 */
	public static String urlSepNormalize(String url) {
		return  StrUtils.ReplaceChar(url, separator, separator_rep);
	}
	
	/**
	 * format site address from URL object
	 * 
	 * @param URL object
	 * @return url site (protocol://host:port)
	 */
	public static String urlGetSite(URL url) {
		StringBuilder sb = new StringBuilder(url.getProtocol());
		sb.append("://").append(url.getHost());
		if (url.getPort() != -1)
			sb.append(":").append(Integer.toString(url.getPort()));
		return sb.toString();
	}
	
	/**
	 * format site address from URL address
	 * 
	 * @param URL object
	 * @return url site (protocol://host:port)
	 */
	public static String urlGetSite(String url_link) {
		URL url;
		try {
			url = new URL(url_link);
		} catch (MalformedURLException e) {
			return null;
		}
		StringBuilder sb = new StringBuilder(url.getProtocol());
		sb.append("://").append(url.getHost());
		if (url.getPort() != -1)
			sb.append(":").append(Integer.toString(url.getPort()));
		return sb.toString();
	}
	
	/**
	 * Get base Filename from URL object (strip full path)
	 * 
	 * @param URL object
	 * @return base filename from url path
	 */
	public static String urlGetFileName(URL url) {
		int loc = url.getPath().lastIndexOf('/');
		if (loc == url.getPath().length() - 1)
			return null;
		else if (loc == -1)
			return url.getPath();
		else
			return  url.getPath().substring(loc + 1);
			
	}
	
	/**
	 * Strip anchor (#) from url address
	 * 
	 * @param URL object
	 * @return url with cated all started with # to the end
	 */
	public static String urlStripAnchor(String url) {
		int loc = url.indexOf('#');
		if (loc == -1)
			return url;
		else if (loc == 0)
			return null;
		else
			return url.substring(0, loc);
		/*
		String stripped_url = url.replaceFirst("#.*", "");
		if (stripped_url == "")
			return null;
		else
			return stripped_url;
		*/
	}

	/**
	 * Extract anchor from url or return blank string if not set
	 * @param url
	 * @return
	 */
	public static String urlExtractAnchor(String url) {
		int loc = url.indexOf('#');
		if (loc == -1)
			return "";
		else if (loc == 0)
			return url;
		else
			return url.substring(loc);
		/*
		String stripped_url = url.replaceFirst("#.*", "");
		if (stripped_url == "")
			return null;
		else
			return stripped_url;
		*/
	}

	/**
	 * Split url to name and extension part (with dot, like .zip)
	 * @param url
	 * @return String array. [0] - name, [1] - extension
	 */
	public static String[] urlExtension(String url) {
		String [] s = new String[2];
		int possep = url.lastIndexOf("/");
		int pos = url.lastIndexOf('.');
		if (pos > 0 && pos > possep) {
			s[0] = url.substring(0, pos);
		    s[1] = url.substring(pos);
		} else {
			s[0] = url;
			s[1] = "";
		}
		
		return s;
	}
	
	/**
	 * Get default file extension for MIME type
	 */
	public static String mimeGetDefaultExtension (String content_type) {
        if (content_type == null)
            return "";
		switch (content_type) {
		case "text/html":
			return ".html";
		case "image/jpeg":
		case "image/pjpeg":
			return ".jpg";
		case "image/png":
			return ".png";
		case "image/gif":
			return ".gif";
		case "image/svg+xml":
			return ".svg";
		case "image/tiff":
			return ".tiff";
		case "image/vnd.microsoft.icon":
			return ".ico";
		case "image/vnd.wap.wbmp":
			return ".wbmp";
		case "image/webp":
		case "text/javascript":
		case "application/javascript":
		case "application/x-javascript":
		case "text/x-jquery-tmpl":
			return ".js";
		case "application/postscript":
			return ".ps";
		case "application/x-tex":
			return ".tex";
		case "text/plain":
			return ".txt";
		case "application/x-rar-compressed":
			return ".rar";
		case "application/gzip":
			return ".gz";
		case "video/x-flv":
			return ".flv";
		case "application/x-font-ttf":
			return ".ttf";
		case "application/x-shockwave-flash":
			return ".swf";
		case "application/octet-stream":
			return "";
		case "application/x-bittorrent":
			return ".torrent";
		case "video/x-ms-wmv":
			return ".wmv";
		case "video/x-ms-wma":
		case "video/x-ms-wax":
			return ".wma";
		case "audio/vnd.wave":
			return ".wav";
		case "audio/vnd.rn-realaudio":
			return ".ra";
		case "audio/mpeg":
			return ".mpg";
		case "audio/basic":
			return ".mulaw";
		case "audio/L24":
			return ".pcm";
		case "model/x3d+binary":
			return ".x3db";
		case "model/x3d+vrml":
			return ".x3dv";
		case "model/x3d+xml":
			return ".x3d";
		case "application/vnd.oasis.opendocument.text":
			return ".odt";
		case "application/vnd.oasis.opendocument.text-template":
			return ".ott";
		case "application/vnd.oasis.opendocument.graphics":
			return ".odg";
		case "application/vnd.oasis.opendocument.graphics-template":
			return ".otg";
		case "application/vnd.oasis.opendocument.presentation":
			return ".odp";
		case "application/vnd.oasis.opendocument.presentation-template":
			return ".otp";
		case "application/vnd.oasis.opendocument.spreadsheet":
			return ".ods";
		case "application/vnd.oasis.opendocument.spreadsheet-template":
			return ".ots";
		case "application/vnd.oasis.opendocument.chart":
			return ".odc";
		case "application/vnd.oasis.opendocument.chart-template":
			return ".otc";
		case "application/vnd.oasis.opendocument.image":
			return ".odi";
		case "application/vnd.oasis.opendocument.image-template":
			return ".oti";
		case "application/vnd.oasis.opendocument.formula":
			return ".odf";
		case "application/vnd.oasis.opendocument.formula-template":
			return ".otf";
		case "application/vnd.oasis.opendocument.text-master":
			return ".odm";
		case "application/vnd.oasis.opendocument.text-web":
			return ".oth";
		case "application/x-pkcs7-certificates":
			return ".p7b";
		case "application/x-pkcs7-certreqresp":
			return ".p7r";
		case "application/x-pkcs7-mime":
			return ".p7c";
		case "application/x-pkcs7-signature":
			return ".p7s";
		}
			
		
		String[] mime_type = content_type.split("/");
		if (mime_type.length != 2)
			return "";
		String[] mime_subtype = mime_type[1].split("\\+");
		if (mime_subtype.length == 2) {
			switch (mime_subtype[0]) {
			case "xhtml":
				return ".xhtml";
			}
			
			switch (mime_subtype[1]) {
			case "xml":
				return ".xml";
			}
		}
		
		if (mime_type[1].startsWith("x-"))
			return mime_type[1].substring(2);
		
		return "." + mime_type[1];
	}

	/**
	 * Get base dir and filename from URL
	 * If path ended with /, return path (without last separator) for base dir and null for filename
	 * If path separator not found, return "" for base dir and path for filename
	 * @param path
	 * @return
	 */
	public static String[] baseDirFileName(String url) {
		String[] f = new String[2];
		int pos = url.lastIndexOf('/');
		if (pos == -1) {
			f[0] = "";
			f[1] = url;
		} else if (pos == 0) {
			f[0] = "/";
			f[1] = url.substring(1);
		} else if (pos == url.length() - 1) {
			f[0] = url.substring(0, url.length());
			f[1] = "";
		} else {
			f[0] = url.substring(0, pos + 1);
			f[1] = url.substring(pos + 1);
		}
		return f;
	}
    
    public static String urlDecode(String s_url, String codepage) throws UnsupportedEncodingException {
        if (s_url== null)
            return null;
        return URLDecoder.decode(s_url, codepage);
    }
    
    /**
     * compare URL'a addresses without protocol (if non-default port is used, addresses are not equal)
     */
    public static boolean equalWithoutProto(URL u1, URL u2) {
        if (u1.getHost().equals(u2.getHost()) && u1.getFile().equals(u2.getFile()) && u1.getPort() == u2.getPort())
            return true;
        else
            return false;
    }
}
