package Utils;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateTimeUtils {
    static final SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    static final SimpleDateFormat sdfDateMilliSec = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	
    public static String formatDate(Date date) {
	    return sdfDate.format(date);
	}
    
	public static String formatDateMilliSec(Date date) {
	    return sdfDateMilliSec.format(date);
	}
}
