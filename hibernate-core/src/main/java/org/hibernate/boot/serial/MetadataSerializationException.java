package org.hibernate.boot.serial;

import org.hibernate.HibernateException;

/// Indicates that a metadata archive could not be created, read, written, or restored.
///
/// @since 9.0
/// @author Steve Ebersole
public class MetadataSerializationException extends HibernateException {
	public MetadataSerializationException(String message) {
		super( message );
	}

	public MetadataSerializationException(String message, Throwable cause) {
		super( message, cause );
	}
}
