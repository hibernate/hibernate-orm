package org.hibernate.orm.test.transactions;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.SynchronizationType;
import jakarta.transaction.Status;
import jakarta.transaction.UserTransaction;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.MetadataSources;
import org.hibernate.resource.transaction.spi.TransactionStatus;

import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.jta.TestingJtaPlatformImpl;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SettingConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hibernate.cfg.SchemaToolingSettings.HBM2DDL_AUTO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Executable transaction-boundary examples for the User Guide.
 *
 * @author Vlad Mihalcea
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@ServiceRegistry(settings = @Setting(name = HBM2DDL_AUTO, value = "create-drop"))
public class TransactionsTest {

	@Test
	public void jdbc(ServiceRegistryScope registryScope) {
		try (EntityManagerFactory entityManagerFactory = buildFactory( registryScope )) {
			inResourceLocalTransaction( entityManagerFactory, entityManager ->
					entityManager.persist( new Customer( "Alex" ) ) );
			assertEquals( "Alex", customerName( entityManagerFactory ) );

			RuntimeException failure = new IllegalStateException( "Failed work" );
			assertSame( failure, assertThrows( IllegalStateException.class, () ->
					inResourceLocalTransaction( entityManagerFactory, entityManager -> {
						entityManager.createStatement( "update Customer set name = 'Rolled back'" ).execute();
						throw failure;
					} ) ) );
			assertEquals( "Alex", customerName( entityManagerFactory ) );
		}
	}

	//tag::transactions-api-jdbc-example[]
	private void inResourceLocalTransaction(
			EntityManagerFactory entityManagerFactory, Consumer<EntityManager> work) {
		try (EntityManager entityManager = entityManagerFactory.createEntityManager()) {
			EntityTransaction transaction = entityManager.getTransaction();
			transaction.begin();
			try {
				work.accept( entityManager );
				transaction.commit();
			}
			catch (RuntimeException failure) {
				try {
					if ( transaction.isActive() ) {
						transaction.rollback();
					}
				}
				catch (RuntimeException rollbackFailure) {
					failure.addSuppressed( rollbackFailure );
				}
				throw failure;
			}
		}
	}
	//end::transactions-api-jdbc-example[]

	@Test
	public void helpers(ServiceRegistryScope registryScope) {
		try (EntityManagerFactory entityManagerFactory = buildFactory( registryScope )) {
			entityManagerFactory.runInTransaction( entityManager ->
					entityManager.persist( new Customer( "Alex" ) ) );

			//tag::transactions-api-helper-example[]
			entityManagerFactory.runInTransaction( entityManager ->
					entityManager.createStatement( "update Customer set name = 'Sir. ' || name" )
							.execute() );

			String name = entityManagerFactory.callInTransaction( entityManager ->
					entityManager.createQuery( "select c.name from Customer c", String.class )
							.getSingleResult() );
			//end::transactions-api-helper-example[]
			assertEquals( "Sir. Alex", name );

			List<EntityManager> contexts = new ArrayList<>();
			RuntimeException failure = new IllegalStateException( "Failed helper work" );
			assertSame( failure, assertThrows( IllegalStateException.class, () ->
					entityManagerFactory.runInTransaction( entityManager -> {
						contexts.add( entityManager );
						entityManager.createStatement( "update Customer set name = 'Rolled back'" ).execute();
						throw failure;
					} ) ) );
			assertFalse( contexts.get( 0 ).isOpen() );
			assertEquals( "Sir. Alex", customerName( entityManagerFactory ) );
		}
	}

	@Test
	@ServiceRegistry(
			settingConfigurations = @SettingConfiguration(configurer = TestingJtaBootstrap.class),
			settings = @Setting(name = HBM2DDL_AUTO, value = "create-drop")
	)
	public void cmt(ServiceRegistryScope registryScope) throws Exception {
		UserTransaction userTransaction = TestingJtaPlatformImpl.userTransaction();
		try (EntityManagerFactory entityManagerFactory = buildFactory( registryScope )) {
			try {
				// The test harness simulates the external transaction owner.
				userTransaction.begin();
				participate( entityManagerFactory );
				assertEquals( Status.STATUS_ACTIVE, userTransaction.getStatus() );
				userTransaction.commit();
				assertEquals( 1L, customerCountInJta( entityManagerFactory ) );

				userTransaction.begin();
				participate( entityManagerFactory );
				userTransaction.setRollbackOnly();
				assertEquals( Status.STATUS_MARKED_ROLLBACK, userTransaction.getStatus() );
				assertThrows( jakarta.transaction.RollbackException.class, userTransaction::commit );
				assertEquals( 1L, customerCountInJta( entityManagerFactory ) );
			}
			finally {
				rollbackIfActive( userTransaction );
			}
		}
	}

	//tag::transactions-api-cmt-example[]
	private void participate(EntityManagerFactory entityManagerFactory) {
		try (EntityManager entityManager = entityManagerFactory.createEntityManager()) {
			entityManager.joinTransaction();
			entityManager.persist( new Customer( "Alex" ) );
			entityManager.flush();
		}
	}
	//end::transactions-api-cmt-example[]

	@Test
	@ServiceRegistry(
			settingConfigurations = @SettingConfiguration(configurer = TestingJtaBootstrap.class),
			settings = @Setting(name = HBM2DDL_AUTO, value = "create-drop")
	)
	public void unsynchronized(ServiceRegistryScope registryScope) throws Exception {
		UserTransaction userTransaction = TestingJtaPlatformImpl.userTransaction();
		try (EntityManagerFactory entityManagerFactory = buildFactory( registryScope )) {
			try {
				userTransaction.begin();
				//tag::transactions-jta-joining-example[]
				try (EntityManager entityManager = entityManagerFactory.createEntityManager(
						SynchronizationType.UNSYNCHRONIZED )) {
					entityManager.persist( new Customer( "Alex" ) );
					assertFalse( entityManager.isJoinedToTransaction() );

					entityManager.joinTransaction();
					assertTrue( entityManager.isJoinedToTransaction() );
					entityManager.flush();
				}
				//end::transactions-jta-joining-example[]
				assertEquals( Status.STATUS_ACTIVE, userTransaction.getStatus() );
				userTransaction.commit();
				assertEquals( 1L, customerCountInJta( entityManagerFactory ) );
			}
			finally {
				rollbackIfActive( userTransaction );
			}
		}
	}

	@Test
	@ServiceRegistry(
			settingConfigurations = @SettingConfiguration(configurer = TestingJtaBootstrap.class),
			settings = @Setting(name = HBM2DDL_AUTO, value = "create-drop")
	)
	public void bmt(ServiceRegistryScope registryScope) throws Exception {
		UserTransaction userTransaction = TestingJtaPlatformImpl.userTransaction();
		try (EntityManagerFactory entityManagerFactory = buildFactory( registryScope )) {
			try {
				inJtaTransaction( entityManagerFactory, userTransaction, entityManager ->
						entityManager.persist( new Customer( "Alex" ) ) );
				assertEquals( 1L, customerCountInJta( entityManagerFactory ) );

				RuntimeException failure = new IllegalStateException( "Failed JTA work" );
				assertSame( failure, assertThrows( IllegalStateException.class, () ->
						inJtaTransaction( entityManagerFactory, userTransaction, entityManager -> {
							entityManager.persist( new Customer( "Rolled back" ) );
							entityManager.flush();
							throw failure;
						} ) ) );
				assertEquals( Status.STATUS_NO_TRANSACTION, userTransaction.getStatus() );
				assertEquals( 1L, customerCountInJta( entityManagerFactory ) );
			}
			finally {
				rollbackIfActive( userTransaction );
			}
		}
	}

	//tag::transactions-api-bmt-example[]
	private void inJtaTransaction(
			EntityManagerFactory entityManagerFactory,
			UserTransaction userTransaction,
			Consumer<EntityManager> work) throws Exception {
		userTransaction.begin();
		try (EntityManager entityManager = entityManagerFactory.createEntityManager()) {
			work.accept( entityManager );
			userTransaction.commit();
		}
		catch (Exception failure) {
			try {
				int status = userTransaction.getStatus();
				if ( status == Status.STATUS_ACTIVE || status == Status.STATUS_MARKED_ROLLBACK ) {
					userTransaction.rollback();
				}
			}
			catch (Exception rollbackFailure) {
				failure.addSuppressed( rollbackFailure );
			}
			throw failure;
		}
	}
	//end::transactions-api-bmt-example[]

	@Test
	public void callbacks(ServiceRegistryScope registryScope) {
		try (SessionFactory sessionFactory = buildFactory( registryScope )) {
			List<String> events = new ArrayList<>();
			List<TransactionStatus> outcomes = new ArrayList<>();
			sessionFactory.inTransaction( session -> {
				//tag::transactions-callbacks-example[]
				Transaction transaction = session.getTransaction();
				transaction.runBeforeCompletion( () -> events.add( "before completion" ) );
				transaction.runAfterCompletion( outcomes::add );
				//end::transactions-callbacks-example[]
				session.persist( new Customer( "Alex" ) );
			} );
			assertEquals( List.of( "before completion" ), events );
			assertEquals( List.of( TransactionStatus.COMMITTED ), outcomes );

			try (Session session = sessionFactory.openSession()) {
				Transaction transaction = session.beginTransaction();
				transaction.runAfterCompletion( outcomes::add );
				transaction.setRollbackOnly();
				assertTrue( transaction.isActive() );
				assertTrue( transaction.getRollbackOnly() );
				transaction.commit();
				assertTrue( transaction.isComplete() );
				assertTrue( transaction.wasFailure() );
			}
			assertEquals( List.of( TransactionStatus.COMMITTED, TransactionStatus.ROLLED_BACK ), outcomes );
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	public void helperRollbackOnly(boolean compliant, ServiceRegistryScope registryScope) {
		try (EntityManagerFactory factory = new MetadataSources( registryScope.getRegistry() )
				.addAnnotatedClass( Customer.class ).buildMetadata()
				.getSessionFactoryBuilder().enableJpaTransactionCompliance( compliant ).build()) {
			factory.runInTransaction( entityManager -> entityManager.persist( new Customer( "Alex" ) ) );
			List<EntityManager> contexts = new ArrayList<>();
			java.util.function.Function<EntityManager, String> work = entityManager -> {
				contexts.add( entityManager );
				entityManager.createStatement( "update Customer set name = 'Rolled back'" ).execute();
				entityManager.getTransaction().setRollbackOnly();
				return "work result";
			};
			if ( compliant ) {
				assertThrows( jakarta.persistence.RollbackException.class, () -> factory.callInTransaction( work ) );
			}
			else {
				assertEquals( "work result", factory.callInTransaction( work ) );
			}
			assertFalse( contexts.get( 0 ).isOpen() );
			assertEquals( "Alex", customerName( factory ) );
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	@ServiceRegistry(
			settingConfigurations = @SettingConfiguration(configurer = TestingJtaBootstrap.class),
			settings = @Setting(name = HBM2DDL_AUTO, value = "create-drop")
	)
	public void jtaCompletionCallbackStatus(boolean trackByThread, ServiceRegistryScope registryScope)
			throws Exception {
		UserTransaction userTransaction = TestingJtaPlatformImpl.userTransaction();
		List<Integer> jtaStatuses = new ArrayList<>();
		List<TransactionStatus> hibernateStatuses = new ArrayList<>();
		try (SessionFactory factory = new MetadataSources( registryScope.getRegistry() )
				.addAnnotatedClass( Customer.class ).buildMetadata()
				.getSessionFactoryBuilder().applyJtaTrackingByThread( trackByThread ).build()) {
			try {
				for ( int completion : new int[] {
						Status.STATUS_COMMITTED, Status.STATUS_ROLLEDBACK, Status.STATUS_MARKED_ROLLBACK
				} ) {
					userTransaction.begin();
					TestingJtaPlatformImpl.transactionManager().getTransaction().registerSynchronization(
							new jakarta.transaction.Synchronization() {
								@Override
								public void beforeCompletion() {
								}

								@Override
								public void afterCompletion(int status) {
									jtaStatuses.add( status );
								}
							} );
					try (Session session = factory.openSession()) {
						session.joinTransaction();
						session.getTransaction().runAfterCompletion( hibernateStatuses::add );
						if ( completion == Status.STATUS_COMMITTED ) {
							userTransaction.commit();
						}
						else if ( completion == Status.STATUS_ROLLEDBACK ) {
							userTransaction.rollback();
						}
						else {
							userTransaction.setRollbackOnly();
							assertThrows( jakarta.transaction.RollbackException.class, userTransaction::commit );
						}
					}
				}
				assertEquals( List.of( Status.STATUS_COMMITTED, Status.STATUS_ROLLEDBACK, Status.STATUS_ROLLEDBACK ),
						jtaStatuses );
				assertEquals( List.of( TransactionStatus.COMMITTED, TransactionStatus.ROLLED_BACK,
						TransactionStatus.ROLLED_BACK ), hibernateStatuses );
			}
			finally {
				rollbackIfActive( userTransaction );
			}
		}
	}

	private SessionFactory buildFactory(ServiceRegistryScope registryScope) {
		return new MetadataSources( registryScope.getRegistry() )
				.addAnnotatedClass( Customer.class )
				.buildMetadata()
				.buildSessionFactory();
	}

	private String customerName(EntityManagerFactory factory) {
		return factory.callInTransaction( entityManager ->
				entityManager.createQuery( "select c.name from Customer c", String.class ).getSingleResult() );
	}

	private long customerCountInJta(EntityManagerFactory factory) throws Exception {
		UserTransaction userTransaction = TestingJtaPlatformImpl.userTransaction();
		userTransaction.begin();
		try (EntityManager entityManager = factory.createEntityManager()) {
			long count = entityManager.createQuery( "select count(c) from Customer c", Long.class ).getSingleResult();
			userTransaction.commit();
			return count;
		}
		finally {
			rollbackIfActive( userTransaction );
		}
	}

	private void rollbackIfActive(UserTransaction userTransaction) throws Exception {
		int status = userTransaction.getStatus();
		if ( status == Status.STATUS_ACTIVE || status == Status.STATUS_MARKED_ROLLBACK ) {
			userTransaction.rollback();
		}
	}

	@Entity(name = "Customer")
	public static class Customer {
		@Id
		@GeneratedValue
		private Long id;
		private String name;

		public Customer() {
		}

		public Customer(String name) {
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}
}
