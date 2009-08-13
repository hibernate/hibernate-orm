/*
 * Copyright (c) 2009, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.ejb;

import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import java.io.Serializable;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContextType;
import javax.persistence.Cache;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.criteria.QueryBuilder;
import javax.persistence.spi.PersistenceUnitTransactionType;

import org.hibernate.SessionFactory;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.cfg.Configuration;
import org.hibernate.ejb.criteria.QueryBuilderImpl;
import org.hibernate.ejb.metamodel.MetamodelImpl;

/**
 * Actual Hiberate implementation of {@link javax.persistence.EntityManagerFactory}.
 *
 * @author Gavin King
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public class EntityManagerFactoryImpl implements HibernateEntityManagerFactory {
	private final SessionFactory sessionFactory;
	private final PersistenceUnitTransactionType transactionType;
	private final boolean discardOnClose;
	private final Class sessionInterceptorClass;
	private final QueryBuilderImpl criteriaQueryBuilder;
	private final Metamodel metamodel;

	public EntityManagerFactoryImpl(
			SessionFactory sessionFactory,
			PersistenceUnitTransactionType transactionType,
			boolean discardOnClose,
			Class<?> sessionInterceptorClass,
			Configuration cfg) {
		this.sessionFactory = sessionFactory;
		this.transactionType = transactionType;
		this.discardOnClose = discardOnClose;
		this.sessionInterceptorClass = sessionInterceptorClass;
		@SuppressWarnings( "unchecked" )
		final Iterator<PersistentClass> classes = cfg.getClassMappings();
		//a safe guard till we are confident that metamodel is wll tested
		if ( !"disabled".equalsIgnoreCase( cfg.getProperty( "hibernate.ejb.metamodel.generation" ) ) ) {
			this.metamodel = new MetamodelImpl( classes );
		}
		else {
			this.metamodel = null;
		}
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
		return metamodel;  //To change body of implemented methods use File | Settings | File Templates.
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
