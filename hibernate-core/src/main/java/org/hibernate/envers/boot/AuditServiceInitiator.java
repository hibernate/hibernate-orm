/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.boot;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.envers.boot.internal.AuditServiceImpl;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.SessionFactoryServiceInitiator;

/**
 * Initiator for the {@link AuditService}.
 *
 * @author Chris Cranford
 * @since 6.0
 */
public class AuditServiceInitiator implements SessionFactoryServiceInitiator<AuditService> {
	public static final AuditServiceInitiator INSTANCE = new AuditServiceInitiator();

	@Override
	public Class<AuditService> getServiceInitiated() {
		return AuditService.class;
	}

	@Override
	public AuditService initiateService(SessionFactoryImplementor sessionFactory,
			SessionFactoryOptions sessionFactoryOptions,
			ServiceRegistryImplementor registry) {
		return new AuditServiceImpl( registry );
	}
}
