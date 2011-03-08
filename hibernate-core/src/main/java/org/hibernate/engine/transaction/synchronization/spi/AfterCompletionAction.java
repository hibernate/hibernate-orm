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


import org.hibernate.engine.transaction.spi.TransactionCoordinator;

/**
 * A pluggable strategy for defining any actions to be performed during
 * {@link javax.transaction.Synchronization#afterCompletion} processing from the the
 * {@link javax.transaction.Synchronization} registered by Hibernate with the underlying JTA platform.
 *
 * @author Steve Ebersole
 */
public interface AfterCompletionAction {
	public void doAction(TransactionCoordinator transactionCoordinator, int status);
}
