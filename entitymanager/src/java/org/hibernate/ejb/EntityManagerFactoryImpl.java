//$Id$
package org.hibernate.ejb;

import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContextType;
import javax.persistence.spi.PersistenceUnitTransactionType;

import org.hibernate.SessionFactory;

/**
 * @author Gavin King
 * @author Emmanuel Bernard
 */
public class EntityManagerFactoryImpl implements HibernateEntityManagerFactory {

	private SessionFactory sessionFactory;
	private PersistenceUnitTransactionType transactionType;
	private boolean discardOnClose;
	private Class sessionInterceptorClass;

	public EntityManagerFactoryImpl(
			SessionFactory sessionFactory,
			PersistenceUnitTransactionType transactionType,
			boolean discardOnClose,
			Class sessionInterceptorClass) {
		this.sessionFactory = sessionFactory;
		this.transactionType = transactionType;
		this.discardOnClose = discardOnClose;
		this.sessionInterceptorClass = sessionInterceptorClass;
	}

	public EntityManager createEntityManager() {
		return createEntityManager( null );
	}

	public EntityManager createEntityManager(Map map) {
		//TODO support discardOnClose, persistencecontexttype?, interceptor,
		return new EntityManagerImpl(
				sessionFactory, PersistenceContextType.EXTENDED, transactionType,
				discardOnClose, sessionInterceptorClass, map
		);
	}

	public void close() {
		sessionFactory.close();
	}

	public boolean isOpen() {
		return ! sessionFactory.isClosed();
	}

	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}

}
