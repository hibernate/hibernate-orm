/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.jboss.as.jpa.hibernate5;


import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

import org.hibernate.engine.transaction.jta.platform.internal.JtaSynchronizationStrategy;
import org.hibernate.engine.transaction.jta.platform.internal.SynchronizationRegistryAccess;
import org.hibernate.engine.transaction.jta.platform.internal.SynchronizationRegistryBasedSynchronizationStrategy;

import org.jipijapa.plugin.spi.JtaManager;


/**
 * @author Steve Ebersole
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class JBossAppServerJtaPlatform
		extends org.hibernate.engine.transaction.jta.platform.internal.JBossAppServerJtaPlatform {

	private final JtaSynchronizationStrategy synchronizationStrategy;

	protected JtaManager getJtaManager() {
		return jtaManager;
	}

	private final JtaManager jtaManager;

	public JBossAppServerJtaPlatform(final JtaManager jtaManager) {
		this.jtaManager = jtaManager;
		this.synchronizationStrategy = new SynchronizationRegistryBasedSynchronizationStrategy( new SynchronizationRegistryAccess() {
			@Override
			public TransactionSynchronizationRegistry getSynchronizationRegistry() {
				return jtaManager.getSynchronizationRegistry();
			}
		} );
	}

	@Override
	protected boolean canCacheTransactionManager() {
		return true;
	}

	@Override
	protected TransactionManager locateTransactionManager() {
		return jtaManager.locateTransactionManager();
	}

	@Override
	protected JtaSynchronizationStrategy getSynchronizationStrategy() {
		return synchronizationStrategy;
	}
}
