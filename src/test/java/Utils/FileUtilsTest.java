package Utils;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;

import Utils.FileUtils;

public class FileUtilsTest {

	/**
	 * Test FileUtils.pathRelative and FileUtils.pathRelativeFile
	 * @param rootdir
	 * @param basefilename
	 * @param filename
	 * @param filename_check
	 */
	private void testPathRelative(String rootdir, String basefilename, String filename, String filename_check) {
		String result;
		String base_filename =  rootdir + File.separator + basefilename;
		String base_dir =  FileUtils.baseDir(base_filename);
		String file_name =  rootdir + File.separator + filename;
		
		result = FileUtils.pathRelativeFile(base_filename, file_name).replaceAll("\\\\", "/");
		assertEquals(result, filename_check);
		
		result = FileUtils.pathRelative(base_dir, file_name).replaceAll("\\\\", "/");
		assertEquals(result, filename_check);
	}
	
	private void testbaseDirFileName(String filename, String basedir_check, String basefile_check) {
		String[] nameEx = FileUtils.baseDirFileName(filename);
		assertEquals(nameEx[0], basedir_check);
		assertEquals(nameEx[1], basefile_check);
		
		String basedir = FileUtils.baseDir(filename);
		String basefile = FileUtils.baseName(filename);
		assertEquals(basedir, basedir_check);
		assertEquals(basefile, basefile_check);
	}
	
	@Test
	public void test() {
        /*
		String rootdir = System.getProperty("user.dir");
		
		String separator = File.separator;
		if (separator.equals("\\"))
			separator = "\\\\";
		
		//Test FileUtils.pathRelative and FileUtils.pathRelativeFile
		String basefilename;
		String filename;
		String filename_check;
		
		basefilename =  FileUtils.pathSepNormalize("index.html");
		filename = FileUtils.pathSepNormalize("site/image.png");
		filename_check = "site/image.png";
		
		testPathRelative(rootdir, basefilename, filename, filename_check);

		basefilename =  FileUtils.pathSepNormalize("wiki/index.html");
		filename = FileUtils.pathSepNormalize("site/image.png");
		filename_check = "../site/image.png";
		
		testPathRelative(rootdir, basefilename, filename, filename_check);

		filename = FileUtils.pathSepNormalize("site/image.png");
		String basedir_check = FileUtils.pathSepNormalize("site/");
		String basefile_check = "image.png";
		testbaseDirFileName(filename, basedir_check, basefile_check);
        */
	}

}
