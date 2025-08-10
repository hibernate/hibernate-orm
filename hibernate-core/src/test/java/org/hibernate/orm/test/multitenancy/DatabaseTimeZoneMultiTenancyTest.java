/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.multitenancy;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import java.util.function.Consumer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.Session;
import org.hibernate.SessionBuilder;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.internal.util.PropertiesHelper;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Stoppable;
import org.hibernate.tool.schema.internal.HibernateSchemaManagementTool;
import org.hibernate.tool.schema.internal.SchemaCreatorImpl;
import org.hibernate.tool.schema.internal.SchemaDropperImpl;
import org.hibernate.tool.schema.internal.exec.GenerationTargetToDatabase;

import org.hibernate.testing.AfterClassOnce;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.util.ServiceRegistryUtil;

import org.hibernate.orm.test.util.DdlTransactionIsolatorTestingImpl;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(H2Dialect.class)
public class DatabaseTimeZoneMultiTenancyTest extends BaseUnitTestCase {

	protected static final String FRONT_END_TENANT = "front_end";
	protected static final String BACK_END_TENANT = "back_end";

	//tag::multitenacy-hibernate-timezone-configuration-context-example[]
	private Map<String, ConnectionProvider> connectionProviderMap = new HashMap<>();

	private Map<String, TimeZone> timeZoneTenantMap = new HashMap<>();
	//end::multitenacy-hibernate-timezone-configuration-context-example[]

	private SessionFactory sessionFactory;

	public DatabaseTimeZoneMultiTenancyTest() {
		init();
	}

	private void init() {
		//tag::multitenacy-hibernate-timezone-configuration-registerConnectionProvider-call-example[]
		registerConnectionProvider(FRONT_END_TENANT, TimeZone.getTimeZone("UTC"));
		registerConnectionProvider(BACK_END_TENANT, TimeZone.getTimeZone("CST"));
		//end::multitenacy-hibernate-timezone-configuration-registerConnectionProvider-call-example[]

		Map<String, Object> settings = new HashMap<>();

		settings.put(
				AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER,
				new ConfigurableMultiTenantConnectionProvider(connectionProviderMap)
	);

		sessionFactory = sessionFactory(settings);
	}

	@AfterClassOnce
	public void destroy() {
		sessionFactory.close();
		for (ConnectionProvider connectionProvider : connectionProviderMap.values()) {
			if (connectionProvider instanceof Stoppable) {
				((Stoppable) connectionProvider).stop();
			}
		}
	}

	//tag::multitenacy-hibernate-timezone-configuration-registerConnectionProvider-example[]
	protected void registerConnectionProvider(String tenantIdentifier, TimeZone timeZone) {
		Properties properties = properties();
		properties.put(
			Environment.URL,
			tenantUrl( properties.getProperty(Environment.URL), tenantIdentifier )
	);

		DriverManagerConnectionProvider connectionProvider =
				new DriverManagerConnectionProvider();
		connectionProvider.configure( PropertiesHelper.map(properties) );

		connectionProviderMap.put(tenantIdentifier, connectionProvider);

		timeZoneTenantMap.put(tenantIdentifier, timeZone);
	}
	//end::multitenacy-hibernate-timezone-configuration-registerConnectionProvider-example[]

	@Test
	public void testBasicExpectedBehavior() {

		//tag::multitenacy-hibernate-applying-timezone-configuration-example[]
		doInSession(FRONT_END_TENANT, session -> {
			Person person = new Person();
			person.setId(1L);
			person.setName("John Doe");
			person.setCreatedOn(LocalDateTime.of(2018, 11, 23, 12, 0, 0));

			session.persist(person);
		}, true);

		doInSession(BACK_END_TENANT, session -> {
			Person person = new Person();
			person.setId(1L);
			person.setName("John Doe");
			person.setCreatedOn(LocalDateTime.of(2018, 11, 23, 12, 0, 0));

			session.persist(person);
		}, true);

		doInSession(FRONT_END_TENANT, session -> {
			Timestamp personCreationTimestamp = session
			.createNativeQuery(
				"select p.created_on " +
				"from Person p " +
				"where p.id = :personId", Timestamp.class)
			.setParameter("personId", 1L)
			.getSingleResult();

			assertEquals(
				Timestamp.valueOf(LocalDateTime.of(2018, 11, 23, 12, 0, 0)),
				personCreationTimestamp
		);
		}, true);

		doInSession(BACK_END_TENANT, session -> {
			Timestamp personCreationTimestamp = session
			.createNativeQuery(
				"select p.created_on " +
				"from Person p " +
				"where p.id = :personId", Timestamp.class)
			.setParameter("personId", 1L)
			.getSingleResult();

			assertEquals(
				Timestamp.valueOf(LocalDateTime.of(2018, 11, 23, 12, 0, 0)),
				personCreationTimestamp
		);
		}, true);
		//end::multitenacy-hibernate-applying-timezone-configuration-example[]

		//tag::multitenacy-hibernate-not-applying-timezone-configuration-example[]
		doInSession(FRONT_END_TENANT, session -> {
			Timestamp personCreationTimestamp = session
			.createNativeQuery(
				"select p.created_on " +
				"from Person p " +
				"where p.id = :personId", Timestamp.class)
			.setParameter("personId", 1L)
			.getSingleResult();

			log.infof(
				"The created_on timestamp value is: [%s]",
				personCreationTimestamp
		);

			long timeZoneOffsetMillis =
					Timestamp.valueOf(LocalDateTime.of(2018, 11, 23, 12, 0, 0)).getTime() -
					personCreationTimestamp.getTime();

			assertEquals(
				TimeZone.getTimeZone(ZoneId.systemDefault()).getRawOffset(),
				timeZoneOffsetMillis
		);

			log.infof(
				"For the current time zone: [%s], the UTC time zone offset is: [%d]",
				TimeZone.getDefault().getDisplayName(), timeZoneOffsetMillis
		);
		}, false);
		//end::multitenacy-hibernate-not-applying-timezone-configuration-example[]
	}

	protected Properties properties() {
		Properties properties = new Properties();
		URL propertiesURL = Thread.currentThread().getContextClassLoader().getResource("hibernate.properties");
		try (FileInputStream inputStream = new FileInputStream(propertiesURL.getFile())) {
			properties.load(inputStream);
		}
		catch (IOException e) {
			throw new IllegalArgumentException(e);
		}
		return properties;
	}

	protected String tenantUrl(String originalUrl, String tenantIdentifier) {
		return originalUrl.replace("db1", tenantIdentifier);
	}

	protected SessionFactory sessionFactory(Map<String, Object> settings) {

		ServiceRegistryImplementor serviceRegistry = (ServiceRegistryImplementor) ServiceRegistryUtil.serviceRegistryBuilder()
				.applySettings(settings)
				.build();

		MetadataSources metadataSources = new MetadataSources(serviceRegistry);
		for (Class annotatedClasses : getAnnotatedClasses()) {
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

	//tag::multitenacy-hibernate-timezone-configuration-session-example[]
	private void doInSession(String tenant, Consumer<Session> function, boolean useTenantTimeZone) {
		Session session = null;
		Transaction txn = null;

		try {
			SessionBuilder sessionBuilder = sessionFactory
					.withOptions()
					.tenantIdentifier(tenant);

			if (useTenantTimeZone) {
				sessionBuilder.jdbcTimeZone(timeZoneTenantMap.get(tenant));
			}

			session = sessionBuilder.openSession();

			txn = session.getTransaction();
			txn.begin();

			function.accept(session);

			txn.commit();
		}
		catch (Throwable e) {
			if (txn != null) {
				txn.rollback();
			}
			throw e;
		}
		finally {
			if (session != null) {
				session.close();
			}
		}
	}
	//end::multitenacy-hibernate-timezone-configuration-session-example[]

	@Entity(name = "Person")
	public static class Person {

		@Id
		private Long id;

		private String name;

		@Column(name = "created_on")
		private LocalDateTime createdOn;

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

		public LocalDateTime getCreatedOn() {
			return createdOn;
		}

		public void setCreatedOn(LocalDateTime createdOn) {
			this.createdOn = createdOn;
		}
	}
}
