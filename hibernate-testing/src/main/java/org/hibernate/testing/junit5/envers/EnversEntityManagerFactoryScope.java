/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.junit5.envers;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.hibernate.testing.jta.TestingJtaPlatformImpl;
import org.hibernate.testing.junit5.EntityManagerFactoryAccess;

/**
 * @author Chris Cranford
 */
public class EnversEntityManagerFactoryScope implements EntityManagerFactoryAccess {
	private final EnversEntityManagerFactoryProducer entityManagerFactoryProducer;
	private final Strategy auditStrategy;

	private EntityManagerFactory entityManagerFactory;

	public EnversEntityManagerFactoryScope(EnversEntityManagerFactoryProducer producer, Strategy auditStrategy) {
		this.auditStrategy = auditStrategy;
		this.entityManagerFactoryProducer = producer;
	}

	public void releaseEntityManagerFactory() {
		if ( entityManagerFactory != null ) {
			entityManagerFactory.close();
			entityManagerFactory = null;
		}
	}

	@Override
	public EntityManagerFactory getEntityManagerFactory() {
		if ( entityManagerFactory == null || !entityManagerFactory.isOpen() ) {
			final String strategy = auditStrategy.getSettingValue();
			entityManagerFactory = entityManagerFactoryProducer.produceEntityManagerFactory( strategy );
		}
		return entityManagerFactory;
	}

	public void inJtaTransaction(Consumer<EntityManager> action) throws Exception {
		inJtaTransaction( getEntityManagerFactory(), action );
	}

	public void inJtaTransaction(EntityManagerFactory factory, Consumer<EntityManager> action) throws Exception {
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		EntityManager entityManager = factory.createEntityManager();
		try {
			action.accept( entityManager );
		}
		finally {
			entityManager.close();
			TestingJtaPlatformImpl.tryCommit();
		}
	}

	public void inTransaction(Consumer<EntityManager> action) {
		inTransaction( getEntityManagerFactory(), action );
	}

	public void inTransaction(EntityManagerFactory factory, Consumer<EntityManager> action) {
		EntityManager entityManager = factory.createEntityManager();
		try {
			entityManager.getTransaction().begin();
			action.accept( entityManager );
			entityManager.getTransaction().commit();
		}
		catch ( Exception e ) {
			if ( entityManager.getTransaction().isActive() ) {
				entityManager.getTransaction().rollback();
			}
			throw e;
		}
		finally {
			entityManager.close();
		}
	}

	public void inTransaction(EntityManager entityManager, Consumer<EntityManager> action) {
		try {
			entityManager.getTransaction().begin();
			action.accept( entityManager );
			entityManager.getTransaction().commit();
		}
		catch ( Exception e ) {
			if ( entityManager.getTransaction().isActive() ) {
				entityManager.getTransaction().rollback();
			}
			throw e;
		}
	}

	@SafeVarargs
	public final void inTransactions(Consumer<EntityManager>... actions) {
		inTransactionsWithInit( null, actions );
	}

	/**
	 * Allows executing a series of transaction-scoped actions with the same {@link EntityManager} with the
	 * ability to provided a special {@code initAction} callback.
	 *
	 * @param initAction A callback allowing initial setup before running transactions, may be {@code null}.
	 * @param actions List of callback actions to be executed within separate transaction scopes.
	 */
	@SafeVarargs
	public final void inTransactionsWithInit(Consumer<EntityManager> initAction, Consumer<EntityManager>... actions) {
		EntityManager entityManager = getEntityManagerFactory().createEntityManager();
		try {
			if ( initAction != null ) {
				initAction.accept( entityManager );
			}
			for ( Consumer<EntityManager> action : actions ) {
				try {
					entityManager.getTransaction().begin();
					action.accept( entityManager );
					entityManager.getTransaction().commit();
				}
				catch ( Exception e ) {
					if ( entityManager.getTransaction().isActive() ) {
						entityManager.getTransaction().rollback();
					}
					throw e;
				}
			}
		}
		finally {
			entityManager.close();
		}
	}

	/**
	 * Allows executing a series of transaction-scoped actions with the same {@link EntityManager} where the
	 * {@link EntityManager}'s persistence context will be cleared after every transaction-scope.
	 *
	 * @param actions List of callback actions to be executed within separate transaction scopes.
	 */
	@SafeVarargs
	public final void inTransactionsWithClear(Consumer<EntityManager>... actions) {
		EntityManager entityManager = getEntityManagerFactory().createEntityManager();
		try {
			for ( Consumer<EntityManager> action : actions ) {
				try {
					entityManager.getTransaction().begin();
					action.accept( entityManager );
					entityManager.getTransaction().commit();
					entityManager.clear();
				}
				catch ( Exception e ) {
					if ( entityManager.getTransaction().isActive() ) {
						entityManager.getTransaction().rollback();
					}
					throw e;
				}
			}
		}
		finally {
			entityManager.close();
		}
	}

	@SafeVarargs
	public final List<Long> inTransactionsWithTimeouts(int timeout, Consumer<EntityManager>... actions) {
		EntityManager entityManager = getEntityManagerFactory().createEntityManager();
		try {
			final List<Long> timestamps = new ArrayList<>();

			timestamps.add( System.currentTimeMillis() );
			for ( Consumer<EntityManager> action : actions ) {
				try {
					Thread.sleep( 100 );
					entityManager.getTransaction().begin();
					action.accept( entityManager );
					entityManager.getTransaction().commit();
					timestamps.add( System.currentTimeMillis() );
				}
				catch ( InterruptedException e ) {
					if ( entityManager.getTransaction().isActive() ) {
						entityManager.getTransaction().rollback();
					}
					throw new RuntimeException( "Failed to wait on timeout", e );
				}
				catch ( Exception e ) {
					if ( entityManager.getTransaction().isActive() ) {
						entityManager.getTransaction().rollback();
					}
					throw e;
				}
			}

			return timestamps;
		}
		finally {
			entityManager.close();
		}
	}

	public <R> R inTransaction(Function<EntityManager, R> action) {
		return inTransaction( getEntityManagerFactory(), action );
	}

	public <R> R inTransaction(EntityManagerFactory factory, Function<EntityManager, R> action) {
		EntityManager entityManager = factory.createEntityManager();
		try {
			return inTransaction( entityManager, action );
		}
		finally {
			entityManager.close();
		}
	}

	public <R> R inTransaction(EntityManager entityManager, Function<EntityManager, R> action) {
		try {
			entityManager.getTransaction().begin();
			R result = action.apply( entityManager );
			entityManager.getTransaction().commit();
			return result;
		}
		catch ( Exception e ) {
			if ( entityManager.getTransaction().isActive() ) {
				entityManager.getTransaction().rollback();
			}
			throw e;
		}
	}

	public void inJPA(Consumer<EntityManager> action) {
		inJPA( getEntityManagerFactory(), action );
	}

	public <R> R inJPA(Function<EntityManager, R> action) {
		return inJPA( getEntityManagerFactory(), action );
	}

	public void inJPA(EntityManagerFactory factory, Consumer<EntityManager> action) {
		EntityManager entityManager = factory.createEntityManager();
		try {
			action.accept( entityManager );
		}
		catch ( Exception e ) {
			if ( entityManager.getTransaction().isActive() ) {
				entityManager.getTransaction().rollback();
			}
			throw e;
		}
		finally {
			entityManager.close();
		}
	}

	public <R> R inJPA(EntityManagerFactory factory, Function<EntityManager, R> action) {
		EntityManager entityManager = factory.createEntityManager();
		try {
			return action.apply( entityManager );
		}
		catch ( Exception e ) {
			if ( entityManager.getTransaction().isActive() ) {
				entityManager.getTransaction().rollback();
			}
			throw e;
		}
		finally {
			entityManager.close();
		}
	}
}
