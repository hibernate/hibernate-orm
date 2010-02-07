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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Iterator;
import java.io.Serializable;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContextType;
import javax.persistence.Cache;
import javax.persistence.PersistenceUnitUtil;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.persistence.spi.LoadState;

import org.hibernate.SessionFactory;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.EntityMode;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.cfg.Configuration;
import org.hibernate.ejb.criteria.CriteriaBuilderImpl;
import org.hibernate.ejb.metamodel.MetamodelImpl;
import org.hibernate.ejb.util.PersistenceUtilHelper;

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
	private final CriteriaBuilderImpl criteriaBuilder;
	private final Metamodel metamodel;
	private final HibernatePersistenceUnitUtil util;
	private final Map<String,Object> properties;

	@SuppressWarnings( "unchecked" )
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
		final Iterator<PersistentClass> classes = cfg.getClassMappings();
		//a safe guard till we are confident that metamodel is wll tested
		if ( !"disabled".equalsIgnoreCase( cfg.getProperty( "hibernate.ejb.metamodel.generation" ) ) ) {
			this.metamodel = MetamodelImpl.buildMetamodel( classes, ( SessionFactoryImplementor ) sessionFactory );
		}
		else {
			this.metamodel = null;
		}
		this.criteriaBuilder = new CriteriaBuilderImpl( this );
		this.util = new HibernatePersistenceUnitUtil( this );

		HashMap<String,Object> props = new HashMap<String, Object>();
		addAll( props, ( (SessionFactoryImplementor) sessionFactory ).getProperties() );
		addAll( props, cfg.getProperties() );
		this.properties = Collections.unmodifiableMap( props );
	}

	private static void addAll(HashMap<String, Object> propertyMap, Properties properties) {
		for ( Map.Entry entry : properties.entrySet() ) {
			if ( String.class.isInstance( entry.getKey() ) ) {
				propertyMap.put( (String)entry.getKey(), entry.getValue() );
			}
		}
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

	public CriteriaBuilder getCriteriaBuilder() {
		return criteriaBuilder;
	}

	public Metamodel getMetamodel() {
		return metamodel;
	}

	public void close() {
		sessionFactory.close();
	}

	public Map<String, Object> getProperties() {
		return properties;
	}

	public Cache getCache() {
		// TODO : cache the cache reference?
		if ( ! isOpen() ) {
			throw new IllegalStateException("EntityManagerFactory is closed");
		}
		return new JPACache( sessionFactory );
	}

	public PersistenceUnitUtil getPersistenceUnitUtil() {
		if ( ! isOpen() ) {
			throw new IllegalStateException("EntityManagerFactory is closed");
		}
		return util;
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

	private static class HibernatePersistenceUnitUtil implements PersistenceUnitUtil, Serializable {
		private final HibernateEntityManagerFactory emf;

		private HibernatePersistenceUnitUtil(EntityManagerFactoryImpl emf) {
			this.emf = emf;
		}

		public boolean isLoaded(Object entity, String attributeName) {
			LoadState state = PersistenceUtilHelper.isLoadedWithoutReference( entity, attributeName );
			if (state == LoadState.LOADED) {
				return true;
			}
			else if (state == LoadState.NOT_LOADED ) {
				return false;
			}
			else {
				return PersistenceUtilHelper.isLoadedWithReference( entity, attributeName ) != LoadState.NOT_LOADED;
			}
		}

		public boolean isLoaded(Object entity) {
			return PersistenceUtilHelper.isLoaded( entity ) != LoadState.NOT_LOADED;
		}

		public Object getIdentifier(Object entity) {
			final Class entityClass = Hibernate.getClass( entity );
			final ClassMetadata classMetadata = emf.getSessionFactory().getClassMetadata( entityClass );
			if (classMetadata == null) {
				throw new IllegalArgumentException( entityClass + " is not an entity" );
			}
			//TODO does that work for @IdClass?
			return classMetadata.getIdentifier( entity, EntityMode.POJO );
		}
	}
}
