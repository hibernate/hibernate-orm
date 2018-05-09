package org.hibernate.tool.ide.completion;

import java.io.ObjectStreamClass;

/**
 * Exception that can be thrown when the lexer encounters errors (such as syntax errors etc.)
 * 
 * @author Max Rydahl Andersen
 *
 */
public class SimpleLexerException extends RuntimeException {

	private static final long serialVersionUID = 
			ObjectStreamClass.lookup(SimpleLexerException.class).getSerialVersionUID();
	
	public SimpleLexerException() {
		super();
	}

	public SimpleLexerException(String message, Throwable cause) {
		super( message, cause );
	}

	public SimpleLexerException(String message) {
		super( message );
	}

	public SimpleLexerException(Throwable cause) {
		super( cause );
	}

}
