/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.internal;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.persistence.Cache;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.NamedSubgraph;
import javax.persistence.PersistenceContextType;
import javax.persistence.PersistenceException;
import javax.persistence.PersistenceUnitUtil;
import javax.persistence.Query;
import javax.persistence.SynchronizationType;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.spi.LoadState;
import javax.persistence.spi.PersistenceUnitTransactionType;

import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cfg.annotations.NamedEntityGraphDefinition;
import org.hibernate.ejb.HibernateEntityManagerFactory;
import org.hibernate.engine.spi.NamedQueryDefinition;
import org.hibernate.engine.spi.NamedQueryDefinitionBuilder;
import org.hibernate.engine.spi.NamedSQLQueryDefinition;
import org.hibernate.engine.spi.NamedSQLQueryDefinitionBuilder;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.UUIDGenerator;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.jpa.AvailableSettings;
import org.hibernate.jpa.HibernateQuery;
import org.hibernate.jpa.boot.internal.SettingsImpl;
import org.hibernate.jpa.criteria.CriteriaBuilderImpl;
import org.hibernate.jpa.graph.internal.AbstractGraphNode;
import org.hibernate.jpa.graph.internal.AttributeNodeImpl;
import org.hibernate.jpa.graph.internal.EntityGraphImpl;
import org.hibernate.jpa.graph.internal.SubgraphImpl;
import org.hibernate.jpa.internal.metamodel.EntityTypeImpl;
import org.hibernate.jpa.internal.metamodel.MetamodelImpl;
import org.hibernate.jpa.internal.util.PersistenceUtilHelper;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.procedure.ProcedureCall;

import org.jboss.logging.Logger;

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

	private final transient SessionFactoryImplementor sessionFactory;
	private final transient PersistenceUnitTransactionType transactionType;
	private final transient boolean discardOnClose;
	private final transient Class sessionInterceptorClass;
	private final transient CriteriaBuilderImpl criteriaBuilder;
	private final transient MetamodelImpl metamodel;
	private final transient HibernatePersistenceUnitUtil util;
	private final transient Map<String,Object> properties;
	private final String entityManagerFactoryName;

	private final transient PersistenceUtilHelper.MetadataCache cache = new PersistenceUtilHelper.MetadataCache();
	private final transient Map<String,EntityGraphImpl> entityGraphs = new ConcurrentHashMap<String, EntityGraphImpl>();

	public EntityManagerFactoryImpl(
			String persistenceUnitName,
			SessionFactoryImplementor sessionFactory,
			MetadataImplementor metadata,
			SettingsImpl settings,
			Map<?, ?> configurationValues) {
		this.sessionFactory = sessionFactory;
		this.transactionType = settings.getTransactionType();
		this.discardOnClose = settings.isReleaseResourcesOnCloseEnabled();
		this.sessionInterceptorClass = settings.getSessionInterceptorClass();

		final JpaMetaModelPopulationSetting jpaMetaModelPopulationSetting = determineJpaMetaModelPopulationSetting( configurationValues );
		if ( JpaMetaModelPopulationSetting.DISABLED == jpaMetaModelPopulationSetting ) {
			this.metamodel = null;
		}
		else {
			this.metamodel = MetamodelImpl.buildMetamodel(
					metadata.getEntityBindings().iterator(),
					metadata.getMappedSuperclassMappingsCopy(),
					sessionFactory,
					JpaMetaModelPopulationSetting.IGNORE_UNSUPPORTED == jpaMetaModelPopulationSetting
			);
		}
		this.criteriaBuilder = new CriteriaBuilderImpl( this );
		this.util = new HibernatePersistenceUnitUtil( this );

		HashMap<String,Object> props = new HashMap<String, Object>();
		addAll( props, sessionFactory.getProperties() );
		addAll( props, configurationValues );
		maskOutSensitiveInformation( props );
		this.properties = Collections.unmodifiableMap( props );
		String entityManagerFactoryName = (String)this.properties.get( AvailableSettings.ENTITY_MANAGER_FACTORY_NAME);
		if (entityManagerFactoryName == null) {
			entityManagerFactoryName = persistenceUnitName;
		}
		if (entityManagerFactoryName == null) {
			entityManagerFactoryName = (String) UUID_GENERATOR.generate(null, null);
		}
		this.entityManagerFactoryName = entityManagerFactoryName;

		applyNamedEntityGraphs( metadata.getNamedEntityGraphs().values() );

		EntityManagerFactoryRegistry.INSTANCE.addEntityManagerFactory( entityManagerFactoryName, this );
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
	
	protected JpaMetaModelPopulationSetting determineJpaMetaModelPopulationSetting(Map configurationValues) {
		String setting = ConfigurationHelper.getString(
				AvailableSettings.JPA_METAMODEL_POPULATION,
				configurationValues,
				null
		);
		if ( setting == null ) {
			setting = ConfigurationHelper.getString( AvailableSettings.JPA_METAMODEL_GENERATION, configurationValues, null );
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

	private void maskOutSensitiveInformation(HashMap<String, Object> props) {
		maskOutIfSet( props, AvailableSettings.JDBC_PASSWORD );
		maskOutIfSet( props, org.hibernate.cfg.AvailableSettings.PASS );
	}

	private void maskOutIfSet(HashMap<String, Object> props, String setting) {
		if ( props.containsKey( setting ) ) {
			props.put( setting, "****" );
		}
	}

	@SuppressWarnings("unchecked")
	private void applyNamedEntityGraphs(Collection<NamedEntityGraphDefinition> namedEntityGraphs) {
		for ( NamedEntityGraphDefinition definition : namedEntityGraphs ) {
			log.debugf(
					"Applying named entity graph [name=%s, entity-name=%s, jpa-entity-name=%s",
					definition.getRegisteredName(),
					definition.getEntityName(),
					definition.getJpaEntityName()
			);
			final EntityType entityType = metamodel.getEntityTypeByName( definition.getEntityName() );
			if ( entityType == null ) {
				throw new IllegalArgumentException(
						"Attempted to register named entity graph [" + definition.getRegisteredName()
								+ "] for unknown entity ["+ definition.getEntityName() + "]"

				);
			}
			final EntityGraphImpl entityGraph = new EntityGraphImpl(
					definition.getRegisteredName(),
					entityType,
					this
			);

			final NamedEntityGraph namedEntityGraph = definition.getAnnotation();

			if ( namedEntityGraph.includeAllAttributes() ) {
				for ( Object attributeObject : entityType.getAttributes() ) {
					entityGraph.addAttributeNodes( (Attribute) attributeObject );
				}
			}

			if ( namedEntityGraph.attributeNodes() != null ) {
				applyNamedAttributeNodes( namedEntityGraph.attributeNodes(), namedEntityGraph, entityGraph );
			}

			entityGraphs.put( definition.getRegisteredName(), entityGraph );
		}
	}

	private void applyNamedAttributeNodes(
			NamedAttributeNode[] namedAttributeNodes,
			NamedEntityGraph namedEntityGraph,
			AbstractGraphNode graphNode) {
		for ( NamedAttributeNode namedAttributeNode : namedAttributeNodes ) {
			final String value = namedAttributeNode.value();
			AttributeNodeImpl attributeNode = graphNode.addAttribute( value );
			if ( StringHelper.isNotEmpty( namedAttributeNode.subgraph() ) ) {
				final SubgraphImpl subgraph = attributeNode.makeSubgraph();
				applyNamedSubgraphs(
						namedEntityGraph,
						namedAttributeNode.subgraph(),
						subgraph
				);
			}
			if ( StringHelper.isNotEmpty( namedAttributeNode.keySubgraph() ) ) {
				final SubgraphImpl subgraph = attributeNode.makeKeySubgraph();

				applyNamedSubgraphs(
						namedEntityGraph,
						namedAttributeNode.keySubgraph(),
						subgraph
				);
			}
		}
	}

	private void applyNamedSubgraphs(NamedEntityGraph namedEntityGraph, String subgraphName, SubgraphImpl subgraph) {
		for ( NamedSubgraph namedSubgraph : namedEntityGraph.subgraphs() ) {
			if ( subgraphName.equals( namedSubgraph.name() ) ) {
				applyNamedAttributeNodes(
						namedSubgraph.attributeNodes(),
						namedEntityGraph,
						subgraph
				);
			}
		}
	}

	@Override
	public EntityManager createEntityManager() {
		return internalCreateEntityManager( SynchronizationType.SYNCHRONIZED, Collections.EMPTY_MAP );
	}

	@Override
	public EntityManager createEntityManager(SynchronizationType synchronizationType) {
		errorIfResourceLocalDueToExplicitSynchronizationType();
		return internalCreateEntityManager( synchronizationType, Collections.EMPTY_MAP );
	}

	private void errorIfResourceLocalDueToExplicitSynchronizationType() {
		if ( transactionType == PersistenceUnitTransactionType.RESOURCE_LOCAL ) {
			throw new IllegalStateException(
					"Illegal attempt to specify a SynchronizationType when building an EntityManager from a " +
							"EntityManagerFactory defined as RESOURCE_LOCAL "
			);
		}
	}

	@Override
	public EntityManager createEntityManager(Map map) {
		return internalCreateEntityManager( SynchronizationType.SYNCHRONIZED, map );
	}

	@Override
	public EntityManager createEntityManager(SynchronizationType synchronizationType, Map map) {
		errorIfResourceLocalDueToExplicitSynchronizationType();
		return internalCreateEntityManager( synchronizationType, map );
	}

	private EntityManager internalCreateEntityManager(SynchronizationType synchronizationType, Map map) {
		validateNotClosed();

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

	@Override
	public CriteriaBuilder getCriteriaBuilder() {
		validateNotClosed();
		return criteriaBuilder;
	}

	@Override
	public Metamodel getMetamodel() {
		validateNotClosed();
		return metamodel;
	}

	@Override
	public void close() {
		// The spec says so, that's why :(
		validateNotClosed();

		sessionFactory.close();
		EntityManagerFactoryRegistry.INSTANCE.removeEntityManagerFactory(entityManagerFactoryName, this);
	}

	@Override
	public Map<String, Object> getProperties() {
		validateNotClosed();
		return properties;
	}

	@Override
	public Cache getCache() {
		validateNotClosed();

		// TODO : cache the cache reference?
		return new JPACache( sessionFactory );
	}

	protected void validateNotClosed() {
		if ( ! isOpen() ) {
			throw new IllegalStateException( "EntityManagerFactory is closed" );
		}
	}

	@Override
	public PersistenceUnitUtil getPersistenceUnitUtil() {
		validateNotClosed();
		return util;
	}

	@Override
	public void addNamedQuery(String name, Query query) {
		validateNotClosed();

		// NOTE : we use Query#unwrap here (rather than direct type checking) to account for possibly wrapped
		// query implementations

		// first, handle StoredProcedureQuery
		try {
			final StoredProcedureQueryImpl unwrapped = query.unwrap( StoredProcedureQueryImpl.class );
			if ( unwrapped != null ) {
				addNamedStoredProcedureQuery( name, unwrapped );
				return;
			}
		}
		catch ( PersistenceException ignore ) {
			// this means 'query' is not a StoredProcedureQueryImpl
		}

		// then try as a native-SQL or JPQL query
		try {
			final HibernateQuery unwrapped = query.unwrap( HibernateQuery.class );
			if ( unwrapped != null ) {
				// create and register the proper NamedQueryDefinition...
				final org.hibernate.Query hibernateQuery = unwrapped.getHibernateQuery();
				if ( org.hibernate.SQLQuery.class.isInstance( hibernateQuery ) ) {
					sessionFactory.registerNamedSQLQueryDefinition(
							name,
							extractSqlQueryDefinition( (org.hibernate.SQLQuery) hibernateQuery, name )
					);
				}
				else {
					sessionFactory.registerNamedQueryDefinition( name, extractHqlQueryDefinition( hibernateQuery, name ) );
				}
				return;
			}
		}
		catch ( PersistenceException ignore ) {
			// this means 'query' is not a native-SQL or JPQL query
		}


		// if we get here, we are unsure how to properly unwrap the incoming query to extract the needed information
		throw new PersistenceException(
				String.format(
						"Unsure how to how to properly unwrap given Query [%s] as basis for named query",
						query
				)
		);
	}

	private void addNamedStoredProcedureQuery(String name, StoredProcedureQueryImpl query) {
		final ProcedureCall procedureCall = query.getHibernateProcedureCall();
		sessionFactory.getNamedQueryRepository().registerNamedProcedureCallMemento(
				name,
				procedureCall.extractMemento( query.getHints() )
		);
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
	public <T> T unwrap(Class<T> type) {

		if ( type.isAssignableFrom( SessionFactory.class ) ) {
			return type.cast( sessionFactory);
		}

		if ( type.isAssignableFrom( SessionFactoryImplementor.class ) ) {
			return type.cast( sessionFactory );
		}

		if ( type.isAssignableFrom( SessionFactoryImpl.class ) ) {
			return type.cast( sessionFactory );
		}

		if ( type.isAssignableFrom( HibernateEntityManagerFactory.class ) ) {
			return type.cast( this );
		}

		if ( type.isAssignableFrom( EntityManagerFactoryImpl.class ) ) {
			return type.cast( this );
		}

		throw new PersistenceException( "Hibernate cannot unwrap EntityManagerFactory as '" + type.getName() + "'" );
	}

	@Override
	public <T> void addNamedEntityGraph(String graphName, EntityGraph<T> entityGraph) {
		if ( ! EntityGraphImpl.class.isInstance( entityGraph ) ) {
			throw new IllegalArgumentException(
					"Unknown type of EntityGraph for making named : " + entityGraph.getClass().getName()
			);
		}
		final EntityGraphImpl<T> copy = ( (EntityGraphImpl<T>) entityGraph ).makeImmutableCopy( graphName );
		final EntityGraphImpl old = entityGraphs.put( graphName, copy );
		if ( old != null ) {
			log.debugf( "EntityGraph being replaced on EntityManagerFactory for name %s", graphName );
		}
	}

	public EntityGraphImpl findEntityGraphByName(String name) {
		return entityGraphs.get( name );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> List<EntityGraph<? super T>> findEntityGraphsByType(Class<T> entityClass) {
		final EntityType<T> entityType = getMetamodel().entity( entityClass );
		if ( entityType == null ) {
			throw new IllegalArgumentException( "Given class is not an entity : " + entityClass.getName() );
		}

		final List<EntityGraph<? super T>> results = new ArrayList<EntityGraph<? super T>>();
		for ( EntityGraphImpl entityGraph : this.entityGraphs.values() ) {
			if ( entityGraph.appliesTo( entityType ) ) {
				results.add( entityGraph );
			}
		}
		return results;
	}

	@Override
	public boolean isOpen() {
		return ! sessionFactory.isClosed();
	}

	@Override
	public SessionFactoryImplementor getSessionFactory() {
		return sessionFactory;
	}

	@Override
	public EntityType getEntityTypeByName(String entityName) {
		final EntityTypeImpl entityType = metamodel.getEntityTypeByName( entityName );
		if ( entityType == null ) {
			throw new IllegalArgumentException( "[" + entityName + "] did not refer to EntityType" );
		}
		return entityType;
	}

	@Override
	public String getEntityManagerFactoryName() {
		return entityManagerFactoryName;
	}

	private static class JPACache implements Cache {
		private SessionFactoryImplementor sessionFactory;

		private JPACache(SessionFactoryImplementor sessionFactory) {
			this.sessionFactory = sessionFactory;
		}

		@Override
		public boolean contains(Class entityClass, Object identifier) {
			return sessionFactory.getCache().containsEntity( entityClass, ( Serializable ) identifier );
		}

		@Override
		public void evict(Class entityClass, Object identifier) {
			sessionFactory.getCache().evictEntity( entityClass, ( Serializable ) identifier );
		}

		@Override
		public void evict(Class entityClass) {
			sessionFactory.getCache().evictEntityRegion( entityClass );
		}

		@Override
		public void evictAll() {
			// Evict only the "JPA cache", which is purely defined as the entity regions.
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

		@Override
		public boolean isLoaded(Object entity, String attributeName) {
			// added log message to help with HHH-7454, if state == LoadState,NOT_LOADED, returning true or false is not accurate.
			log.debug(
					"PersistenceUnitUtil#isLoaded is not always accurate; consider using EntityManager#contains instead"
			);
			LoadState state = PersistenceUtilHelper.isLoadedWithoutReference( entity, attributeName, cache );
			if ( state == LoadState.LOADED ) {
				return true;
			}
			else if ( state == LoadState.NOT_LOADED ) {
				return false;
			}
			else {
				return PersistenceUtilHelper.isLoadedWithReference(
						entity,
						attributeName,
						cache
				) != LoadState.NOT_LOADED;
			}
		}

		@Override
		public boolean isLoaded(Object entity) {
			// added log message to help with HHH-7454, if state == LoadState,NOT_LOADED, returning true or false is not accurate.
			log.debug(
					"PersistenceUnitUtil#isLoaded is not always accurate; consider using EntityManager#contains instead"
			);
			return PersistenceUtilHelper.isLoaded( entity ) != LoadState.NOT_LOADED;
		}

		@Override
		public Object getIdentifier(Object entity) {
			final Class entityClass = Hibernate.getClass( entity );
			final ClassMetadata classMetadata = emf.getSessionFactory().getClassMetadata( entityClass );
			if ( classMetadata == null ) {
				throw new IllegalArgumentException( entityClass + " is not an entity" );
			}
			//TODO does that work for @IdClass?
			return classMetadata.getIdentifier( entity );
		}
	}
}
