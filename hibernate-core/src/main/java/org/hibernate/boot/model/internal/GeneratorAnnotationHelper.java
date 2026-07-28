/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.TableGenerator;
import jakarta.annotation.Nullable;
import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.boot.model.IdentifierGeneratorDefinition;
import org.hibernate.boot.model.relational.ExportableProducer;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.spi.GenericGeneratorRegistration;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.generator.AnnotationBasedGenerator;
import org.hibernate.generator.Generator;
import org.hibernate.generator.GeneratorCreationContext;
import org.hibernate.id.Configurable;
import org.hibernate.id.IdentityGenerator;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.id.uuid.UuidGenerator;
import org.hibernate.id.uuid.UuidValueGenerator;
import org.hibernate.internal.util.GenericsHelper;
import org.hibernate.mapping.GeneratorDescriptor;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.PreparedGenerator;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.models.spi.AnnotationDescriptor;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.resource.beans.internal.Helper;
import org.hibernate.resource.beans.spi.ManagedBean;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.hibernate.boot.model.internal.GeneratorBinder.beanContainer;
import static org.hibernate.boot.model.internal.GeneratorBinder.instantiateGenerator;
import static org.hibernate.boot.model.internal.GeneratorParameters.collectBaselineProperties;
import static org.hibernate.boot.model.internal.GeneratorParameters.fallbackAllocationSize;
import static org.hibernate.id.IdentifierGenerator.GENERATOR_NAME;
import static org.hibernate.id.IdentifierGenerator.ENTITY_NAME;
import static org.hibernate.id.IdentifierGenerator.JPA_ENTITY_NAME;
import static org.hibernate.id.OptimizableGenerator.INCREMENT_PARAM;
import static org.hibernate.id.PersistentIdentifierGenerator.CATALOG;
import static org.hibernate.id.PersistentIdentifierGenerator.SCHEMA;
import static org.hibernate.internal.util.StringHelper.qualifier;

/**
 * Helper for dealing with generators defined via annotations
 *
 * @author Steve Ebersole
 */
public class GeneratorAnnotationHelper {
	private static final GeneratorDescriptor IDENTITY_GENERATOR_DESCRIPTOR =
			new GeneratorDescriptor() {
				@Override
				public Generator createGenerator(GeneratorCreationContext context) {
					// Composite-id preparation reaches the nested descriptor through
					// generator creation instead of the top-level relational hook.
					applyRelationalModel( context );
					return new IdentityGenerator();
				}

				@Override
				public Class<? extends Generator> getGeneratorClass(GeneratorCreationContext context) {
					return IdentityGenerator.class;
				}

				@Override
				public void applyRelationalModel(GeneratorCreationContext context) {
					((SimpleValue) context.getValue()).setColumnToIdentity();
				}

				@Override
				public boolean requiresBootPreparation(GeneratorCreationContext context) {
					return false;
				}
			};

	public static <A extends Annotation> A findLocalizedMatch(
			AnnotationDescriptor<A> generatorAnnotationType,
			MemberDetails idMember,
			ClassDetails entityType,
			@Nullable Function<A,String> nameExtractor,
			@Nullable String matchName,
			MetadataBuildingContext context) {
		return findLocalizedMatch(
				generatorAnnotationType,
				idMember,
				entityType,
				nameExtractor,
				matchName,
				context.getModelsContext()
		);
	}

	public static <A extends Annotation> A findLocalizedMatch(
			AnnotationDescriptor<A> generatorAnnotationType,
			MemberDetails idMember,
			ClassDetails entityType,
			@Nullable Function<A,String> nameExtractor,
			@Nullable String matchName,
			ModelsContext modelsContext) {

		A possibleMatch = null;

		// first we look on the member
		for ( A generatorAnnotation:
				idMember.getRepeatedAnnotationUsages( generatorAnnotationType, modelsContext ) ) {
			if ( nameExtractor != null ) {
				final String registrationName = nameExtractor.apply( generatorAnnotation );
				if ( registrationName.isEmpty() ) {
					possibleMatch = generatorAnnotation;
					continue;
				}

				if ( registrationName.equals( matchName ) ) {
					return generatorAnnotation;
				}
			}
			else {
				return generatorAnnotation;
			}
		}

		// next, on the entity class
		for ( A generatorAnnotation :
				entityType.getRepeatedAnnotationUsages( generatorAnnotationType, modelsContext ) ) {
			if ( nameExtractor != null ) {
				final String registrationName = nameExtractor.apply( generatorAnnotation );
				if ( registrationName.isEmpty() ) {
					if ( possibleMatch == null ) {
						possibleMatch = generatorAnnotation;
					}
				}
				else if ( registrationName.equals( matchName ) ) {
					return generatorAnnotation;
				}
			}
			else {
				return generatorAnnotation;
			}
		}

		// next, on the declaring class
		for ( A generatorAnnotation:
				idMember.getDeclaringType().getRepeatedAnnotationUsages( generatorAnnotationType, modelsContext ) ) {
			if ( nameExtractor != null ) {
				final String registrationName = nameExtractor.apply( generatorAnnotation );
				if ( registrationName.isEmpty() ) {
					if ( possibleMatch == null ) {
						possibleMatch = generatorAnnotation;
					}
				}
				else if ( registrationName.equals( matchName ) ) {
					return generatorAnnotation;
				}
			}
			else {
				return generatorAnnotation;
			}
		}

		// lastly, on the package
		final var packageInfo = locatePackageInfoDetails( idMember.getDeclaringType(), modelsContext );
		if ( packageInfo != null ) {
			for ( A generatorAnnotation:
					packageInfo.getRepeatedAnnotationUsages( generatorAnnotationType, modelsContext ) ) {
				if ( nameExtractor != null ) {
					final String registrationName = nameExtractor.apply( generatorAnnotation );
					if ( registrationName.isEmpty() ) {
						if ( possibleMatch == null ) {
							possibleMatch = generatorAnnotation;
						}
					}
					else if ( registrationName.equals( matchName ) ) {
						return generatorAnnotation;
					}
				}
				else {
					return generatorAnnotation;
				}
			}
		}

		return possibleMatch;
	}

	public static ClassDetails locatePackageInfoDetails(ClassDetails classDetails, MetadataBuildingContext buildingContext) {
		return locatePackageInfoDetails( classDetails, buildingContext.getModelsContext() );
	}

	public static ClassDetails locatePackageInfoDetails(ClassDetails classDetails, ModelsContext modelContext) {
		return locatePackageInfoDetails( classDetails, modelContext.getClassDetailsRegistry() );
	}

	public static ClassDetails locatePackageInfoDetails(ClassDetails classDetails, ClassDetailsRegistry classDetailsRegistry) {
		final String packageInfoFqn = qualifier( classDetails.getName() ) + ".package-info";
		try {
			return classDetailsRegistry.resolveClassDetails( packageInfoFqn );
		}
		catch (ClassLoadingException e) {
			// means there is no package-info
			return null;
		}
	}

	public static void handleSequenceGenerator(
			String nameFromGeneratedValue,
			SequenceGenerator generatorAnnotation,
			SimpleValue idValue,
			MemberDetails idMember,
			MetadataBuildingContext buildingContext) {
		final int fallbackAllocationSize = fallbackAllocationSize( generatorAnnotation, buildingContext );
		idValue.setCustomIdGeneratorCreator(
				new SequenceGeneratorDescriptor(
						generatorAnnotation == null ? nameFromGeneratedValue : generatorAnnotation.name(),
						fallbackAllocationSize,
						sequenceConfiguration( generatorAnnotation )
				)
		);
	}

	private static Map<String, String> sequenceConfiguration(SequenceGenerator generatorAnnotation) {
		if ( generatorAnnotation == null ) {
			return Map.of();
		}
		final Map<String, String> configuration = new LinkedHashMap<>();
		SequenceStyleGenerator.applyConfiguration( generatorAnnotation, configuration::put );
		if ( !generatorAnnotation.sequenceName().isEmpty()
				&& !generatorAnnotation.sequenceName().contains( "." )
				&& generatorAnnotation.schema().isEmpty() ) {
			configuration.put( SCHEMA, null );
		}
		if ( !generatorAnnotation.sequenceName().isEmpty()
				&& !generatorAnnotation.sequenceName().contains( "." )
				&& generatorAnnotation.catalog().isEmpty() ) {
			configuration.put( CATALOG, null );
		}
		return configuration;
	}

	public static void handleTableGenerator(
			String nameFromGeneratedValue,
			TableGenerator generatorAnnotation,
			SimpleValue idValue,
			MemberDetails idMember,
			MetadataBuildingContext buildingContext) {
		final int fallbackAllocationSize = fallbackAllocationSize( generatorAnnotation, buildingContext );
		final Map<String, String> configuration = new LinkedHashMap<>();
		if ( generatorAnnotation != null ) {
			org.hibernate.id.enhanced.TableGenerator.applyConfiguration(
					generatorAnnotation,
					configuration::put
			);
		}
		idValue.setCustomIdGeneratorCreator(
				new TableGeneratorDescriptor(
						generatorAnnotation == null ? nameFromGeneratedValue : generatorAnnotation.name(),
						fallbackAllocationSize,
						configuration
				)
		);
	}

	private abstract static class BuiltInGeneratorDescriptor<G extends Generator>
			implements GeneratorDescriptor {
		private final String generatorName;
		private final int fallbackAllocationSize;
		private final Map<String, String> configuration;

		private BuiltInGeneratorDescriptor(
				String generatorName,
				int fallbackAllocationSize,
				Map<String, String> configuration) {
			this.generatorName = generatorName;
			this.fallbackAllocationSize = fallbackAllocationSize;
			this.configuration =
					Collections.unmodifiableMap( new LinkedHashMap<>( configuration ) );
		}

		@Override
		public Generator createGenerator(GeneratorCreationContext context) {
			return prepareGenerator( context ).getGenerator();
		}

		@Override
		public PreparedGenerator<G> prepareGenerator(GeneratorCreationContext context) {
			final Class<G> generatorClass = implementationClass();
			final ManagedBean<G> managedBean = Helper.getManagedBean(
					beanContainer( context ),
					generatorClass,
					false,
					true,
					() -> instantiateGenerator( null, generatorClass )
			);
			final G generator = managedBean.getBeanInstance();
			final Properties properties =
					new Properties( context.getDatabase().getDialect().getDefaultProperties() );
			if ( generatorName != null ) {
				properties.put( GENERATOR_NAME, generatorName );
			}
			properties.put( INCREMENT_PARAM, fallbackAllocationSize );
			collectBaselineProperties(
					context,
					properties::setProperty,
					context.getServiceRegistry().requireService( ConfigurationService.class )
			);
			configuration.forEach( (name, value) -> {
				if ( value == null ) {
					properties.remove( name );
				}
				else {
					properties.setProperty( name, value );
				}
			} );
			((Configurable) generator).configure( context, properties );
			((ExportableProducer) generator).registerExportables( context.getDatabase() );
			((Configurable) generator).initialize( context.getSqlStringGenerationContext() );
			return new PreparedGenerator<>( managedBean );
		}

		@Override
		public Class<? extends Generator> getGeneratorClass(GeneratorCreationContext context) {
			return implementationClass();
		}

		protected abstract Class<G> implementationClass();
	}

	private static final class SequenceGeneratorDescriptor
			extends BuiltInGeneratorDescriptor<SequenceStyleGenerator> {
		private SequenceGeneratorDescriptor(
				String generatorName,
				int fallbackAllocationSize,
				Map<String, String> configuration) {
			super( generatorName, fallbackAllocationSize, configuration );
		}

		@Override
		protected Class<SequenceStyleGenerator> implementationClass() {
			return SequenceStyleGenerator.class;
		}
	}

	private static final class TableGeneratorDescriptor
			extends BuiltInGeneratorDescriptor<org.hibernate.id.enhanced.TableGenerator> {
		private TableGeneratorDescriptor(
				String generatorName,
				int fallbackAllocationSize,
				Map<String, String> configuration) {
			super( generatorName, fallbackAllocationSize, configuration );
		}

		@Override
		protected Class<org.hibernate.id.enhanced.TableGenerator> implementationClass() {
			return org.hibernate.id.enhanced.TableGenerator.class;
		}
	}

	public static void handleIdGeneratorType(
			Annotation generatorAnnotation,
			SimpleValue idValue,
			MemberDetails idMember,
			MetadataBuildingContext buildingContext) {
		idValue.setCustomIdGeneratorCreator(
				GeneratorBinder.identifierGeneratorDescriptor( generatorAnnotation )
		);
	}

	/**
	 * Prepares a generator for use by handling its various potential means of "configuration".
	 *
	 * @param generator The "empty" generator
	 * @param annotation The annotation which defines configuration for the generator
	 * @param idMember The member defining the id
	 * @param configBaseline Allows to set any default values.  Called before common config is handled.
	 * @param configExtractor Allows to extract values from the generator annotation.  Called after common config is handled.
	 * @param creationContext Access to useful information
	 */
	public static <A extends Annotation> void prepareForUse(
			Generator generator,
			A annotation,
			MemberDetails idMember,
			Consumer<Properties> configBaseline,
			BiConsumer<A,Properties> configExtractor,
			GeneratorCreationContext creationContext) {
		if ( generator instanceof AnnotationBasedGenerator<?> annotationBasedGenerator ) {
			initializeGenerator( annotationBasedGenerator, annotation, creationContext );
		}
		if ( generator instanceof Configurable configurable ) {
			configureGenerator( annotation, configBaseline, configExtractor, creationContext, configurable );
		}
		if ( generator instanceof ExportableProducer exportableProducer ) {
			exportableProducer.registerExportables( creationContext.getDatabase() );
		}
		if ( generator instanceof Configurable configurable ) {
			configurable.initialize( creationContext.getSqlStringGenerationContext() );
		}
	}

	private static <A extends Annotation> void configureGenerator(
			A annotation,
			Consumer<Properties> configBaseline,
			BiConsumer<A, Properties> configExtractor,
			GeneratorCreationContext creationContext,
			Configurable configurable) {
		final var properties = new Properties( creationContext.getDatabase().getDialect().getDefaultProperties() );
		if ( configBaseline != null ) {
			configBaseline.accept( properties );
		}
		collectBaselineProperties(
				creationContext,
				properties::setProperty,
				creationContext.getServiceRegistry().requireService( ConfigurationService.class )
		);
		if ( configExtractor != null ) {
			configExtractor.accept( annotation, properties );
		}
		configurable.configure( creationContext, properties );
	}

	public static <A extends Annotation> void initializeGenerator(
			AnnotationBasedGenerator<A> generator,
			Annotation annotation,
			GeneratorCreationContext creationContext) {
		generator.initialize( castAnnotationType( annotation, generator), creationContext );
	}

	private static <A extends Annotation> A castAnnotationType(
			Annotation typeAnnotation,
			AnnotationBasedGenerator<A> annotationBased) {
		final var annotationType = annotationBased.getClass();
		Type[] typeArguments = GenericsHelper.typeArguments( AnnotationBasedGenerator.class, annotationType );
		if ( typeArguments.length > 0 && typeArguments[0] instanceof Class<?> annotationClass ) {
			if ( !annotationClass.isInstance( typeAnnotation ) ) {
				throw new AnnotationException( String.format( "Annotation '%s' is not assignable to '%s'",
						annotationType.getName(), annotationClass.getName() ) );
			}
			@SuppressWarnings("unchecked") // safe, we just checked it
			final var castAnnotation = (A) typeAnnotation;
			return castAnnotation;
		}
		throw new AssertionFailure( "Could not find implementing interface" );
	}

	public static void handleUuidStrategy(
			SimpleValue idValue,
			MemberDetails idMember,
			ClassDetails entityClass,
			MetadataBuildingContext context) {
		final var generatorConfig = findLocalizedMatch(
				HibernateAnnotations.UUID_GENERATOR,
				idMember,
				entityClass,
				null,
				null,
				context
		);
		idValue.setCustomIdGeneratorCreator( uuidGeneratorDescriptor( generatorConfig ) );
	}

	public static GeneratorDescriptor uuidGeneratorDescriptor(
			org.hibernate.annotations.UuidGenerator generatorConfig) {
		return new UuidGeneratorDescriptor( generatorConfig );
	}

	private static final class UuidGeneratorDescriptor implements GeneratorDescriptor {
		private final org.hibernate.annotations.UuidGenerator.Style style;
		private final String algorithmClassName;
		private transient Class<? extends UuidValueGenerator> algorithmClass;

		private UuidGeneratorDescriptor(org.hibernate.annotations.UuidGenerator generatorConfig) {
			style = generatorConfig == null
					? org.hibernate.annotations.UuidGenerator.Style.AUTO
					: generatorConfig.style();
			algorithmClass = generatorConfig == null
					? UuidValueGenerator.class
					: generatorConfig.algorithm();
			algorithmClassName = algorithmClass.getName();
		}

		@Override
		public Generator createGenerator(GeneratorCreationContext context) {
			final MemberDetails contextMember = context.getMemberDetails();
			return new UuidGenerator(
					style,
					resolveAlgorithmClass( context ),
					contextMember == null ? context.getProperty().getMemberDetails() : contextMember
			);
		}

		@Override
		public Class<? extends Generator> getGeneratorClass(GeneratorCreationContext context) {
			return UuidGenerator.class;
		}

		private Class<? extends UuidValueGenerator> resolveAlgorithmClass(GeneratorCreationContext context) {
			if ( algorithmClass == null ) {
				algorithmClass = context.getServiceRegistry()
						.requireService( org.hibernate.boot.registry.classloading.spi.ClassLoaderService.class )
						.classForName( algorithmClassName )
						.asSubclass( UuidValueGenerator.class );
			}
			return algorithmClass;
		}
	}

	public static void handleIdentityStrategy(SimpleValue idValue) {
		idValue.setCustomIdGeneratorCreator( IDENTITY_GENERATOR_DESCRIPTOR );
	}

	public static void handleGenericGenerator(
			String generatorName,
			GenericGenerator generatorConfig,
			PersistentClass entityMapping,
			SimpleValue idValue,
			MetadataBuildingContext context) {
		//generator settings
		final Map<String,String> configuration = new HashMap<>();
		configuration.put( ENTITY_NAME, entityMapping.getEntityName() );
		configuration.put( JPA_ENTITY_NAME, entityMapping.getJpaEntityName() );

		applyAnnotationParameters( generatorConfig, configuration );
		handleGenericGenerator( generatorName, determineStrategyName( generatorConfig ), configuration, idValue, context );
	}

	public static void handleGenericGenerator(
			String generatorName,
			GenericGeneratorRegistration generatorRegistration,
			PersistentClass entityMapping,
			SimpleValue idValue,
			MetadataBuildingContext context) {
		final Map<String,String> configuration = new HashMap<>( generatorRegistration.parameters() );
		configuration.put( GENERATOR_NAME, generatorRegistration.name() );
		configuration.put( ENTITY_NAME, entityMapping.getEntityName() );
		configuration.put( JPA_ENTITY_NAME, entityMapping.getJpaEntityName() );
		handleGenericGenerator( generatorName, generatorRegistration.strategy(), configuration, idValue, context );
	}

	private static void handleGenericGenerator(
			String generatorName,
			String strategy,
			Map<String, String> configuration,
			SimpleValue idValue,
			MetadataBuildingContext context) {
		configuration.put( PersistentIdentifierGenerator.TABLE, idValue.getTable().getName() );
		if ( idValue.getColumnSpan() == 1 ) {
			configuration.put( PersistentIdentifierGenerator.PK, idValue.getColumns().get(0).getName() );
		}

		GeneratorBinder.createGeneratorFrom(
				new IdentifierGeneratorDefinition(
						generatorName,
						GeneratorStrategies.resolveGeneratorClass( strategy, context ),
						configuration
				),
				idValue,
				context
		);
	}

	private static String determineStrategyName(GenericGenerator generatorConfig) {
		return generatorConfig.type().getName();
	}

	private static void applyAnnotationParameters(GenericGenerator generatorConfig, Map<String, String> configuration) {
		for ( var parameter : generatorConfig.parameters() ) {
			configuration.put( parameter.name(), parameter.value() );
		}
	}
}
