package Utils;

import java.text.MessageFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * Custom Logger (time with millisec)
 */
public class LogCustomFormatter extends Formatter {
	private final MessageFormat messageFormat = new MessageFormat("{0} {1} {2} [T:{3}]: {4} {5}\n");

	public String format(LogRecord record)
    {
        Object[] arguments = new Object[6];
        arguments[0] = DateTimeUtils.formatDateMilliSec(new Date(record.getMillis()));
        arguments[1] = record.getSourceClassName();
        arguments[2] = record.getSourceMethodName();
        arguments[3] = Long.toString( Thread.currentThread().getId() );
        arguments[4] = record.getLevel();
        arguments[5] = record.getMessage();

        return messageFormat.format(arguments);
    }
}
