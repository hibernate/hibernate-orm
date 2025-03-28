/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.model.source.internal.annotations;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.common.reflection.MetadataProviderInjector;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.boot.internal.MetadataBuildingContextRootImpl;
import org.hibernate.boot.jaxb.mapping.JaxbEntityMappings;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.model.convert.spi.ConverterRegistry;
import org.hibernate.boot.model.internal.AnnotationBinder;
import org.hibernate.boot.model.internal.InheritanceState;
import org.hibernate.boot.model.internal.JPAXMLOverriddenMetadataProvider;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.model.source.spi.MetadataSourceProcessor;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.JpaOrmXmlPersistenceUnitDefaultAware;
import org.hibernate.boot.spi.JpaOrmXmlPersistenceUnitDefaultAware.JpaOrmXmlPersistenceUnitDefaults;
import org.hibernate.boot.spi.MetadataBuildingOptions;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;

import static org.hibernate.boot.model.internal.AnnotationBinder.bindFilterDefs;

/**
 * @author Steve Ebersole
 */
public class AnnotationMetadataSourceProcessorImpl implements MetadataSourceProcessor {
	private static final Logger log = Logger.getLogger( AnnotationMetadataSourceProcessorImpl.class );

	private final MetadataBuildingContextRootImpl rootMetadataBuildingContext;

	@SuppressWarnings({ "FieldCanBeLocal", "unused" })
	private final IndexView jandexView;

	private final ReflectionManager reflectionManager;

	private final LinkedHashSet<String> annotatedPackages = new LinkedHashSet<>();

	private final List<XClass> xClasses = new ArrayList<>();
	private final ClassLoaderService classLoaderService;

	/**
	 * Normal constructor used while processing {@linkplain org.hibernate.boot.MetadataSources mapping sources}
	 */
	public AnnotationMetadataSourceProcessorImpl(
			ManagedResources managedResources,
			final MetadataBuildingContextRootImpl rootMetadataBuildingContext,
			IndexView jandexView) {
		this.rootMetadataBuildingContext = rootMetadataBuildingContext;
		this.jandexView = jandexView;

		this.reflectionManager = rootMetadataBuildingContext.getBootstrapContext().getReflectionManager();

		if ( CollectionHelper.isNotEmpty( managedResources.getAnnotatedPackageNames() ) ) {
			annotatedPackages.addAll( managedResources.getAnnotatedPackageNames() );
		}

		final ConverterRegistry converterRegistry =
				rootMetadataBuildingContext.getMetadataCollector().getConverterRegistry();
		this.classLoaderService =
				rootMetadataBuildingContext.getBuildingOptions().getServiceRegistry()
						.requireService( ClassLoaderService.class );

		MetadataBuildingOptions metadataBuildingOptions = rootMetadataBuildingContext.getBuildingOptions();
		if ( metadataBuildingOptions.isXmlMappingEnabled() ) {
			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			// Ewww.  This is temporary until we migrate to Jandex + StAX for annotation binding
			final JPAXMLOverriddenMetadataProvider jpaMetadataProvider = (JPAXMLOverriddenMetadataProvider)
					( (MetadataProviderInjector) reflectionManager ).getMetadataProvider();
			for ( Binding<?> xmlBinding : managedResources.getXmlMappingBindings() ) {
				Object root = xmlBinding.getRoot();
				if ( !( root instanceof JaxbEntityMappings ) ) {
					continue;
				}
				final JaxbEntityMappings entityMappings = (JaxbEntityMappings) xmlBinding.getRoot();

				final List<String> classNames = jpaMetadataProvider.getXMLContext().addDocument( entityMappings );
				for ( String className : classNames ) {
					xClasses.add( toXClass( className, reflectionManager, classLoaderService ) );
				}
			}
			jpaMetadataProvider.getXMLContext().applyDiscoveredAttributeConverters( converterRegistry );
			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		}

		for ( String className : managedResources.getAnnotatedClassNames() ) {
			final Class<?> annotatedClass = classLoaderService.classForName( className );
			categorizeAnnotatedClass( annotatedClass, converterRegistry );
		}

		for ( Class<?> annotatedClass : managedResources.getAnnotatedClassReferences() ) {
			categorizeAnnotatedClass( annotatedClass, converterRegistry );
		}
	}

	/**
	 * Used as part of processing
	 * {@linkplain org.hibernate.boot.spi.AdditionalMappingContributions#contributeEntity(Class)} "additional" mappings}
	 */
	public static void processAdditionalMappings(
			List<Class<?>> additionalClasses,
			List<JaxbEntityMappings> additionalJaxbMappings,
			MetadataBuildingContextRootImpl rootMetadataBuildingContext) {
		final AnnotationMetadataSourceProcessorImpl processor = new AnnotationMetadataSourceProcessorImpl( rootMetadataBuildingContext );

		if ( additionalJaxbMappings != null && rootMetadataBuildingContext.getBuildingOptions().isXmlMappingEnabled() ) {
			final ConverterRegistry converterRegistry = rootMetadataBuildingContext.getMetadataCollector().getConverterRegistry();
			final MetadataProviderInjector injector = (MetadataProviderInjector) processor.reflectionManager;
			final JPAXMLOverriddenMetadataProvider metadataProvider = (JPAXMLOverriddenMetadataProvider) injector.getMetadataProvider();

			for ( int i = 0; i < additionalJaxbMappings.size(); i++ ) {
				final List<String> classNames = metadataProvider.getXMLContext().addDocument( additionalJaxbMappings.get( i ) );
				for ( String className : classNames ) {
					final XClass xClass = processor.toXClass( className, processor.reflectionManager, processor.classLoaderService );
					processor.xClasses.add( xClass );
				}
			}
			metadataProvider.getXMLContext().applyDiscoveredAttributeConverters( converterRegistry );
		}

		for ( int i = 0; i < additionalClasses.size(); i++ ) {
			final XClass xClass = processor.reflectionManager.toXClass( additionalClasses.get( i ) );
			if ( !xClass.isAnnotationPresent( Entity.class ) ) {
				log.debugf( "@Entity not found on additional entity class - `%s`" );
				continue;
			}
			processor.xClasses.add( xClass );
		}

		processor.processEntityHierarchies( new LinkedHashSet<>() );
	}

	/**
	 * Form used from {@link #processAdditionalMappings}
	 */
	private AnnotationMetadataSourceProcessorImpl(MetadataBuildingContextRootImpl rootMetadataBuildingContext) {
		this.rootMetadataBuildingContext = rootMetadataBuildingContext;
		this.jandexView = null;

		this.reflectionManager = rootMetadataBuildingContext.getBootstrapContext().getReflectionManager();
		this.classLoaderService = rootMetadataBuildingContext.getBuildingOptions().getServiceRegistry().getService( ClassLoaderService.class );
	}

	private void categorizeAnnotatedClass(Class<?> annotatedClass, ConverterRegistry converterRegistry) {
		final XClass xClass = reflectionManager.toXClass( annotatedClass );
		// categorize it, based on assumption it does not fall into multiple categories
		if ( xClass.isAnnotationPresent( Converter.class ) ) {
			//noinspection unchecked
			converterRegistry.addAttributeConverter( (Class<? extends AttributeConverter<?,?>>) annotatedClass );
		}
		else {
			xClasses.add( xClass );
		}
	}

	private XClass toXClass(String className, ReflectionManager reflectionManager, ClassLoaderService cls) {
		return reflectionManager.toXClass( cls.classForName( className ) );
	}

	@Override
	public void prepare() {
		// use any persistence-unit-defaults defined in orm.xml
		( (JpaOrmXmlPersistenceUnitDefaultAware) rootMetadataBuildingContext.getBuildingOptions() ).apply(
				new JpaOrmXmlPersistenceUnitDefaults() {
					final Map<?,?> persistenceUnitDefaults = reflectionManager.getDefaults();

					@Override
					public String getDefaultSchemaName() {
						return StringHelper.nullIfEmpty( (String) persistenceUnitDefaults.get( "schema" ) );
					}

					@Override
					public String getDefaultCatalogName() {
						return StringHelper.nullIfEmpty( (String) persistenceUnitDefaults.get( "catalog" ) );
					}

					@Override
					public boolean shouldImplicitlyQuoteIdentifiers() {
						final Object isDelimited = persistenceUnitDefaults.get( "delimited-identifier" );
						return isDelimited == Boolean.TRUE;
					}
				}
		);

		rootMetadataBuildingContext.getMetadataCollector().getDatabase().adjustDefaultNamespace(
				rootMetadataBuildingContext.getBuildingOptions().getMappingDefaults().getImplicitCatalogName(),
				rootMetadataBuildingContext.getBuildingOptions().getMappingDefaults().getImplicitSchemaName()
		);

		AnnotationBinder.bindDefaults( rootMetadataBuildingContext );
		for ( String annotatedPackage : annotatedPackages ) {
			AnnotationBinder.bindPackage( classLoaderService, annotatedPackage, rootMetadataBuildingContext );
		}
	}

	@Override
	public void processTypeDefinitions() {
	}

	@Override
	public void processQueryRenames() {
	}

	@Override
	public void processNamedQueries() {
	}

	@Override
	public void processAuxiliaryDatabaseObjectDefinitions() {
	}

	@Override
	public void processIdentifierGenerators() {
	}

	@Override
	public void processFilterDefinitions() {
	}

	@Override
	public void processFetchProfiles() {
	}

	@Override
	public void prepareForEntityHierarchyProcessing() {
	}

	@Override
	public void processEntityHierarchies(Set<String> processedEntityNames) {
		final List<XClass> orderedClasses = orderAndFillHierarchy( xClasses );
		Map<XClass, InheritanceState> inheritanceStatePerClass = AnnotationBinder.buildInheritanceStates(
				orderedClasses,
				rootMetadataBuildingContext
		);
		// we want to go through all classes and collect the filter definitions first,
		//  so that when we bind the classes we have the complete list of filters to search from:
		for ( XClass clazz : orderedClasses ) {
			if ( !processedEntityNames.contains( clazz.getName() ) ) {
				bindFilterDefs( clazz, rootMetadataBuildingContext );
			}
		}

		for ( XClass clazz : orderedClasses ) {
			if ( processedEntityNames.contains( clazz.getName() ) ) {
				log.debugf( "Skipping annotated class processing of entity [%s], as it has already been processed", clazz );
			}
			else {
				AnnotationBinder.bindClass( clazz, inheritanceStatePerClass, rootMetadataBuildingContext );
				AnnotationBinder.bindFetchProfilesForClass( clazz, rootMetadataBuildingContext );
				processedEntityNames.add( clazz.getName() );
			}
		}
	}

	/**
	 * @return a partially ordered list so entry's ancestors always show up earlier
	 */
	private List<XClass> orderAndFillHierarchy(List<XClass> classes) {
		final boolean debug = log.isDebugEnabled();

		LinkedHashSet<XClass> orderedClasses = CollectionHelper.linkedSetOfSize( classes.size() * 2 );
		List<XClass> clazzHierarchy = new ArrayList<>();

		for ( XClass clazz : classes ) {
			if ( clazz.isAnnotationPresent( MappedSuperclass.class ) ) {
				if ( debug ) {
					log.debugf(
							"Skipping explicit MappedSuperclass %s, the class will be discovered analyzing the implementing class",
							clazz
					);
				}
			}
			else {
				if ( orderedClasses.contains( clazz ) ) {
					continue;
				}

				clazzHierarchy.clear();
				clazzHierarchy.add( clazz );

				XClass superClass = clazz.getSuperclass();
				while ( superClass != null
						&& !reflectionManager.equals( superClass, Object.class ) ) {
					if ( superClass.isAnnotationPresent( Entity.class )
							|| superClass.isAnnotationPresent( MappedSuperclass.class ) ) {
						if ( orderedClasses.contains( superClass ) ) {
							break;
						}
						clazzHierarchy.add( superClass );
					}
					superClass = superClass.getSuperclass();
				}
				for (int i = clazzHierarchy.size() - 1; i >= 0; i-- ) {
					orderedClasses.add( clazzHierarchy.get(i) );
				}
			}
		}

		// order the hierarchy
		ArrayList<XClass> workingCopy = new ArrayList<>( orderedClasses );
		List<XClass> newList = new ArrayList<>( orderedClasses.size() );
		while ( !workingCopy.isEmpty() ) {
			XClass clazz = workingCopy.get( 0 );
			orderHierarchy( workingCopy, newList, orderedClasses, clazz );
		}
		return newList;
	}

	private void orderHierarchy(List<XClass> copy, List<XClass> newList, LinkedHashSet<XClass> original, XClass clazz) {
		if ( clazz != null && !Object.class.getName().equals( clazz.getName() ) ) {
			//process superclass first
			orderHierarchy( copy, newList, original, clazz.getSuperclass() );
			if ( original.contains( clazz ) ) {
				if ( !newList.contains( clazz ) ) {
					newList.add( clazz );
				}
				copy.remove( clazz );
			}
		}
	}

	@Override
	public void postProcessEntityHierarchies() {
		for ( String annotatedPackage : annotatedPackages ) {
			AnnotationBinder.bindFetchProfilesForPackage( classLoaderService, annotatedPackage, rootMetadataBuildingContext );
		}
	}

	@Override
	public void processResultSetMappings() {
	}

	@Override
	public void finishUp() {
	}
}
