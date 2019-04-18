package Utils;

public class StrUtils {
	/**
	 * Replace all chars c to r in string str 
	 * @param s
	 * @param c
	 * @param r
	 * @return
	 */
	public static String ReplaceChar(String str, char c, char r) {
		 int pos = str.indexOf(c);
	     if (pos == -1)
	    	 return str;
	    
		 char[] charArr = str.toCharArray();

	     int length = charArr.length;
	     for (int i = pos; i < length; i++) {
	    	 if (charArr[i] == c)
	    		 charArr[i] = r;
	     }
	     return new String(charArr);
	}
	
	/**
	 * Replace all substrings c to r in string str 
	 * @param s
	 * @param c
	 * @param r
	 * @return
	 */
	public static String ReplaceSubstr(String str, String c, String r) {
		 int pos = str.indexOf(c);
		 int nextpos;
	     if (pos == -1)
	    	 return str;
	    
	     int len = str.length();
		 int lenc = c.length();
		 StringBuilder sb = new StringBuilder(len);
	     sb.append(str.substring(0, pos)).append(r);
		 pos++;
		 while (pos >= 0 || pos < len) {
			 nextpos = str.indexOf(c, pos);
			 if (nextpos == -1) {
				 sb.append(str.substring(pos));
				 break;
			 }
			 sb.append(str.substring(pos, nextpos)).append(r);
			 pos = nextpos + lenc;
			 
	     }
	     return sb.toString();
	}
}
