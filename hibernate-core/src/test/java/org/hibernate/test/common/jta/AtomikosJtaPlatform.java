/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.common.jta;

import javax.sql.DataSource;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;
import org.hibernate.HibernateLogger;
import org.hibernate.service.jta.platform.internal.AbstractJtaPlatform;
import org.hibernate.service.jta.platform.internal.JtaSynchronizationStrategy;
import org.hibernate.service.jta.platform.internal.TransactionManagerBasedSynchronizationStrategy;
import org.hibernate.service.jta.platform.spi.JtaPlatformException;
import org.hibernate.service.spi.Startable;
import org.hibernate.service.spi.Stoppable;
import org.hibernate.test.common.ConnectionProviderBuilder;
import org.jboss.logging.Logger;
import com.atomikos.icatch.jta.UserTransactionImp;
import com.atomikos.icatch.jta.UserTransactionManager;
import com.atomikos.jdbc.nonxa.AtomikosNonXADataSourceBean;

/**
 * @author Steve Ebersole
 */
public class AtomikosJtaPlatform extends AbstractJtaPlatform implements Startable, Stoppable {

    private static final HibernateLogger LOG = Logger.getMessageLogger(HibernateLogger.class, AtomikosJtaPlatform.class.getName());

	private final JtaSynchronizationStrategy synchronizationStrategy = new TransactionManagerBasedSynchronizationStrategy( this );

	private UserTransactionManager transactionManager;
	private AtomikosNonXADataSourceBean dataSourceBean;

	public DataSource getDataSource() {
		return dataSourceBean;
	}

	@Override
	protected TransactionManager locateTransactionManager() {
		return transactionManager;
	}

	@Override
	protected UserTransaction locateUserTransaction() {
		return new UserTransactionImp();
	}

	@Override
	protected JtaSynchronizationStrategy getSynchronizationStrategy() {
		return synchronizationStrategy;
	}

	@Override
	public void start() {
		if ( transactionManager == null ) {
			transactionManager = new UserTransactionManager();
			try {
				transactionManager.init();
			}
			catch (Exception e) {
				throw new JtaPlatformException( "Unable to init Atomikos UserTransactionManager", e );
			}
		}

		if ( dataSourceBean == null ) {
			// todo : extract sys props to handle functional testing...
			dataSourceBean = new AtomikosNonXADataSourceBean();
			dataSourceBean.setUniqueResourceName( "h2" );
			dataSourceBean.setDriverClassName( ConnectionProviderBuilder.DRIVER );
			dataSourceBean.setUrl( ConnectionProviderBuilder.URL );
			dataSourceBean.setUser( ConnectionProviderBuilder.USER );
			dataSourceBean.setPassword( ConnectionProviderBuilder.PASS );
			dataSourceBean.setPoolSize( 3 );
			try {
				dataSourceBean.init();
			}
			catch (Exception e) {
				throw new JtaPlatformException( "Unable to init Atomikos DataSourceBean", e );
			}
		}
	}

	@Override
	public void stop() {
		if ( dataSourceBean != null ) {
			try {
				dataSourceBean.close();
			}
			catch (Exception e) {
                LOG.debug("Error closing DataSourceBean", e);
			}
		}

		if ( transactionManager != null ) {
			try {
				transactionManager.close();
			}
			catch (Exception e) {
                LOG.debug("Error closing UserTransactionManager", e);
			}
		}
	}
}
