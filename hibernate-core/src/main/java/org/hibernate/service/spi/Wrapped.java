/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.service.spi;

/**
 * Optional contract for services that wrap stuff that to which it is useful to have access.  For example, a service
 * that maintains a {@link javax.sql.DataSource} might want to expose access to the {@link javax.sql.DataSource} or
 * its {@link java.sql.Connection} instances.
 *
 * @author Steve Ebersole
 */
public interface Wrapped {
	/**
	 * Can this wrapped service be unwrapped as the indicated type?
	 *
	 * @param unwrapType The type to check.
	 *
	 * @return True/false.
	 */
	public boolean isUnwrappableAs(Class unwrapType);

	/**
	 * Unproxy the service proxy
	 *
	 * @param unwrapType The java type as which to unwrap this instance.
	 *
	 * @return The unwrapped reference
	 *
	 * @throws UnknownUnwrapTypeException if the service cannot be unwrapped as the indicated type
	 */
	public <T> T unwrap(Class<T> unwrapType);
}
