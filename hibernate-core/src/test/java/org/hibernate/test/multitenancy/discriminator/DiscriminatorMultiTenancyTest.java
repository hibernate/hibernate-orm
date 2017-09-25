/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.test.multitenancy.discriminator;

import org.hibernate.MultiTenancyStrategy;
import org.hibernate.Session;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Environment;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.RootClass;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.test.util.DdlTransactionIsolatorTestingImpl;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.cache.CachingRegionFactory;
import org.hibernate.testing.env.ConnectionProviderBuilder;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.tool.schema.internal.HibernateSchemaManagementTool;
import org.hibernate.tool.schema.internal.SchemaCreatorImpl;
import org.hibernate.tool.schema.internal.SchemaDropperImpl;
import org.hibernate.tool.schema.internal.exec.GenerationTargetToDatabase;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.hibernate.test.multitenancy.schema.Customer;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Mårten Svantesson
 */
@TestForIssue(jiraKey = "HHH-11980")
public class DiscriminatorMultiTenancyTest extends BaseUnitTestCase {
	protected SessionFactoryImplementor sessionFactory;
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
		((RootClass) metadata.getEntityBinding(Customer.class.getName())).setCacheConcurrencyStrategy("read-write");

		HibernateSchemaManagementTool tool = new HibernateSchemaManagementTool();
		tool.injectServices(serviceRegistry);

		final GenerationTargetToDatabase target = new GenerationTargetToDatabase(
				new DdlTransactionIsolatorTestingImpl(
						serviceRegistry,
						ConnectionProviderBuilder.buildConnectionProvider()
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

		final SessionFactoryBuilder sfb = metadata.getSessionFactoryBuilder();
		sessionFactory = (SessionFactoryImplementor) sfb.build();
	}

	/**
	 * This tests for current tenant being used for second level cache, but not selecting connection provider.
	 * Discrimination on connection level will for now need to be implemented in the supplied connection provider.
	 */
	@Test
	public void testDiscriminator() {

		Session session = getNewSession("jboss");

		session.beginTransaction();
		Customer steve = new Customer( 1L, "steve" );
		session.save( steve );
		session.getTransaction().commit();
		session.close();

		sessionFactory.getStatisticsImplementor().clear();

		// make sure we get the steve back, from cache if same tenant (jboss)
		{
			session = getNewSession("jboss");
			session.beginTransaction();
			Customer customer = (Customer) session.load( Customer.class, 1L );
			Assert.assertEquals( "steve", customer.getName() );
			// also, make sure this came from second level
			Assert.assertEquals( 1, sessionFactory.getStatisticsImplementor().getSecondLevelCacheHitCount() );
			session.getTransaction().commit();
			session.close();
		}
		sessionFactory.getStatisticsImplementor().clear();
		// then make sure we get the steve back, from db if other tenant (acme)
		{
			session = getNewSession("acme");
			session.beginTransaction();
			Customer customer = (Customer) session.load( Customer.class, 1L );
			Assert.assertEquals( "steve", customer.getName() );
			// also, make sure this doesn't came from second level
			Assert.assertEquals( 0, sessionFactory.getStatisticsImplementor().getSecondLevelCacheHitCount() );
			session.getTransaction().commit();
			session.close();
		}

		// make sure the same works from datastore too
		sessionFactory.getStatisticsImplementor().clear();
		sessionFactory.getCache().evictEntityRegions();
		// first jboss
		{
			session = getNewSession("jboss");
			session.beginTransaction();
			Customer customer = (Customer) session.load( Customer.class, 1L );
			Assert.assertEquals( "steve", customer.getName() );
			// also, make sure this doesn't came from second level
			Assert.assertEquals( 0, sessionFactory.getStatisticsImplementor().getSecondLevelCacheHitCount() );
			session.getTransaction().commit();
			session.close();
		}
		sessionFactory.getStatisticsImplementor().clear();
		// then, acme
		{
			session = getNewSession("acme");
			session.beginTransaction();
			Customer customer = (Customer) session.load( Customer.class, 1L );
			Assert.assertEquals( "steve", customer.getName() );
			// also, make sure this doesn't came from second level
			Assert.assertEquals( 0, sessionFactory.getStatisticsImplementor().getSecondLevelCacheHitCount() );
			session.getTransaction().commit();
			session.close();
		}

		session = getNewSession("jboss");
		session.beginTransaction();
		session.delete( steve );
		session.getTransaction().commit();
		session.close();
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

	private Session getNewSession(String tenant) {
		currentTenantResolver.currentTenantIdentifier = tenant;
		return sessionFactory.withOptions().tenantIdentifier( tenant ).openSession();
	}
}