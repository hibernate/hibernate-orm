/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.context.spi;

import java.util.Objects;

import org.hibernate.Session;
import org.hibernate.SessionBuilder;
import org.hibernate.context.TenantIdentifierMismatchException;
import org.hibernate.engine.spi.SessionFactoryImplementor;

/**
 * Base support for {@link CurrentSessionContext} implementors.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractCurrentSessionContext implements CurrentSessionContext {
	private final SessionFactoryImplementor factory;

	protected AbstractCurrentSessionContext(SessionFactoryImplementor factory) {
		this.factory = factory;
	}

	/**
	 * Access to the SessionFactory
	 *
	 * @return The SessionFactory being serviced by this context
	 */
	public SessionFactoryImplementor factory() {
		return factory;
	}

	protected SessionBuilder baseSessionBuilder() {
		final SessionBuilder builder = factory.withOptions();
		final CurrentTenantIdentifierResolver resolver = factory.getCurrentTenantIdentifierResolver();
		if ( resolver != null ) {
			builder.tenantIdentifier( resolver.resolveCurrentTenantIdentifier() );
		}
		return builder;
	}

	protected void validateExistingSession(Session existingSession) {
		final CurrentTenantIdentifierResolver resolver = factory.getCurrentTenantIdentifierResolver();
		if ( resolver != null && resolver.validateExistingCurrentSessions() ) {
			final String current = resolver.resolveCurrentTenantIdentifier();
			if ( !Objects.equals( existingSession.getTenantIdentifier(), current ) ) {
				throw new TenantIdentifierMismatchException(
						String.format(
								"Reported current tenant identifier [%s] did not match tenant identifier from " +
										"existing session [%s]",
								current,
								existingSession.getTenantIdentifier()
						)
				);
			}
		}
	}
}
