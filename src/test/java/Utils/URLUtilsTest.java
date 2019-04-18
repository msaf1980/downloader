package Utils;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;

import Utils.URLUtils;
import java.net.MalformedURLException;
import java.net.URL;

public class URLUtilsTest {
	private void testbaseDirFileName(String filename, String basedir_check, String basefile_check) {
		String[] nameEx = URLUtils.baseDirFileName(filename);
		assertEquals(nameEx[0], basedir_check);
		assertEquals(nameEx[1], basefile_check);
	}
	
	@Test
	public void testAllbaseDirFileName() {
		String filename;
		String basedir_check;
		String basefile_check;
		
		filename = "site/image.png";
		basedir_check = "site/";
		basefile_check = "image.png";
		testbaseDirFileName(filename, basedir_check, basefile_check);
		
		filename = "wiki/a.png/";
		basedir_check = "wiki/a.png/";
		basefile_check = "";
		testbaseDirFileName(filename, basedir_check, basefile_check);
	}
    
    private void testEqualWithoutProto(String url1, String url2) {
        URL u1;
        URL u2;
		try {
            u1 = new URL(url1);
        	u2 = new URL(url2);
            assert(URLUtils.equalWithoutProto(u1, u2));
		} catch (MalformedURLException e) {
    	}
    }
    
    private void testNonEqualWithoutProto(String url1, String url2) {
        URL u1;
        URL u2;
		try {
            u1 = new URL(url1);
        	u2 = new URL(url2);
            assert(! URLUtils.equalWithoutProto(u1, u2));
		} catch (MalformedURLException e) {
    	}
    }
    
    @Test
	public void testAllEqualWithoutProto() {
        String url1;
        String url2;
        
        url1 = "http://www.ibm.com/developerworks/ru/library/os-apache_1";
        url2 = "https://www.ibm.com/developerworks/ru/library/os-apache_1";
        testEqualWithoutProto(url1, url2);

        url1 = "http://www.ibm.com:8080/developerworks/ru/library/os-apache_1";
        url2 = "https://www.ibm.com/developerworks/ru/library/os-apache_1";
        testNonEqualWithoutProto(url1, url2);
        
        url1 = "http://www.ibm.com/developerworks/ru/library/os-apache_1";
        url2 = "https://www.ibm.com/developerworks/ru/library/os-apache_2";
        testNonEqualWithoutProto(url1, url2);
    }
}
