/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.internal;

import java.util.HashMap;
import java.util.Map;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.NamedSubgraph;
import javax.persistence.metamodel.Attribute;

import org.hibernate.HibernateException;
import org.hibernate.boot.model.domain.EntityMapping;
import org.hibernate.boot.model.domain.EntityMappingHierarchy;
import org.hibernate.boot.model.domain.IdentifiableTypeMapping;
import org.hibernate.boot.model.domain.MappedSuperclassMapping;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;
import org.hibernate.cfg.annotations.NamedEntityGraphDefinition;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.jpa.graph.internal.AbstractGraphNode;
import org.hibernate.jpa.graph.internal.AttributeNodeImpl;
import org.hibernate.jpa.graph.internal.EntityGraphImpl;
import org.hibernate.jpa.graph.internal.SubgraphImpl;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;
import org.hibernate.metamodel.internal.JpaMetaModelPopulationSetting;
import org.hibernate.persister.collection.spi.CollectionPersister;
import org.hibernate.persister.embedded.spi.EmbeddedPersister;
import org.hibernate.persister.entity.internal.EntityHierarchyImpl;
import org.hibernate.persister.entity.spi.EntityPersister;
import org.hibernate.persister.entity.spi.IdentifiableTypeImplementor;
import org.hibernate.persister.model.relational.spi.DatabaseModel;
import org.hibernate.persister.model.relational.spi.DatabaseModelProducer;
import org.hibernate.persister.spi.PersisterCreationContext;
import org.hibernate.persister.spi.PersisterFactory;
import org.hibernate.tuple.component.ComponentTuplizerFactory;
import org.hibernate.tuple.entity.EntityTuplizerFactory;
import org.hibernate.type.spi.DatabaseObjectResolver;
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
	private final PersisterFactory persisterFactory;

	private final Map<EntityMappingHierarchy,IdentifiableTypeImplementor> rootRootByHierarchy = new HashMap<>();
	private final Map<EntityMappingHierarchy,EntityPersister> rootEntityByHierarchy = new HashMap<>();

	private final Map<IdentifiableTypeMapping,IdentifiableTypeImplementor> runtimeByBoot = new HashMap<>();
	private final Map<IdentifiableTypeImplementor,IdentifiableTypeMapping> bootByRuntime = new HashMap<>();

	public RuntimeModelCreationProcess(
			SessionFactoryImplementor sessionFactory,
			MetadataBuildingContext metadataBuildingContext) {
		this.sessionFactory = sessionFactory;
		this.metadataBuildingContext = metadataBuildingContext;

		this.persisterFactory = sessionFactory.getServiceRegistry().getService( PersisterFactory.class );
	}

	public void execute() {
		final InFlightMetadataCollector mappingMetadata = metadataBuildingContext.getMetadataCollector();

		final DatabaseObjectResolutionContextImpl dbObjectResolver = new DatabaseObjectResolutionContextImpl();
		final DatabaseModel databaseModel = new DatabaseModelProducer( metadataBuildingContext.getBootstrapContext() ).produceDatabaseModel(
				mappingMetadata.getDatabase(),
				dbObjectResolver
		);

		final PersisterCreationContext creationContext = new PersisterCreationContextImpl(
				mappingMetadata,
				databaseModel,
				dbObjectResolver
		);

		final JpaMetaModelPopulationSetting jpaMetaModelPopulationSetting = determineJpaMetaModelPopulationSetting(
				sessionFactory.getProperties()
		);

		for ( EntityMappingHierarchy entityHierarchy : mappingMetadata.getEntityHierarchies() ) {
			final IdentifiableTypeImplementor superType = resolveSuper( entityHierarchy.getRootType(), creationContext );
			rootRootByHierarchy.put( entityHierarchy, superType );

			final EntityPersister<?> rootEntityPersister = (EntityPersister<?>) createIdentifiableType(
					entityHierarchy.getRootType(),
					creationContext
			);
			rootEntityByHierarchy.put( entityHierarchy, rootEntityPersister );

			rootRootByHierarchy.put( entityHierarchy, superType );
			rootEntityByHierarchy.put( entityHierarchy, rootEntityPersister );

		}

		for ( Map.Entry<EntityMappingHierarchy, IdentifiableTypeImplementor> entry : rootRootByHierarchy.entrySet() ) {
			final EntityPersister rootEntity = rootEntityByHierarchy.get( entry.getKey() );
			final IdentifiableTypeImplementor rootRoot = rootRootByHierarchy.get( entry.getKey() );
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

		persisterFactory.finishUp( creationContext );

		mappingMetadata.getNamedEntityGraphs().values().forEach( this::applyNamedEntityGraph );
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

	private IdentifiableTypeImplementor<?> createIdentifiableType(
			IdentifiableTypeMapping bootMapping,
			PersisterCreationContext creationContext) {
		final IdentifiableTypeImplementor runtimeType;
		if ( bootMapping instanceof PersistentClass ) {
			runtimeType = creationContext.getPersisterFactory().createEntityPersister(
					(EntityMapping) bootMapping,
					null,
					null,
					creationContext
			);
		}
		else {
			runtimeType = creationContext.getPersisterFactory().createMappedSuperclass(
					(MappedSuperclassMapping) bootMapping,
					null,
					null,
					creationContext
			);
		}

		bootByRuntime.put( runtimeType, bootMapping );
		runtimeByBoot.put( bootMapping, runtimeType );

		return runtimeType;
	}

	private void finishInitialization(
			IdentifiableTypeImplementor runtimeType,
			IdentifiableTypeMapping bootType,
			PersisterCreationContext creationContext,
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

	private IdentifiableTypeImplementor resolveSuper(
			IdentifiableTypeMapping mappingType,
			PersisterCreationContext creationContext) {
		if ( mappingType == null ) {
			return null;
		}

		if ( mappingType.getSuperTypeMapping() == null ) {
			return null;
		}

		// it should be an error for the runtime type to already exist here...
		if ( runtimeByBoot.containsKey( mappingType ) ) {
			throw new HibernateException( "Duplicate mapping-type to runtime-type resolution : " + mappingType );
		}

		return createIdentifiableType( mappingType, creationContext );
	}

	private void applyNamedEntityGraph(NamedEntityGraphDefinition definition) {
		log.debugf(
				"Applying named entity graph [name=%s, entity-name=%s, jpa-entity-name=%s",
				definition.getRegisteredName(),
				definition.getEntityName(),
				definition.getJpaEntityName()
		);

		final EntityPersister<?> entityPersister = metadataBuildingContext.getBootstrapContext()
				.getTypeConfiguration()
				.findEntityPersister( definition.getEntityName() );
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

	private class PersisterCreationContextImpl implements PersisterCreationContext {
		private final InFlightMetadataCollector mappingMetadata;
		private final DatabaseModel databaseModel;
		private final DatabaseObjectResolutionContextImpl dbObjectResolver;

		public PersisterCreationContextImpl(
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
		public PersisterFactory getPersisterFactory() {
			return persisterFactory;
		}

		@Override
		public EntityTuplizerFactory getEntityTuplizerFactory() {
			return metadataBuildingContext.getBootstrapContext().getEntityTuplizerFactory();
		}

		@Override
		public ComponentTuplizerFactory getComponentTuplizerFactory() {
			return metadataBuildingContext.getBootstrapContext().getComponentTuplizerFactory();
		}

		@Override
		public void registerEntityPersister(EntityPersister entityPersister) {
			getTypeConfiguration().register( entityPersister );
		}

		@Override
		public void registerCollectionPersister(CollectionPersister collectionPersister) {
			getTypeConfiguration().register( collectionPersister );
		}

		@Override
		public void registerEmbeddablePersister(EmbeddedPersister embeddablePersister) {
			getTypeConfiguration().register( embeddablePersister );
		}
	}
}
