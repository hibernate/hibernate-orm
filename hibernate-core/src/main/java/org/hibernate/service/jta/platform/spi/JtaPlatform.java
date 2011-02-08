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
package org.hibernate.service.jta.platform.spi;
import javax.transaction.Synchronization;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;
import org.hibernate.service.spi.Service;

/**
 * Defines how we interact with various JTA services on the given platform/environment.
 *
 * @author Steve Ebersole
 */
public interface JtaPlatform extends Service {
	/**
	 * A configuration value key used to indicate that it is safe to cache
	 * {@link TransactionManager} references.
	 *
	 * @todo add to Environment.
	 * @todo same for UserTransaction too?  On many platforms UserTransaction and TransactionManager are the same
	 */
	public static final String CACHE_TM = "hibernate.jta.cacheTransactionManager";

	/**
	 * Locate the {@link TransactionManager}
	 *
	 * @return The {@link TransactionManager}
	 */
	public TransactionManager resolveTransactionManager();

	/**
	 * Locate the {@link UserTransaction}
	 *
	 * @return The {@link UserTransaction}
	 */
	public UserTransaction resolveUserTransaction();

	/**
	 * Register a JTA {@link Synchronization} in the means defined by the platform.
	 *
	 * @param synchronization The synchronization to register
	 */
	public void registerSynchronization(Synchronization synchronization);

	/**
	 * Can we currently regsiter a {@link Synchronization}?
	 *
	 * @return True if regsitering a {@link Synchronization} is currently allowed; false otherwise.
	 */
	public boolean canRegisterSynchronization();
}
