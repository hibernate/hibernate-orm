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

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.boot.model.domain.EmbeddedMapping;
import org.hibernate.boot.model.domain.EntityMapping;
import org.hibernate.boot.model.domain.EntityMappingHierarchy;
import org.hibernate.boot.model.domain.IdentifiableTypeMapping;
import org.hibernate.boot.model.domain.spi.IdentifiableTypeMappingImplementor;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cache.cfg.internal.DomainDataRegionConfigImpl;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cfg.annotations.NamedEntityGraphDefinition;
import org.hibernate.collection.spi.PersistentCollectionTuplizerFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.graph.internal.AbstractAttributeNodeContainer;
import org.hibernate.graph.internal.AttributeNodeImpl;
import org.hibernate.graph.internal.EntityGraphImpl;
import org.hibernate.graph.internal.SubgraphImpl;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.RootClass;
import org.hibernate.metamodel.internal.JpaStaticMetaModelPopulationSetting;
import org.hibernate.metamodel.model.domain.internal.EntityHierarchyImpl;
import org.hibernate.metamodel.model.domain.spi.EmbeddedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.metamodel.model.domain.spi.IdentifiableTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.domain.spi.RepresentationStrategySelector;
import org.hibernate.metamodel.model.relational.spi.DatabaseModel;
import org.hibernate.metamodel.model.relational.spi.RuntimeDatabaseModelProducer;
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

	private final Map<IdentifiableTypeMapping,IdentifiableTypeDescriptor> runtimeByBoot = new HashMap<>();
	private final Map<IdentifiableTypeDescriptor,IdentifiableTypeMapping> bootByRuntime = new HashMap<>();

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
		final DatabaseModel databaseModel = new RuntimeDatabaseModelProducer( metadataBuildingContext.getBootstrapContext() ).produceDatabaseModel(
				mappingMetadata.getDatabase(),
				dbObjectResolver,
				dbObjectResolver
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
					creationContext
			);
			runtimeRootEntityByBootHierarchy.put( bootHierarchy, rootEntityDescriptor );

			walkSupers( bootHierarchy, bootHierarchy.getRootType(), rootEntityDescriptor, creationContext );
			if ( !runtimeRootByBootHierarchy.containsKey( bootHierarchy ) ) {
				runtimeRootByBootHierarchy.put( bootHierarchy, rootEntityDescriptor );
			}

			walkSubs( bootHierarchy.getRootType(), creationContext );
		}

		for ( Map.Entry<EntityMappingHierarchy, IdentifiableTypeDescriptor> entry : runtimeRootByBootHierarchy.entrySet() ) {
			final EntityDescriptor runtimeRootEntity = runtimeRootEntityByBootHierarchy.get( entry.getKey() );
			final IdentifiableTypeDescriptor runtimeRootRoot = entry.getValue();
			final RootClass bootRootEntity = (RootClass) bootByRuntime.get( runtimeRootEntity );

			final EntityHierarchyImpl runtimeHierarchy = new EntityHierarchyImpl(
					creationContext,
					runtimeRootEntity,
					bootRootEntity
			);

			finishInitialization( runtimeRootRoot, bootByRuntime.get( runtimeRootRoot ), creationContext, runtimeHierarchy );

			runtimeHierarchy.finishInitialization( creationContext, bootRootEntity );
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
			IdentifiableTypeDescriptor<?> runtimeMapping,
			RuntimeModelCreationContext creationContext) {
		assert bootMapping != null;

		if ( bootMapping.getSuperTypeMapping() == null ) {
			runtimeRootByBootHierarchy.put( bootHierarchy, runtimeMapping );
		}
		else {
			// always create going up
			final IdentifiableTypeDescriptor<?> runtimeSuperDescriptor = createIdentifiableType(
					(IdentifiableTypeMappingImplementor) bootMapping.getSuperTypeMapping(),
					creationContext
			);
			walkSupers(
					bootHierarchy,
					(IdentifiableTypeMappingImplementor) bootMapping.getSuperTypeMapping(),
					runtimeSuperDescriptor,
					creationContext
			);
		}
	}

	private void walkSubs(IdentifiableTypeMappingImplementor bootMapping, RuntimeModelCreationContext creationContext) {
		for ( IdentifiableTypeMapping bootSubMapping : bootMapping.getSubTypeMappings() ) {
			createIdentifiableType(
					(IdentifiableTypeMappingImplementor) bootSubMapping,
					creationContext
			);

			walkSubs( (IdentifiableTypeMappingImplementor) bootSubMapping, creationContext  );
		}
	}

	private void generateBootModelForeignKeys(InFlightMetadataCollector mappingMetadata) {
		// walk the boot model and create all mapping FKs (so they are ready for db process)
		throw new NotYetImplementedFor6Exception(  );
	}

	private IdentifiableTypeDescriptor<?> createIdentifiableType(
			IdentifiableTypeMappingImplementor bootMapping,
			RuntimeModelCreationContext creationContext) {
		final IdentifiableTypeDescriptor runtimeType = bootMapping.makeRuntimeDescriptor( creationContext );

		bootByRuntime.put( runtimeType, bootMapping );
		runtimeByBoot.put( bootMapping, runtimeType );

		return runtimeType;
	}

	@SuppressWarnings("unchecked")
	private void finishInitialization(
			IdentifiableTypeDescriptor runtimeType,
			IdentifiableTypeMapping bootType,
			RuntimeModelCreationContext creationContext,
			EntityHierarchyImpl runtimeHierarchy) {
		runtimeType.finishInstantiation(
				runtimeHierarchy,
				runtimeType.getSuperclassType(),
				bootType,
				creationContext
		);

		for ( IdentifiableTypeMapping subTypeMapping : bootType.getSubTypeMappings() ) {
			finishInitialization(
					runtimeByBoot.get( subTypeMapping ),
					subTypeMapping,
					creationContext,
					runtimeHierarchy
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
		public RepresentationStrategySelector getRepresentationStrategySelector() {
			return metadataBuildingContext.getBuildingOptions().getRepresentationStrategySelector();
		}

		@Override
		public PersistentCollectionTuplizerFactory getPersistentCollectionTuplizerFactory() {
			return metadataBuildingContext.getBootstrapContext().getPersistentCollectionTuplizerFactory();
		}

		@Override
		public void registerEntityDescriptor(EntityDescriptor runtimeDescriptor, EntityMapping bootDescriptor) {
			getTypeConfiguration().register( runtimeDescriptor );
			if ( RootClass.class.isInstance( bootDescriptor ) ) {
				final RootClass rootBootMapping = (RootClass) bootDescriptor;
				final AccessType accessType = AccessType.fromExternalName( rootBootMapping.getCacheConcurrencyStrategy() );
				if ( accessType != null ) {
					addEntityCachingConfig( runtimeDescriptor, rootBootMapping, accessType );
				}

				if ( rootBootMapping.getNaturalIdCacheRegionName() != null ) {
					addNaturalIdCachingConfig( runtimeDescriptor, rootBootMapping, accessType );
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
		public void registerCollectionDescriptor(
				PersistentCollectionDescriptor runtimeDescriptor,
				Collection bootDescriptor) {
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
				EmbeddedMapping bootDescriptor) {
			getTypeConfiguration().register( runtimeDescriptor );
		}
	}
}
