/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.junit5;

import java.util.function.Consumer;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;

import org.jboss.logging.Logger;

/**
 * A scope or holder for the EntityManagerFactory instance associated with a given test class.
 * Used to:
 *
 * 		* provide lifecycle management related to the EntityManagerFactory
 * 		* access to functional programming using an EntityManager generated
 * 			from the EntityManagerFactory.
 *
 * @author Chris Cranford
 */
public class EntityManagerFactoryScope implements EntityManagerFactoryAccess {
	private static final Logger log = Logger.getLogger( EntityManagerFactoryScope.class );

	private final EntityManagerFactoryProducer producer;

	private EntityManagerFactory entityManagerFactory;

	public EntityManagerFactoryScope(EntityManagerFactoryProducer producer) {
		log.trace( "EntityManagerFactoryScope#<init>" );
		this.producer = producer;
	}

	public void rebuild() {
		log.trace( "EntityManagerFactoryScope#rebuild" );
		releaseEntityManagerFactory();

		entityManagerFactory = producer.produceEntityManagerFactory();
	}

	public void releaseEntityManagerFactory() {
		log.trace( "EntityManagerFactoryScope#releaseEntityManagerFactory" );
		if ( entityManagerFactory != null ) {
			entityManagerFactory.close();
		}
	}

	@Override
	public EntityManagerFactory getEntityManagerFactory() {
		log.trace( "EntityManagerFactoryScope#getEntityManagerFactory" );
		if ( entityManagerFactory == null || !entityManagerFactory.isOpen() ) {
			entityManagerFactory = producer.produceEntityManagerFactory();
		}
		return entityManagerFactory;
	}

	public void inTransaction(Consumer<EntityManager> action) {
		log.trace( "#inTransaction(action)" );
		inTransaction( getEntityManagerFactory(), action );
	}

	public void inTransaction(EntityManagerFactory factory, Consumer<EntityManager> action) {
		log.trace( "#inTransaction(factory, action)" );
		final EntityManager entityManager = factory.createEntityManager();
		try {
			log.trace( "EntityManager opened, calling action" );
			inTransaction( entityManager, action );
			log.trace( "called action" );
		}
		finally {
			log.trace( "EntityManager close" );
			entityManager.close();
		}
	}

	public void inTransaction(EntityManager entityManager, Consumer<EntityManager> action) {
		log.trace( "inTransaction(entityManager, action)" );

		final EntityTransaction trx = entityManager.getTransaction();
		try {
			trx.begin();
			log.trace( "Calling action in trx" );
			action.accept( entityManager );
			log.trace( "Called trx in action" );

			log.trace( "Committing transaction" );
			trx.commit();
			log.trace( "Committed transaction" );
		}
		catch (Exception e) {
			log.tracef( "Error calling action: %s (%s) - rollingback", e.getClass().getName(), e.getMessage() );
			try {
				trx.rollback();
			}
			catch (Exception ignored) {
				log.trace( "Was unable to roll back transaction" );
			}
			throw e;
		}
	}
}
