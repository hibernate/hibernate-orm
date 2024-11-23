/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.junit;

import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.resource.jdbc.spi.StatementInspector;

import org.hibernate.testing.orm.transaction.TransactionUtil;
import org.junit.jupiter.api.extension.ExtensionContext;

import org.jboss.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

abstract class AbstractEntityManagerFactoryScope implements EntityManagerFactoryScope, ExtensionContext.Store.CloseableResource {
	private static final Logger log = Logger.getLogger( EntityManagerFactoryScope.class );

	protected EntityManagerFactory emf;
	protected boolean active = true;

	@Override
	public EntityManagerFactory getEntityManagerFactory() {
		if ( emf == null ) {
			if ( !active ) {
				throw new IllegalStateException( "EntityManagerFactoryScope is no longer active" );
			}

			log.debug( "Creating EntityManagerFactory" );
			emf = createEntityManagerFactory();
		}

		return emf;
	}

	protected abstract EntityManagerFactory createEntityManagerFactory();

	@Override
	public StatementInspector getStatementInspector() {
		return getEntityManagerFactory().unwrap( SessionFactoryImplementor.class )
				.getSessionFactoryOptions()
				.getStatementInspector();
	}

	@Override
	public <T extends StatementInspector> T getStatementInspector(Class<T> type) {
		//noinspection unchecked
		return (T) getStatementInspector();
	}

	@Override
	public void close() {
		if ( !active ) {
			return;
		}

		log.debug( "Closing SessionFactoryScope" );

		active = false;
		releaseEntityManagerFactory();
	}

	public void releaseEntityManagerFactory() {
		if ( emf != null ) {
			log.debug( "Releasing SessionFactory" );

			try {
				emf.close();
			}
			catch (Exception e) {
				log.warn( "Error closing EMF", e );
			}
			finally {
				emf = null;
			}
		}
	}

	@Override
	public void inEntityManager(Consumer<EntityManager> action) {
		log.trace( "#inEntityManager(Consumer)" );

		try (SessionImplementor session = getEntityManagerFactory().createEntityManager()
				.unwrap( SessionImplementor.class )) {
			log.trace( "EntityManager opened, calling action" );
			action.accept( session );
		}
		finally {
			log.trace( "EntityManager close - auto-close block" );
		}
	}

	@Override
	public <T> T fromEntityManager(Function<EntityManager, T> action) {
		log.trace( "#fromEntityManager(Function)" );

		try (SessionImplementor session = getEntityManagerFactory().createEntityManager()
				.unwrap( SessionImplementor.class )) {
			log.trace( "EntityManager opened, calling action" );
			return action.apply( session );
		}
		finally {
			log.trace( "EntityManager close - auto-close block" );
		}
	}

	@Override
	public void inTransaction(Consumer<EntityManager> action) {
		log.trace( "#inTransaction(Consumer)" );

		try (SessionImplementor session = getEntityManagerFactory().createEntityManager()
				.unwrap( SessionImplementor.class )) {
			log.trace( "EntityManager opened, calling action" );
			inTransaction( session, action );
		}
		finally {
			log.trace( "EntityManager close - auto-close block" );
		}
	}

	@Override
	public <T> T fromTransaction(Function<EntityManager, T> action) {
		log.trace( "#fromTransaction(Function)" );

		try (SessionImplementor session = getEntityManagerFactory().createEntityManager()
				.unwrap( SessionImplementor.class )) {
			log.trace( "EntityManager opened, calling action" );
			return fromTransaction( session, action );
		}
		finally {
			log.trace( "EntityManager close - auto-close block" );
		}
	}

	@Override
	public void inTransaction(EntityManager entityManager, Consumer<EntityManager> action) {
		log.trace( "inTransaction(EntityManager,Consumer)" );
		TransactionUtil.inTransaction( entityManager, action );
	}

	@Override
	public <T> T fromTransaction(EntityManager entityManager, Function<EntityManager, T> action) {
		log.trace( "fromTransaction(EntityManager,Function)" );

		final SessionImplementor session = entityManager.unwrap( SessionImplementor.class );
		return TransactionUtil.fromTransaction( session, action );
	}

}

