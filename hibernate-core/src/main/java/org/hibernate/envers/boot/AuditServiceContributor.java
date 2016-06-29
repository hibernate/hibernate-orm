/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.boot;

import org.hibernate.service.spi.SessionFactoryServiceContributor;
import org.hibernate.service.spi.SessionFactoryServiceRegistryBuilder;

/**
 * Envers implementation of the {@link SessionFactoryServiceContributor} contract that adds the
 * {@link AuditService} implementation to the session factory services.
 *
 * @author Chris Cranford
 * @since 6.0
 */
public class AuditServiceContributor implements SessionFactoryServiceContributor {
	@Override
	public void contribute(SessionFactoryServiceRegistryBuilder serviceRegistryBuilder) {
		serviceRegistryBuilder.addInitiator( AuditServiceInitiator.INSTANCE );
	}
}
