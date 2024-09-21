/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.boot.model.IdentifierGeneratorDefinition;
import org.hibernate.boot.model.relational.ExportableProducer;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.generator.AnnotationBasedGenerator;
import org.hibernate.generator.Generator;
import org.hibernate.generator.GeneratorCreationContext;
import org.hibernate.id.Configurable;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.IdentityGenerator;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.id.uuid.UuidGenerator;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.models.spi.AnnotationDescriptor;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.SourceModelBuildingContext;
import org.hibernate.models.spi.SourceModelContext;
import org.hibernate.resource.beans.container.spi.BeanContainer;

import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.TableGenerator;

import static org.hibernate.boot.model.internal.GeneratorParameters.collectBaselineProperties;
import static org.hibernate.boot.model.internal.GeneratorParameters.fallbackAllocationSize;
import static org.hibernate.id.IdentifierGenerator.GENERATOR_NAME;
import static org.hibernate.id.OptimizableGenerator.INCREMENT_PARAM;
import static org.hibernate.internal.util.config.ConfigurationHelper.setIfNotEmpty;

/**
 * Helper for dealing with generators defined via annotations
 *
 * @author Steve Ebersole
 */
public class GeneratorAnnotationHelper {
	public static <A extends Annotation> A findLocalizedMatch(
			AnnotationDescriptor<A> generatorAnnotationType,
			MemberDetails idMember,
			Function<A,String> nameExtractor,
			String matchName,
			MetadataBuildingContext context) {
		final SourceModelBuildingContext sourceModelContext = context.getMetadataCollector().getSourceModelBuildingContext();

		A possibleMatch = null;

		// first we look on the member
		for ( A generatorAnnotation : idMember.getRepeatedAnnotationUsages( generatorAnnotationType, sourceModelContext ) ) {
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

		// next, on the class
		for ( A generatorAnnotation : idMember.getDeclaringType().getRepeatedAnnotationUsages( generatorAnnotationType, sourceModelContext ) ) {
			if ( nameExtractor != null ) {
				final String registrationName = nameExtractor.apply( generatorAnnotation );
				if ( registrationName.isEmpty() ) {
					if ( possibleMatch == null ) {
						possibleMatch = generatorAnnotation;
					}
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

		// lastly, on the package
		final ClassDetails packageInfo = locatePackageInfoDetails( idMember.getDeclaringType(), context );
		if ( packageInfo !=
					null ) {
			for ( A generatorAnnotation : packageInfo.getRepeatedAnnotationUsages( generatorAnnotationType, sourceModelContext ) ) {
				if ( nameExtractor != null ) {
					final String registrationName = nameExtractor.apply( generatorAnnotation );
					if ( registrationName.isEmpty() ) {
						if ( possibleMatch == null ) {
							possibleMatch = generatorAnnotation;
						}
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
		}

		return possibleMatch;
	}

	public static ClassDetails locatePackageInfoDetails(ClassDetails classDetails, MetadataBuildingContext buildingContext) {
		return locatePackageInfoDetails( classDetails, buildingContext.getMetadataCollector().getSourceModelBuildingContext() );
	}

	public static ClassDetails locatePackageInfoDetails(ClassDetails classDetails, SourceModelContext modelContext) {
		return locatePackageInfoDetails( classDetails, modelContext.getClassDetailsRegistry() );
	}

	public static ClassDetails locatePackageInfoDetails(ClassDetails classDetails, ClassDetailsRegistry classDetailsRegistry) {
		final String packageInfoFqn = StringHelper.qualifier( classDetails.getName() ) + ".package-info";
		try {
			return classDetailsRegistry.resolveClassDetails( packageInfoFqn );
		}
		catch (ClassLoadingException e) {
			// means there is no package-info
			return null;
		}
	}

	public static void handleUuidStrategy(
			SimpleValue idValue,
			MemberDetails idMember,
			MetadataBuildingContext context) {
		final org.hibernate.annotations.UuidGenerator generatorConfig = findLocalizedMatch(
				HibernateAnnotations.UUID_GENERATOR,
				idMember,
				null,
				null,
				context
		);
		idValue.setCustomIdGeneratorCreator( (creationContext) -> new UuidGenerator( generatorConfig, idMember ) );
	}

	public static void handleIdentityStrategy(SimpleValue idValue) {
		idValue.setCustomIdGeneratorCreator( (creationContext) -> new IdentityGenerator() );
		idValue.setColumnToIdentity();
	}

	public static void applyBaselineConfiguration(
			SequenceGenerator generatorConfig,
			SimpleValue idValue,
			RootClass rootClass,
			MetadataBuildingContext context,
			BiConsumer<String,String> configurationCollector) {
		if ( generatorConfig != null && !generatorConfig.name().isEmpty() ) {
			configurationCollector.accept( GENERATOR_NAME, generatorConfig.name() );
		}

		GeneratorParameters.collectParameters(
				idValue,
				context.getMetadataCollector().getDatabase().getDialect(),
				rootClass,
				configurationCollector
		);

	}

	static void applyBaselineConfiguration(
			TableGenerator generatorConfig,
			SimpleValue idValue,
			RootClass rootClass,
			MetadataBuildingContext context,
			BiConsumer<String, String> configurationCollector) {
		if ( !generatorConfig.name().isEmpty() ) {
			configurationCollector.accept( GENERATOR_NAME, generatorConfig.name() );
		}

		GeneratorParameters.collectParameters(
				idValue,
				context.getMetadataCollector().getDatabase().getDialect(),
				rootClass,
				configurationCollector
		);

	}

	public static void handleGenericGenerator(
			String generatorName,
			GenericGenerator generatorConfig,
			PersistentClass entityMapping,
			SimpleValue idValue,
			MetadataBuildingContext context) {
		//generator settings
		final Map<String,String> configuration = new HashMap<>();
		setIfNotEmpty( generatorConfig.name(), IdentifierGenerator.GENERATOR_NAME, configuration );
		configuration.put( IdentifierGenerator.ENTITY_NAME, entityMapping.getEntityName() );
		configuration.put( IdentifierGenerator.JPA_ENTITY_NAME, entityMapping.getJpaEntityName() );

		applyAnnotationParameters( generatorConfig, configuration );

		configuration.put( PersistentIdentifierGenerator.TABLE, idValue.getTable().getName() );
		if ( idValue.getColumnSpan() == 1 ) {
			configuration.put( PersistentIdentifierGenerator.PK, idValue.getColumns().get(0).getName() );
		}

		GeneratorBinder.createGeneratorFrom(
				new IdentifierGeneratorDefinition( generatorName, determineStrategyName( generatorConfig ), configuration ),
				idValue,
				context
		);
	}

	private static String determineStrategyName(GenericGenerator generatorConfig) {
		final Class<? extends Generator> type = generatorConfig.type();
		if ( !Objects.equals( type, Generator.class ) ) {
			return type.getName();
		}
		return generatorConfig.strategy();
	}

	private static void applyAnnotationParameters(GenericGenerator generatorConfig, Map<String, String> configuration) {
		for ( Parameter parameter : generatorConfig.parameters() ) {
			configuration.put( parameter.name(), parameter.value() );
		}
	}

	public static void handleTableGenerator(
			String nameFromGeneratedValue,
			TableGenerator generatorAnnotation,
			PersistentClass entityMapping,
			SimpleValue idValue,
			MemberDetails idMember,
			MetadataBuildingContext buildingContext) {
		idValue.setCustomIdGeneratorCreator( (creationContext) -> {
			final BeanContainer beanContainer = GeneratorBinder.beanContainer( buildingContext );
			final org.hibernate.id.enhanced.TableGenerator identifierGenerator = GeneratorBinder.instantiateGenerator(
					beanContainer,
					org.hibernate.id.enhanced.TableGenerator.class
			);
			GeneratorAnnotationHelper.prepareForUse(
					identifierGenerator,
					generatorAnnotation,
					idMember,
					properties -> {
						if ( generatorAnnotation != null ) {
							properties.put( GENERATOR_NAME, generatorAnnotation.name() );
						}
						else if ( nameFromGeneratedValue != null ) {
							properties.put( GENERATOR_NAME, nameFromGeneratedValue );
						}
						// we need to better handle default allocation-size here...
						properties.put(
								INCREMENT_PARAM,
								fallbackAllocationSize( generatorAnnotation, buildingContext )
						);
					},
					generatorAnnotation == null
							? null
							: (a, properties) -> org.hibernate.id.enhanced.TableGenerator.applyConfiguration(
									generatorAnnotation,
									properties::put
							),
					creationContext
			);
			return identifierGenerator;
		} );
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
		if ( generator instanceof AnnotationBasedGenerator ) {
			@SuppressWarnings("unchecked")
			final AnnotationBasedGenerator<A> generation = (AnnotationBasedGenerator<A>) generator;
			generation.initialize( annotation, idMember.toJavaMember(), creationContext );
		}
		if ( generator instanceof Configurable configurable ) {
			final Properties properties = new Properties();
			if ( configBaseline != null ) {
				configBaseline.accept( properties );
			}
			collectBaselineProperties(
					creationContext.getProperty() != null
							? (SimpleValue) creationContext.getProperty().getValue()
							: (SimpleValue) creationContext.getPersistentClass().getIdentifierProperty().getValue(),
					creationContext.getDatabase().getDialect(),
					creationContext.getRootClass(),
					properties::setProperty
			);
			if ( configExtractor != null ) {
				configExtractor.accept( annotation, properties );
			}
			configurable.configure( creationContext, properties );
		}
		if ( generator instanceof ExportableProducer exportableProducer ) {
			exportableProducer.registerExportables( creationContext.getDatabase() );
		}
		if ( generator instanceof Configurable configurable ) {
			configurable.initialize( creationContext.getSqlStringGenerationContext() );
		}

	}
}
