//$Id$
package org.hibernate.ejb;

import java.util.Map;
import java.util.Set;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContextType;
import javax.persistence.Cache;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.criteria.QueryBuilder;
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

	public QueryBuilder getQueryBuilder() {
		//FIXME
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public Metamodel getMetamodel() {
		//FIXME
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public void close() {
		sessionFactory.close();
	}

	public Map<String, Object> getProperties() {
		//FIXME
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public Set<String> getSupportedProperties() {
		//FIXME
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public Cache getCache() {
		//FIXME
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public boolean isOpen() {
		return ! sessionFactory.isClosed();
	}

	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}

}
