package Utils;

//checked exception
//disable full stack trace for perfomance reason (Java 1.6 and later)
public class CustomExceptionStackless extends CustomException {
	public CustomExceptionStackless(String message) {
		super(message);
	}
	
    @Override
    public synchronized Throwable fillInStackTrace() {
        // do nothing
        return this;
    }
}
