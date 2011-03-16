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
package org.hibernate.testing.jta;

import javax.sql.DataSource;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

import com.arjuna.ats.arjuna.common.ObjectStoreEnvironmentBean;
import com.arjuna.ats.arjuna.common.arjPropertyManager;
import com.arjuna.ats.internal.arjuna.objectstore.VolatileStore;
import com.arjuna.common.internal.util.propertyservice.BeanPopulator;
import org.enhydra.jdbc.standard.StandardXADataSource;

import org.hibernate.cfg.Environment;
import org.hibernate.service.jdbc.connections.internal.DatasourceConnectionProviderImpl;
import org.hibernate.service.jta.platform.internal.JBossStandAloneJtaPlatform;
import org.hibernate.service.jta.platform.internal.JtaPlatformInitiator;

/**
 * Manages the {@link TransactionManager}, {@link UserTransaction} and {@link DataSource} instances used for testing.
 *
 * @author Steve Ebersole
 */
@SuppressWarnings( {"UnusedDeclaration", "unchecked"})
public class TestingJtaBootstrap {
	public static final TestingJtaBootstrap INSTANCE = new TestingJtaBootstrap();

	private TransactionManager transactionManager;
	private UserTransaction userTransaction;
	private DataSource dataSource;

	private TestingJtaBootstrap() {
		BeanPopulator
				.getDefaultInstance( ObjectStoreEnvironmentBean.class )
				.setObjectStoreType( VolatileStore.class.getName() );

		BeanPopulator
				.getNamedInstance( ObjectStoreEnvironmentBean.class, "communicationStore" )
				.setObjectStoreType( VolatileStore.class.getName() );

		BeanPopulator
				.getNamedInstance( ObjectStoreEnvironmentBean.class, "stateStore" )
				.setObjectStoreType( VolatileStore.class.getName() );

		this.transactionManager = com.arjuna.ats.jta.TransactionManager.transactionManager();
		this.userTransaction = com.arjuna.ats.jta.UserTransaction.userTransaction();

		Properties environmentProperties = Environment.getProperties();
		StandardXADataSource dataSource = new StandardXADataSource();
		dataSource.setTransactionManager( com.arjuna.ats.jta.TransactionManager.transactionManager() );
		try {
			dataSource.setDriverName( environmentProperties.getProperty( Environment.DRIVER ) );
		}
		catch (SQLException e) {
			throw new RuntimeException( "Unable to set DataSource JDBC driver name", e );
		}
		dataSource.setUrl( environmentProperties.getProperty( Environment.URL ) );
		dataSource.setUser( environmentProperties.getProperty( Environment.USER ) );
		dataSource.setPassword( environmentProperties.getProperty( Environment.PASS ) );
		final String isolationString = environmentProperties.getProperty( Environment.ISOLATION );
		if ( isolationString != null ) {
			dataSource.setTransactionIsolation( Integer.valueOf( isolationString ) );
		}
		this.dataSource = dataSource;
	}

	public TransactionManager getTransactionManager() {
		return transactionManager;
	}

	public UserTransaction getUserTransaction() {
		return userTransaction;
	}

	public DataSource getDataSource() {
		return dataSource;
	}

	public static void prepare(Map configValues) {
		configValues.put( JtaPlatformInitiator.JTA_PLATFORM, new JBossStandAloneJtaPlatform() );
		configValues.put( Environment.CONNECTION_PROVIDER, DatasourceConnectionProviderImpl.class.getName() );
		configValues.put( Environment.DATASOURCE, INSTANCE.getDataSource() );
	}
}
