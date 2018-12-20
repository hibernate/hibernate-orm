/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm;

import java.util.Optional;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.spi.MetadataImplementor;

import org.hibernate.testing.orm.domain.DomainModelDescriptor;
import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.junit.platform.commons.support.AnnotationSupport;

/**
 * @author Steve Ebersole
 */
public class TestDomainExtension implements TestInstancePostProcessor, AfterAllCallback {
	private static final String MODEL_KEY = Metadata.class.getName();


	private static ExtensionContext.Store locateExtensionStore(Object testInstance, ExtensionContext context) {
		return JUnitHelper.locateExtensionStore( ServiceRegistryExtension.class, context, testInstance );
	}

	public static Optional<MetadataImplementor> findMetamodel(Object testInstance, ExtensionContext context) {
		final ExtensionContext.Store store = locateExtensionStore( testInstance, context );
		return Optional.ofNullable( (MetadataImplementor) store.get( MODEL_KEY ) );
	}


	@Override
	public void postProcessTestInstance(Object testInstance, ExtensionContext context) throws Exception {
		if ( !context.getElement().isPresent() ) {
			throw new RuntimeException( "Unable to determine how to handle given ExtensionContext : " + context.getDisplayName() );
		}

		final Optional<TestDomain> testDomainAnnotationWrapper = AnnotationSupport.findAnnotation(
				context.getElement().get(),
				TestDomain.class
		);

		if ( ! testDomainAnnotationWrapper.isPresent() ) {
			throw new RuntimeException( "Could not locate @TestDomain annotation : " + context.getDisplayName() );
		}

		final TestDomain testDomainAnnotation = testDomainAnnotationWrapper.get();

		final Optional<StandardServiceRegistry> serviceRegistry = ServiceRegistryExtension.findServiceRegistry( testInstance, context );
		final StandardServiceRegistry registry = serviceRegistry.orElseThrow(
				() -> new IllegalStateException( "Expecting to find StandardServiceRegistry" )
		);

		final MetadataSources metadataSources = new MetadataSources( registry );

		for ( StandardDomainModel standardDomainModel : testDomainAnnotation.standardModels() ) {
			standardDomainModel.getDescriptor().applyDomainModel( metadataSources );
		}

		for ( Class<? extends DomainModelDescriptor> modelDescriptorClass : testDomainAnnotation.modelDescriptorClasses() ) {
			final DomainModelDescriptor modelDescriptor = modelDescriptorClass.newInstance();
			modelDescriptor.applyDomainModel( metadataSources );
		}

		for ( Class annotatedClass : testDomainAnnotation.annotatedClasses() ) {
			metadataSources.addAnnotatedClass( annotatedClass );
		}

		for ( String annotatedClassName : testDomainAnnotation.annotatedClassNames() ) {
			metadataSources.addAnnotatedClassName( annotatedClassName );
		}

		for ( String xmlMapping : testDomainAnnotation.xmlMappings() ) {
			metadataSources.addResource( xmlMapping );
		}

		final MetadataImplementor model = (MetadataImplementor) metadataSources.buildMetadata();

		locateExtensionStore( testInstance, context ).put( MODEL_KEY, model );

		if ( testInstance instanceof TestDomainAware ) {
			( (TestDomainAware) testInstance ).injectTestModel( model );
		}
	}

	@Override
	public void afterAll(ExtensionContext context) {
		final ExtensionContext.Store store = locateExtensionStore( context.getRequiredTestInstance(), context );
		store.remove( MODEL_KEY );
	}

}
