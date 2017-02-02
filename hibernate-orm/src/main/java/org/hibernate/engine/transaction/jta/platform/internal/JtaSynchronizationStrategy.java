/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.transaction.jta.platform.internal;

import java.io.Serializable;
import javax.transaction.Synchronization;

/**
 * Contract used to centralize {@link Synchronization} handling logic.
 *
 * @author Steve Ebersole
 */
public interface JtaSynchronizationStrategy extends Serializable {
	/**
	 * Register a synchronization
	 *
	 * @param synchronization The synchronization to register.
	 */
	public void registerSynchronization(Synchronization synchronization);

	/**
	 * Can a synchronization be registered?
	 *
	 * @return {@literal true}/{@literal false}
	 */
	public boolean canRegisterSynchronization();
}
