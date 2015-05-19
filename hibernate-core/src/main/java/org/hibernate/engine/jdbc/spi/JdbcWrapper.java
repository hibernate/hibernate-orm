/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.spi;


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
	public T getWrappedObject();
}
