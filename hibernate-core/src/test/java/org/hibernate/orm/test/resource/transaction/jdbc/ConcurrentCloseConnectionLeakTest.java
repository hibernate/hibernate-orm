/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.resource.transaction.jdbc;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProvider;

import static org.hibernate.cfg.TransactionSettings.TRANSACTION_COORDINATOR_STRATEGY;

import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.stat.Statistics;
import org.hibernate.testing.jdbc.SharedDriverManagerConnectionProvider;
import org.hibernate.testing.jta.JtaAwareConnectionProviderImpl;
import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SettingConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author JAEIK JEONG
 */
@DomainModel(
		annotatedClasses = {
				ConcurrentCloseConnectionLeakTest.Parent.class,
				ConcurrentCloseConnectionLeakTest.Child.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.CONNECTION_HANDLING, value = "DELAYED_ACQUISITION_AND_RELEASE_AFTER_STATEMENT"),
				@Setting(name = AvailableSettings.CONNECTION_PROVIDER, value = "org.hibernate.orm.test.resource.transaction.jdbc.ConcurrentCloseConnectionLeakTest$ConnectionLeakDetectingProvider"),
				@Setting(name = TRANSACTION_COORDINATOR_STRATEGY, value = "jta"),
				@Setting(name = AvailableSettings.GENERATE_STATISTICS, value = "true")
		},
		settingConfigurations = @SettingConfiguration(configurer = TestingJtaBootstrap.class)
)
@SessionFactory
public class ConcurrentCloseConnectionLeakTest {
	private static final int PARALLEL_SCENARIOS = 30;
	private static final int ASYNC_DELAY_MS = 1;
	private static final int SESSION_CLOSE_DELAY_MS = 3;
	private Long parentId;

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Child child = new Child();
					child.setName( "Test Child" );
					session.persist( child );

					Parent parent = new Parent();
					parent.setName( "Test Parent" );
					parent.setChild( child );
					session.persist( parent );

					this.parentId = parent.getId();
				}
		);
	}

	@Test
	public void testOsivLikeAsyncLazyAfterTransactionCommit(SessionFactoryScope scope) throws Exception {
		Statistics stats = scope.getSessionFactory().getStatistics();
		stats.clear();

		ExecutorService mainExecutor = Executors.newFixedThreadPool(PARALLEL_SCENARIOS);
		CompletableFuture<?>[] scenarios = new CompletableFuture[PARALLEL_SCENARIOS];

		for (int i = 0; i < PARALLEL_SCENARIOS; i++) {
			scenarios[i] = CompletableFuture.runAsync(() -> {
				Session session = scope.getSessionFactory().openSession();
				ExecutorService asyncExecutor = Executors.newSingleThreadExecutor();

				try {
					Parent parent = session.find(Parent.class, parentId);
					assertFalse(Hibernate.isInitialized(parent.getChild()));

					CompletableFuture<Void> asyncTask = CompletableFuture.runAsync(() -> {
						try {
							Thread.sleep(ASYNC_DELAY_MS);
							String childName = parent.getChild().getName();
						}
						catch (Exception ignored) {
						}
					}, asyncExecutor);

					try {
						Thread.sleep(SESSION_CLOSE_DELAY_MS);
						session.close();
					}
					catch (Exception ignored) {
					}
				} finally {
					if (session.isOpen()) {
						try { session.close(); } catch (Exception ignored) {}
					}
					asyncExecutor.shutdown();
				}
			}, mainExecutor);
		}

		CompletableFuture.allOf(scenarios).get(40, TimeUnit.SECONDS);
		mainExecutor.shutdown();

		Thread.sleep(1000);

		assertEquals(
				stats.getSessionOpenCount(),
				stats.getSessionCloseCount(),
				String.format("Session leak detected! Open: %d, Closed: %d", stats.getSessionOpenCount(), stats.getSessionCloseCount())
		);

		int activeConnections = SharedDriverManagerConnectionProvider.getInstance().getOpenConnections();

		assertEquals(
				0,
				activeConnections,
				"Physical connection leak detected! Active connections: " + activeConnections
		);
	}

	@Entity(name = "Parent")
	public static class Parent {
		@Id @GeneratedValue private Long id;
		private String name;
		@OneToOne(fetch = FetchType.LAZY) private Child child;

		public Long getId() { return id; }
		public void setId(Long id) { this.id = id; }
		public String getName() { return name; }
		public void setName(String name) { this.name = name; }
		public Child getChild() { return child; }
		public void setChild(Child child) { this.child = child; }
	}

	@Entity(name = "Child")
	public static class Child {
		@Id @GeneratedValue private Long id;
		private String name;

		public Long getId() { return id; }
		public void setId(Long id) { this.id = id; }
		public String getName() { return name; }
		public void setName(String name) { this.name = name; }
	}

	public static class ConnectionLeakDetectingProvider extends JtaAwareConnectionProviderImpl {
		@Override
		public void injectServices(ServiceRegistryImplementor serviceRegistry) {
			SharedDriverManagerConnectionProvider.getInstance().injectServices( serviceRegistry );
		}

		@Override
		public void configure(Map<String, Object> configurationValues) {
			Map<String, Object> connectionSettings = new HashMap<>();
			transferSetting( Environment.DRIVER, configurationValues, connectionSettings );
			transferSetting( Environment.URL, configurationValues, connectionSettings );
			transferSetting( Environment.USER, configurationValues, connectionSettings );
			transferSetting( Environment.PASS, configurationValues, connectionSettings );
			transferSetting( Environment.ISOLATION, configurationValues, connectionSettings );
			connectionSettings.remove( AvailableSettings.CONNECTION_HANDLING );

			connectionSettings.put( Environment.AUTOCOMMIT, "false" );
			connectionSettings.put( Environment.POOL_SIZE, "40" );
			connectionSettings.put( DriverManagerConnectionProvider.INITIAL_SIZE, "0" );

			SharedDriverManagerConnectionProvider.getInstance().configure( connectionSettings );
		}

		private static void transferSetting(String settingName, Map<String, Object> source, Map<String, Object> target) {
			Object value = source.get( settingName );
			if ( value != null ) {
				target.put( settingName, value );
			}
		}
	}
}
