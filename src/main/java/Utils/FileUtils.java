package Utils;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.nio.file.Paths;
//import java.util.HashSet;
import java.util.regex.Pattern;

public class FileUtils {
	/*
	private static final HashSet<Character> charsInvalid = new HashSet<Character>();

	static {
		charsInvalid.add('#');
		charsInvalid.add('@');
		charsInvalid.add('*');
		charsInvalid.add('+');
		charsInvalid.add('{');
		charsInvalid.add('}');
		charsInvalid.add('<');
		charsInvalid.add('>');
		charsInvalid.add('[');
		charsInvalid.add(']');
		charsInvalid.add('?');
		charsInvalid.add('!');
		charsInvalid.add('$');
		charsInvalid.add('^');
		charsInvalid.add('%');
	}
	*/
	
	private static final Pattern INVALID_CHARS_PATTERN = 
			Pattern.compile("^[\\w-~\\\\./]+'$", Pattern.UNICODE_CHARACTER_CLASS);

	/**
	 * Validate filename for invalid characters 
	 * @param filename
	 * @return
	 */
	public static boolean validateFilename(String filename) {
		return INVALID_CHARS_PATTERN.matcher(filename).matches();
	}
	
	/**
	 * Get Absolute Path (with parent (dir) and child (file) path
	 * If dir not full path, then current directory appended also 
	 * @param parent
	 * @param child
	 * @return
	 */
	public static String pathJoin(String parent, String child) {
		return new File(parent, child).getAbsolutePath();
	}
	
	/*
	static String separator = File.separator;
	static String separator_rep;
	static Pattern separator_rep_p;
	static {
		if (separator.equals("\\")) {
			separator = "\\\\";
			separator_rep = "/";
		} else {
			separator_rep = "\\";
		}
		separator_rep_p = Pattern.compile(separator_rep);
	}
	
	/**
	 * Replace multiple separators (/ or /) in path to OS-depended
	 * @param path
	 * @return
	 */
	/*
	public static String pathSepNormalize(String path) {
		return separator_rep_p.matcher(path).replaceAll(separator);
	}
	*/
	
	static char separator = '\\';
	static char separator_rep = '/';
	static {
		if (File.separator.equals("\\")) {
			separator = '/';
			separator_rep = '\\';
		}
	}
	
	/**
	 * Replace multiple separators (/ or /) in path to OS-depended
	 * @param path
	 * @return
	 */
	public static String pathSepNormalize(String path) {
		return  StrUtils.ReplaceChar(path, separator,  separator_rep);
	}

	
	/**
	 * Get Relative Path againt base dir
	 * @param baseDir
	 * @param path
	 * @return
	 */
	public static String pathRelative(String baseDir, String path) {
		Path pathAbsolute = Paths.get(path);
        Path pathBase = Paths.get(baseDir);
        return pathBase.relativize(pathAbsolute).toString();
	}
	
	/**
	 * Get Relative Path againt file base dir
	 * @param baseDir
	 * @param path
	 * @return
	 */
	public static String pathRelativeFile(String baseFile, String path) {
		String baseDir = baseDir(baseFile);
		Path pathAbsolute = Paths.get(path);
        Path pathBase = Paths.get(baseDir);
        return pathBase.relativize(pathAbsolute).toString();
	}
	
	/**
	 * Get base dir and filename from path
	 * If path ended with path separator (\ on Windows or / on Unix), 
	 * return path (without last separator) for base dir and null for filename
	 * If path separator not found, return null for base dir and path for filename
	 * @param path
	 * @return
	 */
	public static String[] baseDirFileName(String path) {
		String[] f = new String[2];
		int pos = path.lastIndexOf(File.separator);
		if (pos == -1) {
			f[0] = "";
			f[1] = path;
		} else if (pos == 0) {
			f[0] = File.separator;
			f[1] = path.substring(1);
		} else if (pos == path.length() - 1) {
			f[0] = path.substring(0, path.length());
			f[1] = "";
		} else {
			f[0] = path.substring(0, pos + 1);
			f[1] = path.substring(pos + 1);
		}
		return f;
	}

	/**
	 * Get base dir from path
	 * If path ended with path separator (\ on Windows or / on Unix), 
	 * return path (without last separator)
	 * If path separator not found, return null
	 * @param path
	 * @return
	 */
	public static String baseDir(String path) {
		int pos = path.lastIndexOf(File.separator);
		if (pos == -1)
			return path;
		else if (pos == 0)
			return File.separator;
		else if (pos == path.length() - 1)
			return path.substring(0, path.length());
		else
			return path.substring(0, pos + 1);
	}
	
	/**
	 * Get filename from path
	 * If path ended with path separator (\ on Windows or / on Unix), 
	 * return null
	 * If path separator not found, return path
	 * @param path
	 * @return
	 */
	public static String baseName(String path) {
		int pos = path.lastIndexOf(File.separator);
		if (pos == -1)
			return path;
		else if (pos == 0)
			return path.substring(1);
		else if (pos == path.length() - 1)
			return "";
		else
			return path.substring(pos + 1);
	}
	
	/**
	 * Split filename to name and extension part (with dot, like .zip)
	 * @param fileName
	 * @return String array. [0] - name, [1] - extension
	 */
	public static String[] filenameExtension(String fileName) {
		String [] s = new String[2];
		int possep = fileName.lastIndexOf(File.separator);
		int pos = fileName.lastIndexOf('.');
		if (pos > 0 && pos > possep) {
			s[0] = fileName.substring(0, pos);
		    s[1] = fileName.substring(pos);
		} else {
			s[0] = fileName;
			s[1] = "";
		}
		
		return s;
	}

	/**
	 * Check if Dir
	 * @return
	1 - is a dir
	0 - not a dir
	*/

	public static int isDir(String path) {
		File file = new File(path);
		if (file.isDirectory())
			return 1;
		else
			return 0;
	}

	/**
	 * Check if Dir
	 * @return
	1 - dir is empthy
	2 - dir is not empthy
	0 - not a dir
	*/
	public static int isDirEmpthy(String path) {
		File file = new File(path);
		if (file.isDirectory()) {
			if(file.list().length>0) {
				return 2;
			} else {
				return 1;
			}
		} else
			return 0;
	}

	/**
	 * Check if File
	 * @param path
	 * @return
	1 - is a file
	0 - not a file
	*/
	public static int isFile(String path) {
		File file = new File(path);
		if (file.isFile())
			return 1;
		else
			return 0;
	}

	/**
	 * Check if File
	 * @parent - root filesystem object (dir)
	 * @child - child filesystem object (file or dir  + file)
	 * @return
	1 - is a file
	0 - not a file
	*/
	public static int isFile(String parent, String child) {
		File file = new File(parent, child);
		if (file.isFile())
			return 1;
		else
			return 0;
	}
	
	/**
	 * Check if File
	 * @param path
	 * @return
	1 - file is empthy
	2 - file is not empthy
	0 - not a file
	*/
	public static int isFileEmpthy(String path) {

		File file = new File(path);
		if (file.isFile()) {
			if (file.length() > 0)
				return 2;
			else
				return 1;
		} else
			return 0;
	}
	
	/**
	 * Check if File
	 * @parent - root filesystem object (dir)
	 * @child - child filesystem object (file or dir  + file)
	 * @return
	1 - file is empthy
	2 - file is not empthy
	0 - not a file
	*/
	public static int isFileEmpthy(String parent, String child) {
		File file = new File(parent, child);
		if (file.isFile()) {
			if (file.length() > 0)
				return 2;
			else
				return 1;
		} else
			return 0;
	}
	
	/**
	 * Close java.io.Reader silently with null check
	 * @param r java.io.Reader
	 * @throws IOException 
 	 */
	public void closeReaderQuietly(Reader r) {
		try {
			if (r != null)
				r.close();
		} catch (IOException e) { }
		}

}
