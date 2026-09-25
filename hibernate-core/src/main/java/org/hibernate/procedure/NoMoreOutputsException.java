package org.hibernate.procedure;

import org.hibernate.HibernateException;

/// Indicates that all [outputs][Output] have been processed.
///
/// @author Steve Ebersole
public class NoMoreOutputsException extends HibernateException {
	public NoMoreOutputsException(String message) {
		super( message );
	}

	public NoMoreOutputsException() {
		super( "Results have been exhausted" );
	}
}
