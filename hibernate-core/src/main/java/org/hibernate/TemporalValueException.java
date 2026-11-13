package org.hibernate;

/// Indicates a temporal value cannot be represented within the selected mapping's supported
/// domain, or a stored temporal tuple is invalid.
///
/// @author Steve Ebersole
/// @since 8.0
public class TemporalValueException extends HibernateException {
	public TemporalValueException(String message) {
		super( message );
	}

	public TemporalValueException(String message, Throwable cause) {
		super( message, cause );
	}
}
