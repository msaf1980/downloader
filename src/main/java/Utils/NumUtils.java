package Utils;

public class NumUtils {
	public static long ParseLong2(String s) {
		if (s.endsWith("k"))
			return Long.parseLong(s.substring(0, s.length()-2)) * 1024;
		else if (s.endsWith("M"))
			return Long.parseLong(s.substring(0, s.length()-2)) * 1048576;
		else if (s.endsWith("G"))
			return Long.parseLong(s.substring(0, s.length()-2)) * 1073741824;
		else
			return Long.parseLong(s);
	}
}
