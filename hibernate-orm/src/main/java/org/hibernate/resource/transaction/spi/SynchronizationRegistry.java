/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.resource.transaction.spi;

import java.io.Serializable;
import javax.transaction.Synchronization;

import org.hibernate.resource.transaction.NullSynchronizationException;

/**
 * Manages a registry of (local) JTA {@link Synchronization} instances
 *
 * @author Steve Ebersole
 */
public interface SynchronizationRegistry extends Serializable {
	/**
	 * Register a {@link Synchronization} callback for this transaction.
	 *
	 * @param synchronization The synchronization callback to register.
	 *
	 * @throws NullSynchronizationException if the synchronization is {@code null}
	 */
	void registerSynchronization(Synchronization synchronization);
}
