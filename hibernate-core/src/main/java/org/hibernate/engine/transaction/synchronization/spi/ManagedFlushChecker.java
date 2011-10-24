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
package org.hibernate.engine.transaction.synchronization.spi;

import java.io.Serializable;

import org.hibernate.engine.transaction.spi.TransactionCoordinator;

/**
 * A pluggable strategy for defining how the {@link javax.transaction.Synchronization} registered by Hibernate determines
 * whether to perform a managed flush.  An exceptions from either this delegate or the subsequent flush are routed
 * through the sister strategy {@link ExceptionMapper}.
 *
 * @author Steve Ebersole
 */
public interface ManagedFlushChecker  extends Serializable {
	/**
	 * Check whether we should perform the managed flush
	 *
	 * @param coordinator The transaction coordinator
	 * @param jtaStatus The status of the current JTA transaction.
	 *
	 * @return True to indicate to perform the managed flush; false otherwise.
	 */
	public boolean shouldDoManagedFlush(TransactionCoordinator coordinator, int jtaStatus);
}
