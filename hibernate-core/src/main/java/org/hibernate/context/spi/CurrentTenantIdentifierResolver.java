/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.context.spi;

/**
 * A callback registered with the {@link org.hibernate.SessionFactory} that is responsible for resolving the
 * current tenant identifier for use with {@link CurrentSessionContext} and
 * {@link org.hibernate.SessionFactory#getCurrentSession()}
 *
 * @author Steve Ebersole
 */
public interface CurrentTenantIdentifierResolver {
	/**
	 * Resolve the current tenant identifier.
	 * 
	 * @return The current tenant identifier
	 */
	public String resolveCurrentTenantIdentifier();

	/**
	 * Should we validate that the tenant identifier on "current sessions" that already exist when
	 * {@link CurrentSessionContext#currentSession()} is called matches the value returned here from
	 * {@link #resolveCurrentTenantIdentifier()}?
	 * 
	 * @return {@code true} indicates that the extra validation will be performed; {@code false} indicates it will not.
	 *
	 * @see org.hibernate.context.TenantIdentifierMismatchException
	 */
	public boolean validateExistingCurrentSessions();
}
