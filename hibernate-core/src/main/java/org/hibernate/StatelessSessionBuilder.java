/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

import java.sql.Connection;

/**
 * Represents a consolidation of all stateless session creation options into a builder style delegate.
 *
 * @author Steve Ebersole
 */
public interface StatelessSessionBuilder<T extends StatelessSessionBuilder> {
	/**
	 * Opens a session with the specified options.
	 *
	 * @return The session
	 */
	StatelessSession openStatelessSession();

	/**
	 * Adds a specific connection to the session options.
	 *
	 * @param connection The connection to use.
	 *
	 * @return {@code this}, for method chaining
	 */
	T connection(Connection connection);

	/**
	 * Define the tenant identifier to be associated with the opened session.
	 *
	 * @param tenantIdentifier The tenant identifier.
	 *
	 * @return {@code this}, for method chaining
	 */
	T tenantIdentifier(String tenantIdentifier);

	/**
	 * Should {@link org.hibernate.query.Query#setParameter} perform parameter validation
	 * when the Session is bootstrapped via JPA {@link javax.persistence.EntityManagerFactory}
	 *
	 * @param enabled {@code true} indicates the validation should be performed, {@code false} otherwise
	 * <p>
	 * The default value is {@code true}
	 *
	 * @return {@code this}, for method chaining
	 */
	default T setQueryParameterValidation(boolean enabled) {
		return (T) this;
	}
}
