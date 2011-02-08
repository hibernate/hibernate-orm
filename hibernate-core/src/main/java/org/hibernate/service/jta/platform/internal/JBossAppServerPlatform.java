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
package org.hibernate.service.jta.platform.internal;
import java.util.Map;
import javax.transaction.Synchronization;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.internal.util.jta.JtaStatusHelper;
import org.hibernate.service.jndi.spi.JndiService;
import org.hibernate.service.jta.platform.spi.JtaPlatform;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.InjectService;


/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class JBossAppServerPlatform implements JtaPlatform, Configurable {
	public static final String TM_NAME = "java:/TransactionManager";
	public static final String UT_NAME = "UserTransaction";
	public static final String REG_NAME = "java:comp/TransactionSynchronizationRegistry";

	private JndiService jndiService;

	@InjectService
	public void setJndiService(JndiService jndiService) {
		this.jndiService = jndiService;
	}

	private boolean cacheTransactionManager;

	public void configure(Map configValues) {
		cacheTransactionManager = ConfigurationHelper.getBoolean( CACHE_TM, configValues, true );
	}

	private TransactionManager transactionManager;

	public TransactionManager resolveTransactionManager() {
		if ( cacheTransactionManager ) {
			if ( transactionManager == null ) {
				transactionManager = (TransactionManager) jndiService.locate( TM_NAME );
			}
			return transactionManager;
		}
		else {
			return (TransactionManager) jndiService.locate( TM_NAME );
		}
	}

	public UserTransaction resolveUserTransaction() {
		return (UserTransaction) jndiService.locate( UT_NAME );
	}

	public void registerSynchronization(Synchronization synchronization) {
		getTransactionSynchronizationRegistry().registerInterposedSynchronization( synchronization );
	}

	private TransactionSynchronizationRegistry getTransactionSynchronizationRegistry() {
		return (TransactionSynchronizationRegistry) jndiService.locate( REG_NAME );
	}

	public boolean canRegisterSynchronization() {
		TransactionSynchronizationRegistry registry = getTransactionSynchronizationRegistry();
		int status = registry.getTransactionStatus();
		return JtaStatusHelper.isActive( status ) && ! registry.getRollbackOnly();
	}
}
