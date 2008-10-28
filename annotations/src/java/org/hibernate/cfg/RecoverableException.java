//$
package org.hibernate.cfg;

import org.hibernate.AnnotationException;

/**
 * Should neven be exposed to the client
 * An exception that wrap an underlying exception whith the hope
 * subsequent processing will recover from it.
 *
 * @author Emmanuel Bernard
 */
public class RecoverableException extends AnnotationException {
	public RecoverableException(String msg, Throwable root) {
		super( msg, root );
	}

	public RecoverableException(Throwable root) {
		super( root );
	}

	public RecoverableException(String s) {
		super( s );
	}
}
