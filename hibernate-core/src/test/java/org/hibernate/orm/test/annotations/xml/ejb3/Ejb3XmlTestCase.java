/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.xml.ejb3;

import org.hibernate.boot.internal.MetadataBuilderImpl;
import org.hibernate.boot.internal.RootMappingDefaults;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.model.source.internal.annotations.AdditionalManagedResourcesImpl;
import org.hibernate.boot.models.internal.DomainModelCategorizationCollector;
import org.hibernate.boot.models.internal.GlobalRegistrationsImpl;
import org.hibernate.boot.models.internal.OrmAnnotationHelper;
import org.hibernate.boot.models.xml.internal.PersistenceUnitMetadataImpl;
import org.hibernate.boot.models.xml.spi.XmlPreProcessingResult;
import org.hibernate.boot.models.xml.spi.XmlPreProcessor;
import org.hibernate.boot.models.xml.spi.XmlProcessingResult;
import org.hibernate.boot.models.xml.spi.XmlProcessor;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.models.internal.BasicModelBuildingContextImpl;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.MethodDetails;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.hibernate.testing.boot.BootstrapContextImpl;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.After;
import org.junit.Before;

import jakarta.persistence.Transient;

import static org.hibernate.models.internal.SimpleClassLoading.SIMPLE_CLASS_LOADING;

/**
 * Test superclass to provide utility methods for testing the mapping of JPA
 * XML to JPA annotations.  The configuration is built within each test, and no
 * database is used.  Thus, no schema generation or cleanup will be performed.
 */
public abstract class Ejb3XmlTestCase extends BaseUnitTestCase {
	private BootstrapContextImpl bootstrapContext;

	protected Ejb3XmlTestCase() {
	}

	@Before
	public void init() {
		bootstrapContext = new BootstrapContextImpl();
	}

	@After
	public void destroy() {
		bootstrapContext.close();
	}

	protected MemberDetails getAttributeMember(Class<?> entityClass, String fieldName, String xmlResourceName) {
		final ClassDetails classDetails = getClassDetails( entityClass, xmlResourceName );
		final FieldDetails fieldByName = classDetails.findFieldByName( fieldName );
		if ( !fieldByName.getDirectAnnotationUsages().isEmpty() ) {
			if ( !fieldByName.hasDirectAnnotationUsage( Transient.class ) ) {
				return fieldByName;
			}
		}

		// look for the getter
		for ( MethodDetails method : classDetails.getMethods() ) {
			if ( method.getMethodKind() == MethodDetails.MethodKind.GETTER
					&& fieldName.equals( method.resolveAttributeName() ) ) {
				if ( !method.hasDirectAnnotationUsage( Transient.class ) ) {
					return method;
				}
			}
		}

		throw new IllegalStateException( "Unable to locate persistent attribute : " + fieldName );
	}

	private SourceModelBuildingContext sourceModelContext;

	protected ClassDetails getClassDetails(Class<?> entityClass, String xmlResourceName) {
		final ManagedResources managedResources = new AdditionalManagedResourcesImpl.Builder().addLoadedClasses( entityClass )
				.addXmlMappings( "org/hibernate/orm/test/annotations/xml/ejb3/" + xmlResourceName )
				.build();
		final PersistenceUnitMetadataImpl persistenceUnitMetadata = new PersistenceUnitMetadataImpl();
		final XmlPreProcessingResult xmlPreProcessingResult = XmlPreProcessor.preProcessXmlResources(
				managedResources,
				persistenceUnitMetadata
		);

		final SourceModelBuildingContext modelBuildingContext = new BasicModelBuildingContextImpl(
				SIMPLE_CLASS_LOADING,
				(contributions, inFlightContext) -> {
					OrmAnnotationHelper.forEachOrmAnnotation( contributions::registerAnnotation );
				}
		);
		final BootstrapContext bootstrapContext = new BootstrapContextImpl();
		final GlobalRegistrationsImpl globalRegistrations = new GlobalRegistrationsImpl(
				modelBuildingContext,
				bootstrapContext
		);

		final DomainModelCategorizationCollector modelCategorizationCollector = new DomainModelCategorizationCollector(
				true,
				globalRegistrations,
				modelBuildingContext
		);


		final RootMappingDefaults rootMappingDefaults = new RootMappingDefaults(
				new MetadataBuilderImpl.MappingDefaultsImpl( new StandardServiceRegistryBuilder().build() ),
				persistenceUnitMetadata
		);

		final XmlProcessingResult xmlProcessingResult = XmlProcessor.processXml(
				xmlPreProcessingResult,
				modelCategorizationCollector,
				modelBuildingContext,
				bootstrapContext,
				rootMappingDefaults
		);

		xmlProcessingResult.apply( persistenceUnitMetadata );

		return modelBuildingContext.getClassDetailsRegistry().resolveClassDetails( entityClass.getName() );
	}

}
