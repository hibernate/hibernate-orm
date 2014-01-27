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
package org.hibernate.integrator.spi;

import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

/**
 * Contract for stuff that integrates with Hibernate.
 * <p/>
 * IMPL NOTE: called during session factory initialization (constructor), so not all parts of the passed session factory
 * will be available.
 * <p/>
 * For more information, see the following jiras:<ul>
 *     <li><a href="https://hibernate.onjira.com/browse/HHH-5562">HHH-5562</a></li>
 *     <li><a href="https://hibernate.onjira.com/browse/HHH-6081">HHH-6081</a></li>
 * </ul>
 *
 * @author Steve Ebersole
 * @since 4.0
 *
 * @todo : the signature here *will* change, guaranteed
 * @todo : better name ?
 */
public interface Integrator {

	/**
	 * Perform integration.
	 *
	 * @param configuration The configuration used to create the session factory
	 * @param sessionFactory The session factory being created
	 * @param serviceRegistry The session factory's service registry
	 */
	public void integrate(
			Configuration configuration,
			SessionFactoryImplementor sessionFactory,
			SessionFactoryServiceRegistry serviceRegistry);

	/**
     * Perform integration.
     *
     * @param metadata The metadata used to create the session factory
     * @param sessionFactory The session factory being created
     * @param serviceRegistry The session factory's service registry
     */
    public void integrate( MetadataImplementor metadata,
                           SessionFactoryImplementor sessionFactory,
                           SessionFactoryServiceRegistry serviceRegistry );

	/**
	 * Tongue-in-cheek name for a shutdown callback.
	 *
	 * @param sessionFactory The session factory being closed.
	 * @param serviceRegistry That session factory's service registry
	 */
	public void disintegrate(SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry);

}
