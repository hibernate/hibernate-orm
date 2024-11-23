/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.junit;

import java.util.Iterator;
import java.util.Locale;
import java.util.Optional;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.internal.MetadataBuilderImpl;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;

import org.hibernate.testing.orm.domain.DomainModelDescriptor;
import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.junit.platform.commons.support.AnnotationSupport;

/**
 * hibernate-testing implementation of a few JUnit5 contracts to support SessionFactory-based testing,
 * including argument injection (or see {@link DomainModelScopeAware})
 *
 * @see ServiceRegistryScope
 * @see DomainModelExtension
 *
 * @author Steve Ebersole
 */
public class DomainModelExtension
		implements TestInstancePostProcessor, AfterAllCallback, TestExecutionExceptionHandler {

	private static final String MODEL_KEY = MetadataImplementor.class.getName();

	private static ExtensionContext.Store locateExtensionStore(Object testInstance, ExtensionContext context) {
		return JUnitHelper.locateExtensionStore( ServiceRegistryExtension.class, context, testInstance );
	}

	public static DomainModelScope findDomainModelScope(Object testInstance, ExtensionContext context) {
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

				final Optional<DomainModel> domainModelAnnotationWrapper = AnnotationSupport.findAnnotation(
						context.getElement().get(),
						DomainModel.class
				);

				if ( !domainModelAnnotationWrapper.isPresent() ) {
					throw new RuntimeException( "Could not locate @DomainModel annotation : " + context.getDisplayName() );
				}

				final DomainModel domainModelAnnotation = domainModelAnnotationWrapper.get();

				final MetadataSources metadataSources = new MetadataSources( serviceRegistry );
				final ManagedBeanRegistry managedBeanRegistry = serviceRegistry.getService( ManagedBeanRegistry.class );

				for ( String annotatedPackageName : domainModelAnnotation.annotatedPackageNames() ) {
					metadataSources.addPackage( annotatedPackageName );
				}

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

				final MetadataBuilderImpl metadataBuilder = (MetadataBuilderImpl) metadataSources.getMetadataBuilder();

				for ( Class<? extends TypeContributor> contributorType : domainModelAnnotation.typeContributors() ) {
					final TypeContributor contributor = managedBeanRegistry.getBean( contributorType ).getBeanInstance();
					contributor.contribute( metadataBuilder, serviceRegistry );
				}

				MetadataImplementor metadataImplementor = metadataBuilder.build();
				applyCacheSettings(
						metadataImplementor,
						domainModelAnnotation.overrideCacheStrategy(),
						domainModelAnnotation.concurrencyStrategy()
				);

				return metadataImplementor;
			};
		}

		final DomainModelScopeImpl scope = new DomainModelScopeImpl( serviceRegistryScope, modelProducer );

		if ( testInstance instanceof DomainModelScopeAware ) {
			( (DomainModelScopeAware) testInstance ).injectTestModelScope( scope );
		}

		locateExtensionStore( testInstance, context ).put( MODEL_KEY, scope );

		return scope;
	}

	protected static final void applyCacheSettings(Metadata metadata, boolean overrideCacheStrategy, String cacheConcurrencyStrategy) {
		if ( !overrideCacheStrategy ) {
			return;
		}

		if ( cacheConcurrencyStrategy.equals( "" ) ) {
			return;
		}

		for ( PersistentClass entityBinding : metadata.getEntityBindings() ) {
			if ( entityBinding.isInherited() ) {
				continue;
			}

			boolean hasLob = false;

			final Iterator props = entityBinding.getPropertyClosureIterator();
			while ( props.hasNext() ) {
				final Property prop = (Property) props.next();
				if ( prop.getValue().isSimpleValue() ) {
					if ( isLob( (SimpleValue) prop.getValue() ) ) {
						hasLob = true;
						break;
					}
				}
			}

			if ( !hasLob ) {
				( (RootClass) entityBinding ).setCacheConcurrencyStrategy( cacheConcurrencyStrategy );
				entityBinding.setCached( true );
			}
		}

		for ( Collection collectionBinding : metadata.getCollectionBindings() ) {
			boolean isLob = false;

			if ( collectionBinding.getElement().isSimpleValue() ) {
				isLob = isLob( (SimpleValue) collectionBinding.getElement() );
			}

			if ( !isLob ) {
				collectionBinding.setCacheConcurrencyStrategy( cacheConcurrencyStrategy );
			}
		}
	}

	private static boolean isLob(SimpleValue value) {
		final String typeName = value.getTypeName();
		if ( typeName != null ) {
			String significantTypeNamePart = typeName.substring( typeName.lastIndexOf( '.' ) + 1 )
					.toLowerCase( Locale.ROOT );
			switch ( significantTypeNamePart ) {
				case "blob":
				case "blobtype":
				case "clob":
				case "clobtype":
				case "nclob":
				case "nclobtype":
					return true;
			}
		}
		return false;
	}

	@Override
	public void postProcessTestInstance(Object testInstance, ExtensionContext context) {
		findDomainModelScope( testInstance, context );
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

	protected void afterMetadataBuilt(Metadata metadata) {
	}

}
