package org.hibernate.testing.jdbc.leak;

/**
 * @author Vlad Mihalcea
 */
public class ConnectionLeakException extends RuntimeException {

	public ConnectionLeakException() {
	}

	public ConnectionLeakException(String message) {
		super( message );
	}

	public ConnectionLeakException(String message, Throwable cause) {
		super( message, cause );
	}

	public ConnectionLeakException(Throwable cause) {
		super( cause );
	}
}
