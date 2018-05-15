/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.creation.spi;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.NamedSubgraph;
import javax.persistence.metamodel.Attribute;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.boot.model.domain.EntityMapping;
import org.hibernate.boot.model.domain.EntityMappingHierarchy;
import org.hibernate.boot.model.domain.IdentifiableTypeMapping;
import org.hibernate.boot.model.domain.MappedSuperclassMapping;
import org.hibernate.boot.model.domain.ResolutionContext;
import org.hibernate.boot.model.domain.spi.EmbeddedValueMappingImplementor;
import org.hibernate.boot.model.domain.spi.IdentifiableTypeMappingImplementor;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cache.cfg.internal.DomainDataRegionConfigImpl;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.cfg.annotations.NamedEntityGraphDefinition;
import org.hibernate.collection.spi.CollectionSemanticsResolver;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.graph.internal.RootGraphImpl;
import org.hibernate.graph.spi.AttributeNodeImplementor;
import org.hibernate.graph.spi.GraphImplementor;
import org.hibernate.graph.spi.SubGraphImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.RootClass;
import org.hibernate.metamodel.internal.JpaStaticMetaModelPopulationSetting;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.model.domain.spi.EmbeddedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityHierarchy;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.IdentifiableTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeRepresentationResolver;
import org.hibernate.metamodel.model.domain.spi.MappedSuperclassTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.relational.spi.DatabaseModel;
import org.hibernate.metamodel.model.relational.spi.RuntimeDatabaseModelProducer;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator;
import org.hibernate.type.spi.TypeConfiguration;

import org.jboss.logging.Logger;

import static org.hibernate.metamodel.internal.JpaStaticMetaModelPopulationSetting.determineJpaMetaModelPopulationSetting;

/**
 * @author Steve Ebersole
 */
public class RuntimeModelCreationProcess implements ResolutionContext {
	private static final Logger log = Logger.getLogger( RuntimeModelCreationProcess.class );

	public static MetamodelImplementor execute(
			SessionFactoryImplementor sessionFactory,
			BootstrapContext bootstrapContext,
			MetadataBuildingContext metadataBuildingContext) {
		// todo (6.0) : Access to the finalized MetadataImplementor would be good
		//		^^ why??
		return new RuntimeModelCreationProcess( sessionFactory, bootstrapContext, metadataBuildingContext ).execute();
	}

	private final SessionFactoryImplementor sessionFactory;
	private final BootstrapContext bootstrapContext;
	private final MetadataBuildingContext metadataBuildingContext;
	private final InFlightRuntimeModel inFlightRuntimeModel;

	private final RuntimeModelDescriptorFactory descriptorFactory;

	private final Map<EntityMappingHierarchy,IdentifiableTypeDescriptor> runtimeRootByBootHierarchy = new LinkedHashMap<>();
	private final Map<EntityMappingHierarchy, EntityTypeDescriptor> runtimeRootEntityByBootHierarchy = new LinkedHashMap<>();

	private final Map<IdentifiableTypeMappingImplementor,IdentifiableTypeDescriptor> runtimeByBoot = new LinkedHashMap<>();
	private final Map<IdentifiableTypeDescriptor,IdentifiableTypeMappingImplementor> bootByRuntime = new LinkedHashMap<>();

	private final Map<EmbeddedValueMappingImplementor,EmbeddedTypeDescriptor> embeddableRuntimeByBoot = new LinkedHashMap<>();
	private final Map<Collection,PersistentCollectionDescriptor> collectionRuntimeByBoot = new LinkedHashMap<>();

	private final Map<String, DomainDataRegionConfigImpl.Builder> regionConfigBuilders = new ConcurrentHashMap<>();

	private final Map<Navigable,Object> navigablesToFinalize = new LinkedHashMap<>();

	private RuntimeModelCreationProcess(
			SessionFactoryImplementor sessionFactory,
			BootstrapContext bootstrapContext,
			MetadataBuildingContext metadataBuildingContext) {
		this.sessionFactory = sessionFactory;
		this.bootstrapContext = bootstrapContext;
		this.metadataBuildingContext = metadataBuildingContext;

		this.inFlightRuntimeModel = new InFlightRuntimeModel( metadataBuildingContext );

		this.descriptorFactory = sessionFactory.getServiceRegistry().getService( RuntimeModelDescriptorFactory.class );

	}

	public SessionFactoryImplementor getSessionFactory() {
		return sessionFactory;
	}

	@Override
	public BootstrapContext getBootstrapContext() {
		return bootstrapContext;
	}

	@Override
	public MetadataBuildingContext getMetadataBuildingContext() {
		return metadataBuildingContext;
	}

	public MetamodelImplementor execute() {
		final InFlightMetadataCollector mappingMetadata = metadataBuildingContext.getMetadataCollector();

		final DatabaseObjectResolutionContextImpl dbObjectResolver = new DatabaseObjectResolutionContextImpl();
		final DatabaseModel databaseModel = new RuntimeDatabaseModelProducer( metadataBuildingContext.getBootstrapContext() )
				.produceDatabaseModel( mappingMetadata.getDatabase(), dbObjectResolver, dbObjectResolver );

		final JpaStaticMetaModelPopulationSetting jpaMetaModelPopulationSetting = determineJpaMetaModelPopulationSetting(
				sessionFactory.getProperties()
		);

		final RuntimeModelCreationContext creationContext = new RuntimeModelCreationContextImpl(
				mappingMetadata,
				databaseModel,
				jpaMetaModelPopulationSetting,
				dbObjectResolver
		);

//		resolveForeignKeys( mappingMetadata, creationContext );

		for ( EntityMappingHierarchy bootHierarchy : mappingMetadata.getEntityHierarchies() ) {
			final EntityTypeDescriptor<?> rootEntityDescriptor = (EntityTypeDescriptor<?>) createIdentifiableType(
					bootHierarchy.getRootType(),
					null,
					creationContext
			);

			if ( runtimeRootEntityByBootHierarchy.containsKey( bootHierarchy )
					|| runtimeRootByBootHierarchy.containsKey( bootHierarchy ) ) {
				throw new HibernateException(
						"Entity boot hierarchy was encountered twice while transforming to runtime model : " +
								bootHierarchy.getRootType().getName()
				);
			}

			runtimeRootEntityByBootHierarchy.put( bootHierarchy, rootEntityDescriptor );

			creationContext.registerEntityHierarchy( rootEntityDescriptor.getHierarchy(), bootHierarchy );

			walkSupers(
					bootHierarchy,
					bootHierarchy.getRootType(),
					rootEntityDescriptor.getHierarchy(),
					rootEntityDescriptor,
					creationContext
			);

			walkSubs(
					bootHierarchy.getRootType(),
					rootEntityDescriptor.getHierarchy(),
					rootEntityDescriptor,
					creationContext
			);
		}

		// NOTE : at this point all ManagedTypeDescriptors have been created and
		//		their hierarchies have been linked.  Allow the ManagedTypeDescriptors
		// 		to finish their initialization, which involves building attributes
		//		etc
		//
		// NOTE 2 : notice that we iterate the "root root" which is the IdentifiableTypeDescriptor
		//		that is the "very top" of the hierarchy, as opposed to the "root entity" which
		//		is the first entity in that hierarchy.  This "root entity" is very special in
		//		Hibernate (and JPA overall) because it defines many values pertaining to or
		//		applied to the hierarchy as a whole (identifier, version, etc).  The "root entity"
		//		may very well be the "root root" as well.
		//
		//		Anyway, the point here is that we start at the very, very top so we only ever
		//		have to walk down here as opposed to above with `#walkSupers` and `#walkSubs`

		for ( Map.Entry<EntityMappingHierarchy, IdentifiableTypeDescriptor> entry : runtimeRootByBootHierarchy.entrySet() ) {
			final EntityTypeDescriptor runtimeRootEntity = runtimeRootEntityByBootHierarchy.get( entry.getKey() );
			final IdentifiableTypeDescriptor runtimeRootRoot = entry.getValue();
			final RootClass bootRootEntity = (RootClass) bootByRuntime.get( runtimeRootEntity );

			finishInitialization(
					runtimeRootRoot,
					bootByRuntime.get( runtimeRootRoot ),
					creationContext
			);

			runtimeRootEntity.getHierarchy().finishInitialization( creationContext, bootRootEntity );
		}

		while ( true ) {
			boolean anyEmbeddablesFinalized = false;
			boolean anyCollectionsFinalized = false;
			boolean anyNavigablesFinalized = false;

			if ( ! embeddableRuntimeByBoot.isEmpty() ) {
				final LinkedHashMap<EmbeddedValueMappingImplementor, EmbeddedTypeDescriptor> copy = new LinkedHashMap<>( embeddableRuntimeByBoot );
				embeddableRuntimeByBoot.clear();

				anyEmbeddablesFinalized = copy.entrySet().removeIf(
						entry -> entry.getValue().finishInitialization( entry.getKey(), creationContext )
				);

				if ( ! copy.isEmpty() ) {
					embeddableRuntimeByBoot.putAll( copy );
				}
			}

			if ( ! collectionRuntimeByBoot.isEmpty() ) {
				final LinkedHashMap<Collection, PersistentCollectionDescriptor> copy = new LinkedHashMap<>( collectionRuntimeByBoot );
				collectionRuntimeByBoot.clear();

				anyCollectionsFinalized = copy.entrySet().removeIf(
						entry -> entry.getValue().finishInitialization( entry.getKey(), creationContext )
				);

				if ( ! copy.isEmpty() ) {
					collectionRuntimeByBoot.putAll( copy );
				}
			}

			if ( ! navigablesToFinalize.isEmpty() ) {
				final LinkedHashMap<Navigable, Object> copy = new LinkedHashMap<>( navigablesToFinalize );
				navigablesToFinalize.clear();

				anyNavigablesFinalized = copy.entrySet().removeIf(
						entry -> entry.getKey().finishInitialization( entry.getValue(), creationContext )
				);

				if ( ! copy.isEmpty() ) {
					navigablesToFinalize.putAll( copy );
				}
			}

			if ( embeddableRuntimeByBoot.isEmpty()
					&& collectionRuntimeByBoot.isEmpty()
					&& navigablesToFinalize.isEmpty() ) {
				// we are done
				break;
			}

			if ( ! anyEmbeddablesFinalized && ! anyCollectionsFinalized && ! anyNavigablesFinalized ) {
				throw new MappingException( "Unable to complete initialization of run-time meta-model" );
			}
		}

		for ( Map.Entry<EntityMappingHierarchy, IdentifiableTypeDescriptor> entry : runtimeRootByBootHierarchy.entrySet() ) {
			final EntityTypeDescriptor runtimeRootEntity = runtimeRootEntityByBootHierarchy.get( entry.getKey() );
			runtimeRootEntity.postInitialization( creationContext );
		}

		descriptorFactory.finishUp( creationContext );

		SchemaManagementToolCoordinator.process(
				databaseModel,
				sessionFactory.getServiceRegistry(),
				action -> sessionFactory.addObserver( action )
		);

		mappingMetadata.getNamedEntityGraphs().values().forEach( this::applyNamedEntityGraph );

		// todo (6.0) : this needs to happen earlier
		//		specifically, needs to happen before we start creating the descriptors.  ideally
		//		the descriptors can just get it from the context, etc
		sessionFactory.getCache().prime(
				regionConfigBuilders.values()
						.stream()
						.map( DomainDataRegionConfigImpl.Builder::build )
						.collect( Collectors.toSet() )
		);

		return inFlightRuntimeModel.complete( sessionFactory, metadataBuildingContext );
	}

//	private void resolveForeignKeys(
//			InFlightMetadataCollector mappingMetadata,
//			RuntimeModelCreationContext creationContext) {
//		final List<Function<RuntimeModelCreationContext, Boolean>> resolvers = mappingMetadata.getForeignKeyCreators();
//
//		while ( true ) {
//			final boolean anyRemoved = resolvers.removeIf(
//					resolver -> resolver.apply( creationContext )
//			);
//
//			if ( ! anyRemoved ) {
//				if ( ! resolvers.isEmpty() ) {
//					throw new MappingException( "Unable to complete initialization of boot meta-model" );
//				}
//
//				break;
//			}
//		}
//	}

	private void walkSupers(
			EntityMappingHierarchy bootHierarchy,
			IdentifiableTypeMappingImplementor bootMapping,
			EntityHierarchy runtimeHierarchy,
			IdentifiableTypeDescriptor<?> runtimeMapping,
			RuntimeModelCreationContext creationContext) {
		assert bootHierarchy != null;
		assert bootMapping != null;

		assert runtimeHierarchy != null;
		assert runtimeMapping != null;

		if ( runtimeMapping instanceof EntityTypeDescriptor ) {
			creationContext.registerEntityDescriptor( (EntityTypeDescriptor) runtimeMapping, (EntityMapping) bootMapping );
		}

		if ( bootMapping.getSuperTypeMapping() == null ) {
			runtimeRootByBootHierarchy.put( bootHierarchy, runtimeMapping );
		}
		else {
			// always create going up
			final IdentifiableTypeDescriptor<?> runtimeSuperDescriptor = createIdentifiableType(
					(IdentifiableTypeMappingImplementor) bootMapping.getSuperTypeMapping(),
					runtimeMapping,
					creationContext
			);

			walkSupers(
					bootHierarchy,
					(IdentifiableTypeMappingImplementor) bootMapping.getSuperTypeMapping(),
					runtimeHierarchy,
					runtimeSuperDescriptor,
					creationContext
			);
		}
	}

	private void walkSubs(
			IdentifiableTypeMappingImplementor bootMapping,
			EntityHierarchy runtimeHierarchy,
			IdentifiableTypeDescriptor runtimeDescriptor,
			RuntimeModelCreationContext creationContext) {
		for ( IdentifiableTypeMapping bootSubTypeMapping : bootMapping.getSubTypeMappings() ) {
			final IdentifiableTypeDescriptor<?> runtimeSubclassDescriptor = createIdentifiableType(
					(IdentifiableTypeMappingImplementor) bootSubTypeMapping,
					runtimeDescriptor,
					creationContext
			);

			walkSubs(
					(IdentifiableTypeMappingImplementor) bootSubTypeMapping,
					runtimeHierarchy,
					runtimeSubclassDescriptor,
					creationContext
			);
		}
	}

	private IdentifiableTypeDescriptor<?> createIdentifiableType(
			IdentifiableTypeMappingImplementor bootMapping,
			IdentifiableTypeDescriptor superTypeDescriptor,
			RuntimeModelCreationContext creationContext) {
		final IdentifiableTypeDescriptor runtimeType = bootMapping.makeRuntimeDescriptor(
				superTypeDescriptor,
				creationContext
		);

		bootByRuntime.put( runtimeType, bootMapping );
		runtimeByBoot.put( bootMapping, runtimeType );

		if ( runtimeType instanceof EntityTypeDescriptor ) {
			creationContext.registerEntityDescriptor( (EntityTypeDescriptor) runtimeType, (EntityMapping) bootMapping );
		}
		else if ( runtimeType instanceof MappedSuperclassTypeDescriptor ) {
			creationContext.registerMappedSuperclassDescriptor(
					(MappedSuperclassTypeDescriptor) runtimeType,
					(MappedSuperclassMapping) bootMapping
			);
		}
		return runtimeType;
	}

	@SuppressWarnings("unchecked")
	private void finishInitialization(
			IdentifiableTypeDescriptor runtimeType,
			IdentifiableTypeMappingImplementor bootType,
			RuntimeModelCreationContext creationContext) {
		runtimeType.finishInitialization(
				bootType,
				creationContext
		);

		for ( IdentifiableTypeMapping subTypeMapping : bootType.getSubTypeMappings() ) {
			finishInitialization(
					runtimeByBoot.get( subTypeMapping ),
					(IdentifiableTypeMappingImplementor) subTypeMapping,
					creationContext
			);
		}
	}

	private void applyNamedEntityGraph(NamedEntityGraphDefinition definition) {
		log.debugf(
				"Applying named entity graph [name=%s, entity-name=%s, jpa-entity-name=%s",
				definition.getRegisteredName(),
				definition.getEntityName(),
				definition.getJpaEntityName()
		);

		final EntityTypeDescriptor<Object> entityDescriptor = inFlightRuntimeModel.findEntityDescriptor( definition.getEntityName() );
		if ( entityDescriptor == null ) {
			throw new IllegalArgumentException(
					"Attempted to register named entity graph [" + definition.getRegisteredName()
							+ "] for unknown entity ["+ definition.getEntityName() + "]"

			);
		}

		final RootGraphImpl<?> entityGraph = new RootGraphImpl<>(
				definition.getRegisteredName(),
				entityDescriptor,
				sessionFactory
		);

		final NamedEntityGraph namedEntityGraph = definition.getAnnotation();

		if ( namedEntityGraph.includeAllAttributes() ) {
			for ( Object attributeObject : entityDescriptor.getAttributes() ) {
				entityGraph.addAttributeNodes( (Attribute) attributeObject );
			}
		}

		if ( namedEntityGraph.attributeNodes().length > 0 ) {
			applyNamedAttributeNodes( namedEntityGraph.attributeNodes(), namedEntityGraph, entityGraph );
		}

		inFlightRuntimeModel.addNamedRootGraph( definition.getRegisteredName(), entityGraph );
	}

	private void applyNamedAttributeNodes(
			NamedAttributeNode[] namedAttributeNodes,
			NamedEntityGraph namedEntityGraph,
			GraphImplementor graphNode) {
		for ( NamedAttributeNode namedAttributeNode : namedAttributeNodes ) {
			final String value = namedAttributeNode.value();
			AttributeNodeImplementor attributeNode = graphNode.addAttributeNode( value );
			if ( StringHelper.isNotEmpty( namedAttributeNode.subgraph() ) ) {
				final SubGraphImplementor subgraph = attributeNode.makeSubGraph();
				applyNamedSubgraphs(
						namedEntityGraph,
						namedAttributeNode.subgraph(),
						subgraph
				);
			}
			if ( StringHelper.isNotEmpty( namedAttributeNode.keySubgraph() ) ) {
				final SubGraphImplementor subgraph = attributeNode.makeKeySubGraph();

				applyNamedSubgraphs(
						namedEntityGraph,
						namedAttributeNode.keySubgraph(),
						subgraph
				);
			}
		}
	}

	private void applyNamedSubgraphs(NamedEntityGraph namedEntityGraph, String subgraphName, SubGraphImplementor subgraph) {
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

	private class RuntimeModelCreationContextImpl implements RuntimeModelCreationContext {
		private final InFlightMetadataCollector mappingMetadata;
		private final DatabaseModel databaseModel;
		private final JpaStaticMetaModelPopulationSetting jpaMetaModelPopulationSetting;
		private final DatabaseObjectResolutionContextImpl dbObjectResolver;

		public RuntimeModelCreationContextImpl(
				InFlightMetadataCollector mappingMetadata,
				DatabaseModel databaseModel,
				JpaStaticMetaModelPopulationSetting jpaMetaModelPopulationSetting,
				DatabaseObjectResolutionContextImpl dbObjectResolver) {
			this.mappingMetadata = mappingMetadata;
			this.databaseModel = databaseModel;
			this.jpaMetaModelPopulationSetting = jpaMetaModelPopulationSetting;
			this.dbObjectResolver = dbObjectResolver;
		}

		@Override
		public BootstrapContext getBootstrapContext() {
			return bootstrapContext;
		}


		@Override
		public SessionFactoryImplementor getSessionFactory() {
			return sessionFactory;
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
		public DatabaseObjectResolver getDatabaseObjectResolver() {
			return dbObjectResolver;
		}

		@Override
		public InFlightRuntimeModel getInFlightRuntimeModel() {
			return inFlightRuntimeModel;
		}

		@Override
		public TypeConfiguration getTypeConfiguration() {
			return metadataBuildingContext.getBootstrapContext().getTypeConfiguration();
		}

		@Override
		public JpaStaticMetaModelPopulationSetting getJpaStaticMetaModelPopulationSetting() {
			return jpaMetaModelPopulationSetting;
		}

		@Override
		public RuntimeModelDescriptorFactory getRuntimeModelDescriptorFactory() {
			return descriptorFactory;
		}

		@Override
		public ManagedTypeRepresentationResolver getRepresentationStrategySelector() {
			return metadataBuildingContext.getBuildingOptions().getManagedTypeRepresentationResolver();
		}

		@Override
		public CollectionSemanticsResolver getCollectionSemanticsResolver() {
			return metadataBuildingContext.getBootstrapContext().getCollectionRepresentationResolver();
		}

		@Override
		public void registerEntityHierarchy(EntityHierarchy runtimeHierarchy, EntityMappingHierarchy bootHierarchy) {
			inFlightRuntimeModel.addEntityHierarchy( runtimeHierarchy );
		}

		@Override
		@SuppressWarnings("unchecked")
		public void registerEntityDescriptor(EntityTypeDescriptor runtimeDescriptor, EntityMapping bootDescriptor) {
			inFlightRuntimeModel.addEntityDescriptor( runtimeDescriptor );

			if ( RootClass.class.isInstance( bootDescriptor ) ) {
				// prepare both the entity and natural-id second level cache access for this hierarchy
				final RootClass rootBootMapping = (RootClass) bootDescriptor;
				final AccessType accessType = AccessType.fromExternalName( rootBootMapping.getCacheConcurrencyStrategy() );

				if ( accessType != null && rootBootMapping.isCached() ) {
					addEntityCachingConfig( runtimeDescriptor, rootBootMapping, accessType );
				}

				if ( rootBootMapping.hasNaturalId() && accessType != null && rootBootMapping.getNaturalIdCacheRegionName() != null ) {
					addNaturalIdCachingConfig( runtimeDescriptor, rootBootMapping, accessType );
				}
			}
		}

		private void addEntityCachingConfig(
				EntityTypeDescriptor runtimeDescriptor,
				RootClass bootDescriptor,
				AccessType accessType) {
			final DomainDataRegionConfigImpl.Builder  builder = locateBuilder( bootDescriptor.getCacheRegionName() );
			builder.addEntityConfig( bootDescriptor, accessType );
		}

		private DomainDataRegionConfigImpl.Builder  locateBuilder(String regionName) {
			return regionConfigBuilders.computeIfAbsent(
					regionName,
					DomainDataRegionConfigImpl.Builder ::new
			);
		}

		private void addNaturalIdCachingConfig(
				EntityTypeDescriptor runtimeDescriptor,
				RootClass bootDescriptor,
				AccessType accessType) {
			final DomainDataRegionConfigImpl.Builder configBuilder = locateBuilder( bootDescriptor.getCacheRegionName() );
			configBuilder.addNaturalIdConfig( bootDescriptor, accessType );
		}

		@Override
		@SuppressWarnings("unchecked")
		public void registerMappedSuperclassDescriptor(
				MappedSuperclassTypeDescriptor runtimeType,
				MappedSuperclassMapping bootMapping) {
			inFlightRuntimeModel.addMappedSuperclassDescriptor( runtimeType );
		}

		@Override
		@SuppressWarnings("unchecked")
		public void registerEmbeddableDescriptor(
				EmbeddedTypeDescriptor runtimeDescriptor,
				EmbeddedValueMappingImplementor bootDescriptor) {
			embeddableRuntimeByBoot.put( bootDescriptor, runtimeDescriptor );
			inFlightRuntimeModel.addEmbeddedDescriptor( runtimeDescriptor );
		}

		@Override
		public void registerNavigable(Navigable navigable, Object bootModelReference) {
			navigablesToFinalize.put( navigable, bootModelReference );
		}

		@Override
		@SuppressWarnings("unchecked")
		public void registerCollectionDescriptor(
				PersistentCollectionDescriptor runtimeDescriptor,
				Collection bootDescriptor) {
			collectionRuntimeByBoot.put( bootDescriptor, runtimeDescriptor );
			inFlightRuntimeModel.addCollectionDescriptor( runtimeDescriptor );

			final AccessType accessType = AccessType.fromExternalName( bootDescriptor.getCacheConcurrencyStrategy() );
			if ( accessType != null ) {
				addCollectionCachingConfig( runtimeDescriptor, bootDescriptor, accessType );
			}
		}

		private void addCollectionCachingConfig(
				PersistentCollectionDescriptor runtimeDescriptor,
				Collection bootDescriptor,
				AccessType accessType) {
			final DomainDataRegionConfigImpl.Builder configBuilder = locateBuilder( bootDescriptor.getCacheRegionName() );
			configBuilder.addCollectionConfig( bootDescriptor, accessType );
		}

		@Override
		public EntityDataAccess getEntityCacheAccess(NavigableRole navigableRole) {
			return null;
		}

		@Override
		public NaturalIdDataAccess getNaturalIdCacheAccess(NavigableRole navigableRole) {
			return null;
		}

		@Override
		public CollectionDataAccess getCollectionCacheAccess(NavigableRole navigableRole) {
			return null;
		}
	}

}
