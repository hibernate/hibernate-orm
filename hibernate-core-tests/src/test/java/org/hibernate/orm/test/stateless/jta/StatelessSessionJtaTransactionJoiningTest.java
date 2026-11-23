package org.hibernate.orm.test.stateless.jta;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.transaction.Status;
import jakarta.transaction.TransactionManager;

import org.hibernate.StatelessSession;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.transaction.internal.jta.JtaStatusHelper;
import org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorImpl;

import org.hibernate.testing.jta.TestingJtaPlatformImpl;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that StatelessSession operations properly join JTA transactions.
 * <p>
 * This test verifies the fix for the issue where StatelessSession.insert(),
 * update(), delete(), and upsert() did not call checkTransactionSynchStatus()
 * before executing, causing them to not join active JTA transactions.
 *
 * @author Hibernate Community
 */
@DomainModel(annotatedClasses = StatelessSessionJtaTransactionJoiningTest.SimpleEntity.class)
@SessionFactory
@ServiceRegistry(settings = {
		@Setting(name = AvailableSettings.JTA_PLATFORM, value = "org.hibernate.testing.jta.TestingJtaPlatformImpl"),
		@Setting(name = AvailableSettings.TRANSACTION_COORDINATOR_STRATEGY, value = "jta")
})
public class StatelessSessionJtaTransactionJoiningTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(session -> session.createMutationQuery("delete SimpleEntity").executeUpdate());
	}

	/**
	 * Test that StatelessSession auto-joins a JTA transaction when insert() is
	 * called.
	 * <p>
	 * Before the fix, StatelessSession.insert() only called checkOpen() but not
	 * checkTransactionSynchStatus(), so it would not join the active JTA
	 * transaction.
	 */
	@Test
	public void testInsertJoinsJtaTransaction(SessionFactoryScope scope) throws Exception {
		assertFalse(JtaStatusHelper.isActive(TestingJtaPlatformImpl.INSTANCE.getTransactionManager()));

		// Create StatelessSession BEFORE starting JTA transaction
		// (simulates @PostConstruct in Jakarta Data repositories)
		try (StatelessSession statelessSession = scope.getSessionFactory().openStatelessSession()) {
			SharedSessionContractImplementor sessionImpl = (SharedSessionContractImplementor) statelessSession;
			JtaTransactionCoordinatorImpl transactionCoordinator = (JtaTransactionCoordinatorImpl) sessionImpl
					.getTransactionCoordinator();

			// Session created before JTA transaction - should not be joined yet
			assertFalse(transactionCoordinator.isJoined(),
					"StatelessSession should not be joined to JTA before transaction starts");

			// Now start JTA transaction (like UserTransaction.begin() in test code)
			inJtaTransaction(() -> {
				assertTrue(JtaStatusHelper.isActive(TestingJtaPlatformImpl.INSTANCE.getTransactionManager()));

				// Session still not joined (transaction started after session creation)
				assertFalse(transactionCoordinator.isJoined(),
						"StatelessSession should not auto-join until an operation is performed");

				// Perform insert - this SHOULD join the JTA transaction after the fix
				SimpleEntity entity = new SimpleEntity("test");
				statelessSession.insert(entity);

				// After insert, session should now be joined to JTA transaction
				assertTrue(transactionCoordinator.isJoined(),
						"StatelessSession should join JTA transaction when insert() is called");
				assertTrue(transactionCoordinator.isSynchronizationRegistered(),
						"StatelessSession should register synchronization with JTA");

			});
		}
	}

	/**
	 * Test that StatelessSession auto-joins a JTA transaction when update() is
	 * called.
	 */
	@Test
	public void testUpdateJoinsJtaTransaction(SessionFactoryScope scope) throws Exception {
		// First, create an entity to update
		SimpleEntity entity = new SimpleEntity("original");
		scope.inTransaction(session -> session.persist(entity));

		assertFalse(JtaStatusHelper.isActive(TestingJtaPlatformImpl.INSTANCE.getTransactionManager()));

		try (StatelessSession statelessSession = scope.getSessionFactory().openStatelessSession()) {
			SharedSessionContractImplementor sessionImpl = (SharedSessionContractImplementor) statelessSession;
			JtaTransactionCoordinatorImpl transactionCoordinator = (JtaTransactionCoordinatorImpl) sessionImpl
					.getTransactionCoordinator();

			// Start JTA transaction after session creation
			inJtaTransaction(() -> {

				assertFalse(transactionCoordinator.isJoined());

				// Update should join JTA transaction
				entity.setName("updated");
				statelessSession.update(entity);

				assertTrue(transactionCoordinator.isJoined(),
						"StatelessSession should join JTA transaction when update() is called");

			});
		}
	}

	/**
	 * Test that StatelessSession auto-joins a JTA transaction when delete() is
	 * called.
	 */
	@Test
	public void testDeleteJoinsJtaTransaction(SessionFactoryScope scope) throws Exception {
		// First, create an entity to delete
		SimpleEntity entity = new SimpleEntity("to-delete");
		scope.inTransaction(session -> session.persist(entity));

		assertFalse(JtaStatusHelper.isActive(TestingJtaPlatformImpl.INSTANCE.getTransactionManager()));

		try (StatelessSession statelessSession = scope.getSessionFactory().openStatelessSession()) {
			SharedSessionContractImplementor sessionImpl = (SharedSessionContractImplementor) statelessSession;
			JtaTransactionCoordinatorImpl transactionCoordinator = (JtaTransactionCoordinatorImpl) sessionImpl
					.getTransactionCoordinator();

			// Start JTA transaction after session creation
			inJtaTransaction(() -> {

				assertFalse(transactionCoordinator.isJoined());

				// Delete should join JTA transaction
				statelessSession.delete(entity);

				assertTrue(transactionCoordinator.isJoined(),
						"StatelessSession should join JTA transaction when delete() is called");

			});
		}
	}

	/**
	 * Test that StatelessSession auto-joins a JTA transaction when upsert() is
	 * called.
	 */
	@Test
	public void testUpsertJoinsJtaTransaction(SessionFactoryScope scope) throws Exception {
		assertFalse(JtaStatusHelper.isActive(TestingJtaPlatformImpl.INSTANCE.getTransactionManager()));

		try (StatelessSession statelessSession = scope.getSessionFactory().openStatelessSession()) {
			SharedSessionContractImplementor sessionImpl = (SharedSessionContractImplementor) statelessSession;
			JtaTransactionCoordinatorImpl transactionCoordinator = (JtaTransactionCoordinatorImpl) sessionImpl
					.getTransactionCoordinator();

			// Start JTA transaction after session creation
			inJtaTransaction(() -> {

				assertFalse(transactionCoordinator.isJoined());

				// Upsert should join JTA transaction
				SimpleEntity entity = new SimpleEntity(1L, "upserted");
				statelessSession.upsert(entity);

				assertTrue(transactionCoordinator.isJoined(),
						"StatelessSession should join JTA transaction when upsert() is called");

			});
		}
	}

	@Test
	public void testSessionJoinsSuccessiveJtaTransactions(SessionFactoryScope scope) throws Exception {
		try (StatelessSession session = scope.getSessionFactory().openStatelessSession()) {
			JtaTransactionCoordinatorImpl coordinator = (JtaTransactionCoordinatorImpl)
					((SharedSessionContractImplementor) session).getTransactionCoordinator();

			for (int i = 0; i < 2; i++) {
				final int transactionNumber = i;
				inJtaTransaction(() -> {
					assertFalse(coordinator.isJoined());
					session.insert(new SimpleEntity("transaction-" + transactionNumber));
					assertTrue(coordinator.isJoined());
					assertTrue(coordinator.isSynchronizationRegistered());
				});
				assertFalse(coordinator.isJoined());
			}
		}
	}

	private static void inJtaTransaction(Runnable action) throws Exception {
		TransactionManager transactionManager = TestingJtaPlatformImpl.INSTANCE.getTransactionManager();
		transactionManager.begin();
		try {
			action.run();
			transactionManager.commit();
		}
		finally {
			if (transactionManager.getStatus() != Status.STATUS_NO_TRANSACTION) {
				transactionManager.rollback();
			}
		}
	}

	@Entity(name = "SimpleEntity")
	public static class SimpleEntity {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		public SimpleEntity() {
		}

		public SimpleEntity(String name) {
			this.name = name;
		}

		public SimpleEntity(Long id, String name) {
			this.id = id;
			this.name = name;
		}

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
