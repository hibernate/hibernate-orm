/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.spi;

import org.hibernate.boot.SessionFactoryBuilder;
import static org.hibernate.internal.log.DeprecationLogger.DEPRECATION_LOGGER;

/**
 * Additional SPI contract for {@link SessionFactoryBuilder}, mainly intended for
 * implementors of {@link SessionFactoryBuilderFactory}.
 *
 * @author Steve Ebersole
 */
public interface SessionFactoryBuilderImplementor extends SessionFactoryBuilder {

	/**
	 * Called if {@link org.hibernate.cfg.AvailableSettings#ALLOW_JTA_TRANSACTION_ACCESS}
	 * is not enabled.
	 */
	void disableJtaTransactionAccess();

	/**
	 * @deprecated with no replacement.
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	default void disableRefreshDetachedEntity() {
		DEPRECATION_LOGGER.deprecatedRefreshLockDetachedEntity();
	}

	/**
	 * Build the {@link SessionFactoryOptions} that will ultimately be passed to the
	 * constructor of {@link org.hibernate.internal.SessionFactoryImpl}.
	 */
	SessionFactoryOptions buildSessionFactoryOptions();
}
