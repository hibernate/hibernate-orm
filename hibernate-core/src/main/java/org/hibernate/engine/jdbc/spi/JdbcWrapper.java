package org.hibernate.engine.jdbc.spi;

import jakarta.annotation.Nonnull;

/**
 * Generic contract for wrapped JDBC objects.
 *
 * @param <T> One of either {@link java.sql.Connection}, {@link java.sql.Statement} or {@link java.sql.ResultSet}
 *
 * @author Steve Ebersole
 */
public interface JdbcWrapper<T> {
	/**
	 * Retrieve the wrapped JDBC object.
	 *
	 * @return The wrapped object
	 */
	@Nonnull
	T getWrappedObject();
}
