/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.internal;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.spi.MetamodelImplementor;

import static org.hibernate.metamodel.internal.JpaStaticMetaModelPopulationSetting.determineJpaMetaModelPopulationSetting;

/**
 * Responsible for interpreting the Hibernate boot metamodel into
 * its runtime metamodel
 *
 * @see org.hibernate.boot.model
 * @see org.hibernate.metamodel
 *
 * @author Steve Ebersole
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


	/**
	 * Perform the runtime metamodel creation based on the information obtained during
	 * the first phase of booting, returning the
	 */
	public static MetamodelImplementor execute(
			SessionFactoryImplementor sessionFactory,
			BootstrapContext bootstrapContext,
			MetadataBuildingContext metadataBuildingContext,
			MetadataImplementor bootMetamodel) {
		return new RuntimeModelCreationProcess( sessionFactory, bootstrapContext, metadataBuildingContext ).execute();
	}

	private final SessionFactoryImplementor sessionFactory;
	private final BootstrapContext bootstrapContext;
	private final MetadataBuildingContext metadataBuildingContext;

	public RuntimeModelCreationProcess(
			SessionFactoryImplementor sessionFactory,
			BootstrapContext bootstrapContext,
			MetadataBuildingContext metadataBuildingContext) {
		this.sessionFactory = sessionFactory;
		this.bootstrapContext = bootstrapContext;
		this.metadataBuildingContext = metadataBuildingContext;
	}


	public MetamodelImplementor execute() {
		final InflightRuntimeMetamodel inflightRuntimeMetamodel = new InflightRuntimeMetamodel( bootstrapContext.getTypeConfiguration() );

		final JpaStaticMetaModelPopulationSetting jpaStaticMetaModelPopulationSetting = determineJpaMetaModelPopulationSetting(
				sessionFactory.getProperties()
		);

		throw new NotYetImplementedFor6Exception();
	}
}
