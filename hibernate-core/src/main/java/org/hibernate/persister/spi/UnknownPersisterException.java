package org.hibernate.persister.spi;

import jakarta.annotation.Nonnull;

import org.hibernate.HibernateException;

/**
 * Indicates that the persister to use is not known and could not be determined.
 *
 * @author Steve Ebersole
 */
public class UnknownPersisterException extends HibernateException {
	public UnknownPersisterException(@Nonnull String s) {
		super( s );
	}

	public UnknownPersisterException(@Nonnull String string, @Nonnull Throwable root) {
		super( string, root );
	}
}
