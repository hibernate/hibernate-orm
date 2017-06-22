/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.creation.spi;

import java.util.HashMap;
import java.util.Map;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.NamedSubgraph;
import javax.persistence.metamodel.Attribute;

import org.hibernate.boot.model.domain.EntityMappingHierarchy;
import org.hibernate.boot.model.domain.IdentifiableTypeMapping;
import org.hibernate.boot.model.domain.spi.IdentifiableTypeMappingImplementor;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;
import org.hibernate.cfg.annotations.NamedEntityGraphDefinition;
import org.hibernate.collection.spi.PersistentCollectionTuplizerFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.graph.internal.AbstractAttributeNodeContainer;
import org.hibernate.graph.internal.AttributeNodeImpl;
import org.hibernate.graph.internal.EntityGraphImpl;
import org.hibernate.graph.internal.SubgraphImpl;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.RootClass;
import org.hibernate.metamodel.internal.JpaMetaModelPopulationSetting;
import org.hibernate.metamodel.model.domain.internal.EntityHierarchyImpl;
import org.hibernate.metamodel.model.domain.spi.EmbeddedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.metamodel.model.domain.spi.IdentifiableTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.InstantiatorFactory;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.relational.spi.DatabaseModel;
import org.hibernate.metamodel.model.relational.spi.RuntimeDatabaseModelProducer;
import org.hibernate.sql.NotYetImplementedException;
import org.hibernate.type.spi.TypeConfiguration;

import org.jboss.logging.Logger;

import static org.hibernate.metamodel.internal.JpaMetaModelPopulationSetting.determineJpaMetaModelPopulationSetting;

/**
 * @author Steve Ebersole
 */
public class RuntimeModelCreationProcess {
	private static final Logger log = Logger.getLogger( RuntimeModelCreationProcess.class );

	private final SessionFactoryImplementor sessionFactory;
	private final MetadataBuildingContext metadataBuildingContext;
	private final RuntimeModelDescriptorFactory descriptorFactory;

	private final Map<EntityMappingHierarchy,IdentifiableTypeDescriptor> rootRootByHierarchy = new HashMap<>();
	private final Map<EntityMappingHierarchy,EntityDescriptor> rootEntityByHierarchy = new HashMap<>();

	private final Map<IdentifiableTypeMapping,IdentifiableTypeDescriptor> runtimeByBoot = new HashMap<>();
	private final Map<IdentifiableTypeDescriptor,IdentifiableTypeMapping> bootByRuntime = new HashMap<>();

	public RuntimeModelCreationProcess(
			SessionFactoryImplementor sessionFactory,
			MetadataBuildingContext metadataBuildingContext) {
		this.sessionFactory = sessionFactory;
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

		final RuntimeModelCreationContext creationContext = new RuntimeModelCreationContextImpl(
				mappingMetadata,
				databaseModel,
				dbObjectResolver
		);

		final JpaMetaModelPopulationSetting jpaMetaModelPopulationSetting = determineJpaMetaModelPopulationSetting(
				sessionFactory.getProperties()
		);

		for ( EntityMappingHierarchy bootHierarchy : mappingMetadata.getEntityHierarchies() ) {
			final EntityDescriptor<?> rootEntityDescriptor = (EntityDescriptor<?>) createIdentifiableType(
					bootHierarchy.getRootType(),
					creationContext
			);
			rootEntityByHierarchy.put( bootHierarchy, rootEntityDescriptor );

			walkSupers( bootHierarchy, bootHierarchy.getRootType(), rootEntityDescriptor, creationContext );
			if ( !rootRootByHierarchy.containsKey( bootHierarchy ) ) {
				rootRootByHierarchy.put( bootHierarchy, rootEntityDescriptor );
			}

			walkSubs( bootHierarchy.getRootType(), creationContext );
		}

		for ( Map.Entry<EntityMappingHierarchy, IdentifiableTypeDescriptor> entry : rootRootByHierarchy.entrySet() ) {
			final EntityDescriptor rootEntity = rootEntityByHierarchy.get( entry.getKey() );
			final IdentifiableTypeDescriptor rootRoot = entry.getValue();
			final RootClass rootMapping = (RootClass) bootByRuntime.get( rootEntity );

			// todo (6.0) : these will all change based on the Cache changes planned for 6.0

			final EntityRegionAccessStrategy accessStrategy = resolveEntityCacheAccessStrategy( entry.getKey() );
			final NaturalIdRegionAccessStrategy naturalIdAccessStrategy = resolveNaturalIdCacheAccessStrategy( entry.getKey() );

			final EntityHierarchyImpl runtimeHierarchy = new EntityHierarchyImpl(
					creationContext,
					rootEntity,
					rootMapping,
					accessStrategy,
					naturalIdAccessStrategy
			);

			finishInitialization( rootRoot, bootByRuntime.get( rootRoot ), creationContext, runtimeHierarchy );
		}

		descriptorFactory.finishUp( creationContext );

		mappingMetadata.getNamedEntityGraphs().values().forEach( this::applyNamedEntityGraph );
	}

	private void walkSupers(
			EntityMappingHierarchy bootHierarchy,
			IdentifiableTypeMappingImplementor bootMapping,
			IdentifiableTypeDescriptor<?> runtimeMapping,
			RuntimeModelCreationContext creationContext) {
		assert bootMapping != null;

		if ( bootMapping.getSuperTypeMapping() == null ) {
			rootRootByHierarchy.put( bootHierarchy, runtimeMapping );
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
		throw new NotYetImplementedException(  );
	}

	private EntityRegionAccessStrategy resolveEntityCacheAccessStrategy(EntityMappingHierarchy mappingHierarchy) {
		//final RootClass rootEntityMapping = mappingHierarchy.getRootType();
		//return sessionFactory.getCache().determineEntityRegionAccessStrategy(  );
		return null;
	}

	private NaturalIdRegionAccessStrategy resolveNaturalIdCacheAccessStrategy(EntityMappingHierarchy mappingHierarchy) {
		// atm natural-id caching can only be specified on the root Entity
		//final RootClass rootEntityMapping = mappingHierarchy.getRootType();
		//return sessionFactory.getCache().determineEntityRegionAccessStrategy(  );
		return null;
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
				runtimeType,
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
		private final DatabaseObjectResolutionContextImpl dbObjectResolver;

		public RuntimeModelCreationContextImpl(
				InFlightMetadataCollector mappingMetadata,
				DatabaseModel databaseModel,
				DatabaseObjectResolutionContextImpl dbObjectResolver) {
			this.mappingMetadata = mappingMetadata;
			this.databaseModel = databaseModel;
			this.dbObjectResolver = dbObjectResolver;
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
		public RuntimeModelDescriptorFactory getRuntimeModelDescriptorFactory() {
			return descriptorFactory;
		}

		@Override
		public InstantiatorFactory getInstantiatorFactory() {
			return metadataBuildingContext.getBootstrapContext().getInstantiatorFactory();
		}

		@Override
		public PersistentCollectionTuplizerFactory getPersistentCollectionTuplizerFactory() {
			return metadataBuildingContext.getBootstrapContext().getPersistentCollectionTuplizerFactory();
		}

		@Override
		public void registerEntityDescriptor(EntityDescriptor entityDescriptor) {
			getTypeConfiguration().register( entityDescriptor );
		}

		@Override
		public void registerCollectionDescriptor(PersistentCollectionDescriptor collectionDescriptor) {
			getTypeConfiguration().register( collectionDescriptor );
		}

		@Override
		public void registerEmbeddableDescriptor(EmbeddedTypeDescriptor embeddedTypeDescriptor) {
			getTypeConfiguration().register( embeddedTypeDescriptor );
		}
	}
}
