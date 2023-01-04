/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.spi;

import org.hibernate.boot.SessionFactoryBuilder;

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
	 * Called if {@link org.hibernate.cfg.AvailableSettings#ALLOW_REFRESH_DETACHED_ENTITY}
	 * is not enabled.
	 */
	default void disableRefreshDetachedEntity() {
	}

	/**
	 * Build the {@link SessionFactoryOptions} that will ultimately be passed to the
	 * constructor of {@link org.hibernate.internal.SessionFactoryImpl}.
	 */
	SessionFactoryOptions buildSessionFactoryOptions();
}
