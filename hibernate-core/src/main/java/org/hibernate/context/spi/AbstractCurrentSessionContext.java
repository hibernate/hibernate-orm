/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.context.spi;

import org.hibernate.Session;
import org.hibernate.SessionBuilder;
import org.hibernate.context.TenantIdentifierMismatchException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.compare.EqualsHelper;

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
			if ( ! EqualsHelper.equals( existingSession.getTenantIdentifier(), current ) ) {
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
