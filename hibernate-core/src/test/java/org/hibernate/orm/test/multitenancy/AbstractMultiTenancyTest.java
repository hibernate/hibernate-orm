/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.multitenancy;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.Session;
import org.hibernate.SessionException;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProviderImpl;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.internal.util.PropertiesHelper;
import org.hibernate.query.Query;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Stoppable;
import org.hibernate.tool.schema.internal.HibernateSchemaManagementTool;
import org.hibernate.tool.schema.internal.SchemaCreatorImpl;
import org.hibernate.tool.schema.internal.SchemaDropperImpl;
import org.hibernate.tool.schema.internal.exec.GenerationTargetToDatabase;

import org.hibernate.testing.AfterClassOnce;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.util.ServiceRegistryUtil;

import org.hibernate.orm.test.util.DdlTransactionIsolatorTestingImpl;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public abstract class AbstractMultiTenancyTest extends BaseUnitTestCase {

	protected static final String FRONT_END_TENANT = "front_end";
	protected static final String BACK_END_TENANT = "back_end";

	protected Map<String, ConnectionProvider> connectionProviderMap = new HashMap<>();

	protected SessionFactory sessionFactory;

	public AbstractMultiTenancyTest() {
		init();
	}

	//tag::multitenacy-hibernate-MultiTenantConnectionProvider-example[]
	private void init() {
		registerConnectionProvider(FRONT_END_TENANT);
		registerConnectionProvider(BACK_END_TENANT);
		sessionFactory = sessionFactory(createSettings());
	}

	protected Map<String, Object> createSettings() {
		Map<String, Object> settings = new HashMap<>();

		settings.put(AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER,
				new ConfigurableMultiTenantConnectionProvider(connectionProviderMap));
		return settings;
	}
	//end::multitenacy-hibernate-MultiTenantConnectionProvider-example[]

	@AfterClassOnce
	public void destroy() {
		sessionFactory.close();
		for (ConnectionProvider connectionProvider : connectionProviderMap.values()) {
			if (connectionProvider instanceof Stoppable) {
				((Stoppable) connectionProvider).stop();
			}
		}
	}

	@After
	public void cleanup() {
		doInSession(FRONT_END_TENANT, session -> session.createMutationQuery( "delete from Person" ).executeUpdate() );
		doInSession(BACK_END_TENANT, session -> session.createMutationQuery( "delete from Person" ).executeUpdate() );
	}

	//tag::multitenacy-hibernate-MultiTenantConnectionProvider-example[]

	protected void registerConnectionProvider(String tenantIdentifier) {
		Properties properties = properties();
		properties.put(Environment.URL,
			tenantUrl(properties.getProperty(Environment.URL), tenantIdentifier));

		DriverManagerConnectionProviderImpl connectionProvider =
			new DriverManagerConnectionProviderImpl();
		connectionProvider.configure( PropertiesHelper.map(properties) );
		connectionProviderMap.put(tenantIdentifier, connectionProvider);
	}
	//end::multitenacy-hibernate-MultiTenantConnectionProvider-example[]

	@Test
	public void testBasicExpectedBehavior() {

		//tag::multitenacy-multitenacy-hibernate-same-entity-example[]
		doInSession(FRONT_END_TENANT, session -> {
			Person person = new Person();
			person.setId(1L);
			person.setName("John Doe");
			session.persist(person);
		});

		doInSession(BACK_END_TENANT, session -> {
			Person person = new Person();
			person.setId(1L);
			person.setName("John Doe");
			session.persist(person);
		});
		//end::multitenacy-multitenacy-hibernate-same-entity-example[]
	}

	@Test
	@JiraKey( value = "HHH-17972")
	public void testChangeTenantWithoutConnectionReuse() {
		Person person = new Person();
		person.setId( 1L );
		person.setName( "John Doe" );
		Person person2 = new Person();
		person2.setId( 2L );
		person2.setName( "Jane Doe" );

		Transaction t;
		Session session = null;
		Session newSession = null;
		try {
			session = sessionFactory.withOptions().tenantIdentifier( FRONT_END_TENANT ).openSession();
			t = session.beginTransaction();
			session.persist( person );
			t.commit();

			Query<Person> sessionQuery = session.createQuery( "from Person", Person.class );
			assertEquals( 1, sessionQuery.getResultList().size() );
			assertEquals( "John Doe", sessionQuery.getResultList().get( 0 ).getName() );

			newSession = session.sessionWithOptions().tenantIdentifier( BACK_END_TENANT ).openSession();
			t = newSession.beginTransaction();
			newSession.persist( person2 );
			t.commit();

			Query<Person> newSessionQuery = newSession.createQuery( "from Person", Person.class );
			assertEquals( 1, newSessionQuery.getResultList().size() );
			assertEquals( "Jane Doe", newSessionQuery.getResultList().get( 0 ).getName() );
		}
		finally {
			if (session != null) {
				session.close();
			}
			if (newSession != null) {
				newSession.close();
			}
		}
	}

	@Test
	@JiraKey( value = "HHH-17972")
	public void testChangeTenantWithConnectionReuse() {
		try (Session session = sessionFactory.withOptions().tenantIdentifier( FRONT_END_TENANT ).openSession()) {
			Assert.assertThrows( "Cannot redefine the tenant identifier on a child session if the connection is reused",
								SessionException.class,
								() -> session.sessionWithOptions().tenantIdentifier( BACK_END_TENANT ).connection().openSession()
			);
			Assert.assertThrows( "Cannot redefine the tenant identifier on a child session if the connection is reused",
								SessionException.class,
								() -> session.sessionWithOptions().connection().tenantIdentifier( BACK_END_TENANT ).openSession()
			);
		}
	}

	protected Properties properties() {
		Properties properties = new Properties();
		URL propertiesURL = Thread.currentThread().getContextClassLoader().getResource("hibernate.properties");
		try(FileInputStream inputStream = new FileInputStream(propertiesURL.getFile())) {
			properties.load(inputStream);
		}
		catch (IOException e) {
			throw new IllegalArgumentException(e);
		}
		return properties;
	}

	protected abstract String tenantUrl(String originalUrl, String tenantIdentifier);

	protected SessionFactory sessionFactory(Map<String, Object> settings) {

		ServiceRegistryImplementor serviceRegistry = (ServiceRegistryImplementor) ServiceRegistryUtil.serviceRegistryBuilder()
			.applySettings(settings)
			.build();

		MetadataSources metadataSources = new MetadataSources(serviceRegistry);
		for(Class annotatedClasses : getAnnotatedClasses()) {
			metadataSources.addAnnotatedClass(annotatedClasses);
		}

		Metadata metadata = metadataSources.buildMetadata();

		HibernateSchemaManagementTool tool = new HibernateSchemaManagementTool();
		tool.injectServices(serviceRegistry);

		new SchemaDropperImpl( serviceRegistry ).doDrop(
				metadata,
				serviceRegistry,
				settings,
				true,
				new GenerationTargetToDatabase(
						new DdlTransactionIsolatorTestingImpl(
								serviceRegistry,
								connectionProviderMap.get( FRONT_END_TENANT )
						)
				),
				new GenerationTargetToDatabase(
						new DdlTransactionIsolatorTestingImpl(
								serviceRegistry,
								connectionProviderMap.get( BACK_END_TENANT )
						)
				)
		);

		new SchemaCreatorImpl( serviceRegistry ).doCreation(
				metadata,
				serviceRegistry,
				settings,
				true,
				new GenerationTargetToDatabase(
						new DdlTransactionIsolatorTestingImpl(
								serviceRegistry,
								connectionProviderMap.get( FRONT_END_TENANT )
						)
				),
				new GenerationTargetToDatabase(
						new DdlTransactionIsolatorTestingImpl(
								serviceRegistry,
								connectionProviderMap.get( BACK_END_TENANT )
						)
				)
		);

		final SessionFactoryBuilder sessionFactoryBuilder = metadata.getSessionFactoryBuilder();
		return sessionFactoryBuilder.build();
	}

	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Person.class
		};
	}

	//tag::multitenacy-hibernate-session-example[]
	private void doInSession(String tenant, Consumer<Session> function) {
		Session session = null;
		Transaction txn = null;
		try {
			session = sessionFactory
				.withOptions()
				.tenantIdentifier(tenant)
				.openSession();
			txn = session.getTransaction();
			txn.begin();
			function.accept(session);
			txn.commit();
		} catch (Throwable e) {
			if (txn != null) txn.rollback();
			throw e;
		} finally {
			if (session != null) {
				session.close();
			}
		}
	}
	//end::multitenacy-hibernate-session-example[]

	@Entity(name = "Person")
	public static class Person {

		@Id
		private Long id;

		private String name;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

}
