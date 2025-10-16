/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.envers;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.strategy.spi.AuditStrategy;
import org.hibernate.testing.jta.TestingJtaPlatformImpl;

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
		EntityManager entityManager = getEntityManagerFactory().createEntityManager();
		try {
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

	public static AuditStrategy getAuditStrategy(EntityManager entityManager) {
		if ( entityManager.getDelegate() instanceof Session ) {
			return getAuditStrategy( (Session) entityManager.getDelegate() );
		}
		else if ( entityManager.getDelegate() instanceof EntityManager ) {
			return getAuditStrategy( (EntityManager) entityManager.getDelegate() );
		}
		else {
			throw new IllegalArgumentException( "Could not resolve AuditStrategy" );
		}
	}

	public static AuditStrategy getAuditStrategy(Session session) {
		SessionImplementor sessionImpl;
		if ( !(session instanceof SessionImplementor) ) {
			sessionImpl = (SessionImplementor) session.getSessionFactory().getCurrentSession();
		}
		else {
			sessionImpl = (SessionImplementor) session;
		}
		final var enversService = sessionImpl.getFactory()
				.getServiceRegistry()
				.getService( EnversService.class );
		if ( enversService == null ) {
			throw new IllegalArgumentException( "EnversService is not available in the provided Session" );
		}
		return enversService.getAuditStrategy();
	}
}
