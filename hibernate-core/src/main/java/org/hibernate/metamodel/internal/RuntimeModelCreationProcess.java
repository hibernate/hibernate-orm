/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.internal;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cache.cfg.internal.DomainDataRegionConfigImpl;
import org.hibernate.cache.cfg.spi.DomainDataRegionConfig;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;
import org.hibernate.metamodel.model.domain.JpaMetamodel;
import org.hibernate.metamodel.model.domain.internal.DomainMetamodelImpl;
import org.hibernate.metamodel.model.domain.internal.JpaMetamodelImpl;
import org.hibernate.metamodel.spi.DomainMetamodel;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.persister.spi.PersisterCreationContext;
import org.hibernate.persister.spi.PersisterFactory;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.metamodel.internal.JpaStaticMetaModelPopulationSetting.determineJpaMetaModelPopulationSetting;

/**
 * Responsible for interpreting the Hibernate boot metamodel into
 * its runtime metamodel
 *
 * @author Steve Ebersole
 * @see org.hibernate.boot.model
 * @see org.hibernate.metamodel
 */
public class RuntimeModelCreationProcess {

	// todo (6.0) : look at removing reliance on SessionFactory here as well.  Just pass in what we need.
	//  	See RuntimeModelCreationContext.  Ultimately we will need SessionFactory to create the persisters
	//
	// todo (6.0) : ^^ A running list of what we use from SessionFactory:
	//		- ServiceRegistry
	//		- Properties
	//		- Cache (to prime, so we could use a functional interface)
	//		- Database dropping support for auto-schema-tooling (again, could be functional interface - in fact if ultimately is in terms of how we do that)
	//		- Creation of named entity-graphs
	//		- Access to IdentifierGenerators, though we could manage this as part of the DomainMetamodel (and logically maybe that is where it belongs)
	//		- SessionFactoryOptions
	//			- BytecodeProvider
	//			- JpaCompliance
	//
	// Ultimately the idea here is to build the `InflightRuntimeMetamodel` and pass that along to
	//		the JpaMetamodel and DomainMetamodel.  At a high-level; the details may be to instead
	//		build the `InflightRuntimeMetamodel` and use the collected information individually to
	//		each  - e.g.:
	// ````
	//     new JpaMetamodel(
	//     		inflightRuntimeMetamodel.getJpaEntityTypes(),
	//     		inflightRuntimeMetamodel.getJpaEmbeddableTypes,
	//			...
	//	   );
	// ````
	//
	// ^^ Possibly account for either, e.g.:
	// ````
	//	class JpaMetamodelImpl implements JpaMetamodel {
	//		static JpaMetamodelImpl create(
	//				InflightRuntimeMetamodel inflightRuntimeMetamodel,
	//				... ) {
	//			return new JpaMetamodel(
	//					inflightRuntimeMetamodel.getJpaEntityTypes(),
	//					inflightRuntimeMetamodel.getJpaEmbeddableTypes,
	//					...
	//	   		);
	//		}
	//	}
	// ````


	private final SessionFactoryImplementor sessionFactory;
	private TypeConfiguration typeConfiguration;

	public RuntimeModelCreationProcess(
			SessionFactoryImplementor sessionFactory,
			TypeConfiguration typeConfiguration) {
		this.sessionFactory = sessionFactory;
		this.typeConfiguration = typeConfiguration;
	}

	/**
	 * Perform the runtime metamodel creation based on the information obtained during
	 * the first phase of booting, returning the
	 */
	public DomainMetamodel create(
			MetadataImplementor bootMetamodel,
			JpaMetaModelPopulationSetting jpaMetaModelPopulationSetting) {
		final PersisterCreationContext persisterCreationContext = new PersisterCreationContext() {
			@Override
			public SessionFactoryImplementor getSessionFactory() {
				return sessionFactory;
			}

			@Override
			public MetadataImplementor getMetadata() {
				return bootMetamodel;
			}
		};

		final PersisterFactory persisterFactory = sessionFactory.getServiceRegistry()
				.getService( PersisterFactory.class );
		final InflightRuntimeMetamodel inflightRuntimeMetamodel = new InflightRuntimeMetamodel( typeConfiguration );

		primeSecondLevelCacheRegions( bootMetamodel );

		inflightRuntimeMetamodel.processBootMetaModel(
				bootMetamodel,
				sessionFactory.getQueryEngine().getCriteriaBuilder(),
				sessionFactory.getCache(),
				persisterFactory,
				persisterCreationContext,
				jpaMetaModelPopulationSetting,
				determineJpaMetaModelPopulationSetting(
						sessionFactory.getProperties() )

		);

		JpaMetamodel jpaMetamodel = new JpaMetamodelImpl(
				inflightRuntimeMetamodel,
				bootMetamodel.getNamedEntityGraphs().values()
		);

		DomainMetamodelImpl domainMetamodel = new DomainMetamodelImpl(
				sessionFactory,
				inflightRuntimeMetamodel,
				jpaMetamodel
		);
		return domainMetamodel;
	}

	private void primeSecondLevelCacheRegions(MetadataImplementor mappingMetadata) {
		final Map<String, DomainDataRegionConfigImpl.Builder> regionConfigBuilders = new ConcurrentHashMap<>();

		// todo : ultimately this code can be made more efficient when we have a better intrinsic understanding of the hierarchy as a whole

		for ( PersistentClass bootEntityDescriptor : mappingMetadata.getEntityBindings() ) {
			final AccessType accessType = AccessType.fromExternalName( bootEntityDescriptor.getCacheConcurrencyStrategy() );

			if ( accessType != null ) {
				if ( bootEntityDescriptor.isCached() ) {
					regionConfigBuilders.computeIfAbsent(
							bootEntityDescriptor.getRootClass().getCacheRegionName(),
							DomainDataRegionConfigImpl.Builder::new
					)
							.addEntityConfig( bootEntityDescriptor, accessType );
				}

				if ( RootClass.class.isInstance( bootEntityDescriptor )
						&& bootEntityDescriptor.hasNaturalId()
						&& bootEntityDescriptor.getNaturalIdCacheRegionName() != null ) {
					regionConfigBuilders.computeIfAbsent(
							bootEntityDescriptor.getNaturalIdCacheRegionName(),
							DomainDataRegionConfigImpl.Builder::new
					)
							.addNaturalIdConfig( (RootClass) bootEntityDescriptor, accessType );
				}
			}
		}

		for ( Collection collection : mappingMetadata.getCollectionBindings() ) {
			final AccessType accessType = AccessType.fromExternalName( collection.getCacheConcurrencyStrategy() );
			if ( accessType != null ) {
				regionConfigBuilders.computeIfAbsent(
						collection.getCacheRegionName(),
						DomainDataRegionConfigImpl.Builder::new
				)
						.addCollectionConfig( collection, accessType );
			}
		}

		final Set<DomainDataRegionConfig> regionConfigs;
		if ( regionConfigBuilders.isEmpty() ) {
			regionConfigs = Collections.emptySet();
		}
		else {
			regionConfigs = new HashSet<>();
			for ( DomainDataRegionConfigImpl.Builder builder : regionConfigBuilders.values() ) {
				regionConfigs.add( builder.build() );
			}
		}

		sessionFactory.getCache().prime( regionConfigs );
	}
}
