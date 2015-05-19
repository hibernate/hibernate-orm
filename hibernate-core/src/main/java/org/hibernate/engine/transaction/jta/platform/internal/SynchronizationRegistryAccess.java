/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.transaction.jta.platform.internal;

import java.io.Serializable;
import javax.transaction.TransactionSynchronizationRegistry;

/**
 * Provides access to a {@link TransactionSynchronizationRegistry} for use by {@link TransactionSynchronizationRegistry}-based
 * {@link JtaSynchronizationStrategy} implementations.
 *
 * @author Steve Ebersole
 */
public interface SynchronizationRegistryAccess extends Serializable {
	/**
	 * Obtain the synchronization registry
	 *
	 * @return the synchronization registry
	 */
	public TransactionSynchronizationRegistry getSynchronizationRegistry();
}
