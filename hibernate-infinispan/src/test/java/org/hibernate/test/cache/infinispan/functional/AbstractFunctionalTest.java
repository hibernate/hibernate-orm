/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache.infinispan.functional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.resource.transaction.TransactionCoordinatorBuilder;
import org.hibernate.resource.transaction.backend.jdbc.internal.JdbcResourceLocalTransactionCoordinatorBuilderImpl;
import org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorBuilderImpl;

import org.hibernate.test.cache.infinispan.tm.XaConnectionProvider;
import org.hibernate.test.cache.infinispan.util.InfinispanTestingSetup;
import org.hibernate.test.cache.infinispan.util.TestInfinispanRegionFactory;
import org.hibernate.test.cache.infinispan.util.TxUtil;
import org.hibernate.testing.BeforeClassOnce;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.test.cache.infinispan.tm.JtaPlatformImpl;

import org.hibernate.testing.junit4.CustomParameterized;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * @author Galder Zamarreño
 * @since 3.5
 */
@RunWith(CustomParameterized.class)
public abstract class AbstractFunctionalTest extends BaseNonConfigCoreFunctionalTestCase {
	private static final Log log = LogFactory.getLog( AbstractFunctionalTest.class );

	protected static final Object[] TRANSACTIONAL = new Object[]{"transactional", JtaPlatformImpl.class, JtaTransactionCoordinatorBuilderImpl.class, XaConnectionProvider.class, AccessType.TRANSACTIONAL, TestInfinispanRegionFactory.Transactional.class};
	protected static final Object[] READ_WRITE = new Object[]{"read-write", null, JdbcResourceLocalTransactionCoordinatorBuilderImpl.class, null, AccessType.READ_WRITE, TestInfinispanRegionFactory.class};
	protected static final Object[] READ_ONLY = new Object[]{"read-only", null, JdbcResourceLocalTransactionCoordinatorBuilderImpl.class, null, AccessType.READ_ONLY, TestInfinispanRegionFactory.class};

	@ClassRule
	public static final InfinispanTestingSetup infinispanTestIdentifier = new InfinispanTestingSetup();

	@Parameterized.Parameter(value = 0)
	public String mode;

	@Parameterized.Parameter(value = 1)
	public Class<? extends JtaPlatform> jtaPlatformClass;

	@Parameterized.Parameter(value = 2)
	public Class<? extends TransactionCoordinatorBuilder> transactionCoordinatorBuilderClass;

	@Parameterized.Parameter(value = 3)
	public Class<? extends ConnectionProvider> connectionProviderClass;

	@Parameterized.Parameter(value = 4)
	public AccessType accessType;

	@Parameterized.Parameter(value = 5)
	public Class<? extends RegionFactory> regionFactoryClass;

	protected boolean useJta;

	@Parameterized.Parameters(name = "{0}")
	public abstract List<Object[]> getParameters();

	@BeforeClassOnce
	public void setUseJta() {
		useJta = jtaPlatformClass != null;
	}

	@Override
	public String[] getMappings() {
		return new String[] {
				"cache/infinispan/functional/entities/Item.hbm.xml",
				"cache/infinispan/functional/entities/Customer.hbm.xml",
				"cache/infinispan/functional/entities/Contact.hbm.xml"
		};
	}

	@Override
	public String getCacheConcurrencyStrategy() {
		return accessType.getExternalName();
	}

	protected boolean getUseQueryCache() {
		return true;
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void addSettings(Map settings) {
		super.addSettings( settings );

		settings.put( Environment.USE_SECOND_LEVEL_CACHE, "true" );
		settings.put( Environment.GENERATE_STATISTICS, "true" );
		settings.put( Environment.USE_QUERY_CACHE, String.valueOf( getUseQueryCache() ) );
		settings.put( Environment.CACHE_REGION_FACTORY, regionFactoryClass.getName() );

		if ( jtaPlatformClass != null ) {
			settings.put( AvailableSettings.JTA_PLATFORM, jtaPlatformClass.getName() );
		}
		settings.put( Environment.TRANSACTION_COORDINATOR_STRATEGY, transactionCoordinatorBuilderClass.getName() );
		if ( connectionProviderClass != null) {
			settings.put(Environment.CONNECTION_PROVIDER, connectionProviderClass.getName());
		}
	}

	protected void markRollbackOnly(Session session) {
		TxUtil.markRollbackOnly(useJta, session);
	}
}