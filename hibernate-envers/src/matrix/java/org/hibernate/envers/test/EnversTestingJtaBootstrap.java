package org.hibernate.envers.test;

import com.arjuna.ats.arjuna.common.ObjectStoreEnvironmentBean;
import com.arjuna.ats.internal.arjuna.objectstore.VolatileStore;
import com.arjuna.common.internal.util.propertyservice.BeanPopulator;
import org.enhydra.jdbc.standard.StandardXADataSource;
import org.hibernate.cfg.Environment;
import org.hibernate.ejb.AvailableSettings;
import org.hibernate.service.jdbc.connections.internal.DatasourceConnectionProviderImpl;
import org.hibernate.service.jta.platform.internal.JBossStandAloneJtaPlatform;

import javax.transaction.Status;
import javax.transaction.TransactionManager;
import java.sql.SQLException;
import java.util.Map;

/**
 * Copied from {@link org.hibernate.testing.jta.TestingJtaBootstrap}, as Envers tests use a different URL for
 * testing databases.
 * @author Adam Warski (adam at warski dot org)
 */
public class EnversTestingJtaBootstrap {
	public static TransactionManager updateConfigAndCreateTM(Map configValues) {
        BeanPopulator
				.getDefaultInstance(ObjectStoreEnvironmentBean.class)
				.setObjectStoreType( VolatileStore.class.getName() );

		BeanPopulator
				.getNamedInstance( ObjectStoreEnvironmentBean.class, "communicationStore" )
				.setObjectStoreType( VolatileStore.class.getName() );

		BeanPopulator
				.getNamedInstance( ObjectStoreEnvironmentBean.class, "stateStore" )
				.setObjectStoreType( VolatileStore.class.getName() );

		TransactionManager transactionManager = com.arjuna.ats.jta.TransactionManager.transactionManager();

		StandardXADataSource dataSource = new StandardXADataSource();
		dataSource.setTransactionManager( transactionManager );
		try {
			dataSource.setDriverName( configValues.get(Environment.DRIVER).toString() );
		}
		catch (SQLException e) {
			throw new RuntimeException( "Unable to set DataSource JDBC driver name", e );
		}
		dataSource.setUrl(configValues.get(Environment.URL).toString() + ";AUTOCOMMIT=OFF");
		dataSource.setUser(configValues.get(Environment.USER).toString());

        configValues.remove(Environment.URL);
        configValues.remove(Environment.USER);
        configValues.remove(Environment.DRIVER);

		configValues.put( org.hibernate.cfg.AvailableSettings.JTA_PLATFORM, new JBossStandAloneJtaPlatform() );
		configValues.put( Environment.CONNECTION_PROVIDER, DatasourceConnectionProviderImpl.class.getName() );
		configValues.put( Environment.DATASOURCE, dataSource );

        configValues.put(AvailableSettings.TRANSACTION_TYPE, "JTA");

        return transactionManager;
	}

    public static void tryCommit(TransactionManager tm) throws Exception {
        if (tm.getStatus() == Status.STATUS_MARKED_ROLLBACK) {
            tm.rollback();
        } else {
            tm.commit();
        }
    }
}
