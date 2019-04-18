package downloader;

import java.util.InputMismatchException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class URLObject {
	//path content_type level loaded url
	static final String split_string = "^(\\S+) (\\S+) (\\d+) ([01]) (.+)$";
	static final Pattern split_pattern = Pattern.compile(split_string);
    
    static final String redirect_string = "^REDIRECT (\\S+) (.+)$";
	static final Pattern redirect_pattern = Pattern.compile(redirect_string);
	
	final String url;
    String redirect_url;
    
	String path;
	String content_type;
	private int level;
	
	// loaded
	// 0 - not loaded
	// 1 - done
	// -1 - in progress
	private byte loaded = 0;
	
	// -1 - skip by restriction
    // n - error count
	private byte error = 0;
	
	private byte processed = 0;
	
	public synchronized boolean isProcessed() {
		return processed > 0 ? true : false;
	}

	public synchronized void setProcessed() {
		if (processed == 0) 
			processed = 1;
	}

	public synchronized boolean isRejected() {
		return error == -1 ? true : false;
	}

	public synchronized void setRejected() {
		this.error = -1;
	}
    
    public synchronized void incError() {
		this.error++;
	}

    public synchronized boolean isRejected(int retry) {
		return error >= retry || error == -1 ? true : false;
	}
    
	public URLObject(String url, int level) {
		this.url = url;
		this.level = level;
	}
	
	public synchronized boolean setLLevel(int level) {
		if (this.level > level) {
			//level changed - chage it
			this.level = level;
			return true;
		} else
			return false;
	}
	
	public synchronized void setContentType(String content_type) {
		if (content_type == null)
			return;
		if (this.content_type == null || ! content_type.equals(this.content_type))
			this.content_type = content_type;
	}
	
	public synchronized int getLevel() {
		return level;
	}
	
	public synchronized void reset() {
		loaded = 0;
	}
	
	public synchronized boolean isDone() {
		if (loaded == 1)
			return true;
		else
			return false;
	}
	
	public synchronized boolean setLoad() {
		if (loaded == 0) {
			loaded = -1;
			return true;
		} else
			return false;
	}

	public synchronized boolean endLoad(boolean status) {
		if (loaded == -1) {
			if (status)
				loaded = 1;
			else
				loaded = 0;
			return true;
		} else
			return false;
	}

	/**
	 * serialize to string as:
	 * path content_type level loaded url
	 */
	public synchronized String toString() {
		StringBuilder sb = new StringBuilder();
		if (path == null)
			sb.append("-");
		else
			sb.append(path);

		if (content_type == null)
			sb.append(" -");
		else
			sb.append(" ").append(content_type);
		
		sb.append(" ").append(Integer.toString(level));
		
		if (loaded < 0)
			sb.append(" 0");
		else
			sb.append(" 1");
		
		sb.append(" ").append(url);
        if (redirect_url != null)
            sb.append(System.lineSeparator()).append("REDIRECT ").append(redirect_url).append(" ").append(url);
		return sb.toString();
	}
	
	public URLObject(String s) {
		if (s == null)
			throw new InputMismatchException("string is null");
		Matcher m = split_pattern.matcher(s);
		m.find();
		if (m.groupCount() != 5)
    		throw new InputMismatchException("string format mismatch " + s);
		int len = m.groupCount();
		
		if (m.group(1).equals("-"))
			this.path = null;
		else
			this.path = m.group(1);
		
		if (m.group(2).equals("-"))
			this.content_type = null;
		else
			this.content_type = m.group(2);
		
		try {
			this.level = Integer.parseInt(m.group(3));
			if (this.level < 1)
				throw new InputMismatchException("level must be unsigned number " + s);
		} catch (NumberFormatException e) {
			throw new InputMismatchException("level must be unsigned number " + s);
		}
		
		try {
			this.loaded = Byte.parseByte(m.group(4));
			if (this.loaded != 0 && this.loaded != 1)
				throw new InputMismatchException("loaded must be 0 or 1 in " + s);
		} catch (NumberFormatException e) {
			throw new InputMismatchException("loaded must be 0 or 1 " + s);
		}
		
		this.url = m.group(5);
	}
    
    public synchronized void setRedirect(String redirect) {
        redirect_url = redirect;
    }
    
    public static boolean setRedirect(ConcurrentHashMap<String, URLObject> urls_map, ConcurrentHashMap<String, URLObject> redirect_map, String s) {
        Matcher m = redirect_pattern.matcher(s);
		if (m.find()) {
            String redirect = m.group(1);
            String url = m.group(2);
            setRedirect(urls_map, redirect_map, url, redirect);
            return true;
        } else
            return false;
    }
    
    public static void setRedirect(ConcurrentHashMap<String, URLObject> urls_map, ConcurrentHashMap<String, URLObject> redirect_map, String url, String redirect) {
        if (! urls_map.containsKey(url))
            throw new InputMismatchException(url + " not loaded with redirect");
        URLObject url_obj = urls_map.get(url);
        url_obj.setRedirect(redirect);
        redirect_map.put(redirect, url_obj);
    }
}
