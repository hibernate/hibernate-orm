package org.hibernate.query;

import jakarta.annotation.Nonnull;

import org.hibernate.HibernateException;
import org.hibernate.QueryException;

import java.util.Map;

/**
 * Indicates that validation and translation of one or more named
 * queries failed at initialization time. This exception packages
 * every {@link org.hibernate.QueryException} that occurred for an
 * invalid HQL/JPQL query, together with any exceptions that indicate
 * problems with named native SQL queries.
 *
 * @author Gavin King
 */
public class NamedQueryValidationException extends QueryException {
	@Nonnull
	private final Map<String, HibernateException> errors;

	public NamedQueryValidationException(@Nonnull String message, @Nonnull Map<String, HibernateException> errors) {
		super( message );
		this.errors = errors;
	}

	/**
	 * A map from query name to the error that occurred while
	 * interpreting or translating the named query.
	 */
	@Nonnull
	public Map<String, HibernateException> getErrors() {
		return errors;
	}
}
