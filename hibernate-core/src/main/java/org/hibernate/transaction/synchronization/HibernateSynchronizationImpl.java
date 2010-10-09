/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.transaction.synchronization;

import javax.transaction.Synchronization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link Synchronization} implementation Hibernate registers with the JTA {@link javax.transaction.Transaction}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class HibernateSynchronizationImpl implements Synchronization {
	private static final Logger log = LoggerFactory.getLogger( HibernateSynchronizationImpl.class );

	private final CallbackCoordinator coordinator;

	public HibernateSynchronizationImpl(CallbackCoordinator coordinator) {
		this.coordinator = coordinator;
	}

	/**
	 * {@inheritDoc}
	 */
	public void beforeCompletion() {
		log.trace( "JTA sync : beforeCompletion()" );
		coordinator.beforeCompletion();
	}

	/**
	 * {@inheritDoc}
	 */
	public void afterCompletion(int status) {
		log.trace( "JTA sync : afterCompletion({})", status );
		coordinator.afterCompletion( status );
	}
}
