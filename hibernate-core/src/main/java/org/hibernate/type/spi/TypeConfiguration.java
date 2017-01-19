/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.spi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.persistence.EntityGraph;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.NamedSubgraph;
import javax.persistence.metamodel.Attribute;

import org.hibernate.EntityNameResolver;
import org.hibernate.HibernateException;
import org.hibernate.Incubating;
import org.hibernate.MappingException;
import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.UnknownEntityTypeException;
import org.hibernate.boot.cfgxml.spi.CfgXmlAccessService;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.cfg.annotations.NamedEntityGraphDefinition;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.graph.spi.EntityGraphImplementor;
import org.hibernate.id.factory.IdentifierGeneratorFactory;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.SessionFactoryRegistry;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.jpa.graph.internal.AbstractGraphNode;
import org.hibernate.jpa.graph.internal.AttributeNodeImpl;
import org.hibernate.jpa.graph.internal.EntityGraphImpl;
import org.hibernate.jpa.graph.internal.SubgraphImpl;
import org.hibernate.mapping.DenormalizedTable;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.hibernate.metamodel.internal.JpaMetaModelPopulationSetting;
import org.hibernate.persister.collection.spi.CollectionPersister;
import org.hibernate.persister.common.internal.DatabaseModelImpl;
import org.hibernate.persister.common.internal.PolymorphicEntityReferenceImpl;
import org.hibernate.persister.common.spi.DatabaseModel;
import org.hibernate.persister.common.spi.DerivedTable;
import org.hibernate.persister.common.spi.PhysicalTable;
import org.hibernate.persister.embedded.spi.EmbeddedPersister;
import org.hibernate.persister.entity.spi.EntityHierarchy;
import org.hibernate.persister.entity.spi.EntityPersister;
import org.hibernate.persister.entity.spi.EntityReference;
import org.hibernate.persister.spi.PersisterCreationContext;
import org.hibernate.persister.spi.PersisterFactory;
import org.hibernate.sqm.domain.SqmDomainMetamodel;
import org.hibernate.sqm.domain.SqmExpressableTypeBasic;
import org.hibernate.sqm.domain.SqmExpressableTypeEntity;
import org.hibernate.sqm.query.expression.BinaryArithmeticSqmExpression;
import org.hibernate.tuple.component.ComponentMetamodel;
import org.hibernate.tuple.entity.EntityTuplizer;
import org.hibernate.type.ArrayType;
import org.hibernate.type.BagType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.ComponentType;
import org.hibernate.type.CustomCollectionType;
import org.hibernate.type.EmbeddedComponentType;
import org.hibernate.type.ForeignKeyDirection;
import org.hibernate.type.IdentifierBagType;
import org.hibernate.type.ListType;
import org.hibernate.type.ManyToOneType;
import org.hibernate.type.MapType;
import org.hibernate.type.OneToOneType;
import org.hibernate.type.OrderedMapType;
import org.hibernate.type.OrderedSetType;
import org.hibernate.type.SetType;
import org.hibernate.type.SortedMapType;
import org.hibernate.type.SortedSetType;
import org.hibernate.type.SpecialOneToOneType;
import org.hibernate.type.descriptor.java.spi.EmbeddableJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptorRegistry;
import org.hibernate.type.descriptor.java.spi.MappedSuperclassJavaDescriptor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptorRegistry;
import org.hibernate.usertype.ParameterizedType;

import static org.hibernate.internal.CoreLogging.messageLogger;
import static org.hibernate.metamodel.internal.JpaMetaModelPopulationSetting.determineJpaMetaModelPopulationSetting;

/**
 * Defines a set of available Type instances as isolated from other configurations.  The
 * isolation is defined by each instance of a TypeConfiguration.
 * <p/>
 * Note that each Type is inherently "scoped" to a TypeConfiguration.  We only ever access
 * a Type through a TypeConfiguration - specifically the TypeConfiguration in effect for
 * the current persistence unit.
 * <p/>
 * Even though each Type instance is scoped to a TypeConfiguration, Types do not inherently
 * have access to that TypeConfiguration (mainly because Type is an extension contract - meaning
 * that Hibernate does not manage the full set of Types available in ever TypeConfiguration).
 * However Types will often want access to the TypeConfiguration, which can be achieved by the
 * Type simply implementing the {@link TypeConfigurationAware} interface.
 *
 * @author Steve Ebersole
 *
 * @since 6.0
 */
@Incubating
public class TypeConfiguration implements SqmDomainMetamodel, SessionFactoryObserver {
	private static final CoreMessageLogger log = messageLogger( Scope.class );

	private final Scope scope;
	private boolean initialized = false;

	private final JavaTypeDescriptorRegistry javaTypeDescriptorRegistry;
	private final SqlTypeDescriptorRegistry sqlTypeDescriptorRegistry;

	// todo : review with eye on using JavaTypeDescriptor instead of Class and Strings
	//		see

	private final BasicTypeRegistry basicTypeRegistry;

	private final DatabaseModelImpl databaseModel  = new DatabaseModelImpl();

	private final Map<String,EntityPersister<?>> entityPersisterMap = new ConcurrentHashMap<>();
	private final Set<EntityHierarchy> entityHierarchies = ConcurrentHashMap.newKeySet();
	private final Map<String,CollectionPersister<?,?,?>> collectionPersisterMap = new ConcurrentHashMap<>();
	private final Map<String,EmbeddedPersister<?>> embeddablePersisterMap = new ConcurrentHashMap<>();

	private final Map<String,String> importMap = new ConcurrentHashMap<>();
	private final Set<EntityNameResolver> entityNameResolvers = ConcurrentHashMap.newKeySet();

	private final Map<Class, PolymorphicEntityReferenceImpl> polymorphicEntityReferenceMap = new HashMap<>();
	private final Map<JavaTypeDescriptor,String> entityProxyInterfaceMap = new ConcurrentHashMap<>();
	private final Map<String,Set<String>> collectionRolesByEntityParticipant = new ConcurrentHashMap<>();
	private final Map<EmbeddableJavaDescriptor<?>,Set<String>> embeddedRolesByEmbeddableType = new ConcurrentHashMap<>();

	private final Map<String,EntityGraph> entityGraphMap = new ConcurrentHashMap<>();


	public TypeConfiguration() {
		this( new EolScopeMapping() );
	}

	public TypeConfiguration(Mapping mapping) {
		this.scope = new Scope( mapping );
		this.javaTypeDescriptorRegistry = new JavaTypeDescriptorRegistry( this );
		this.sqlTypeDescriptorRegistry = new SqlTypeDescriptorRegistry( this );
		this.basicTypeRegistry = new BasicTypeRegistry( this );

		this.initialized = true;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Access to registries

	public BasicTypeRegistry getBasicTypeRegistry() {
		return basicTypeRegistry;
	}

	public JavaTypeDescriptorRegistry getJavaTypeDescriptorRegistry() {
		if ( !initialized ) {
			throw new IllegalStateException( "TypeConfiguration initialization incomplete; not yet ready for access" );
		}
		return javaTypeDescriptorRegistry;
	}

	public SqlTypeDescriptorRegistry getSqlTypeDescriptorRegistry() {
		if ( !initialized ) {
			throw new IllegalStateException( "TypeConfiguration initialization incomplete; not yet ready for access" );
		}
		return sqlTypeDescriptorRegistry;
	}

	public Set<EntityNameResolver> getEntityNameResolvers() {
		return entityNameResolvers;
	}

	public Map<String, String> getImportMap() {
		return Collections.unmodifiableMap( importMap );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Named EntityGraph map access

	public Map<String, EntityGraph> getEntityGraphMap() {
		return Collections.unmodifiableMap( entityGraphMap );
	}

	public <T> void addNamedEntityGraph(String graphName, EntityGraph<T> entityGraph) {
		if ( entityGraph instanceof EntityGraphImplementor ) {
			entityGraph = ( (EntityGraphImplementor<T>) entityGraph ).makeImmutableCopy( graphName );
		}

		final EntityGraph old = entityGraphMap.put( graphName, entityGraph );
		if ( old != null ) {
			log.debugf( "EntityGraph being replaced on EntityManagerFactory for name %s", graphName );
		}
	}

	public <T> EntityGraph<T> findEntityGraphByName(String name) {
		return entityGraphMap.get( name );
	}

	public <T> List<EntityGraph<? super T>> findEntityGraphsByType(Class<T> entityClass) {
		final EntityPersister<? extends T> entityPersister = findEntityPersister( entityClass );
		if ( entityPersister == null ) {
			throw new IllegalArgumentException( "Given class is not an entity : " + entityClass.getName() );
		}

		final List<EntityGraph<? super T>> results = new ArrayList<>();

		for ( EntityGraph entityGraph : entityGraphMap.values() ) {
			if ( !EntityGraphImplementor.class.isInstance( entityGraph ) ) {
				continue;
			}

			final EntityGraphImplementor egi = (EntityGraphImplementor) entityGraph;
			if ( egi.appliesTo( entityPersister ) ) {
				results.add( egi );
			}
		}

		return results;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// EntityPersister access

	/**
	 * Retrieve all EntityPersisters, keyed by entity-name
	 */
	public Map<String,EntityPersister<?>> getEntityPersisterMap() {
		return Collections.unmodifiableMap( entityPersisterMap );
	}

	/**
	 * Retrieve all EntityPersisters
	 */
	public Collection<EntityPersister<?>> getEntityPersisters() {
		return entityPersisterMap.values();
	}

	/**
	 * Retrieve an EntityPersister by entity-name.  Returns {@code null} if not known.
	 */
	@SuppressWarnings("unchecked")
	public <T> EntityPersister<T> findEntityPersister(String entityName) {
		if ( importMap.containsKey( entityName ) ) {
			entityName = importMap.get( entityName );
		}
		return (EntityPersister<T>) entityPersisterMap.get( entityName );
	}

	/**
	 * Retrieve an EntityPersister by entity-name.  Throws exception if not known.
	 */
	public <T> EntityPersister<T> resolveEntityPersister(String entityName) throws UnknownEntityTypeException {
		final EntityPersister<T> resolved = findEntityPersister( entityName );
		if ( resolved != null ) {
			return resolved;
		}

		throw new UnknownEntityTypeException( "Could not resolve EntityPersister by entity name [" + entityName + "]" );
	}

	public <T> EntityPersister<? extends T> findEntityPersister(Class<T> javaType) {
		EntityPersister<? extends T> entityPersister = findEntityPersister( javaType.getName() );
		if ( entityPersister == null ) {
			JavaTypeDescriptor javaTypeDescriptor = getJavaTypeDescriptorRegistry().getDescriptor( javaType );
			if ( javaTypeDescriptor != null && javaTypeDescriptor instanceof MappedSuperclassJavaDescriptor ) {
				String mappedEntityName = entityProxyInterfaceMap.get( javaTypeDescriptor );
				if ( mappedEntityName != null ) {
					entityPersister = findEntityPersister( mappedEntityName );
				}
			}
		}

		return entityPersister;
	}

	public <T> EntityPersister<? extends T> resolveEntityPersister(Class<T> javaType) {
		final EntityPersister<? extends T> entityPersister = findEntityPersister( javaType );
		if ( entityPersister == null ) {
			throw new UnknownEntityTypeException( "Could not resolve EntityPersister by Java type [" + javaType.getName() + "]" );
		}
		return entityPersister;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// CollectionPersister access

	/**
	 * Retrieve all CollectionPersisters, keyed by role (path)
	 */
	public Map<String,CollectionPersister<?,?,?>> getCollectionPersisterMap() {
		return Collections.unmodifiableMap( collectionPersisterMap );
	}

	/**
	 * Retrieve all CollectionPersisters
	 */
	public Collection<CollectionPersister<?,?,?>> getCollectionPersisters() {
		return collectionPersisterMap.values();
	}

	/**
	 * Locate a CollectionPersister by role (path).  Returns {@code null} if not known
	 */
	@SuppressWarnings("unchecked")
	public <O,C,E> CollectionPersister<O,C,E> findCollectionPersister(String roleName) {
		return (CollectionPersister<O, C, E>) collectionPersisterMap.get( roleName );
	}

	public Set<String> getCollectionRolesByEntityParticipant(String entityName) {
		return collectionRolesByEntityParticipant.get( entityName );
	}



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// EmbeddablePersister access

	public Collection<EmbeddedPersister<?>> getEmbeddablePersisters() {
		return embeddablePersisterMap.values();
	}

	@SuppressWarnings("unchecked")
	public <T> EmbeddedPersister<T> findEmbeddablePersister(String roleName) {
		return (EmbeddedPersister<T>) embeddablePersisterMap.get( roleName );
	}

	public <T> EmbeddedPersister<T> findEmbeddablePersister(Class<T> javaType) {
		final JavaTypeDescriptor javaTypeDescriptor = getJavaTypeDescriptorRegistry().getDescriptor( javaType );
		if ( javaType == null || !EmbeddableJavaDescriptor.class.isInstance( javaTypeDescriptor ) ) {
			return null;
		}

		final Set<String> roles = embeddedRolesByEmbeddableType.get( javaTypeDescriptor );
		if ( roles == null || roles.isEmpty() || roles.size() > 1 ) {
			return null;
		}

		return findEmbeddablePersister( roles.iterator().next() );
	}



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SQM Query handling
	//		- everything within this "block" of methods relates to SQM
	// 			interpretation of queries and implements its calls accordingly

	@Override
	public SqmExpressableTypeEntity resolveEntityReference(String entityName) {
		if ( importMap.containsKey( entityName ) ) {
			entityName = importMap.get( entityName );
		}

		final EntityPersister namedPersister = findEntityPersister( entityName );
		if ( namedPersister != null ) {
			return namedPersister;
		}

		final Class requestedClass = resolveRequestedClass( entityName );
		if ( requestedClass != null ) {
			return resolveEntityReference( requestedClass );
		}

		throw new IllegalArgumentException( "Per JPA spec : no entity named " + entityName );
	}

	private Class resolveRequestedClass(String entityName) {
		try {
			return getSessionFactory().getServiceRegistry().getService( ClassLoaderService.class ).classForName( entityName );
		}
		catch (ClassLoadingException e) {
			return null;
		}
	}

	@Override
	public SqmExpressableTypeEntity resolveEntityReference(Class javaType) {
		// see if we know of this Class by name as an EntityPersister key
		if ( getEntityPersisterMap().containsKey( javaType.getName() ) ) {
			// and if so, return that persister
			return getEntityPersisterMap().get( javaType.getName() );
		}

		// next check entityProxyInterfaceMap
		final String proxyEntityName = entityProxyInterfaceMap.get( javaType );
		if ( proxyEntityName != null ) {
			return getEntityPersisterMap().get( proxyEntityName );
		}

		// otherwise, trye to handle it as a polymorphic reference
		if ( polymorphicEntityReferenceMap.containsKey( javaType ) ) {
			return polymorphicEntityReferenceMap.get( javaType );
		}

		final Set<EntityPersister<?>> implementors = getImplementors( javaType );
		if ( !implementors.isEmpty() ) {
			final PolymorphicEntityReferenceImpl entityReference = new PolymorphicEntityReferenceImpl(
					javaType.getName(),
					implementors
			);
			polymorphicEntityReferenceMap.put( javaType, entityReference );
			return entityReference;
		}

		throw new IllegalArgumentException( "Could not resolve entity reference : " + javaType.getName() );
	}

	@SuppressWarnings("unchecked")
	public Set<EntityPersister<?>> getImplementors(Class javaType) {
		// if the javaType refers directly to an EntityPersister by Class name, return just it.
		final EntityPersister<?> exactMatch = getEntityPersisterMap().get( javaType.getName() );
		if ( exactMatch != null ) {
			return Collections.singleton( exactMatch );
		}

		final HashSet<EntityPersister<?>> matchingPersisters = new HashSet<>();

		for ( EntityPersister entityPersister : getEntityPersisterMap().values() ) {
			if ( entityPersister.getJavaType() == null ) {
				continue;
			}

			// todo : explicit/implicit polymorphism...
			// todo : handle "duplicates" within a hierarchy
			// todo : in fact we may want to cycle through persisters via entityHierarchies and walking the subclass graph rather than walking each persister linearly (in random order)

			if ( javaType.isAssignableFrom( entityPersister.getJavaType() ) ) {
				matchingPersisters.add( entityPersister );
			}
		}

		return matchingPersisters;
	}

	@Override
	public SqmExpressableTypeBasic resolveBasicType(Class javaType) {
		return null;
	}
//
//	@Override
//	public <T> SqmExpressableTypeBasic<T> resolveBasicType(Class<T> javaType) {
//		return getBasicTypeRegistry().getBasicType( javaType );
//	}

	@Override
	public SqmExpressableTypeBasic resolveArithmeticType(
			SqmExpressableTypeBasic firstType,
			SqmExpressableTypeBasic secondType,
			BinaryArithmeticSqmExpression.Operation operation) {
		return null;
	}

	@Override
	public SqmExpressableTypeBasic resolveSumFunctionType(SqmExpressableTypeBasic argumentType) {
		return null;
	}

	public BasicType resolveCastTargetType(String name) {
		throw new NotYetImplementedException(  );
	}



















	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Scoping


	/**
	 * Get access to the generic Mapping contract.  This is implemented for both the
	 * boot-time model (Metamodel) and the run-time model (SessionFactory).
	 *
	 * @return The mapping object.  Should almost never return {@code null}.  There is a minor
	 * chance this method would get a {@code null}, but that would be an unsupported use-case.
	 */
	public Mapping getMapping() {
		return scope.getMapping();
	}

	/**
	 * Attempt to resolve the {@link #getMapping()} reference as a SessionFactory (the runtime model).
	 * This will throw an exception if the SessionFactory is not yet bound here.
	 *
	 * @return The SessionFactory
	 *
	 * @throws IllegalStateException if the Mapping reference is not a SessionFactory or the SessionFactory
	 * cannot be resolved; generally either of these cases would mean that the SessionFactory was not yet
	 * bound to this scope object
	 */
	public SessionFactoryImplementor getSessionFactory() {
		return scope.getSessionFactory();
	}

	public MetadataBuildingContext getMetadataBuildingContext() {
		return scope.getMetadataBuildingContext();
	}

	public void scope(MetadataBuildingContext metadataBuildingContext) {
		log.debugf( "Scoping TypeConfiguration [%s] to MetadataBuildingContext [%s]", this, metadataBuildingContext );
		scope.setMetadataBuildingContext( metadataBuildingContext );

		for ( Map.Entry<String, String> importEntry : metadataBuildingContext.getMetadataCollector().getImports().entrySet() ) {
			if ( importMap.containsKey( importEntry.getKey() ) ) {
				continue;
			}

			importMap.put( importEntry.getKey(), importEntry.getValue() );
		}
	}

	public void scope(SessionFactoryImplementor factory) {
		log.debugf( "Scoping TypeConfiguration [%s] to SessionFactory [%s]", this, factory );

		final JpaMetaModelPopulationSetting jpaMetaModelPopulationSetting = determineJpaMetaModelPopulationSetting(
				factory.getProperties()
		);
		final MetadataImplementor mappingMetadata = getMetadataBuildingContext().getMetadataCollector();

		scope.setSessionFactory( factory );
		factory.addObserver( this );

		populateDatabaseModel( mappingMetadata );


		final PersisterFactory persisterFactory = factory.getServiceRegistry().getService( PersisterFactory.class );

		final PersisterCreationContext persisterCreationContext = new PersisterCreationContext() {
			@Override
			public SessionFactoryImplementor getSessionFactory() {
				return factory;
			}

			@Override
			public MetadataImplementor getMetadata() {
				return mappingMetadata;
			}

			@Override
			public DatabaseModel getDatabaseModel() {
				return databaseModel;
			}

			@Override
			public TypeConfiguration getTypeConfiguration() {
				return TypeConfiguration.this;
			}

			@Override
			public PersisterFactory getPersisterFactory() {
				return persisterFactory;
			}

			@Override
			public void registerEntityPersister(EntityPersister entityPersister) {
				TypeConfiguration.this.register( entityPersister );
			}

			@Override
			public void registerCollectionPersister(CollectionPersister collectionPersister) {
				TypeConfiguration.this.register( collectionPersister );
			}

			@Override
			public void registerEmbeddablePersister(EmbeddedPersister embeddablePersister) {
				TypeConfiguration.this.register( embeddablePersister );
			}
		};

		for ( final PersistentClass model : mappingMetadata.getEntityBindings() ) {
			final EntityRegionAccessStrategy accessStrategy = getSessionFactory().getCache().determineEntityRegionAccessStrategy(
					model
			);

			final NaturalIdRegionAccessStrategy naturalIdAccessStrategy = getSessionFactory().getCache().determineNaturalIdRegionAccessStrategy(
					model
			);

			persisterFactory.createEntityPersister(
					model,
					accessStrategy,
					naturalIdAccessStrategy,
					persisterCreationContext
			);
		}

		persisterFactory.finishUp( persisterCreationContext );

		// make sure we got a CollectionPersister for every mapping collection binding
		for ( final org.hibernate.mapping.Collection model : mappingMetadata.getCollectionBindings() ) {
			if ( findCollectionPersister( model.getRole() ) == null ) {
				throw new HibernateException( "Collection role not properly materialized to CollectionPersister : " + model.getRole() );
			}
		}

		applyNamedEntityGraphs( mappingMetadata.getNamedEntityGraphs().values() );
	}

	@SuppressWarnings("unchecked")
	private void applyNamedEntityGraphs(java.util.Collection<NamedEntityGraphDefinition> namedEntityGraphs) {
		for ( NamedEntityGraphDefinition definition : namedEntityGraphs ) {
			log.debugf(
					"Applying named entity graph [name=%s, entity-name=%s, jpa-entity-name=%s",
					definition.getRegisteredName(),
					definition.getEntityName(),
					definition.getJpaEntityName()
			);
			final javax.persistence.metamodel.EntityType entityType = findEntityPersister( definition.getEntityName() );
			if ( entityType == null ) {
				throw new IllegalArgumentException(
						"Attempted to register named entity graph [" + definition.getRegisteredName()
								+ "] for unknown entity ["+ definition.getEntityName() + "]"

				);
			}
			final EntityGraphImpl entityGraph = new EntityGraphImpl(
					definition.getRegisteredName(),
					entityType,
					this.getSessionFactory()
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

			entityGraphMap.put( definition.getRegisteredName(), entityGraph );
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

	private void populateDatabaseModel(MetadataImplementor mappingMetadata) {
		// todo : ? - apply PhysicalNamingStrategy here, rather than as we create the "mapping model"?

		// todo : we need DatabaseModel to incorporate catalogs/schemas in some fashion
		//		either like org.hibernate.boot.model.relational.Database does
		//		or via catalogs/schemas-specific names
		for ( Namespace namespace : mappingMetadata.getDatabase().getNamespaces() ) {
			for ( Table mappingTable : namespace.getTables() ) {
				// todo : incorporate mapping Table's isAbstract indicator
				final org.hibernate.persister.common.spi.Table table;
				if ( mappingTable instanceof DenormalizedTable ) {
					// this is akin to a UnionSubclassTable
					throw new NotYetImplementedException( "DenormalizedTable support not yet implemented" );
				}
				else if ( mappingTable.getSubselect() != null ) {
					table = new DerivedTable( mappingTable.getSubselect() );
				}
				else {
					final JdbcEnvironment jdbcEnvironment = getSessionFactory().getJdbcServices().getJdbcEnvironment();
					final String qualifiedTableName = jdbcEnvironment.getQualifiedObjectNameFormatter().format(
							mappingTable.getQualifiedTableName(),
							jdbcEnvironment.getDialect()
					);
					table = new PhysicalTable( qualifiedTableName );
				}

				databaseModel.registerTable( table );
			}

		}
	}

	@Override
	public void sessionFactoryClosed(SessionFactory factory) {
		log.tracef( "Handling #sessionFactoryClosed from [%s] for TypeConfiguration" );
		scope.unsetSessionFactory( factory );

		// todo : come back and implement this...
		//		release Database, persister Maps, etc... things that are only
		// 		valid while the TypeConfiguration is scoped to SessionFactory
		throw new NotYetImplementedException(  );
	}

	/**
	 * Encapsulation of lifecycle concerns for a TypeConfiguration, mainly in regards to
	 * eventually being associated with a SessionFactory.  Goes through the following stages:<ol>
	 *     <li>
	 *         TypeConfiguration initialization - during this phase {@link #getMapping()} will
	 *         return a non-null, no-op impl.  Calls to {@link #getMetadataBuildingContext()} will
	 *         simply return {@code null}, while calls to {@link #getSessionFactory()} will throw
	 *         an exception.
	 *     </li>
	 *     <li>
	 *         Metadata building - during this phase {@link #getMetadataBuildingContext()} will
	 *         return a non-null value and the {@link #getMapping()} return will be the
	 *         {@link MetadataBuildingContext#getMetadataCollector()} reference.  Calls to
	 *         {@link #getSessionFactory()} will throw an exception.
	 *     </li>
	 *     <li>
	 *         live SessionFactory - this is the only phase where calls to {@link #getSessionFactory()}
	 *         are allowed and {@link #getMapping()} returns the SessionFactory itself (since it
	 *         implements that Mapping contract (for now) too.  Calls to {@link #getMetadataBuildingContext()}
	 *         will simply return {@code null}.
	 *     </li>
	 * </ol>
	 */
	private static class Scope {
		private transient Mapping mapping;

		private transient MetadataBuildingContext metadataBuildingContext;

		private String sessionFactoryName;
		private String sessionFactoryUuid;

		Scope(Mapping mapping) {
			this.mapping = mapping;
		}

		public Mapping getMapping() {
			return mapping;
		}

		public MetadataBuildingContext getMetadataBuildingContext() {
			if ( metadataBuildingContext == null ) {
				throw new HibernateException( "TypeConfiguration is not currently scoped to MetadataBuildingContext" );
			}
			return metadataBuildingContext;
		}

		public void setMetadataBuildingContext(MetadataBuildingContext metadataBuildingContext) {
			this.metadataBuildingContext = metadataBuildingContext;
			this.mapping = metadataBuildingContext.getMetadataCollector();
		}

		public SessionFactoryImplementor getSessionFactory() {
			if ( mapping == null ) {
				if ( sessionFactoryName == null && sessionFactoryUuid == null ) {
					throw new HibernateException( "TypeConfiguration was not yet scoped to SessionFactory" );
				}
				mapping = (SessionFactoryImplementor) SessionFactoryRegistry.INSTANCE.findSessionFactory(
						sessionFactoryUuid,
						sessionFactoryName
				);
				if ( mapping == null ) {
					throw new HibernateException(
							"Could not find a SessionFactory [uuid=" + sessionFactoryUuid + ",name=" + sessionFactoryName + "]"
					);
				}
			}

			if ( !SessionFactoryImplementor.class.isInstance( mapping ) ) {
				throw new HibernateException( "TypeConfiguration was not yet scoped to SessionFactory" );
			}

			return (SessionFactoryImplementor) mapping;
		}

		/**
		 * Used by TypeFactory scoping.
		 *
		 * @param factory The SessionFactory that the TypeFactory is being bound to
		 */
		void setSessionFactory(SessionFactoryImplementor factory) {
			if ( this.mapping != null && mapping instanceof SessionFactoryImplementor ) {
				log.scopingTypesToSessionFactoryAfterAlreadyScoped( (SessionFactoryImplementor) mapping, factory );
			}
			else {
				metadataBuildingContext = null;

				sessionFactoryUuid = factory.getUuid();
				String sfName = factory.getSessionFactoryOptions().getSessionFactoryName();
				if ( sfName == null ) {
					final CfgXmlAccessService cfgXmlAccessService = factory.getServiceRegistry()
							.getService( CfgXmlAccessService.class );
					if ( cfgXmlAccessService.getAggregatedConfig() != null ) {
						sfName = cfgXmlAccessService.getAggregatedConfig().getSessionFactoryName();
					}
				}
				sessionFactoryName = sfName;
			}
			this.mapping = factory;
		}

		public void unsetSessionFactory(SessionFactory factory) {
			log.debugf( "Un-scoping TypeConfiguration [%s] from SessionFactory [%s]", this, factory );
			this.mapping = EolScopeMapping.INSTANCE;
		}
	}

	private static class EolScopeMapping implements Mapping {
		/**
		 * Singleton access
		 */
		public static final EolScopeMapping INSTANCE = new EolScopeMapping();

		@Override
		public IdentifierGeneratorFactory getIdentifierGeneratorFactory() {
			throw invalidAccess();
		}

		private RuntimeException invalidAccess() {
			return new IllegalStateException( "Access to this TypeConfiguration is no longer valid" );
		}

		@Override
		public Type getIdentifierType(String className) {
			throw invalidAccess();
		}

		@Override
		public String getIdentifierPropertyName(String className) {
			throw invalidAccess();
		}

		@Override
		public Type getReferencedPropertyType(String className, String propertyName) {
			throw invalidAccess();
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SF-based initialization

	public void register(EntityPersister entityPersister) {
		entityPersisterMap.put( entityPersister.getEntityName(), entityPersister );
		entityHierarchies.add( entityPersister.getHierarchy() );

		if ( entityPersister.getConcreteProxyClass() != null
				&& entityPersister.getConcreteProxyClass().isInterface()
				&& !Map.class.isAssignableFrom( entityPersister.getConcreteProxyClass() )
				&& entityPersister.getMappedClass() != entityPersister.getConcreteProxyClass() ) {
			// IMPL NOTE : we exclude Map based proxy interfaces here because that should
			//		indicate MAP entity mode.0

			if ( entityPersister.getMappedClass().equals( entityPersister.getConcreteProxyClass() ) ) {
				// this part handles an odd case in the Hibernate test suite where we map an interface
				// as the class and the proxy.  I cannot think of a real life use case for that
				// specific test, but..
				log.debugf(
						"Entity [%s] mapped same interface [%s] as class and proxy",
						entityPersister.getEntityName(),
						entityPersister.getMappedClass()
				);
			}
			else {
				final JavaTypeDescriptor proxyInterfaceJavaDescriptor = getJavaTypeDescriptorRegistry().getDescriptor( entityPersister.getConcreteProxyClass() );
				final String old = entityProxyInterfaceMap.put( proxyInterfaceJavaDescriptor, entityPersister.getEntityName() );
				if ( old != null ) {
					throw new HibernateException(
							String.format(
									Locale.ENGLISH,
									"Multiple entities [%s, %s] named the same interface [%s] as their proxy which is not supported",
									old,
									entityPersister.getEntityName(),
									entityPersister.getConcreteProxyClass().getName()
							)
					);
				}
			}
		}

		registerEntityNameResolvers( entityPersister );
	}

	public void register(CollectionPersister collectionPersister) {
		collectionPersisterMap.put( collectionPersister.getRoleName(), collectionPersister );

		if ( collectionPersister.getIndexReference() != null
				&& collectionPersister.getIndexReference() instanceof EntityReference ) {
			final String entityName = ( (EntityReference) collectionPersister.getIndexReference() ).getEntityName();
			final Set<String> roles = collectionRolesByEntityParticipant.computeIfAbsent(
					entityName,
					k -> new HashSet<>()
			);
			roles.add( collectionPersister.getRoleName() );
		}

		if ( collectionPersister.getElementReference() instanceof EntityReference ) {
			final String entityName = ( (EntityReference) collectionPersister.getElementReference() ).getEntityName();
			final Set<String> roles = collectionRolesByEntityParticipant.computeIfAbsent(
					entityName,
					k -> new HashSet<>()
			);
			roles.add( collectionPersister.getRoleName() );
		}
	}

	public void register(EmbeddedPersister embeddablePersister) {
		embeddablePersisterMap.put( embeddablePersister.getRoleName(), embeddablePersister );

		final Set<String> roles = embeddedRolesByEmbeddableType.computeIfAbsent(
				embeddablePersister.getJavaTypeDescriptor(),
				k -> ConcurrentHashMap.newKeySet()
		);
		roles.add( embeddablePersister.getNavigableName() );
	}

	private void registerEntityNameResolvers(EntityPersister persister) {
		if ( persister.getEntityMetamodel() == null || persister.getEntityMetamodel().getTuplizer() == null ) {
			return;
		}
		registerEntityNameResolvers( persister.getEntityMetamodel().getTuplizer() );
	}

	private void registerEntityNameResolvers(EntityTuplizer tuplizer) {
		EntityNameResolver[] resolvers = tuplizer.getEntityNameResolvers();
		if ( resolvers == null ) {
			return;
		}
		Collections.addAll( entityNameResolvers, resolvers );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Legacy stuff
	//		todo : look at removing these...  they (most of them anyway) at least need new signatures
	//		e.g. A *-to-one Type (the EntityType) is actually specific to each specific usage (navigable)

	public EntityType manyToOne(Class clazz) {
		assert clazz != null;
		return manyToOne( clazz.getName() );
	}

	public EntityType manyToOne(String entityName) {
		throw new NotYetImplementedException(  );
	}

	public EntityType manyToOne(Class clazz, boolean lazy) {
		assert clazz != null;
		return manyToOne( clazz.getName(), lazy );
	}

	public EntityType manyToOne(String entityName, boolean lazy) {
		return manyToOne( entityName, true, null, lazy, true, false, false );
	}

	public EntityType manyToOne(
			String persistentClass,
			boolean referenceToPrimaryKey,
			String uniqueKeyPropertyName,
			boolean lazy,
			boolean unwrapProxy,
			boolean ignoreNotFound,
			boolean isLogicalOneToOne) {
		return new ManyToOneType(
				this,
				persistentClass,
				referenceToPrimaryKey,
				uniqueKeyPropertyName,
				lazy,
				unwrapProxy,
				ignoreNotFound,
				isLogicalOneToOne
		);
	}

	// one-to-one type builders ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public EntityType oneToOne(
			String persistentClass,
			ForeignKeyDirection foreignKeyType,
			boolean referenceToPrimaryKey,
			String uniqueKeyPropertyName,
			boolean lazy,
			boolean unwrapProxy,
			String entityName,
			String propertyName) {
		return new OneToOneType(
				this, persistentClass, foreignKeyType, referenceToPrimaryKey,
				uniqueKeyPropertyName, lazy, unwrapProxy, entityName, propertyName
		);
	}

	public EntityType specialOneToOne(
			String persistentClass,
			ForeignKeyDirection foreignKeyType,
			boolean referenceToPrimaryKey,
			String uniqueKeyPropertyName,
			boolean lazy,
			boolean unwrapProxy,
			String entityName,
			String propertyName) {
		return new SpecialOneToOneType(
				this, persistentClass, foreignKeyType, referenceToPrimaryKey,
				uniqueKeyPropertyName, lazy, unwrapProxy, entityName, propertyName
		);
	}

	public Type heuristicType(String typename) {
		throw new NotYetImplementedException(  );
	}

	// collection type builders ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public CollectionType array(String role, String propertyRef, Class elementClass) {
		return new ArrayType( this, role, propertyRef, elementClass );
	}

	public CollectionType list(String role, String propertyRef) {
		return new ListType( this, role, propertyRef );
	}

	public CollectionType bag(String role, String propertyRef) {
		return new BagType( this, role, propertyRef );
	}

	public CollectionType idbag(String role, String propertyRef) {
		return new IdentifierBagType( this, role, propertyRef );
	}

	public CollectionType map(String role, String propertyRef) {
		return new MapType( this, role, propertyRef );
	}

	public CollectionType orderedMap(String role, String propertyRef) {
		return new OrderedMapType( this, role, propertyRef );
	}

	public CollectionType sortedMap(String role, String propertyRef, Comparator comparator) {
		return new SortedMapType( this, role, propertyRef, comparator );
	}

	public CollectionType set(String role, String propertyRef) {
		return new SetType( this, role, propertyRef );
	}

	public CollectionType orderedSet(String role, String propertyRef) {
		return new OrderedSetType( this, role, propertyRef );
	}

	public CollectionType sortedSet(String role, String propertyRef, Comparator comparator) {
		return new SortedSetType( this, role, propertyRef, comparator );
	}

	// component type builders ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public EmbeddedComponentType embeddedComponent(ComponentMetamodel metamodel) {
		return new EmbeddedComponentType( this, metamodel );
	}

	public ComponentType component(ComponentMetamodel metamodel) {
		return new ComponentType( this, metamodel );
	}


	public CollectionType customCollection(
			String typeName,
			Properties typeParameters,
			String role,
			String propertyRef) {
		Class typeClass;
		try {
			typeClass = ReflectHelper.classForName( typeName );
		}
		catch (ClassNotFoundException cnfe) {
			throw new MappingException( "user collection type class not found: " + typeName, cnfe );
		}
		CustomCollectionType result = new CustomCollectionType( this, typeClass, role, propertyRef );
		if ( typeParameters != null ) {
			injectParameters( result.getUserType(), typeParameters );
		}
		return result;
	}

	private final static Properties EMPTY_PROPERTIES = new Properties();

	public static void injectParameters(Object type, Properties parameters) {
		if ( ParameterizedType.class.isInstance( type ) ) {
			if ( parameters == null ) {
				( (ParameterizedType) type ).setParameterValues( EMPTY_PROPERTIES );
			}
			else {
				( (ParameterizedType) type ).setParameterValues( parameters );
			}
		}
		else if ( parameters != null && !parameters.isEmpty() ) {
			throw new MappingException( "type is not parameterized: " + type.getClass().getName() );
		}
	}

}