//$Id$
package org.hibernate.ejb;

import java.util.Map;
import java.util.Set;
import java.io.Serializable;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContextType;
import javax.persistence.Cache;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.criteria.QueryBuilder;
import javax.persistence.spi.PersistenceUnitTransactionType;

import org.hibernate.SessionFactory;
import org.hibernate.ejb.criteria.QueryBuilderImpl;

/**
 * @author Gavin King
 * @author Emmanuel Bernard
 */
public class EntityManagerFactoryImpl implements HibernateEntityManagerFactory {
	private SessionFactory sessionFactory;
	private PersistenceUnitTransactionType transactionType;
	private boolean discardOnClose;
	private Class sessionInterceptorClass;
	private QueryBuilderImpl criteriaQueryBuilder;

	public EntityManagerFactoryImpl(
			SessionFactory sessionFactory,
			PersistenceUnitTransactionType transactionType,
			boolean discardOnClose,
			Class sessionInterceptorClass) {
		this.sessionFactory = sessionFactory;
		this.transactionType = transactionType;
		this.discardOnClose = discardOnClose;
		this.sessionInterceptorClass = sessionInterceptorClass;
		this.criteriaQueryBuilder = new QueryBuilderImpl( this );
	}

	public EntityManager createEntityManager() {
		return createEntityManager( null );
	}

	public EntityManager createEntityManager(Map map) {
		//TODO support discardOnClose, persistencecontexttype?, interceptor,
		return new EntityManagerImpl(
				this, PersistenceContextType.EXTENDED, transactionType,
				discardOnClose, sessionInterceptorClass, map
		);
	}

	public QueryBuilder getQueryBuilder() {
		return criteriaQueryBuilder;
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
		// TODO : cache the cache reference?
		return new JPACache( sessionFactory );
	}

	public boolean isOpen() {
		return ! sessionFactory.isClosed();
	}

	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}

	private static class JPACache implements Cache {
		private SessionFactory sessionFactory;

		private JPACache(SessionFactory sessionFactory) {
			this.sessionFactory = sessionFactory;
		}

		public boolean contains(Class entityClass, Object identifier) {
			return sessionFactory.getCache().containsEntity( entityClass, ( Serializable ) identifier );
		}

		public void evict(Class entityClass, Object identifier) {
			sessionFactory.getCache().evictEntity( entityClass, ( Serializable ) identifier );
		}

		public void evict(Class entityClass) {
			sessionFactory.getCache().evictEntityRegion( entityClass );
		}

		public void evictAll() {
			sessionFactory.getCache().evictEntityRegions();
// TODO : if we want to allow an optional clearing of all cache data, the additional calls would be:
//			sessionFactory.getCache().evictCollectionRegions();
//			sessionFactory.getCache().evictQueryRegions();
		}
	}
}
