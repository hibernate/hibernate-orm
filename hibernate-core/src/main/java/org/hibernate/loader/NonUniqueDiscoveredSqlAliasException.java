package org.hibernate.loader;

import jakarta.annotation.Nonnull;

import org.hibernate.HibernateException;

/**
 * Thrown when Hibernate encounters a non-unique SQL alias in the ResultSet
 * while processing the results of a {@linkplain org.hibernate.query.NativeQuery}
 * using auto-discovery to understand the {@linkplain java.sql.ResultSetMetaData ResultSet metadata}
 * for mapping the JDBC values to the domain result.
 *
 * @author Steve Ebersole
 */
public class NonUniqueDiscoveredSqlAliasException extends HibernateException {
	public NonUniqueDiscoveredSqlAliasException(@Nonnull String message) {
		super( message );
	}
}
