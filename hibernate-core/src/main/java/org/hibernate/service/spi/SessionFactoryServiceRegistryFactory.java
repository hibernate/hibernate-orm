/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.service.spi;

import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.source.MetadataImplementor;
import org.hibernate.service.Service;
import org.hibernate.service.internal.SessionFactoryServiceRegistryImpl;

/**
 * Contract for builder of {@link SessionFactoryServiceRegistry} instances.
 * <p/>
 * Is itself a service within the standard service registry.
 *
 * @author Steve Ebersole
 */
public interface SessionFactoryServiceRegistryFactory extends Service {
	/**
	 * Create the registry.
	 *
	 * @todo : fully expect this signature to change!
	 *
	 * @param sessionFactory The (in flux) session factory.  Generally this is useful for grabbing a reference for later
	 * 		use.  However, care should be taken when invoking on the session factory until after it has been fully
	 * 		initialized.
	 * @param configuration The configuration object.
	 *
	 * @return The registry
	 */
	public SessionFactoryServiceRegistryImpl buildServiceRegistry(
			SessionFactoryImplementor sessionFactory,
			Configuration configuration);

	/**
	 * Create the registry.
	 *
	 * @todo : fully expect this signature to change!
	 *
	 * @param sessionFactory The (in flux) session factory.  Generally this is useful for grabbing a reference for later
	 * 		use.  However, care should be taken when invoking on the session factory until after it has been fully
	 * 		initialized.
	 * @param metadata The configuration object.
	 *
	 * @return The registry
	 */
	public SessionFactoryServiceRegistryImpl buildServiceRegistry(
			SessionFactoryImplementor sessionFactory,
			MetadataImplementor metadata);
}
