/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.junit5.envers;

import java.util.function.Consumer;
import java.util.function.Function;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

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

	public void inTransactions(Consumer<EntityManager>... actions) {
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

	public <R> R inTransaction(Function<EntityManager, R> action) {
		return inTransaction( getEntityManagerFactory(), action );
	}

	public <R> R inTransaction(EntityManagerFactory factory, Function<EntityManager, R> action) {
		EntityManager entityManager = factory.createEntityManager();
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
		finally {
			entityManager.close();
		}
	}
}
