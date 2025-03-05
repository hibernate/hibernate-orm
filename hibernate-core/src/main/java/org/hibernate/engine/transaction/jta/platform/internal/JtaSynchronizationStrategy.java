/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.transaction.jta.platform.internal;

import java.io.Serializable;
import jakarta.transaction.Synchronization;

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
	void registerSynchronization(Synchronization synchronization);

	/**
	 * Can a synchronization be registered?
	 *
	 * @return {@literal true}/{@literal false}
	 */
	boolean canRegisterSynchronization();
}
