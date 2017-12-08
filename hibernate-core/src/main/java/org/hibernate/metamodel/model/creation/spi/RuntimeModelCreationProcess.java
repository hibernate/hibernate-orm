/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.creation.spi;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.NamedSubgraph;
import javax.persistence.metamodel.Attribute;

import org.hibernate.HibernateException;
import org.hibernate.boot.model.domain.EntityMapping;
import org.hibernate.boot.model.domain.EntityMappingHierarchy;
import org.hibernate.boot.model.domain.IdentifiableTypeMapping;
import org.hibernate.boot.model.domain.MappedSuperclassMapping;
import org.hibernate.boot.model.domain.spi.EmbeddedValueMappingImplementor;
import org.hibernate.boot.model.domain.spi.IdentifiableTypeMappingImplementor;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cache.cfg.internal.DomainDataRegionConfigImpl;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cfg.annotations.NamedEntityGraphDefinition;
import org.hibernate.collection.spi.PersistentCollectionRepresentationResolver;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.graph.internal.AbstractAttributeNodeContainer;
import org.hibernate.graph.internal.AttributeNodeImpl;
import org.hibernate.graph.internal.EntityGraphImpl;
import org.hibernate.graph.internal.SubgraphImpl;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.RootClass;
import org.hibernate.metamodel.internal.JpaStaticMetaModelPopulationSetting;
import org.hibernate.metamodel.model.domain.spi.EmbeddedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityHierarchy;
import org.hibernate.metamodel.model.domain.spi.IdentifiableTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.MappedSuperclassDescriptor;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeRepresentationResolver;
import org.hibernate.metamodel.model.relational.spi.DatabaseModel;
import org.hibernate.metamodel.model.relational.spi.RuntimeDatabaseModelProducer;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator;
import org.hibernate.type.spi.TypeConfiguration;

import org.jboss.logging.Logger;

import static org.hibernate.metamodel.internal.JpaStaticMetaModelPopulationSetting.determineJpaMetaModelPopulationSetting;

/**
 * @author Steve Ebersole
 */
public class RuntimeModelCreationProcess {
	private static final Logger log = Logger.getLogger( RuntimeModelCreationProcess.class );

	private final SessionFactoryImplementor sessionFactory;
	private final BootstrapContext bootstrapContext;
	private final MetadataBuildingContext metadataBuildingContext;
	private final RuntimeModelDescriptorFactory descriptorFactory;

	private final Map<EntityMappingHierarchy,IdentifiableTypeDescriptor> runtimeRootByBootHierarchy = new HashMap<>();
	private final Map<EntityMappingHierarchy,EntityDescriptor> runtimeRootEntityByBootHierarchy = new HashMap<>();

	private final Map<IdentifiableTypeMappingImplementor,IdentifiableTypeDescriptor> runtimeByBoot = new HashMap<>();
	private final Map<IdentifiableTypeDescriptor,IdentifiableTypeMappingImplementor> bootByRuntime = new HashMap<>();

	private final Map<EmbeddedValueMappingImplementor,EmbeddedTypeDescriptor> embeddableRuntimeByBoot = new HashMap<>();
	private final Map<Collection,PersistentCollectionDescriptor> collectonRuntimeByBoot = new HashMap<>();

	private final Map<String, DomainDataRegionConfigImpl.Builder> regionConfigBuilders = new ConcurrentHashMap<>();

	public RuntimeModelCreationProcess(
			SessionFactoryImplementor sessionFactory,
			BootstrapContext bootstrapContext,
			MetadataBuildingContext metadataBuildingContext) {
		this.sessionFactory = sessionFactory;
		this.bootstrapContext = bootstrapContext;
		this.metadataBuildingContext = metadataBuildingContext;

		this.descriptorFactory = sessionFactory.getServiceRegistry().getService( RuntimeModelDescriptorFactory.class );
	}

	public void execute() {
		final InFlightMetadataCollector mappingMetadata = metadataBuildingContext.getMetadataCollector();

		// todo (7.0) : better design where all FKs are created b4 we enter here
		generateBootModelForeignKeys( mappingMetadata );

		final DatabaseObjectResolutionContextImpl dbObjectResolver = new DatabaseObjectResolutionContextImpl();
		final DatabaseModel databaseModel = new RuntimeDatabaseModelProducer( metadataBuildingContext.getBootstrapContext() )
				.produceDatabaseModel(
				mappingMetadata.getDatabase(),
				dbObjectResolver,
				dbObjectResolver
		);

		SchemaManagementToolCoordinator.process(
				databaseModel,
				sessionFactory.getServiceRegistry(),
				action -> sessionFactory.addObserver( action )
		);

		final JpaStaticMetaModelPopulationSetting jpaMetaModelPopulationSetting = determineJpaMetaModelPopulationSetting(
				sessionFactory.getProperties()
		);

		final RuntimeModelCreationContext creationContext = new RuntimeModelCreationContextImpl(
				mappingMetadata,
				databaseModel,
				jpaMetaModelPopulationSetting,
				dbObjectResolver
		);

		for ( EntityMappingHierarchy bootHierarchy : mappingMetadata.getEntityHierarchies() ) {
			final EntityDescriptor<?> rootEntityDescriptor = (EntityDescriptor<?>) createIdentifiableType(
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
		//		have to walk dowwn here as opposed to above with `#walkSupers` and `#walkSubs`

		for ( Map.Entry<EntityMappingHierarchy, IdentifiableTypeDescriptor> entry : runtimeRootByBootHierarchy.entrySet() ) {
			final EntityDescriptor runtimeRootEntity = runtimeRootEntityByBootHierarchy.get( entry.getKey() );
			final IdentifiableTypeDescriptor runtimeRootRoot = entry.getValue();
			final RootClass bootRootEntity = (RootClass) bootByRuntime.get( runtimeRootEntity );

			finishInitialization(
					runtimeRootRoot,
					bootByRuntime.get( runtimeRootRoot ),
					creationContext
			);

			runtimeRootEntity.getHierarchy().finishInitialization( creationContext, bootRootEntity );
		}

		for ( Map.Entry<EmbeddedValueMappingImplementor, EmbeddedTypeDescriptor> entry : embeddableRuntimeByBoot.entrySet() ) {
			entry.getValue().finishInitialization( entry.getKey(), creationContext );
		}

		for ( Map.Entry<Collection, PersistentCollectionDescriptor> entry : collectonRuntimeByBoot.entrySet() ) {
			entry.getValue().finishInitialization( entry.getKey(), creationContext );
		}

		descriptorFactory.finishUp( creationContext );

		mappingMetadata.getNamedEntityGraphs().values().forEach( this::applyNamedEntityGraph );

		sessionFactory.getCache().prime(
				regionConfigBuilders.values()
						.stream()
						.map( DomainDataRegionConfigImpl.Builder::build )
						.collect( Collectors.toSet() )
		);
	}

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

		if ( runtimeMapping instanceof EntityDescriptor ) {
			creationContext.registerEntityDescriptor( (EntityDescriptor) runtimeMapping, (EntityMapping) bootMapping );
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

	private void generateBootModelForeignKeys(InFlightMetadataCollector mappingMetadata) {
		// walk the boot model and create all mapping FKs (so they are ready for db process)
		// todo (6.0) : implement this
//		throw new NotYetImplementedFor6Exception(  );
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

		if ( runtimeType instanceof EntityDescriptor ) {
			creationContext.registerEntityDescriptor( (EntityDescriptor) runtimeType, (EntityMapping) bootMapping );
		}
		else if ( runtimeType instanceof MappedSuperclassDescriptor ) {
			creationContext.registerMappedSuperclassDescriptor(
					(MappedSuperclassDescriptor) runtimeType,
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

		final EntityDescriptor<?> entityPersister = metadataBuildingContext.getBootstrapContext()
				.getTypeConfiguration()
				.findEntityDescriptor( definition.getEntityName() );
		if ( entityPersister == null ) {
			throw new IllegalArgumentException(
					"Attempted to register named entity graph [" + definition.getRegisteredName()
							+ "] for unknown entity ["+ definition.getEntityName() + "]"

			);
		}

		final EntityGraphImpl<?> entityGraph = new EntityGraphImpl<>(
				definition.getRegisteredName(),
				entityPersister,
				sessionFactory
		);

		final NamedEntityGraph namedEntityGraph = definition.getAnnotation();

		if ( namedEntityGraph.includeAllAttributes() ) {
			for ( Object attributeObject : entityPersister.getAttributes() ) {
				entityGraph.addAttributeNodes( (Attribute) attributeObject );
			}
		}

		if ( namedEntityGraph.attributeNodes().length > 0 ) {
			applyNamedAttributeNodes( namedEntityGraph.attributeNodes(), namedEntityGraph, entityGraph );
		}

		metadataBuildingContext.getBootstrapContext().getTypeConfiguration().addNamedEntityGraph(
				definition.getRegisteredName(),
				entityGraph
		);
	}

	private void applyNamedAttributeNodes(
			NamedAttributeNode[] namedAttributeNodes,
			NamedEntityGraph namedEntityGraph,
			AbstractAttributeNodeContainer graphNode) {
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
		public PersistentCollectionRepresentationResolver getPersistentCollectionRepresentationResolver() {
			return metadataBuildingContext.getBootstrapContext().getCollectionRepresentationResolver();
		}

		@Override
		public void registerEntityHierarchy(EntityHierarchy runtimeHierarchy, EntityMappingHierarchy bootHierarchy) {
			getTypeConfiguration().register( runtimeHierarchy );
		}

		@Override
		public void registerEntityDescriptor(EntityDescriptor runtimeDescriptor, EntityMapping bootDescriptor) {
			getTypeConfiguration().register( runtimeDescriptor );

			if ( RootClass.class.isInstance( bootDescriptor ) ) {
				// prepare both the entity and natural-id second level cache access for this hierarchy
				final RootClass rootBootMapping = (RootClass) bootDescriptor;
				final AccessType accessType = AccessType.fromExternalName( rootBootMapping.getCacheConcurrencyStrategy() );
				if ( accessType != null ) {
					addEntityCachingConfig( runtimeDescriptor, rootBootMapping, accessType );

					if ( runtimeDescriptor.getHierarchy().getNaturalIdDescriptor() != null
							&& rootBootMapping.getNaturalIdCacheRegionName() != null ) {
						addNaturalIdCachingConfig( runtimeDescriptor, rootBootMapping, accessType );
					}
				}
			}
		}

		private void addEntityCachingConfig(
				EntityDescriptor runtimeDescriptor,
				RootClass bootDescriptor,
				AccessType accessType) {
			final DomainDataRegionConfigImpl.Builder  builder = locateBuilder( bootDescriptor.getNaturalIdCacheRegionName() );
			builder.addEntityConfig( runtimeDescriptor.getHierarchy(), accessType );
		}

		private DomainDataRegionConfigImpl.Builder  locateBuilder(String regionName) {
			return regionConfigBuilders.computeIfAbsent(
					regionName,
					DomainDataRegionConfigImpl.Builder ::new
			);
		}

		private void addNaturalIdCachingConfig(
				EntityDescriptor runtimeDescriptor,
				RootClass bootDescriptor,
				AccessType accessType) {
			final DomainDataRegionConfigImpl.Builder configBuilder = locateBuilder( bootDescriptor.getCacheRegionName() );
			configBuilder.addNaturalIdConfig( runtimeDescriptor.getHierarchy(), accessType );
		}

		@Override
		public void registerMappedSuperclassDescriptor(
				MappedSuperclassDescriptor runtimeType,
				MappedSuperclassMapping bootMapping) {
			getTypeConfiguration().register( runtimeType );
		}

		@Override
		public void registerCollectionDescriptor(
				PersistentCollectionDescriptor runtimeDescriptor,
				Collection bootDescriptor) {
			collectonRuntimeByBoot.put( bootDescriptor, runtimeDescriptor );
			getTypeConfiguration().register( runtimeDescriptor );
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
			configBuilder.addCollectionConfig( runtimeDescriptor, accessType );
		}

		@Override
		public void registerEmbeddableDescriptor(
				EmbeddedTypeDescriptor runtimeDescriptor,
				EmbeddedValueMappingImplementor bootDescriptor) {
			getTypeConfiguration().register( runtimeDescriptor );
			embeddableRuntimeByBoot.put( bootDescriptor, runtimeDescriptor );
		}
	}
}
