/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.test.multitenancy.discriminator;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.hibernate.MultiTenancyStrategy;
import org.hibernate.Session;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Environment;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProviderImpl;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.tool.schema.internal.HibernateSchemaManagementTool;
import org.hibernate.tool.schema.internal.SchemaCreatorImpl;
import org.hibernate.tool.schema.internal.SchemaDropperImpl;
import org.hibernate.tool.schema.internal.exec.GenerationTargetToDatabase;

import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.cache.CachingRegionFactory;
import org.hibernate.testing.env.ConnectionProviderBuilder;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.transaction.TransactionUtil;
import org.hibernate.test.multitenancy.schema.Customer;
import org.hibernate.test.util.DdlTransactionIsolatorTestingImpl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Mårten Svantesson
 */
@TestForIssue(jiraKey = "HHH-11980")
@RequiresDialectFeature( value = ConnectionProviderBuilder.class )
public class DiscriminatorMultiTenancyTest extends BaseUnitTestCase {

	private SessionFactoryImplementor sessionFactory;

	private DriverManagerConnectionProviderImpl connectionProvider;

	private final TestCurrentTenantIdentifierResolver currentTenantResolver = new TestCurrentTenantIdentifierResolver();

	@Before
	public void setUp() {
		Map settings = new HashMap();
		settings.put(Environment.MULTI_TENANT, MultiTenancyStrategy.DISCRIMINATOR);
		settings.put(Environment.MULTI_TENANT_IDENTIFIER_RESOLVER, currentTenantResolver);
		settings.put(Environment.CACHE_REGION_FACTORY, CachingRegionFactory.class.getName());
		settings.put(Environment.GENERATE_STATISTICS, "true");

		ServiceRegistryImplementor serviceRegistry = (ServiceRegistryImplementor) new StandardServiceRegistryBuilder()
				.applySettings(settings)
				.build();

		MetadataSources ms = new MetadataSources(serviceRegistry);
		ms.addAnnotatedClass(Customer.class);

		Metadata metadata = ms.buildMetadata();
		final PersistentClass customerMapping = metadata.getEntityBinding( Customer.class.getName() );
		customerMapping.setCached( true );
		((RootClass) customerMapping ).setCacheConcurrencyStrategy( "read-write");

		HibernateSchemaManagementTool tool = new HibernateSchemaManagementTool();
		tool.injectServices(serviceRegistry);

		connectionProvider = ConnectionProviderBuilder.buildConnectionProvider();

		final GenerationTargetToDatabase target = new GenerationTargetToDatabase(
				new DdlTransactionIsolatorTestingImpl(
						serviceRegistry,
						connectionProvider
				)
		);


		new SchemaDropperImpl(serviceRegistry).doDrop(
				metadata,
				serviceRegistry,
				settings,
				true,
				target
		);

		new SchemaCreatorImpl(serviceRegistry).doCreation(
				metadata,
				serviceRegistry,
				settings,
				true,
				target
		);

		target.release();

		final SessionFactoryBuilder sfb = metadata.getSessionFactoryBuilder();
		sessionFactory = (SessionFactoryImplementor) sfb.build();
	}

	@After
	public void destroy() {
		sessionFactory.close();
		connectionProvider.stop();
	}

	public SessionFactoryImplementor sessionFactory() {
		return sessionFactory;
	}

	/**
	 * This tests for current tenant being used for second level cache, but not selecting connection provider.
	 * Discrimination on connection level will for now need to be implemented in the supplied connection provider.
	 */
	@Test
	public void testDiscriminator() {

		doInHibernate( "jboss", session -> {
			Customer steve = new Customer( 1L, "steve" );
			session.save( steve );
		} );

		sessionFactory.getStatistics().clear();

		// make sure we get the steve back, from cache if same tenant (jboss)
		doInHibernate( "jboss", session -> {
			Customer customer = session.load( Customer.class, 1L );
			Assert.assertEquals( "steve", customer.getName() );
			// also, make sure this came from second level
			Assert.assertEquals( 1, sessionFactory.getStatistics().getSecondLevelCacheHitCount() );
		} );

		sessionFactory.getStatistics().clear();

		// then make sure we get the steve back, from db if other tenant (acme)
		doInHibernate( "acme", session -> {
			Customer customer = session.load( Customer.class, 1L );
			Assert.assertEquals( "steve", customer.getName() );
			// also, make sure this doesn't came from second level
			Assert.assertEquals( 0, sessionFactory.getStatistics().getSecondLevelCacheHitCount() );
		} );

		// make sure the same works from data store too
		sessionFactory.getStatistics().clear();
		sessionFactory.getCache().evictEntityRegions();

		// first jboss
		doInHibernate( "jboss", session -> {
			Customer customer = session.load( Customer.class, 1L );
			Assert.assertEquals( "steve", customer.getName() );
			// also, make sure this doesn't came from second level
			Assert.assertEquals( 0, sessionFactory.getStatistics().getSecondLevelCacheHitCount() );
		} );

		sessionFactory.getStatistics().clear();
		// then, acme
		doInHibernate( "acme", session -> {
			Customer customer = session.load( Customer.class, 1L );
			Assert.assertEquals( "steve", customer.getName() );
			// also, make sure this doesn't came from second level
			Assert.assertEquals( 0, sessionFactory.getStatistics().getSecondLevelCacheHitCount() );
		} );

		doInHibernate( "jboss", session -> {
			Customer customer = session.load( Customer.class, 1L );
			session.delete( customer );
		} );
	}

	private static class TestCurrentTenantIdentifierResolver implements CurrentTenantIdentifierResolver {
		private String currentTenantIdentifier;

		@Override
		public String resolveCurrentTenantIdentifier() {
			return currentTenantIdentifier;
		}

		@Override
		public boolean validateExistingCurrentSessions() {
			return false;
		}
	}

	public void doInHibernate(String tenant, Consumer<Session> function) {
		currentTenantResolver.currentTenantIdentifier = tenant;
		//Careful: do not use the #doInHibernate version of the method which takes a tenant: the goal of these tests is
		// to verify that the CurrentTenantIdentifierResolver is being applied!
		TransactionUtil.doInHibernate( this::sessionFactory, function);
	}
}