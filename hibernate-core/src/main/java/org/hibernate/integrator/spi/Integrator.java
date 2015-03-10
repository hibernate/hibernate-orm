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

import org.hibernate.boot.Metadata;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

/**
 * Contract for stuff that integrates with Hibernate.
 * <p/>
 * IMPL NOTE: called during session factory initialization (constructor), so not all parts of the passed session factory
 * will be available.
 *
 * @author Steve Ebersole
 * @since 4.0
 */
public interface Integrator {

	/**
	 * Perform integration.
	 *
	 * @param metadata The "compiled" representation of the mapping information
	 * @param sessionFactory The session factory being created
	 * @param serviceRegistry The session factory's service registry
	 */
	public void integrate(
			Metadata metadata,
			SessionFactoryImplementor sessionFactory,
			SessionFactoryServiceRegistry serviceRegistry);

	/**
	 * Tongue-in-cheek name for a shutdown callback.
	 *
	 * @param sessionFactory The session factory being closed.
	 * @param serviceRegistry That session factory's service registry
	 */
	public void disintegrate(SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry);

}
