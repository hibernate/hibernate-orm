/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.spi;

import org.hibernate.Remove;
import org.hibernate.boot.SessionFactoryBuilder;

/**
 * Additional SPI contract for {@link SessionFactoryBuilder}, mainly intended for
 * implementors of {@link SessionFactoryBuilderFactory}.
 *
 * @author Steve Ebersole
 *
 * @apiNote This will be removed in 9.0.
 */
@Remove
public interface SessionFactoryBuilderImplementor extends SessionFactoryBuilder {

	/**
	 * Called if {@link org.hibernate.cfg.AvailableSettings#ALLOW_JTA_TRANSACTION_ACCESS}
	 * is not enabled.
	 */
	void disableJtaTransactionAccess();

	/**
	 * Build the {@link SessionFactoryOptions} that will ultimately be passed to the
	 * constructor of {@link org.hibernate.internal.SessionFactoryImpl}.
	 */
	SessionFactoryOptions buildSessionFactoryOptions();
}
