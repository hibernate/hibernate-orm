/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.internal;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.persistence.EntityGraph;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.NamedSubgraph;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.EmbeddableType;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.MappedSuperclassType;

import org.hibernate.EntityNameResolver;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.UnknownEntityTypeException;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.cfg.annotations.NamedEntityGraphDefinition;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.graph.spi.EntityGraphImplementor;
import org.hibernate.internal.EntityManagerMessageLogger;
import org.hibernate.internal.HEMLogging;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.jpa.graph.internal.AbstractGraphNode;
import org.hibernate.jpa.graph.internal.AttributeNodeImpl;
import org.hibernate.jpa.graph.internal.EntityGraphImpl;
import org.hibernate.jpa.graph.internal.SubgraphImpl;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.DenormalizedTable;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.metamodel.spi.SqmDomainMetamodelImplementor;
import org.hibernate.persister.collection.spi.CollectionPersister;
import org.hibernate.persister.common.internal.DatabaseModelImpl;
import org.hibernate.persister.common.internal.PolymorphicEntityReferenceImpl;
import org.hibernate.persister.common.internal.SingularAttributeEmbedded;
import org.hibernate.persister.common.spi.AttributeContainer;
import org.hibernate.persister.common.spi.DatabaseModel;
import org.hibernate.persister.common.spi.DerivedTable;
import org.hibernate.persister.common.spi.OrmTypeExporter;
import org.hibernate.persister.common.spi.PhysicalTable;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.persister.entity.spi.EntityPersister;
import org.hibernate.persister.spi.PersisterCreationContext;
import org.hibernate.persister.spi.PersisterFactory;
import org.hibernate.sqm.domain.AttributeReference;
import org.hibernate.sqm.domain.BasicType;
import org.hibernate.sqm.domain.DomainReference;
import org.hibernate.sqm.domain.EntityReference;
import org.hibernate.sqm.domain.NoSuchAttributeException;
import org.hibernate.sqm.query.expression.BinaryArithmeticSqmExpression;
import org.hibernate.tuple.entity.EntityTuplizer;
import org.hibernate.type.CollectionType;
import org.hibernate.type.spi.CompositeType;
import org.hibernate.type.spi.Type;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.type.spi.basic.BasicTypeHelper;
import org.hibernate.type.spi.basic.RegistryKey;
import org.hibernate.type.spi.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.spi.descriptor.sql.SqlTypeDescriptor;

/**
 * Hibernate implementation of the JPA {@link javax.persistence.metamodel.Metamodel} contract.
 *
 * @author Steve Ebersole
 * @author Emmanuel Bernard
 */
public class MetamodelImpl implements MetamodelImplementor, Serializable {
	// todo : Integrate EntityManagerLogger into CoreMessageLogger
	private static final EntityManagerMessageLogger log = HEMLogging.messageLogger( MetamodelImpl.class );
	private static final Object ENTITY_NAME_RESOLVER_MAP_VALUE = new Object();

	private final SessionFactoryImplementor sessionFactory;
	private final TypeConfiguration typeConfiguration;

	private final DatabaseModelImpl databaseModel  = new DatabaseModelImpl();

	private final Map<String,String> imports = new ConcurrentHashMap<>();
	private final Map<String,EntityPersister> entityPersisterMap = new ConcurrentHashMap<>();
	private final Map<Class,String> entityProxyInterfaceMap = new ConcurrentHashMap<>();
	private final ConcurrentMap<EntityNameResolver,Object> entityNameResolvers = new ConcurrentHashMap<>();

	private final Map<String,CollectionPersister> collectionPersisterMap = new ConcurrentHashMap<>();
	private final Map<String,Set<String>> collectionRolesByEntityParticipant = new ConcurrentHashMap<>();

	private final Map<Class<?>, EntityTypeImpl<?>> jpaEntityTypeMap = new ConcurrentHashMap<>();
	private final Map<Class<?>, EmbeddableTypeImpl<?>> jpaEmbeddableTypeMap = new ConcurrentHashMap<>();
	private final Map<Class<?>, MappedSuperclassType<?>> jpaMappedSuperclassTypeMap = new ConcurrentHashMap<>();
	private final Map<String, EntityTypeImpl<?>> jpaEntityTypesByEntityName = new ConcurrentHashMap<>();

	private final Map<String,EntityGraph> entityGraphMap = new ConcurrentHashMap<>();
	private Map<String,PolymorphicEntityReferenceImpl> polymorphicEntityTypeDescriptorMap;

	/**
	 * Instantiate the MetamodelImpl.
	 * <p/>
	 * Note that building a fully-functional MetamodelImpl instance is a 2-step process.  The
	 * create instance returned here must still be initialized via call to {@link #initialize}
	 *
	 * @param typeConfiguration The TypeConfiguration to use for this Metamodel
	 */
	public MetamodelImpl(SessionFactoryImplementor sessionFactory, TypeConfiguration typeConfiguration) {
		this.sessionFactory = sessionFactory;
		this.typeConfiguration = typeConfiguration;
	}

	/**
	 * Prepare the MetamodelImpl for use, using the information from the Hibernate mapping model.
	 *
	 * @param mappingMetadata The Hibernate mapping model
	 * @param jpaMetaModelPopulationSetting Should the JPA Metamodel be built as well?
	 */
	public void initialize(
			MetadataImplementor mappingMetadata,
			JpaMetaModelPopulationSetting jpaMetaModelPopulationSetting) {

		populateDatabaseModel( mappingMetadata );

		this.imports.putAll( mappingMetadata.getImports() );

		final PersisterCreationContext persisterCreationContext = new PersisterCreationContext() {
			@Override
			public SessionFactoryImplementor getSessionFactory() {
				return MetamodelImpl.this.getSessionFactory();
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
			public void registerCollectionPersister(CollectionPersister collectionPersister) {
				if ( collectionPersisterMap.put( collectionPersister.getRole(), collectionPersister ) != null ) {
					throw new HibernateException( "CollectionPersister was already registered under that role : " + collectionPersister.getRole() );
				}
			}

			@Override
			public void registerEntityNameResolvers(EntityPersister entityPersister) {
				MetamodelImpl.registerEntityNameResolvers( entityPersister, entityNameResolvers );
			}
		};

		final PersisterFactory persisterFactory = getSessionFactory().getServiceRegistry().getService( PersisterFactory.class );

		for ( final PersistentClass model : mappingMetadata.getEntityBindings() ) {
			final EntityRegionAccessStrategy accessStrategy = getSessionFactory().getCache().determineEntityRegionAccessStrategy(
					model
			);

			final NaturalIdRegionAccessStrategy naturalIdAccessStrategy = getSessionFactory().getCache().determineNaturalIdRegionAccessStrategy(
					model
			);

			final EntityPersister cp = persisterFactory.createEntityPersister(
					model,
					accessStrategy,
					naturalIdAccessStrategy,
					persisterCreationContext
			);
			entityPersisterMap.put( model.getEntityName(), cp );

			if ( cp.getConcreteProxyClass() != null
					&& cp.getConcreteProxyClass().isInterface()
					&& !Map.class.isAssignableFrom( cp.getConcreteProxyClass() )
					&& cp.getMappedClass() != cp.getConcreteProxyClass() ) {
				// IMPL NOTE : we exclude Map based proxy interfaces here because that should
				//		indicate MAP entity mode.0

				if ( cp.getMappedClass().equals( cp.getConcreteProxyClass() ) ) {
					// this part handles an odd case in the Hibernate test suite where we map an interface
					// as the class and the proxy.  I cannot think of a real life use case for that
					// specific test, but..
					log.debugf( "Entity [%s] mapped same interface [%s] as class and proxy", cp.getEntityName(), cp.getMappedClass() );
				}
				else {
					final String old = entityProxyInterfaceMap.put( cp.getConcreteProxyClass(), cp.getEntityName() );
					if ( old != null ) {
						throw new HibernateException(
								String.format(
										Locale.ENGLISH,
										"Multiple entities [%s, %s] named the same interface [%s] as their proxy which is not supported",
										old,
										cp.getEntityName(),
										cp.getConcreteProxyClass().getName()
								)
						);
					}
				}
			}
		}

		persisterFactory.finishUp( persisterCreationContext );

		// make sure we got a CollectionPersister for every mapping collection binding
		for ( final Collection model : mappingMetadata.getCollectionBindings() ) {
			if ( collectionPersister( model.getRole() ) == null ) {
				throw new HibernateException( "Collection role not properly materialized to CollectionPersister : " + model.getRole() );
			}
		}

		// todo : deal with this somewhere
		if ( jpaMetaModelPopulationSetting != JpaMetaModelPopulationSetting.DISABLED ) {
			MetadataContext context = new MetadataContext(
					getSessionFactory(),
					mappingMetadata.getMappedSuperclassMappingsCopy(),
					jpaMetaModelPopulationSetting
			);

			for ( PersistentClass entityBinding : mappingMetadata.getEntityBindings() ) {
				locateOrBuildEntityType( entityBinding, context );
			}
			handleUnusedMappedSuperclasses( context );

			context.wrapUp();

			this.jpaEntityTypeMap.putAll( context.getEntityTypeMap() );
			this.jpaEmbeddableTypeMap.putAll( context.getEmbeddableTypeMap() );
			this.jpaMappedSuperclassTypeMap.putAll( context.getMappedSuperclassTypeMap() );
			this.jpaEntityTypesByEntityName.putAll( context.getEntityTypesByEntityName() );
		}

		applyNamedEntityGraphs( mappingMetadata.getNamedEntityGraphs().values() );

		typeConfiguration.scope( sessionFactory );
	}

	private void populateDatabaseModel(MetadataImplementor mappingMetadata) {
		// todo : apply PhysicalNamingStrategy here, rather than as we create the "mapping model"?

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
					final JdbcEnvironment jdbcEnvironment = sessionFactory.getJdbcServices().getJdbcEnvironment();
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

	@SuppressWarnings("unchecked")
	private void applyNamedEntityGraphs(java.util.Collection<NamedEntityGraphDefinition> namedEntityGraphs) {
		for ( NamedEntityGraphDefinition definition : namedEntityGraphs ) {
			log.debugf(
					"Applying named entity graph [name=%s, entity-name=%s, jpa-entity-name=%s",
					definition.getRegisteredName(),
					definition.getEntityName(),
					definition.getJpaEntityName()
			);
			final EntityType entityType = entity( definition.getEntityName() );
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

	@Override
	public java.util.Collection<EntityNameResolver> getEntityNameResolvers() {
		return entityNameResolvers.keySet();
	}

	private static void registerEntityNameResolvers(EntityPersister persister, Map<EntityNameResolver,Object> entityNameResolvers) {
		if ( persister.getEntityMetamodel() == null || persister.getEntityMetamodel().getTuplizer() == null ) {
			return;
		}
		registerEntityNameResolvers( persister.getEntityMetamodel().getTuplizer(), entityNameResolvers );
	}

	private static void registerEntityNameResolvers(EntityTuplizer tuplizer, Map<EntityNameResolver,Object> entityNameResolvers) {
		EntityNameResolver[] resolvers = tuplizer.getEntityNameResolvers();
		if ( resolvers == null ) {
			return;
		}

		for ( EntityNameResolver resolver : resolvers ) {
			entityNameResolvers.put( resolver, ENTITY_NAME_RESOLVER_MAP_VALUE );
		}
	}

	private static void handleUnusedMappedSuperclasses(MetadataContext context) {
		final Set<MappedSuperclass> unusedMappedSuperclasses = context.getUnusedMappedSuperclasses();
		if ( !unusedMappedSuperclasses.isEmpty() ) {
			for ( MappedSuperclass mappedSuperclass : unusedMappedSuperclasses ) {
				log.unusedMappedSuperclass( mappedSuperclass.getMappedClass().getName() );
				locateOrBuildMappedsuperclassType( mappedSuperclass, context );
			}
		}
	}

	private static EntityTypeImpl<?> locateOrBuildEntityType(PersistentClass persistentClass, MetadataContext context) {
		EntityTypeImpl<?> entityType = context.locateEntityType( persistentClass );
		if ( entityType == null ) {
			entityType = buildEntityType( persistentClass, context );
		}
		return entityType;
	}

	//TODO remove / reduce @SW scope
	@SuppressWarnings("unchecked")
	private static EntityTypeImpl<?> buildEntityType(PersistentClass persistentClass, MetadataContext context) {
		final Class javaType = persistentClass.getMappedClass();
		context.pushEntityWorkedOn( persistentClass );
		final MappedSuperclass superMappedSuperclass = persistentClass.getSuperMappedSuperclass();
		AbstractIdentifiableType<?> superType = superMappedSuperclass == null
				? null
				: locateOrBuildMappedsuperclassType( superMappedSuperclass, context );
		//no mappedSuperclass, check for a super entity
		if ( superType == null ) {
			final PersistentClass superPersistentClass = persistentClass.getSuperclass();
			superType = superPersistentClass == null
					? null
					: locateOrBuildEntityType( superPersistentClass, context );
		}
		EntityTypeImpl entityType = new EntityTypeImpl(
				javaType,
				superType,
				persistentClass
		);

		context.registerEntityType( persistentClass, entityType );
		context.popEntityWorkedOn( persistentClass );
		return entityType;
	}

	private static MappedSuperclassTypeImpl<?> locateOrBuildMappedsuperclassType(
			MappedSuperclass mappedSuperclass, MetadataContext context) {
		MappedSuperclassTypeImpl<?> mappedSuperclassType = context.locateMappedSuperclassType( mappedSuperclass );
		if ( mappedSuperclassType == null ) {
			mappedSuperclassType = buildMappedSuperclassType( mappedSuperclass, context );
		}
		return mappedSuperclassType;
	}

	//TODO remove / reduce @SW scope
	@SuppressWarnings("unchecked")
	private static MappedSuperclassTypeImpl<?> buildMappedSuperclassType(
			MappedSuperclass mappedSuperclass,
			MetadataContext context) {
		final MappedSuperclass superMappedSuperclass = mappedSuperclass.getSuperMappedSuperclass();
		AbstractIdentifiableType<?> superType = superMappedSuperclass == null
				? null
				: locateOrBuildMappedsuperclassType( superMappedSuperclass, context );
		//no mappedSuperclass, check for a super entity
		if ( superType == null ) {
			final PersistentClass superPersistentClass = mappedSuperclass.getSuperPersistentClass();
			superType = superPersistentClass == null
					? null
					: locateOrBuildEntityType( superPersistentClass, context );
		}
		final Class javaType = mappedSuperclass.getMappedClass();
		MappedSuperclassTypeImpl mappedSuperclassType = new MappedSuperclassTypeImpl(
				javaType,
				mappedSuperclass,
				superType
		);
		context.registerMappedSuperclassType( mappedSuperclass, mappedSuperclassType );
		return mappedSuperclassType;
	}

//	/**
//	 * Instantiate the metamodel.
//	 *
//	 * @param entityNameResolvers
//	 * @param entities The entity mappings.
//	 * @param embeddables The embeddable (component) mappings.
//	 * @param mappedSuperclassTypeMap The {@link javax.persistence.MappedSuperclass} mappings
//	 */
//	private MetamodelImpl(
//			SessionFactoryImplementor sessionFactory,
//			Map<String, String> imports,
//			Map<String, EntityPersister> entityPersisterMap,
//			Map<Class, String> entityProxyInterfaceMap,
//			ConcurrentHashMap<EntityNameResolver, Object> entityNameResolvers,
//			Map<String, CollectionPersister> collectionPersisterMap,
//			Map<String, Set<String>> collectionRolesByEntityParticipant,
//			Map<Class<?>, EntityTypeImpl<?>> entities,
//			Map<Class<?>, EmbeddableTypeImpl<?>> embeddables,
//			Map<Class<?>, MappedSuperclassType<?>> mappedSuperclassTypeMap,
//			Map<String, EntityTypeImpl<?>> entityTypesByEntityName) {
//		this.sessionFactory = sessionFactory;
//		this.imports = imports;
//		this.entityPersisterMap = entityPersisterMap;
//		this.entityProxyInterfaceMap = entityProxyInterfaceMap;
//		this.entityNameResolvers = entityNameResolvers;
//		this.collectionPersisterMap = collectionPersisterMap;
//		this.collectionRolesByEntityParticipant = collectionRolesByEntityParticipant;
//		this.entities = entities;
//		this.embeddables = embeddables;
//		this.mappedSuperclassTypeMap = mappedSuperclassTypeMap;
//		this.entityTypesByEntityName = entityTypesByEntityName;
//	}


	@Override
	public TypeConfiguration getTypeConfiguration() {
		return typeConfiguration;
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public <X> EntityType<X> entity(Class<X> cls) {
		final EntityType<?> entityType = jpaEntityTypeMap.get( cls );
		if ( entityType == null ) {
			throw new IllegalArgumentException( "Not an entity: " + cls );
		}
		return (EntityType<X>) entityType;
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public <X> ManagedType<X> managedType(Class<X> cls) {
		ManagedType<?> type = jpaEntityTypeMap.get( cls );
		if ( type == null ) {
			type = jpaMappedSuperclassTypeMap.get( cls );
		}
		if ( type == null ) {
			type = jpaEmbeddableTypeMap.get( cls );
		}
		if ( type == null ) {
			throw new IllegalArgumentException( "Not a managed type: " + cls );
		}
		return (ManagedType<X>) type;
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public <X> EmbeddableType<X> embeddable(Class<X> cls) {
		final EmbeddableType<?> embeddableType = jpaEmbeddableTypeMap.get( cls );
		if ( embeddableType == null ) {
			throw new IllegalArgumentException( "Not an embeddable: " + cls );
		}
		return (EmbeddableType<X>) embeddableType;
	}

	@Override
	public Set<ManagedType<?>> getManagedTypes() {
		final int setSize = CollectionHelper.determineProperSizing(
				jpaEntityTypeMap.size() + jpaMappedSuperclassTypeMap.size() + jpaEmbeddableTypeMap.size()
		);
		final Set<ManagedType<?>> managedTypes = new HashSet<>( setSize );
		managedTypes.addAll( jpaEntityTypeMap.values() );
		managedTypes.addAll( jpaMappedSuperclassTypeMap.values() );
		managedTypes.addAll( jpaEmbeddableTypeMap.values() );
		return managedTypes;
	}

	@Override
	public Set<EntityType<?>> getEntities() {
		return new HashSet<>( jpaEntityTypesByEntityName.values() );
	}

	@Override
	public Set<EmbeddableType<?>> getEmbeddables() {
		return new HashSet<>( jpaEmbeddableTypeMap.values() );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> EntityType<X> entity(String entityName) {
		return (EntityType<X>) jpaEntityTypesByEntityName.get( entityName );
	}

	@Override
	public String getImportedClassName(String className) {
		String result = imports.get( className );
		if ( result == null ) {
			try {
				getSessionFactory().getServiceRegistry().getService( ClassLoaderService.class ).classForName( className );
				imports.put( className, className );
				return className;
			}
			catch ( ClassLoadingException cnfe ) {
				return null;
			}
		}
		else {
			return result;
		}
	}

	/**
	 * Given the name of an entity class, determine all the class and interface names by which it can be
	 * referenced in an HQL query.
	 *
	 * @param className The name of the entity class
	 *
	 * @return the names of all persistent (mapped) classes that extend or implement the
	 *     given class or interface, accounting for implicit/explicit polymorphism settings
	 *     and excluding mapped subclasses/joined-subclasses of other classes in the result.
	 * @throws MappingException
	 */
	public String[] getImplementors(String className) throws MappingException {

		final Class clazz;
		try {
			clazz = getSessionFactory().getServiceRegistry().getService( ClassLoaderService.class ).classForName( className );
		}
		catch (ClassLoadingException e) {
			return new String[] { className }; //for a dynamic-class
		}

		ArrayList<String> results = new ArrayList<>();
		for ( EntityPersister checkPersister : entityPersisters().values() ) {
			if ( ! Queryable.class.isInstance( checkPersister ) ) {
				continue;
			}
			final Queryable checkQueryable = Queryable.class.cast( checkPersister );
			final String checkQueryableEntityName = checkQueryable.getEntityName();
			final boolean isMappedClass = className.equals( checkQueryableEntityName );
			if ( checkQueryable.isExplicitPolymorphism() ) {
				if ( isMappedClass ) {
					return new String[] { className }; //NOTE EARLY EXIT
				}
			}
			else {
				if ( isMappedClass ) {
					results.add( checkQueryableEntityName );
				}
				else {
					final Class mappedClass = checkQueryable.getMappedClass();
					if ( mappedClass != null && clazz.isAssignableFrom( mappedClass ) ) {
						final boolean assignableSuperclass;
						if ( checkQueryable.isInherited() ) {
							Class mappedSuperclass = entityPersister( checkQueryable.getMappedSuperclass() ).getMappedClass();
							assignableSuperclass = clazz.isAssignableFrom( mappedSuperclass );
						}
						else {
							assignableSuperclass = false;
						}
						if ( !assignableSuperclass ) {
							results.add( checkQueryableEntityName );
						}
					}
				}
			}
		}
		return results.toArray( new String[results.size()] );
	}

	@Override
	public Map<String, EntityPersister> entityPersisters() {
		return entityPersisterMap;
	}

	@Override
	public CollectionPersister collectionPersister(String role) {
		final CollectionPersister persister = collectionPersisterMap.get( role );
		if ( persister == null ) {
			throw new MappingException( "Could not locate CollectionPersister for role : " + role );
		}
		return persister;
	}

	@Override
	public Map<String, CollectionPersister> collectionPersisters() {
		return collectionPersisterMap;
	}

	@Override
	public EntityPersister entityPersister(Class entityClass) {
		return entityPersister( entityClass.getName() );
	}

	@Override
	public EntityPersister entityPersister(String entityName) throws MappingException {
		EntityPersister result = entityPersisterMap.get( entityName );
		if ( result == null ) {
			throw new MappingException( "Unknown entity: " + entityName );
		}
		return result;
	}


	@Override
	public EntityPersister locateEntityPersister(Class byClass) {
		EntityPersister entityPersister = entityPersisterMap.get( byClass.getName() );
		if ( entityPersister == null ) {
			String mappedEntityName = entityProxyInterfaceMap.get( byClass );
			if ( mappedEntityName != null ) {
				entityPersister = entityPersisterMap.get( mappedEntityName );
			}
		}

		if ( entityPersister == null ) {
			throw new UnknownEntityTypeException( "Unable to locate persister: " + byClass.getName() );
		}

		return entityPersister;
	}

	@Override
	public EntityPersister locateEntityPersister(String byName) {
		final EntityPersister entityPersister = entityPersisterMap.get( byName );
		if ( entityPersister == null ) {
			throw new UnknownEntityTypeException( "Unable to locate persister: " + byName );
		}
		return entityPersister;
	}

	@Override
	public Set<String> getCollectionRolesByEntityParticipant(String entityName) {
		return collectionRolesByEntityParticipant.get( entityName );
	}

	@Override
	public String[] getAllEntityNames() {
		return ArrayHelper.toStringArray( entityPersisterMap.keySet() );
	}

	@Override
	public String[] getAllCollectionRoles() {
		return ArrayHelper.toStringArray( entityPersisterMap.keySet() );
	}

	@Override
	public <T> void addNamedEntityGraph(String graphName, EntityGraph<T> entityGraph) {
		if ( entityGraph instanceof EntityGraphImplementor ) {
			entityGraph = ( (EntityGraphImplementor<T>) entityGraph ).makeImmutableCopy( graphName );
		}
		final EntityGraph old = entityGraphMap.put( graphName, entityGraph );
		if ( old != null ) {
			log.debugf( "EntityGraph being replaced on EntityManagerFactory for name %s", graphName );
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> EntityGraph<T> findEntityGraphByName(String name) {
		return entityGraphMap.get( name );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> List<EntityGraph<? super T>> findEntityGraphsByType(Class<T> entityClass) {
		final EntityType<T> entityType = entity( entityClass );
		if ( entityType == null ) {
			throw new IllegalArgumentException( "Given class is not an entity : " + entityClass.getName() );
		}

		final List<EntityGraph<? super T>> results = new ArrayList<>();

		for ( EntityGraph entityGraph : entityGraphMap.values() ) {
			if ( !EntityGraphImplementor.class.isInstance( entityGraph ) ) {
				continue;
			}

			final EntityGraphImplementor egi = (EntityGraphImplementor) entityGraph;
			if ( egi.appliesTo( entityType ) ) {
				results.add( egi );
			}
		}

		return results;
	}

	@Override
	public SqmDomainMetamodelImplementor getSqmDomainMetamodel() {
		return this;
	}

	@Override
	public void close() {
		// anything to do ?
	}



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// DomainMetamodel impl

	public SessionFactoryImplementor getSessionFactory() {
		return sessionFactory;
	}

	@Override
	public EntityReference resolveEntityReference(String entityName) {
		final String importedName = sessionFactory.getMetamodel().getImportedClassName( entityName );
		if ( importedName != null ) {
			entityName = importedName;
		}

		// look at existing non-polymorphic descriptors
		final EntityPersister persister = sessionFactory.getMetamodel().entityPersister( entityName );
		if ( persister != null ) {
			return persister;
		}

		// look at existing polymorphic descriptors
		if ( polymorphicEntityTypeDescriptorMap != null ) {
			PolymorphicEntityReferenceImpl existingEntry = polymorphicEntityTypeDescriptorMap.get( entityName );
			if ( existingEntry != null ) {
				return existingEntry;
			}
		}


		final String[] implementors = sessionFactory.getMetamodel().getImplementors( entityName );
		if ( implementors != null ) {
			if ( implementors.length == 1 ) {
				return entityPersister( implementors[0] );
			}
			else if ( implementors.length > 1 ) {
				final List<EntityPersister> implementDescriptors = new ArrayList<>();
				for ( String implementor : implementors ) {
					implementDescriptors.add( entityPersister( implementor ) );
				}
				if ( polymorphicEntityTypeDescriptorMap == null ) {
					polymorphicEntityTypeDescriptorMap = new HashMap<>();
				}
				PolymorphicEntityReferenceImpl descriptor = new PolymorphicEntityReferenceImpl(
						this,
						entityName,
						implementDescriptors
				);
				polymorphicEntityTypeDescriptorMap.put( entityName, descriptor );
				return descriptor;
			}
		}

		throw new HibernateException( "Could not resolve entity reference [" + entityName + "] from query" );
	}

	@Override
	public EntityReference resolveEntityReference(Class javaType) {
		return resolveEntityReference( javaType.getName() );
	}

	@Override
	public AttributeReference resolveAttributeReference(DomainReference source, String attributeName) {
		final AttributeReference attrRef = locateAttributeReference( source, attributeName );
		if ( attrRef == null ) {
			throw new NoSuchAttributeException( "Could not locate attribute named [" + attributeName + "] relative to [" + source.asLoggableText() + "]" );
		}
		return attrRef;
	}

	@Override
	public AttributeReference locateAttributeReference(DomainReference sourceBinding, String attributeName) {
		if ( sourceBinding instanceof AttributeContainer ) {
			return ( (AttributeContainer) sourceBinding ).findAttribute( attributeName );
		}

		if ( sourceBinding instanceof SingularAttributeEmbedded ) {
			return ( (SingularAttributeEmbedded) sourceBinding ).getEmbeddablePersister().findAttribute( attributeName );
		}

		if ( sourceBinding instanceof OrmTypeExporter ) {
			return resolveAttributeReferenceSource( ( (OrmTypeExporter) sourceBinding ) ).findAttribute( attributeName );
		}

		throw new IllegalArgumentException( "Unexpected type [" + sourceBinding + "] passed as 'attribute source'" );
	}

	private AttributeContainer resolveAttributeReferenceSource(OrmTypeExporter typeAccess) {
		// todo
		final Type type = typeAccess.getOrmType();
		if ( type instanceof org.hibernate.type.spi.EntityType ) {
			return entityPersister( ( (org.hibernate.type.spi.EntityType) type ).getAssociatedEntityName() );
		}
		else if ( type instanceof CollectionType ) {
			return resolveAttributeReferenceSource(
					collectionPersister( ( (CollectionType) type ).getRole() ).getElementReference()
			);
		}
		else if ( type instanceof CompositeType ) {
			throw new org.hibernate.cfg.NotYetImplementedException( "Resolving CompositeType attribute references is not yet implemented; requires Type system changes" );
		}

		throw new IllegalArgumentException( "Unexpected type [" + typeAccess + "] passed" );
	}

	@Override
	@SuppressWarnings("unchecked")
	public BasicType resolveBasicType(Class javaType) {
		return BasicTypeHelper.getRegisteredBasicType( javaType, typeConfiguration );
	}

	@Override
	public BasicType resolveArithmeticType(
			DomainReference firstType,
			DomainReference secondType,
			BinaryArithmeticSqmExpression.Operation operation) {
		return resolveArithmeticType( toBasicType( firstType ), toBasicType( secondType ), operation );
	}

	private BasicType toBasicType(DomainReference domainReference) {
		if ( domainReference == null ) {
			return null;
		}

		if ( domainReference instanceof BasicType ) {
			return (BasicType) domainReference;
		}

		if ( domainReference instanceof OrmTypeExporter ) {
			return resolveBasicType( ( (OrmTypeExporter) domainReference ).getOrmType().getReturnedClass() );
		}

		throw new IllegalArgumentException( "Unexpected type [" + domainReference + "]" );
	}

	private BasicType resolveArithmeticType(
			BasicType firstType,
			BasicType secondType,
			BinaryArithmeticSqmExpression.Operation operation) {
		if ( operation == BinaryArithmeticSqmExpression.Operation.DIVIDE ) {
			// covered under the note in 6.5.7.1 discussing the unportable
			// "semantics of the SQL division operation"..
			return resolveBasicType( Number.class );
		}
		else if ( firstType != null && Double.class.isAssignableFrom( firstType.getJavaType() ) ) {
			return firstType;
		}
		else if ( secondType != null && Double.class.isAssignableFrom( secondType.getJavaType() ) ) {
			return secondType;
		}
		else if ( firstType != null && Float.class.isAssignableFrom( firstType.getJavaType() ) ) {
			return firstType;
		}
		else if ( secondType != null && Float.class.isAssignableFrom( secondType.getJavaType() ) ) {
			return secondType;
		}
		else if ( firstType != null && BigDecimal.class.isAssignableFrom( firstType.getJavaType() ) ) {
			return firstType;
		}
		else if ( secondType != null && BigDecimal.class.isAssignableFrom( secondType.getJavaType() ) ) {
			return secondType;
		}
		else if ( firstType != null && BigInteger.class.isAssignableFrom( firstType.getJavaType() ) ) {
			return firstType;
		}
		else if ( secondType != null && BigInteger.class.isAssignableFrom( secondType.getJavaType() ) ) {
			return secondType;
		}
		else if ( firstType != null && Long.class.isAssignableFrom( firstType.getJavaType() ) ) {
			return firstType;
		}
		else if ( secondType != null && Long.class.isAssignableFrom( secondType.getJavaType() ) ) {
			return secondType;
		}
		else if ( firstType != null && Integer.class.isAssignableFrom( firstType.getJavaType() ) ) {
			return firstType;
		}
		else if ( secondType != null && Integer.class.isAssignableFrom( secondType.getJavaType() ) ) {
			return secondType;
		}
		else if ( firstType != null && Short.class.isAssignableFrom( firstType.getJavaType() ) ) {
			return resolveBasicType( Integer.class );
		}
		else if ( secondType != null && Short.class.isAssignableFrom( secondType.getJavaType() ) ) {
			return resolveBasicType( Integer.class );
		}
		else {
			return resolveBasicType( Number.class );
		}
	}

	@Override
	public BasicType resolveSumFunctionType(DomainReference argumentType) {
		return resolveSumFunctionType( toBasicType( argumentType ) );
	}

	public BasicType resolveSumFunctionType(BasicType argumentType) {
		if ( argumentType == null ) {
			return resolveBasicType( Number.class );
		}

		if ( Double.class.isAssignableFrom( argumentType.getJavaType() ) ) {
			return argumentType;
		}
		else if ( Float.class.isAssignableFrom( argumentType.getJavaType() ) ) {
			return argumentType;
		}
		else if ( BigDecimal.class.isAssignableFrom( argumentType.getJavaType() ) ) {
			return argumentType;
		}
		else if ( BigInteger.class.isAssignableFrom( argumentType.getJavaType() ) ) {
			return argumentType;
		}
		else if ( Long.class.isAssignableFrom( argumentType.getJavaType() ) ) {
			return argumentType;
		}
		else if ( Integer.class.isAssignableFrom( argumentType.getJavaType() ) ) {
			return argumentType;
		}
		else if ( Short.class.isAssignableFrom( argumentType.getJavaType() ) ) {
			return resolveBasicType( Integer.class );
		}
		else {
			return resolveBasicType( Number.class );
		}
	}

	@Override
	public BasicType resolveCastTargetType(String name) {
		// todo : how broad to we want to allow HQL casts to be?  What types?  Just basic types?
		//		for now assume just basic types
		BasicType ormBasicType = null;
//		BasicType ormBasicType = (BasicType) sessionFactory.getMetamodel().getTypeConfiguration()
//				.getBasicTypeRegistry()
//				.getRegisteredBasicType( Re )
//		..getTypeHelper().heuristicType( name );
		if ( ormBasicType == null ) {
			return null;
		}

		return resolveBasicType( ormBasicType.getJavaType() );
	}
}
