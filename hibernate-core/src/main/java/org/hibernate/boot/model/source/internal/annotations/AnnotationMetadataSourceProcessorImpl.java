/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.model.source.internal.annotations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.annotations.common.reflection.MetadataProviderInjector;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.boot.internal.MetadataBuildingContextRootImpl;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.model.convert.internal.ClassBasedConverterDescriptor;
import org.hibernate.boot.model.convert.spi.ConverterRegistry;
import org.hibernate.boot.model.convert.spi.RegisteredConversion;
import org.hibernate.boot.model.internal.AnnotationBinder;
import org.hibernate.boot.model.internal.InheritanceState;
import org.hibernate.boot.model.internal.JPAXMLOverriddenMetadataProvider;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.model.process.spi.MetadataBuildingProcess;
import org.hibernate.boot.model.source.spi.MetadataSourceProcessor;
import org.hibernate.boot.models.categorize.spi.FilterDefRegistration;
import org.hibernate.boot.models.xml.spi.PersistenceUnitMetadata;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.JpaOrmXmlPersistenceUnitDefaultAware;
import org.hibernate.boot.spi.JpaOrmXmlPersistenceUnitDefaultAware.JpaOrmXmlPersistenceUnitDefaults;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.MetadataBuildingOptions;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.usertype.UserType;
import org.jboss.logging.Logger;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Entity;
import jakarta.persistence.MappedSuperclass;

import static org.hibernate.boot.jaxb.SourceType.OTHER;
import static org.hibernate.boot.model.internal.AnnotationBinder.resolveAttributeConverter;
import static org.hibernate.boot.model.internal.AnnotationBinder.resolveBasicType;
import static org.hibernate.boot.model.internal.AnnotationBinder.resolveFilterParamType;
import static org.hibernate.boot.model.internal.AnnotationBinder.resolveJavaType;
import static org.hibernate.boot.model.internal.AnnotationBinder.resolveUserType;
import static org.hibernate.models.internal.jdk.VoidClassDetails.VOID_CLASS_DETAILS;
import static org.hibernate.models.internal.jdk.VoidClassDetails.VOID_OBJECT_CLASS_DETAILS;

/**
 * @author Steve Ebersole
 */
public class AnnotationMetadataSourceProcessorImpl implements MetadataSourceProcessor {
	private static final Logger log = Logger.getLogger( AnnotationMetadataSourceProcessorImpl.class );

	// NOTE : we de-categorize the classes into a single collection (xClasses) here to work with the
	// 		existing "binder" infrastructure.
	// todo : once we move to the phased binding approach, come back and handle that

	private final DomainModelSource domainModelSource;

	private final MetadataBuildingContextRootImpl rootMetadataBuildingContext;
	private final ClassLoaderService classLoaderService;

	private final ReflectionManager reflectionManager;
	private final LinkedHashSet<String> annotatedPackages = new LinkedHashSet<>();
	private final List<XClass> xClasses = new ArrayList<>();

	/**
	 * Normal constructor used while processing {@linkplain org.hibernate.boot.MetadataSources mapping sources}
	 */
	public AnnotationMetadataSourceProcessorImpl(
			ManagedResources managedResources,
			DomainModelSource domainModelSource,
			MetadataBuildingContextRootImpl rootMetadataBuildingContext) {
		this.domainModelSource = domainModelSource;
		this.rootMetadataBuildingContext = rootMetadataBuildingContext;

		this.reflectionManager = rootMetadataBuildingContext.getBootstrapContext().getReflectionManager();

		final MetadataBuildingOptions metadataBuildingOptions = rootMetadataBuildingContext.getBuildingOptions();
		this.classLoaderService = metadataBuildingOptions.getServiceRegistry().getService( ClassLoaderService.class );

		final ConverterRegistry converterRegistry = rootMetadataBuildingContext.getMetadataCollector().getConverterRegistry();
		domainModelSource.getConversionRegistrations().forEach( (registration) -> {
			final Class<?> domainType;
			if ( registration.getExplicitDomainType() == VOID_CLASS_DETAILS || registration.getExplicitDomainType() == VOID_OBJECT_CLASS_DETAILS ) {
				domainType = void.class;
			}
			else {
				domainType = classLoaderService.classForName( registration.getExplicitDomainType().getClassName() );
			}
			converterRegistry.addRegisteredConversion( new RegisteredConversion(
					domainType,
					classLoaderService.classForName( registration.getConverterType().getClassName() ),
					registration.isAutoApply(),
					rootMetadataBuildingContext
			) );
		} );
		domainModelSource.getConverterRegistrations().forEach( (registration) -> {
			converterRegistry.addAttributeConverter( new ClassBasedConverterDescriptor(
					classLoaderService.classForName( registration.converterClass().getClassName() ),
					registration.autoApply(),
					rootMetadataBuildingContext.getBootstrapContext().getClassmateContext()
			) );
		} );


		if ( metadataBuildingOptions.isXmlMappingEnabled() ) {
			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			// Ewww.  This is temporary until we migrate to Jandex + StAX for annotation binding
			final JPAXMLOverriddenMetadataProvider jpaMetadataProvider = (JPAXMLOverriddenMetadataProvider)
					( (MetadataProviderInjector) reflectionManager ).getMetadataProvider();
			for ( Binding<?> xmlBinding : managedResources.getXmlMappingBindings() ) {
				Object root = xmlBinding.getRoot();
				if ( !( root instanceof JaxbEntityMappingsImpl ) ) {
					continue;
				}
				final JaxbEntityMappingsImpl entityMappings = (JaxbEntityMappingsImpl) xmlBinding.getRoot();
				final List<String> classNames = jpaMetadataProvider.getXMLContext().addDocument( entityMappings );
				for ( String className : classNames ) {
					xClasses.add( toXClass( className ) );
				}
			}
		}

		for ( String className : managedResources.getAnnotatedClassNames() ) {
			final Class<?> annotatedClass = classLoaderService.classForName( className );
			xClasses.add( toXClass( annotatedClass ) );
		}

		for ( Class<?> annotatedClass : managedResources.getAnnotatedClassReferences() ) {
			xClasses.add( toXClass( annotatedClass ) );
		}

		annotatedPackages.addAll( managedResources.getAnnotatedPackageNames() );
	}

	private XClass toXClass(String className) {
		return reflectionManager.toXClass( classLoaderService.classForName( className ) );
	}

	private XClass toXClass(Class<?> classRef) {
		return reflectionManager.toXClass( classRef );
	}

	/**
	 * Used as part of processing
	 * {@linkplain org.hibernate.boot.spi.AdditionalMappingContributions#contributeEntity(Class) "additional" mappings}
	 */
	public static void processAdditionalMappings(
			List<Class<?>> additionalClasses,
			List<JaxbEntityMappingsImpl> additionalJaxbMappings,
			MetadataBuildingContextRootImpl rootMetadataBuildingContext) {
		final AdditionalManagedResourcesImpl.Builder mrBuilder = new AdditionalManagedResourcesImpl.Builder();
		mrBuilder.addLoadedClasses( additionalClasses );
		for ( JaxbEntityMappingsImpl additionalJaxbMapping : additionalJaxbMappings ) {
			mrBuilder.addXmlBinding( new Binding<>( additionalJaxbMapping, new Origin( OTHER, "additional" ) ) );
		}

		final ManagedResources mr = mrBuilder.build();
		final DomainModelSource additionalDomainModelSource = MetadataBuildingProcess.processManagedResources(
				mr,
				rootMetadataBuildingContext.getBootstrapContext()
		);
		final AnnotationMetadataSourceProcessorImpl processor = new AnnotationMetadataSourceProcessorImpl( mr, additionalDomainModelSource, rootMetadataBuildingContext );
		processor.processEntityHierarchies( new LinkedHashSet<>() );
	}

	@Override
	public void prepare() {
		// use any persistence-unit-defaults defined in orm.xml
		// todo : invert this to use the PersistenceUnitMetadata directly (defaulting to the settings)
		( (JpaOrmXmlPersistenceUnitDefaultAware) rootMetadataBuildingContext.getBuildingOptions() ).apply(
				new JpaOrmXmlPersistenceUnitDefaults() {
					final PersistenceUnitMetadata persistenceUnitMetadata = domainModelSource.getPersistenceUnitMetadata();

					@Override
					public String getDefaultSchemaName() {
						return StringHelper.nullIfEmpty( persistenceUnitMetadata.getDefaultSchema() );
					}

					@Override
					public String getDefaultCatalogName() {
						return StringHelper.nullIfEmpty( persistenceUnitMetadata.getDefaultCatalog() );
					}

					@Override
					public boolean shouldImplicitlyQuoteIdentifiers() {
						return persistenceUnitMetadata.useQuotedIdentifiers();
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
		final Map<String, FilterDefRegistration> filterDefRegistrations = domainModelSource.getGlobalRegistrations().getFilterDefRegistrations();
		for ( Map.Entry<String, FilterDefRegistration> filterDefRegistrationEntry : filterDefRegistrations.entrySet() ) {
			final Map<String, JdbcMapping> parameterJdbcMappings = new HashMap<>();
			final Map<String, ClassDetails> parameterDefinitions = filterDefRegistrationEntry.getValue().getParameters();
			if ( CollectionHelper.isNotEmpty( parameterDefinitions ) ) {
				for ( Map.Entry<String, ClassDetails> parameterEntry : parameterDefinitions.entrySet() ) {
					final String parameterClassName = parameterEntry.getValue().getClassName();
					final ClassDetails parameterClassDetails = domainModelSource.getClassDetailsRegistry().resolveClassDetails( parameterClassName );
					parameterJdbcMappings.put(
							parameterEntry.getKey(),
							resolveFilterParamType( parameterClassDetails, rootMetadataBuildingContext )
					);
				}
			}

			rootMetadataBuildingContext.getMetadataCollector().addFilterDefinition( new FilterDefinition(
					filterDefRegistrationEntry.getValue().getName(),
					filterDefRegistrationEntry.getValue().getDefaultCondition(),
					parameterJdbcMappings
			) );
		}

	}

	private static JdbcMapping resolveFilterParamType(
			ClassDetails classDetails,
			MetadataBuildingContext context) {
		if ( classDetails.isImplementor( UserType.class ) ) {
			final Class<UserType<?>> impl = classDetails.toJavaClass();
			return resolveUserType( impl, context );

		}

		if ( classDetails.isImplementor( AttributeConverter.class ) ) {
			final Class<AttributeConverter<?,?>> impl = classDetails.toJavaClass();
			return resolveAttributeConverter( impl, context );
		}

		if ( classDetails.isImplementor( JavaType.class ) ) {
			final Class<JavaType<?>> impl = classDetails.toJavaClass();
			return resolveJavaType( impl, context );
		}

		return resolveBasicType( classDetails.toJavaClass(), context );
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
		return new ArrayList<>( orderedClasses );
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
