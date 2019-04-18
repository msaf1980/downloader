package downloader;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import Utils.CustomException;

public class RegexReplace {
	private String regex = null;
	private String replace;
	private int flags = Pattern.UNICODE_CHARACTER_CLASS;
	private Pattern p = null;
	
	public RegexReplace(String regex, String replace, String s_flags) {
		if (regex == null || regex.equals(""))
			return;
		this.regex = regex;
		if (replace == null)
			this.replace = "";
		else
			this.replace = replace;
		
		if (s_flags != null) {
			char[] c_flags = s_flags.toCharArray();
			for (int i = c_flags.length - 1; i >= 0; i--) {
				switch(c_flags[i]) {
				case 'i':
					flags |= Pattern.CASE_INSENSITIVE;
					break;
				}
			}
		}
		
		p = Pattern.compile(regex, flags);
	}
	
	/**
	 * Return rewrited string 
	 * @param s
	 * @return
	 */
	public String replace(String s) {
		if (p == null || s == null)
			return s;
		Matcher m = p.matcher(s);
		if (m.find())
			return m.replaceAll(replace);
		return s;
	}
}
