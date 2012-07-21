/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009, 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.jpa.internal;

import javax.persistence.Cache;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContextType;
import javax.persistence.PersistenceException;
import javax.persistence.PersistenceUnitUtil;
import javax.persistence.Query;
import javax.persistence.SynchronizationType;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.spi.LoadState;
import javax.persistence.spi.PersistenceUnitTransactionType;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.jboss.logging.Logger;

import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.ejb.HibernateEntityManagerFactory;
import org.hibernate.engine.spi.NamedQueryDefinition;
import org.hibernate.engine.spi.NamedQueryDefinitionBuilder;
import org.hibernate.engine.spi.NamedSQLQueryDefinition;
import org.hibernate.engine.spi.NamedSQLQueryDefinitionBuilder;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.UUIDGenerator;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.jpa.AvailableSettings;
import org.hibernate.jpa.HibernateQuery;
import org.hibernate.jpa.boot.internal.SettingsImpl;
import org.hibernate.jpa.criteria.CriteriaBuilderImpl;
import org.hibernate.jpa.internal.metamodel.MetamodelImpl;
import org.hibernate.jpa.internal.util.PersistenceUtilHelper;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.service.ServiceRegistry;

/**
 * Actual Hibernate implementation of {@link javax.persistence.EntityManagerFactory}.
 *
 * @author Gavin King
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public class EntityManagerFactoryImpl implements HibernateEntityManagerFactory {
	private static final long serialVersionUID = 5423543L;
	private static final IdentifierGenerator UUID_GENERATOR = UUIDGenerator.buildSessionFactoryUniqueIdentifierGenerator();

	private static final Logger log = Logger.getLogger( EntityManagerFactoryImpl.class );

	private final transient SessionFactoryImpl sessionFactory;
	private final transient PersistenceUnitTransactionType transactionType;
	private final transient boolean discardOnClose;
	private final transient Class sessionInterceptorClass;
	private final transient CriteriaBuilderImpl criteriaBuilder;
	private final transient Metamodel metamodel;
	private final transient HibernatePersistenceUnitUtil util;
	private final transient Map<String,Object> properties;
	private final String entityManagerFactoryName;

	private final transient PersistenceUtilHelper.MetadataCache cache = new PersistenceUtilHelper.MetadataCache();

	@SuppressWarnings( "unchecked" )
	public EntityManagerFactoryImpl(
			PersistenceUnitTransactionType transactionType,
			boolean discardOnClose,
			Class sessionInterceptorClass,
			Configuration cfg,
			ServiceRegistry serviceRegistry,
			String persistenceUnitName) {
		this(
				persistenceUnitName,
				(SessionFactoryImplementor) cfg.buildSessionFactory( serviceRegistry ),
				new SettingsImpl().setReleaseResourcesOnCloseEnabled( discardOnClose ).setSessionInterceptorClass( sessionInterceptorClass ).setTransactionType( transactionType ),
				cfg.getProperties(),
				cfg
		);
	}

	public EntityManagerFactoryImpl(
			String persistenceUnitName,
			SessionFactoryImplementor sessionFactory,
			SettingsImpl settings,
			Map<?, ?> configurationValues,
			Configuration cfg) {
		this.sessionFactory = (SessionFactoryImpl) sessionFactory;
		this.transactionType = settings.getTransactionType();
		this.discardOnClose = settings.isReleaseResourcesOnCloseEnabled();
		this.sessionInterceptorClass = settings.getSessionInterceptorClass();

		final Iterator<PersistentClass> classes = cfg.getClassMappings();
		final JpaMetaModelPopulationSetting jpaMetaModelPopulationSetting = determineJpaMetaModelPopulationSetting( cfg );
		if ( JpaMetaModelPopulationSetting.DISABLED == jpaMetaModelPopulationSetting ) {
			this.metamodel = null;
		}
		else {
			this.metamodel = MetamodelImpl.buildMetamodel(
					classes,
					sessionFactory,
					JpaMetaModelPopulationSetting.IGNORE_UNSUPPORTED == jpaMetaModelPopulationSetting
			);
		}
		this.criteriaBuilder = new CriteriaBuilderImpl( this );
		this.util = new HibernatePersistenceUnitUtil( this );

		HashMap<String,Object> props = new HashMap<String, Object>();
		addAll( props, sessionFactory.getProperties() );
		addAll( props, cfg.getProperties() );
		addAll( props, configurationValues );
		this.properties = Collections.unmodifiableMap( props );
		String entityManagerFactoryName = (String)this.properties.get( AvailableSettings.ENTITY_MANAGER_FACTORY_NAME);
		if (entityManagerFactoryName == null) {
			entityManagerFactoryName = persistenceUnitName;
		}
		if (entityManagerFactoryName == null) {
			entityManagerFactoryName = (String) UUID_GENERATOR.generate(null, null);
		}
		this.entityManagerFactoryName = entityManagerFactoryName;
		EntityManagerFactoryRegistry.INSTANCE.addEntityManagerFactory(entityManagerFactoryName, this);
	}

	private enum JpaMetaModelPopulationSetting {
		ENABLED,
		DISABLED,
		IGNORE_UNSUPPORTED;
		
		private static JpaMetaModelPopulationSetting parse(String setting) {
			if ( "enabled".equalsIgnoreCase( setting ) ) {
				return ENABLED;
			}
			else if ( "disabled".equalsIgnoreCase( setting ) ) {
				return DISABLED;
			}
			else {
				return IGNORE_UNSUPPORTED;
			}
		}
	}
	
	protected JpaMetaModelPopulationSetting determineJpaMetaModelPopulationSetting(Configuration cfg) {
		String setting = ConfigurationHelper.getString(
				AvailableSettings.JPA_METAMODEL_POPULATION,
				cfg.getProperties(),
				null
		);
		if ( setting == null ) {
			setting = ConfigurationHelper.getString( AvailableSettings.JPA_METAMODEL_GENERATION, cfg.getProperties(), null );
			if ( setting != null ) {
				log.infof( 
						"Encountered deprecated setting [%s], use [%s] instead",
						AvailableSettings.JPA_METAMODEL_GENERATION,
						AvailableSettings.JPA_METAMODEL_POPULATION
				);
			}
		}
		return JpaMetaModelPopulationSetting.parse( setting );
	}

	private static void addAll(HashMap<String, Object> destination, Map<?,?> source) {
		for ( Map.Entry entry : source.entrySet() ) {
			if ( String.class.isInstance( entry.getKey() ) ) {
				destination.put( (String) entry.getKey(), entry.getValue() );
			}
		}
	}

	public EntityManager createEntityManager() {
		return createEntityManager( null );
	}

	public EntityManager createEntityManager(Map map) {
		return createEntityManager( SynchronizationType.SYNCHRONIZED, map );
	}

	@Override
	public EntityManager createEntityManager(SynchronizationType synchronizationType, Map map) {
		//TODO support discardOnClose, persistencecontexttype?, interceptor,
		return new EntityManagerImpl(
				this,
				PersistenceContextType.EXTENDED,
				synchronizationType,
				transactionType,
				discardOnClose,
				sessionInterceptorClass,
				map
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
		EntityManagerFactoryRegistry.INSTANCE.removeEntityManagerFactory(entityManagerFactoryName, this);
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

	@Override
	public void addNamedQuery(String name, Query query) {
		if ( ! isOpen() ) {
			throw new IllegalStateException( "EntityManagerFactory is closed" );
		}

		if ( ! HibernateQuery.class.isInstance( query ) ) {
			throw new PersistenceException( "Cannot use query non-Hibernate EntityManager query as basis for named query" );
		}

		// create and register the proper NamedQueryDefinition...
		final org.hibernate.Query hibernateQuery = ( (HibernateQuery) query ).getHibernateQuery();
		if ( org.hibernate.SQLQuery.class.isInstance( hibernateQuery ) ) {
			final NamedSQLQueryDefinition namedQueryDefinition = extractSqlQueryDefinition( ( org.hibernate.SQLQuery ) hibernateQuery, name );
			sessionFactory.registerNamedSQLQueryDefinition( name, namedQueryDefinition );
		}
		else {
			final NamedQueryDefinition namedQueryDefinition = extractHqlQueryDefinition( hibernateQuery, name );
			sessionFactory.registerNamedQueryDefinition( name, namedQueryDefinition );
		}
	}

	private NamedSQLQueryDefinition extractSqlQueryDefinition(org.hibernate.SQLQuery nativeSqlQuery, String name) {
		final NamedSQLQueryDefinitionBuilder builder = new NamedSQLQueryDefinitionBuilder( name );
		fillInNamedQueryBuilder( builder, nativeSqlQuery );
		builder.setCallable( nativeSqlQuery.isCallable() )
				.setQuerySpaces( nativeSqlQuery.getSynchronizedQuerySpaces() )
				.setQueryReturns( nativeSqlQuery.getQueryReturns() );
		return builder.createNamedQueryDefinition();
	}

	private NamedQueryDefinition extractHqlQueryDefinition(org.hibernate.Query hqlQuery, String name) {
		final NamedQueryDefinitionBuilder builder = new NamedQueryDefinitionBuilder( name );
		fillInNamedQueryBuilder( builder, hqlQuery );
		// LockOptions only valid for HQL/JPQL queries...
		builder.setLockOptions( hqlQuery.getLockOptions().makeCopy() );
		return builder.createNamedQueryDefinition();
	}

	private void fillInNamedQueryBuilder(NamedQueryDefinitionBuilder builder, org.hibernate.Query query) {
		builder.setQuery( query.getQueryString() )
				.setComment( query.getComment() )
				.setCacheable( query.isCacheable() )
				.setCacheRegion( query.getCacheRegion() )
				.setCacheMode( query.getCacheMode() )
				.setTimeout( query.getTimeout() )
				.setFetchSize( query.getFetchSize() )
				.setFirstResult( query.getFirstResult() )
				.setMaxResults( query.getMaxResults() )
				.setReadOnly( query.isReadOnly() )
				.setFlushMode( query.getFlushMode() );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> cls) {
		if ( SessionFactory.class.isAssignableFrom( cls ) ) {
			return ( T ) sessionFactory;
		}
		if ( SessionFactoryImplementor.class.isAssignableFrom( cls ) ) {
			return ( T ) sessionFactory;
		}
		if ( EntityManager.class.isAssignableFrom( cls ) ) {
			return ( T ) this;
		}
		throw new PersistenceException( "Hibernate cannot unwrap EntityManagerFactory as " + cls.getName() );
	}

	public boolean isOpen() {
		return ! sessionFactory.isClosed();
	}

	public SessionFactoryImpl getSessionFactory() {
		return sessionFactory;
	}

	public String getEntityManagerFactoryName() {
		return entityManagerFactoryName;
	}

	private static class JPACache implements Cache {
		private SessionFactoryImplementor sessionFactory;

		private JPACache(SessionFactoryImplementor sessionFactory) {
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

		@Override
		@SuppressWarnings("unchecked")
		public <T> T unwrap(Class<T> cls) {
			if ( RegionFactory.class.isAssignableFrom( cls ) ) {
				return (T) sessionFactory.getSettings().getRegionFactory();
			}
			if ( org.hibernate.Cache.class.isAssignableFrom( cls ) ) {
				return (T) sessionFactory.getCache();
			}
			throw new PersistenceException( "Hibernate cannot unwrap Cache as " + cls.getName() );
		}
	}

	private static EntityManagerFactory getNamedEntityManagerFactory(String entityManagerFactoryName) throws InvalidObjectException {
		Object result = EntityManagerFactoryRegistry.INSTANCE.getNamedEntityManagerFactory(entityManagerFactoryName);

		if ( result == null ) {
			throw new InvalidObjectException( "could not resolve entity manager factory during entity manager deserialization [name=" + entityManagerFactoryName + "]" );
		}

		return (EntityManagerFactory)result;
	}

	private void writeObject(ObjectOutputStream oos) throws IOException {
		if (entityManagerFactoryName == null) {
			throw new InvalidObjectException( "could not serialize entity manager factory with null entityManagerFactoryName" );
		}
		oos.defaultWriteObject();
	}

	/**
	 * After deserialization of an EntityManagerFactory, this is invoked to return the EntityManagerFactory instance
	 * that is already in use rather than a cloned copy of the object.
	 *
	 * @return
	 * @throws InvalidObjectException
	 */
	private Object readResolve() throws InvalidObjectException {
		return getNamedEntityManagerFactory(entityManagerFactoryName);
	}



	private static class HibernatePersistenceUnitUtil implements PersistenceUnitUtil, Serializable {
		private final HibernateEntityManagerFactory emf;
		private transient PersistenceUtilHelper.MetadataCache cache;

		private HibernatePersistenceUnitUtil(EntityManagerFactoryImpl emf) {
			this.emf = emf;
			this.cache = emf.cache;
		}

		public boolean isLoaded(Object entity, String attributeName) {
			// added log message to help with HHH-7454, if state == LoadState,NOT_LOADED, returning true or false is not accurate.
			log.debug("PersistenceUnitUtil#isLoaded is not always accurate; consider using EntityManager#contains instead");
			LoadState state = PersistenceUtilHelper.isLoadedWithoutReference( entity, attributeName, cache );
			if (state == LoadState.LOADED) {
				return true;
			}
			else if (state == LoadState.NOT_LOADED ) {
				return false;
			}
			else {
				return PersistenceUtilHelper.isLoadedWithReference( entity, attributeName, cache ) != LoadState.NOT_LOADED;
			}
		}

		public boolean isLoaded(Object entity) {
			// added log message to help with HHH-7454, if state == LoadState,NOT_LOADED, returning true or false is not accurate.
			log.debug("PersistenceUnitUtil#isLoaded is not always accurate; consider using EntityManager#contains instead");
			return PersistenceUtilHelper.isLoaded( entity ) != LoadState.NOT_LOADED;
		}

		public Object getIdentifier(Object entity) {
			final Class entityClass = Hibernate.getClass( entity );
			final ClassMetadata classMetadata = emf.getSessionFactory().getClassMetadata( entityClass );
			if (classMetadata == null) {
				throw new IllegalArgumentException( entityClass + " is not an entity" );
			}
			//TODO does that work for @IdClass?
			return classMetadata.getIdentifier( entity );
		}
	}
}
