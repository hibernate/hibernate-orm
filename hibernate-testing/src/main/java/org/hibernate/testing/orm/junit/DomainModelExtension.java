/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.junit;

import java.util.Optional;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.spi.MetadataImplementor;

import org.hibernate.testing.orm.domain.DomainModelDescriptor;
import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.junit.platform.commons.support.AnnotationSupport;

/**
 * @author Steve Ebersole
 */
public class DomainModelExtension
		implements TestInstancePostProcessor, AfterAllCallback, TestExecutionExceptionHandler {

	private static final String MODEL_KEY = MetadataImplementor.class.getName();

	private static ExtensionContext.Store locateExtensionStore(Object testInstance, ExtensionContext context) {
		return JUnitHelper.locateExtensionStore( ServiceRegistryExtension.class, context, testInstance );
	}

	public static DomainModelScope findMetamodelScope(Object testInstance, ExtensionContext context) {
		final ExtensionContext.Store store = locateExtensionStore( testInstance, context );
		final DomainModelScope existing = (DomainModelScope) store.get( MODEL_KEY );
		if ( existing != null ) {
			return existing;
		}


		final ServiceRegistryScope serviceRegistryScope = ServiceRegistryExtension.findServiceRegistryScope(
				testInstance,
				context
		);

		final DomainModelProducer modelProducer;

		if ( testInstance instanceof DomainModelProducer ) {
			modelProducer = (DomainModelProducer) testInstance;
		}
		else {
			modelProducer = serviceRegistry -> {
				if ( !context.getElement().isPresent() ) {
					throw new RuntimeException( "Unable to determine how to handle given ExtensionContext : " + context.getDisplayName() );
				}

				final Optional<DomainModel> testDomainAnnotationWrapper = AnnotationSupport.findAnnotation(
						context.getElement().get(),
						DomainModel.class
				);

				if ( !testDomainAnnotationWrapper.isPresent() ) {
					throw new RuntimeException( "Could not locate @TestDomain annotation : " + context.getDisplayName() );
				}

				final DomainModel domainModelAnnotation = testDomainAnnotationWrapper.get();

				final MetadataSources metadataSources = new MetadataSources( serviceRegistry );

				for ( StandardDomainModel standardDomainModel : domainModelAnnotation.standardModels() ) {
					standardDomainModel.getDescriptor().applyDomainModel( metadataSources );
				}

				for ( Class<? extends DomainModelDescriptor> modelDescriptorClass : domainModelAnnotation.modelDescriptorClasses() ) {
					try {
						final DomainModelDescriptor modelDescriptor = modelDescriptorClass.newInstance();
						modelDescriptor.applyDomainModel( metadataSources );
					}
					catch (IllegalAccessException | InstantiationException e) {
						throw new RuntimeException( "Error instantiating DomainModelDescriptor - " + modelDescriptorClass.getName(), e );
					}
				}

				for ( Class annotatedClass : domainModelAnnotation.annotatedClasses() ) {
					metadataSources.addAnnotatedClass( annotatedClass );
				}

				for ( String annotatedClassName : domainModelAnnotation.annotatedClassNames() ) {
					metadataSources.addAnnotatedClassName( annotatedClassName );
				}

				for ( String xmlMapping : domainModelAnnotation.xmlMappings() ) {
					metadataSources.addResource( xmlMapping );
				}

				for ( DomainModel.ExtraQueryImport extraQueryImport : domainModelAnnotation.extraQueryImports() ) {
					metadataSources.addQueryImport( extraQueryImport.name(), extraQueryImport.importedClass() );
				}

				for ( Class<?> importedClass : domainModelAnnotation.extraQueryImportClasses() ) {
					metadataSources.addQueryImport( importedClass.getSimpleName(), importedClass );
				}

				return (MetadataImplementor) metadataSources.buildMetadata();
			};
		}

		final DomainModelScopeImpl scope = new DomainModelScopeImpl( serviceRegistryScope, modelProducer );

		if ( testInstance instanceof DomainModelScopeAware ) {
			( (DomainModelScopeAware) testInstance ).injectTestModelScope( scope );
		}

		locateExtensionStore( testInstance, context ).put( MODEL_KEY, scope );

		return scope;
	}

	@Override
	public void postProcessTestInstance(Object testInstance, ExtensionContext context) {
		findMetamodelScope( testInstance, context );
	}

	@Override
	public void afterAll(ExtensionContext context) {
		final ExtensionContext.Store store = locateExtensionStore( context.getRequiredTestInstance(), context );
		final DomainModelScopeImpl scope = (DomainModelScopeImpl) store.remove( MODEL_KEY );

		if ( scope != null ) {
			scope.close();
		}
	}

	@Override
	public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
		final ExtensionContext.Store store = locateExtensionStore( context.getRequiredTestInstance(), context );
		final DomainModelScopeImpl scope = (DomainModelScopeImpl) store.get( MODEL_KEY );

		if ( scope != null ) {
			scope.releaseModel();
		}

		throw throwable;
	}

	public static class DomainModelScopeImpl implements DomainModelScope, ExtensionContext.Store.CloseableResource {
		private final ServiceRegistryScope serviceRegistryScope;
		private final DomainModelProducer producer;

		private MetadataImplementor model;
		private boolean active = true;

		public DomainModelScopeImpl(
				ServiceRegistryScope serviceRegistryScope,
				DomainModelProducer producer) {
			this.serviceRegistryScope = serviceRegistryScope;
			this.producer = producer;

			this.model = createDomainModel();
		}

		private MetadataImplementor createDomainModel() {
			verifyActive();

			final StandardServiceRegistry registry = serviceRegistryScope.getRegistry();
			model = producer.produceModel( registry );

			return model;
		}

		@Override
		public MetadataImplementor getDomainModel() {
			verifyActive();

			if ( model == null ) {
				model = createDomainModel();
			}
			return model;
		}

		private void verifyActive() {
			if ( !active ) {
				throw new RuntimeException( "DomainModelScope no longer active" );
			}
		}


		@Override
		public void close() {
			active = false;
			releaseModel();
		}

		public void releaseModel() {
			model = null;
		}
	}
}
