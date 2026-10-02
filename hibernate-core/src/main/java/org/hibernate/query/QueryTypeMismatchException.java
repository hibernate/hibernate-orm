package org.hibernate.query;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.HibernateException;

/**
 * Indicates a mismatch between the expected and actual result types of a query.
 *
 * @author Steve Ebersole
 */
public class QueryTypeMismatchException extends HibernateException {
	public QueryTypeMismatchException(@Nonnull String message) {
		super( message );
	}

	public QueryTypeMismatchException(@Nonnull String message, @Nullable Throwable cause) {
		super( message, cause );
	}
}
