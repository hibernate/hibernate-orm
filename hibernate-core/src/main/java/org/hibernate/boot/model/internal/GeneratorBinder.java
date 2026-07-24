/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.TableGenerator;
import jakarta.persistence.Version;
import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.MappingException;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.IdGeneratorType;
import org.hibernate.annotations.ValueGenerationType;
import org.hibernate.boot.model.IdentifierGeneratorDefinition;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.ExportableProducer;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.spi.GlobalRegistrar;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.generator.AnnotationBasedGenerator;
import org.hibernate.generator.Assigned;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.Generator;
import org.hibernate.generator.GeneratorCreationContext;
import org.hibernate.generator.OnExecutionGenerator;
import org.hibernate.generator.internal.GeneratorTypeHelper;
import org.hibernate.id.CompositeNestedGeneratedValueGenerator;
import org.hibernate.id.CompositeNestedGeneratedValueGenerator.GenerationPlan;
import org.hibernate.id.Configurable;
import org.hibernate.id.IdentifierGenerationException;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.id.uuid.UuidValueGenerator;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.GeneratorCreator;
import org.hibernate.mapping.GeneratorSettings;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.Value;
import org.hibernate.models.spi.AnnotationTarget;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.property.access.spi.PropertyAccessStrategyResolver;
import org.hibernate.property.access.spi.Setter;
import org.hibernate.resource.beans.container.spi.BeanContainer;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.resource.beans.internal.Helper;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.ComponentType;
import org.hibernate.type.Type;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static jakarta.persistence.GenerationType.AUTO;
import static java.util.Collections.emptyMap;
import static org.hibernate.boot.model.internal.BinderHelper.isGlobalGeneratorNameGlobal;
import static org.hibernate.boot.model.internal.Constructors.construct;
import static org.hibernate.boot.model.internal.GeneratorAnnotationHelper.findLocalizedMatch;
import static org.hibernate.boot.model.internal.GeneratorAnnotationHelper.initializeGenerator;
import static org.hibernate.boot.model.internal.GeneratorParameters.collectParameters;
import static org.hibernate.boot.model.internal.GeneratorParameters.interpretSequenceGenerator;
import static org.hibernate.boot.model.internal.GeneratorParameters.interpretTableGenerator;
import static org.hibernate.boot.model.internal.GeneratorStrategies.generatorClass;
import static org.hibernate.id.IdentifierGenerator.GENERATOR_NAME;
import static org.hibernate.boot.BootLogging.BOOT_LOGGER;
import static org.hibernate.internal.util.NullnessUtil.castNonNull;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;
import static org.hibernate.internal.util.collections.CollectionHelper.combineUntyped;
import static org.hibernate.resource.beans.internal.Helper.getBean;

/**
 * Responsible for configuring and instantiating {@link Generator}s.
 *
 * @author Gavin King
 */
public class GeneratorBinder {

	public static final String ASSIGNED_GENERATOR_NAME = "assigned";
	public static final GeneratorCreator ASSIGNED_IDENTIFIER_GENERATOR_CREATOR =
			new GeneratorCreator() {
				@Override
				public Generator createGenerator(GeneratorCreationContext context) {
					return new Assigned();
				}
				@Override
				public boolean isAssigned() {
					return true;
				}
			};

	/**
	 * Create a generator, based on a {@link GeneratedValue} annotation.
	 */
	public static void makeIdGenerator(
			SimpleValue identifierValue,
			MemberDetails idMember,
			String generatorType,
			String generatorName,
			MetadataBuildingContext context,
			Map<String, ? extends IdentifierGeneratorDefinition> localGenerators) {

		//generator settings
		final var configuration = initializeGeneratorSettings( identifierValue, generatorName, context );

		final String generatorStrategy;
		if ( generatorName.isEmpty() ) {
			if ( idMember.hasDirectAnnotationUsage( GeneratedValue.class )
					&& handleDefaultGenerator( identifierValue, context, localGenerators, idMember, configuration ) ) {
				// we found an appropriate a "default" generator (as per JPA 3.2)
				return; // EARLY EXIT
			}
			else {
				generatorStrategy = generatorType;
			}
		}
		else if ( generatorName.isBlank() ) {
			throw new MappingException( "Generator name is cannot be blank" );
		}
		else {
			//we have a named generator
			generatorStrategy = determineStrategy(
					idMember,
					generatorType,
					generatorName,
					context,
					localGenerators,
					configuration
			);
		}

		setGeneratorCreator( identifierValue, configuration, generatorStrategy, context );
	}

	/**
	 * Called if {@link GeneratedValue @GeneratedValue} specified no name.
	 * This is a new special case added in JPA 3.2.
	 * We look for an appropriate matching "default generator recipe"
	 * based on the {@link GenerationType}.
	 */
	private static boolean handleDefaultGenerator(
			SimpleValue identifierValue,
			MetadataBuildingContext context,
			Map<String, ? extends IdentifierGeneratorDefinition> localGenerators,
			MemberDetails idMember,
			Map<String, Object> configuration) {
		final var strategy = idMember.getDirectAnnotationUsage( GeneratedValue.class ).strategy();
		final String strategyGeneratorClassName = correspondingGeneratorName( strategy );
		final var impliedGenerator =
				determineImpliedGenerator( strategy, strategyGeneratorClassName, localGenerators );
		if ( impliedGenerator != null ) {
			configuration.putAll( impliedGenerator.getParameters() );
			instantiateNamedStrategyGenerator(
					identifierValue,
					generatorStrategy( strategyGeneratorClassName, impliedGenerator ),
					configuration
			);
			return true;
		}
		else {
			return false;
		}
	}

	private static Map<String, Object> initializeGeneratorSettings(
			SimpleValue identifierValue,
			String generatorName,
			MetadataBuildingContext context) {
		final Map<String,Object> configuration = new HashMap<>();
		configuration.put( GENERATOR_NAME, generatorName );
		applyTableDetails( identifierValue, configuration, context );
		if ( identifierValue.getColumnSpan() == 1 ) {
			configuration.put( PersistentIdentifierGenerator.PK, identifierValue.getColumns().get(0).getName() );
		}
		return configuration;
	}

	private static void applyTableDetails(
			Value value,
			Map<String, Object> configuration,
			MetadataBuildingContext context) {
		final Table table = value.getTable();
		configuration.put( PersistentIdentifierGenerator.TABLE, table.getName() );
		final String catalog = implicitNamespaceCatalog( table.getCatalog(), context )
				? defaultCatalog( context )
				: table.getCatalog();
		if ( isNotEmpty( catalog ) ) {
			configuration.put( PersistentIdentifierGenerator.CATALOG, catalog );
		}
		final String schema = implicitNamespaceSchema( table.getSchema(), context )
				? defaultSchema( context )
				: table.getSchema();
		if ( isNotEmpty( schema ) ) {
			configuration.put( PersistentIdentifierGenerator.SCHEMA, schema );
		}
	}

	private static boolean implicitNamespaceCatalog(String catalog, MetadataBuildingContext context) {
		if ( !isNotEmpty( catalog ) ) {
			return true;
		}
		final String defaultCatalog = defaultCatalog( context );
		if ( isNotEmpty( defaultCatalog ) && catalog.equalsIgnoreCase( defaultCatalog ) ) {
			return true;
		}
		final var implicitCatalog = context.getMetadataCollector()
				.getDatabase()
				.getPhysicalImplicitNamespaceName()
				.catalog();
		return implicitCatalog != null && catalog.equals( implicitCatalog.getText() );
	}

	private static boolean implicitNamespaceSchema(String schema, MetadataBuildingContext context) {
		if ( !isNotEmpty( schema ) ) {
			return true;
		}
		final String defaultSchema = defaultSchema( context );
		if ( isNotEmpty( defaultSchema ) && schema.equalsIgnoreCase( defaultSchema ) ) {
			return true;
		}
		final var implicitSchema = context.getMetadataCollector()
				.getDatabase()
				.getPhysicalImplicitNamespaceName()
				.schema();
		return implicitSchema != null && schema.equals( implicitSchema.getText() );
	}

	private static String defaultCatalog(MetadataBuildingContext context) {
		final String mappingDefault = context
				.getBuildingPlan()
				.getMappingDefaults()
				.getImplicitCatalogName();
		return isNotEmpty( mappingDefault )
				? mappingDefault
				: context.getMetadataCollector()
						.getPersistenceUnitMetadata()
						.getDefaultCatalog();
	}

	private static String defaultSchema(MetadataBuildingContext context) {
		final String mappingDefault = context
				.getBuildingPlan()
				.getMappingDefaults()
				.getImplicitSchemaName();
		return isNotEmpty( mappingDefault )
				? mappingDefault
				: context.getMetadataCollector()
						.getPersistenceUnitMetadata()
						.getDefaultSchema();
	}

	private static IdentifierGeneratorDefinition determineImpliedGenerator(
			GenerationType strategy,
			String strategyGeneratorClassName,
			Map<String, ? extends IdentifierGeneratorDefinition> localGenerators) {
		if ( localGenerators == null ) {
			return null;
		}

		if ( localGenerators.size() == 1 ) {
			final var generatorDefinition = localGenerators.values().iterator().next();
			// NOTE: a little bit of a special rule here for the case of just one -
			// 		 consider it a match, based on strategy, if the strategy is AUTO or matches
			if ( strategy == AUTO
					|| isImpliedGenerator( strategyGeneratorClassName, generatorDefinition ) ) {
				return generatorDefinition;
			}
		}

		return matchingLocalGenerator( strategyGeneratorClassName, localGenerators );
	}

	private static IdentifierGeneratorDefinition matchingLocalGenerator(
			String strategyGeneratorClassName,
			Map<String, ? extends IdentifierGeneratorDefinition> localGenerators) {
		IdentifierGeneratorDefinition matching = null;
		for ( var localGenerator : localGenerators.values() ) {
			if ( isImpliedGenerator( strategyGeneratorClassName, localGenerator ) ) {
				if ( matching != null ) {
					// we found multiple matching generators
					return null;
				}
				matching = localGenerator;
			}
		}
		return matching;
	}

	private static boolean isImpliedGenerator(
			String strategyGeneratorClassName,
			IdentifierGeneratorDefinition generatorDefinition) {
		return generatorDefinition.getStrategy().equals( strategyGeneratorClassName );
	}

	private static String correspondingGeneratorName(GenerationType strategy) {
		return switch ( strategy ) {
//			case UUID -> org.hibernate.id.uuid.UuidGenerator.class.getName();
			case UUID -> UuidValueGenerator.class.getName();
			case TABLE -> org.hibernate.id.enhanced.TableGenerator.class.getName();
			case IDENTITY -> null;
			default -> SequenceStyleGenerator.class.getName();
		};
	}

	private static String determineStrategy(
			MemberDetails idAttributeMember,
			String generatorType,
			String generatorName,
			MetadataBuildingContext context,
			Map<String, ? extends IdentifierGeneratorDefinition> localGenerators,
			Map<String, Object> configuration) {
		final var definition =
				makeIdentifierGeneratorDefinition( generatorName, idAttributeMember, localGenerators, context );
		if ( definition == null ) {
			throw new AnnotationException( "No id generator was declared with the name '" + generatorName
					+ "' specified by '@GeneratedValue'"
					+ " (define a named generator using '@SequenceGenerator' or '@TableGenerator')" );
		}
		configuration.putAll( definition.getParameters() );
		// This is quite vague in the spec,
		// but a generator could override the generator choice
		return generatorStrategy( generatorType, definition );
	}

	private static String generatorStrategy(String generatorType, IdentifierGeneratorDefinition definition) {
		return generatorType != null
			// Yuck! this is a hack to not override 'AUTO',
			// even if GeneratedValue.generator is specified
			&& definition.getStrategy().equals( "identity" )
				? generatorType
				: definition.getStrategy();
	}

	private static IdentifierGeneratorDefinition makeIdentifierGeneratorDefinition(
			String name,
			MemberDetails idAttributeMember,
			Map<String, ? extends IdentifierGeneratorDefinition> localGenerators,
			MetadataBuildingContext buildingContext) {
		if ( localGenerators != null ) {
			final var result = localGenerators.get( name );
			if ( result != null ) {
				return result;
			}
		}

		final var globalDefinition =
				buildingContext.getMetadataCollector()
						.getIdentifierGenerator( name );
		if ( globalDefinition != null ) {
			return globalDefinition;
		}
		else {
			final var generatedValue = idAttributeMember.getDirectAnnotationUsage( GeneratedValue.class );
			if ( generatedValue == null ) {
				throw new AssertionFailure( "No @GeneratedValue annotation" );
			}
			return IdentifierGeneratorDefinition.createImplicit(
					name,
					idAttributeMember.getType(),
					generatedValue.generator(),
					interpretGenerationType( generatedValue )
			);
		}
	}

	private static GenerationType interpretGenerationType(GeneratedValue generatedValueAnn) {
		// todo (jpa32) : when can this ever be null?
		final var strategy = generatedValueAnn.strategy();
		return strategy == null ? AUTO : strategy;
	}

	public static void visitIdGeneratorDefinitions(
			AnnotationTarget annotatedElement,
			Consumer<IdentifierGeneratorDefinition> consumer,
			MetadataBuildingContext buildingContext) {
		final var modelsContext = buildingContext.getModelsContext();
		annotatedElement.forEachAnnotationUsage( TableGenerator.class, modelsContext,
				usage -> consumer.accept( buildTableIdGenerator( usage ) ) );
		annotatedElement.forEachAnnotationUsage( SequenceGenerator.class, modelsContext,
				usage -> consumer.accept( buildSequenceIdGenerator( usage ) ) );
	}

	public static void registerGlobalGenerators(
			AnnotationTarget annotatedElement,
			MetadataBuildingContext context) {
		if ( context.getJpaCompliance().isGlobalGeneratorScopeEnabled() ) {
			final var metadataCollector = context.getMetadataCollector();
			visitIdGeneratorDefinitions(
					annotatedElement,
					definition -> {
						if ( !definition.getName().isEmpty() ) {
							metadataCollector.addIdentifierGenerator( definition );
						}
					},
					context
			);
		}
	}

	private static IdentifierGeneratorDefinition buildSequenceIdGenerator(SequenceGenerator generatorAnnotation) {
		final var definitionBuilder = new IdentifierGeneratorDefinition.Builder();
		interpretSequenceGenerator( generatorAnnotation, definitionBuilder );
		if ( BOOT_LOGGER.isTraceEnabled() ) {
			BOOT_LOGGER.addedSequenceGenerator( definitionBuilder.getName() );
		}
		return definitionBuilder.build();
	}

	private static IdentifierGeneratorDefinition buildTableIdGenerator(TableGenerator generatorAnnotation) {
		final var definitionBuilder = new IdentifierGeneratorDefinition.Builder();
		interpretTableGenerator( generatorAnnotation, definitionBuilder );
		if ( BOOT_LOGGER.isTraceEnabled() ) {
			BOOT_LOGGER.addedTableGenerator( definitionBuilder.getName() );
		}
		return definitionBuilder.build();
	}

	private static void checkGeneratorClass(Class<? extends Generator> generatorClass) {
		if ( !BeforeExecutionGenerator.class.isAssignableFrom( generatorClass )
				&& !OnExecutionGenerator.class.isAssignableFrom( generatorClass ) ) {
			throw new MappingException( "Generator class '" + generatorClass.getName()
					+ "' must implement either 'BeforeExecutionGenerator' or 'OnExecutionGenerator'" );
		}
	}

	private static <A extends Annotation> Generator instantiateAndInitializeGenerator(
			Value value,
			A annotation,
			BeanContainer beanContainer,
			GeneratorCreationContext creationContext,
			Class<? extends Generator> generatorClass,
			MemberDetails memberDetails,
			Class<A> annotationType) {
		final var generator = instantiateGenerator(
				annotation,
				beanContainer,
				creationContext,
				generatorClass,
				memberDetails,
				annotationType
		);
		callInitialize( annotation, creationContext, generator );
		callConfigure( creationContext, generator, emptyMap() );
		return generator;
	}

	/**
	 * Return a {@link GeneratorCreator} for an id attribute annotated
	 * with an {@linkplain IdGeneratorType id generator annotation}.
	 */
	private static <A extends Annotation> GeneratorCreator identifierGeneratorCreator(
			A annotation,
			ModelsContext modelsContext) {
		@SuppressWarnings("unchecked") // totally fine
		final var annotationType = (Class<A>) annotation.annotationType();
		final var idGeneratorAnnotation = annotationType.getAnnotation( IdGeneratorType.class );
		assert idGeneratorAnnotation != null;
		final var generatorClass = idGeneratorAnnotation.value();
		checkGeneratorClass( generatorClass );
		return new IdentifierGeneratorCreator<>( annotation, annotationType, generatorClass, modelsContext );
	}

	private static final class IdentifierGeneratorCreator<A extends Annotation> implements GeneratorCreator {
		private final String annotationTypeName;
		private final String generatorClassName;
		private transient A annotation;
		private transient Class<A> annotationType;
		private transient Class<? extends Generator> generatorClass;
		private transient ModelsContext modelsContext;

		private IdentifierGeneratorCreator(
				A annotation,
				Class<A> annotationType,
				Class<? extends Generator> generatorClass,
				ModelsContext modelsContext) {
			this.annotation = annotation;
			this.annotationType = annotationType;
			this.generatorClass = generatorClass;
			this.annotationTypeName = annotationType.getName();
			this.generatorClassName = generatorClass.getName();
			this.modelsContext = modelsContext;
		}

		@Override
		@SuppressWarnings("unchecked")
		public Generator createGenerator(GeneratorCreationContext creationContext) {
			final MemberDetails contextMember = creationContext.getMemberDetails();
			final var idAttributeMember =
					contextMember != null || creationContext.getProperty() == null
							? contextMember
							: creationContext.getProperty().getMemberDetails();
			final var identifierValue = (SimpleValue) creationContext.getValue();
			final Class<A> resolvedAnnotationType = (Class<A>) resolveClass(
					annotationType,
					annotationTypeName,
					"identifier generator annotation",
					Annotation.class,
					identifierValue
			);
			final Class<? extends Generator> resolvedGeneratorClass = resolveClass(
					generatorClass,
					generatorClassName,
					"identifier generator implementation",
					Generator.class,
					identifierValue
			);
			annotationType = resolvedAnnotationType;
			generatorClass = resolvedGeneratorClass;
			final A restoredAnnotation = annotation != null
					? annotation
					: idAttributeMember.locateAnnotationUsage( resolvedAnnotationType, modelsContext );
			if ( restoredAnnotation == null ) {
				throw new MappingException(
						"Could not reconstruct identifier generator annotation '" + annotationTypeName
								+ "' for mapping role '" + identifierValue.getMappingRole() + "'"
				);
			}
			annotation = restoredAnnotation;
			final var localizedAnnotation = localizedIdentifierGeneratorAnnotation(
					restoredAnnotation,
					idAttributeMember,
					creationContext,
					modelsContext
			);
			final var generator = instantiateAndInitializeGenerator(
					identifierValue,
					localizedAnnotation,
					beanContainer( creationContext ),
					creationContext,
					resolvedGeneratorClass,
					idAttributeMember,
					resolvedAnnotationType
			);
			checkIdGeneratorTiming( resolvedAnnotationType, generator );
			if ( generator.requiresIdentityColumn() ) {
				identifierValue.setColumnToIdentity();
			}
			return generator;
		}

		@SuppressWarnings("unchecked")
		private <T> Class<? extends T> resolveClass(
				Class<? extends T> resolvedClass,
				String className,
				String archiveRole,
				Class<T> contract,
				SimpleValue identifierValue) {
			if ( resolvedClass != null ) {
				return resolvedClass;
			}
			try {
				final Class<?> candidate = modelsContext.getClassDetailsRegistry()
						.resolveClassDetails( className )
						.toJavaClass();
				if ( !contract.isAssignableFrom( candidate ) ) {
					throw new MappingException(
							"Archived " + archiveRole + " class '" + className
									+ "' does not implement '" + contract.getName() + "'"
					);
				}
				return (Class<? extends T>) candidate;
			}
			catch (RuntimeException e) {
				throw new MappingException(
						"Could not resolve archived " + archiveRole + " class '" + className + "' for mapping role '"
								+ identifierValue.getMappingRole() + "'",
						e
				);
			}
		}

		@Override
		public void reattachModelsContext(ModelsContext modelsContext) {
			this.modelsContext = modelsContext;
		}
	}

	@SuppressWarnings("unchecked")
	private static <A extends Annotation> A localizedIdentifierGeneratorAnnotation(
			A annotation,
			MemberDetails idAttributeMember,
			GeneratorCreationContext creationContext,
			ModelsContext modelsContext) {
		if ( annotation.annotationType() != GenericGenerator.class ) {
			return annotation;
		}

		final var rootClass = creationContext.getRootClass();
		final String rootClassName = rootClass.getClassName();
		if ( rootClassName == null || rootClassName.equals( idAttributeMember.getDeclaringType().getName() ) ) {
			return annotation;
		}

		final var entityType = modelsContext.getClassDetailsRegistry()
				.getClassDetails( rootClassName );
		final var localized = findLocalizedMatch(
				HibernateAnnotations.GENERIC_GENERATOR,
				idAttributeMember,
				entityType,
				null,
				null,
				modelsContext
		);
		return localized == null ? annotation : (A) localized;
	}

	/**
	 * Instantiate a {@link Generator}, using the given {@link BeanContainer} if any,
	 * for the case where the generator was specified using a generator annotation.
	 *
	 * @param annotation the generator annotation
	 * @param beanContainer an optional {@code BeanContainer}
	 * @param generatorClass a class which implements {@code Generator}
	 */
	private static <A extends Annotation> Generator instantiateGenerator(
			A annotation,
			BeanContainer beanContainer,
			GeneratorCreationContext creationContext,
			Class<? extends Generator> generatorClass,
			MemberDetails memberDetails,
			Class<A> annotationType) {
		if ( beanContainer != null ) {
			return instantiateGeneratorAsBean(
					annotation,
					beanContainer,
					creationContext,
					generatorClass,
					memberDetails,
					annotationType
			);
		}
		else {
			return instantiateGenerator(
					annotation,
					memberDetails,
					annotationType,
					creationContext,
					generatorClass
			);
		}
	}

	/**
	 * Instantiate a {@link Generator}, using the given {@link BeanContainer},
	 * for the case where the generator was specified using a generator annotation.
	 *
	 * @param annotation the generator annotation
	 * @param beanContainer an optional {@code BeanContainer}
	 * @param generatorClass a class which implements {@code Generator}
	 */
	private static <T extends Generator, A extends Annotation> Generator instantiateGeneratorAsBean(
			A annotation,
			BeanContainer beanContainer,
			GeneratorCreationContext creationContext,
			Class<T> generatorClass,
			MemberDetails memberDetails,
			Class<A> annotationType) {
		return getBean(
			beanContainer,
			generatorClass,
			false,
			true,
			() -> instantiateGenerator(
				annotation,
				memberDetails,
				annotationType,
				creationContext,
				generatorClass
			)
		);
	}

	/**
	 * Instantiate a {@link Generator}, using the given {@link BeanContainer},
	 * for the case where no generator annotation is available.
	 *
	 * @param beanContainer an optional {@code BeanContainer}
	 * @param generatorClass a class which implements {@code Generator}
	 */
	private static <T extends Generator> T instantiateGeneratorAsBean(
			BeanContainer beanContainer,
			Class<T> generatorClass) {
		return getBean(
			beanContainer,
			generatorClass,
			false,
			true,
			() -> instantiateGeneratorViaDefaultConstructor( generatorClass )
		);
	}

	/**
	 * Instantiate a {@link Generator} by calling an appropriate constructor,
	 * for the case where the generator was specified using a generator annotation.
	 * We look for three possible signatures:
	 * <ol>
	 *     <li>{@code (Annotation, Member, GeneratorCreationContext)}</li>
	 *     <li>{@code (Annotation)}</li>
	 *     <li>{@code ()}</li>
	 * </ol>
	 * where {@code Annotation} is the generator annotation type.
	 *
	 * @param annotation the generator annotation
	 * @param generatorClass a class which implements {@code Generator}
	 */
	private static <G extends Generator, A extends Annotation> G instantiateGenerator(
			A annotation,
			MemberDetails memberDetails,
			Class<A> annotationType,
			GeneratorCreationContext creationContext,
			Class<? extends G> generatorClass) {
		try {
			G generator =
					// support for deprecated signature (eventually remove)
					memberDetails == null
							? null
							: construct( generatorClass,
									annotationType, annotation,
									Member.class, memberDetails.toJavaMember(),
									GeneratorCreationContext.class, creationContext );
			if ( generator != null ) {
				return generator;
			}
			generator =
					construct( generatorClass,
							annotationType, annotation,
							GeneratorCreationContext.class, creationContext );
			if ( generator != null ) {
				return generator;
			}
			generator = construct( generatorClass, annotationType, annotation );
			if ( generator != null ) {
				return generator;
			}
			generator = construct( generatorClass, GeneratorCreationContext.class, creationContext );
			if ( generator != null ) {
				return generator;
			}
			return instantiateGeneratorViaDefaultConstructor( generatorClass );
		}
		catch (InvocationTargetException | InstantiationException | IllegalAccessException | IllegalArgumentException e) {
			throw new org.hibernate.InstantiationException( "Could not instantiate id generator", generatorClass, e );
		}
	}

	/**
	 * Instantiate a {@link Generator}, using the given {@link BeanContainer} if any,
	 * or by calling the default constructor otherwise.
	 *
	 * @param beanContainer an optional {@code BeanContainer}
	 * @param generatorClass a class which implements {@code Generator}
	 */
	public static <T extends Generator> T instantiateGenerator(BeanContainer beanContainer, Class<T> generatorClass) {
		return beanContainer != null
				? instantiateGeneratorAsBean( beanContainer, generatorClass )
				: instantiateGeneratorViaDefaultConstructor( generatorClass );
	}

	/**
	 * Instantiate a {@link Generator} by calling the default constructor.
	 */
	private static <G extends Generator> G instantiateGeneratorViaDefaultConstructor(Class<? extends G> generatorClass) {
		try {
			final var constructor = generatorClass.getDeclaredConstructor();
			constructor.setAccessible( true );
			return constructor.newInstance();
		}
		catch (NoSuchMethodException e) {
			throw new org.hibernate.InstantiationException( "No appropriate constructor for id generator class", generatorClass);
		}
		catch (Exception e) {
			throw new org.hibernate.InstantiationException( "Could not instantiate id generator", generatorClass, e );
		}
	}

	private static <A extends Annotation> void callInitialize(
			A annotation,
			GeneratorCreationContext creationContext,
			Generator generator) {
		if ( generator instanceof AnnotationBasedGenerator<?> annotationBasedGenerator ) {
			initializeGenerator( annotationBasedGenerator, annotation, creationContext );
		}
	}

	private static void checkVersionGenerationAlways(MemberDetails property, Generator generator) {
		if ( property.hasDirectAnnotationUsage( Version.class ) ) {
			if ( !generator.generatesOnInsert() ) {
				throw new AnnotationException("Property '" + property.getName()
						+ "' is annotated '@Version' but has a 'Generator' which does not generate on inserts"
				);
			}
			if ( !generator.generatesOnUpdate() ) {
				throw new AnnotationException("Property '" + property.getName()
						+ "' is annotated '@Version' but has a 'Generator' which does not generate on updates"
				);
			}
		}
	}

	/**
	 * If the given {@link Generator} also implements {@link Configurable},
	 * call its {@link Configurable#configure(GeneratorCreationContext, Properties)
	 * configure()} method.
	 */
	private static void callConfigure(
			GeneratorCreationContext creationContext,
			Generator generator,
			Map<String, Object> configuration) {
		if ( generator instanceof Configurable configurable ) {
			final var parameters = collectParameters(
					creationContext,
					configuration,
					creationContext.getServiceRegistry()
							.requireService( ConfigurationService.class )
			);
			configurable.configure( creationContext, parameters );
		}
		if ( generator instanceof ExportableProducer exportableProducer ) {
			exportableProducer.registerExportables( creationContext.getDatabase() );
		}
		if ( generator instanceof Configurable configurable ) {
			configurable.initialize( creationContext.getSqlStringGenerationContext() );
		}
	}

	private static void checkIdGeneratorTiming(Class<? extends Annotation> annotationType, Generator generator) {
		if ( !generator.generatesOnInsert() ) {
			throw new MappingException( "Annotation '" + annotationType
					+ "' is annotated 'IdGeneratorType' but the given 'Generator' does not generate on inserts" );
		}
		if ( generator.generatesOnUpdate() ) {
			throw new MappingException( "Annotation '" + annotationType
					+ "' is annotated 'IdGeneratorType' but the given 'Generator' generates on updates (it must generate only on inserts)" );
		}
	}

	/**
	 * Create a generator, based on a {@link GeneratedValue} annotation.
	 */
	private static void createIdGenerator(
			MemberDetails idMember,
			SimpleValue idValue,
			PersistentClass persistentClass,
			MetadataBuildingContext context) {
		// NOTE: generatedValue is never null here
		final var generatedValue = castNonNull( idMember.getDirectAnnotationUsage( GeneratedValue.class ) );
		final var metadataCollector = context.getMetadataCollector();
		if ( isGlobalGeneratorNameGlobal( context ) ) {
			// process and register any generators defined on the member.
			// according to JPA these are also global.
			metadataCollector.getGlobalRegistrations().as( GlobalRegistrar.class ).collectIdGenerators( idMember );
			new StrictIdentifierGeneratorResolver(
					persistentClass,
					idValue,
					idMember,
					generatedValue,
					context
			).resolveIdentifierGenerator();
		}
		else {
			new IdentifierGeneratorResolver(
					persistentClass,
					idValue,
					idMember,
					generatedValue,
					context
			).resolveIdentifierGenerator();
		}
	}

	public static void resolveGeneratedValueGenerator(
			PersistentClass persistentClass,
			SimpleValue idValue,
			MemberDetails idMember,
			GeneratedValue generatedValue,
			MetadataBuildingContext context) {
		if ( isGlobalGeneratorNameGlobal( context ) ) {
			context.getMetadataCollector()
					.getGlobalRegistrations()
					.as( GlobalRegistrar.class )
					.collectIdGenerators( idMember );
			new StrictIdentifierGeneratorResolver(
					persistentClass,
					idValue,
					idMember,
					generatedValue,
					context
			).resolveIdentifierGenerator();
		}
		else {
			new IdentifierGeneratorResolver(
					persistentClass,
					idValue,
					idMember,
					generatedValue,
					context
			).resolveIdentifierGenerator();
		}
	}

	public static void createGeneratorFrom(
			IdentifierGeneratorDefinition defaultedGenerator,
			SimpleValue idValue,
			Map<String, Object> configuration,
			MetadataBuildingContext context) {
		configuration.putAll( defaultedGenerator.getParameters() );
		instantiateNamedStrategyGenerator( idValue, defaultedGenerator.getStrategy(), configuration );
	}


	public static void createGeneratorFrom(
			IdentifierGeneratorDefinition defaultedGenerator,
			SimpleValue idValue,
			MetadataBuildingContext context) {
		createGeneratorFrom( defaultedGenerator, idValue, buildConfigurationMap( idValue, context ), context );
	}

	private static Map<String, Object> buildConfigurationMap(
			KeyValue idValue,
			MetadataBuildingContext context) {
		final Map<String,Object> configuration = new HashMap<>();
		applyTableDetails( idValue, configuration, context );
		if ( idValue.getColumnSpan() == 1 ) {
			configuration.put( PersistentIdentifierGenerator.PK, idValue.getColumns().get(0).getName() );
		}
		return configuration;
	}

	/**
	 * Obtain a {@link BeanContainer} to be used for instantiating generators.
	 */
	public static BeanContainer beanContainer(MetadataBuildingContext buildingContext) {
		return Helper.getBeanContainer(
				buildingContext.getConfigurationService(),
				buildingContext.getManagedBeanRegistry()
		);
	}

	public static BeanContainer beanContainer(GeneratorCreationContext creationContext) {
		final ServiceRegistry serviceRegistry = creationContext.getServiceRegistry();
		return Helper.getBeanContainer(
				serviceRegistry.requireService( ConfigurationService.class ),
				serviceRegistry.requireService( ManagedBeanRegistry.class )
		);
	}

	/**
	 * Set up the {@link GeneratorCreator} for a case where there is no
	 * generator annotation.
	 */
	private static void setGeneratorCreator(
			SimpleValue identifierValue,
			Map<String, Object> configuration,
			String generatorStrategy,
			MetadataBuildingContext context) {
		if ( ASSIGNED_GENERATOR_NAME.equals( generatorStrategy )
				|| org.hibernate.id.Assigned.class.getName().equals( generatorStrategy ) ) {
			identifierValue.setCustomIdGeneratorCreator( ASSIGNED_IDENTIFIER_GENERATOR_CREATOR );
		}
		else {
			instantiateNamedStrategyGenerator( identifierValue, generatorStrategy, configuration );
		}
	}

	private static void instantiateNamedStrategyGenerator(
			SimpleValue identifierValue,
			String generatorStrategy,
			Map<String, Object> configuration) {
		identifierValue.setCustomIdGeneratorCreator( creationContext -> {
			final var restoredIdentifierValue = (SimpleValue) creationContext.getValue();
			final var identifierGenerator =
					instantiateGenerator(
							beanContainer( creationContext ),
							generatorClass(
									generatorStrategy,
									creationContext.getDatabase().getDialect(),
									creationContext.getServiceRegistry().requireService( ClassLoaderService.class )
							)
					);
			// in this code path, there's no generator annotation,
			// and therefore no need to call initialize()
			callConfigure( creationContext, identifierGenerator, configuration );
			if ( identifierGenerator.requiresIdentityColumn() ) {
				restoredIdentifierValue.setColumnToIdentity();
			}

			return identifierGenerator;
		} );
	}

	public static boolean createIdGeneratorFromGeneratorAnnotation(
			SimpleValue idValue,
			MemberDetails idMemberDetails,
			MetadataBuildingContext buildingContext,
			String attributePath) {
		return createIdGeneratorFromGeneratorAnnotation(
				idValue,
				idMemberDetails,
				idMemberDetails,
				buildingContext,
				attributePath
		);
	}

	public static boolean createIdGeneratorFromGeneratorAnnotation(
			SimpleValue idValue,
			MemberDetails idMemberDetails,
			AnnotationTarget generatorAnnotationTarget,
			MetadataBuildingContext buildingContext,
			String attributePath) {
		final var modelsContext = buildingContext.getModelsContext();
		final var idGeneratorAnnotations = generatorAnnotationTarget.getMetaAnnotated( IdGeneratorType.class, modelsContext );
		final var generatorAnnotations = generatorAnnotationTarget.getMetaAnnotated( ValueGenerationType.class, modelsContext );
		// Since these collections may contain proxies created by common-annotations module, we cannot reliably use
		// simple remove/removeAll collection methods as those proxies do not implement hashcode/equals and even a
		// simple 'a.equals(a)' will return 'false'. Instead, we will check the annotation types. Since generator
		// annotations should not be "repeatable", we should have only at most one annotation for a generator.
		for ( var id : idGeneratorAnnotations ) {
			generatorAnnotations.removeIf( gen -> gen.annotationType().equals( id.annotationType() ) );
		}

		if ( idGeneratorAnnotations.size() + generatorAnnotations.size() > 1 ) {
			throw new AnnotationException( String.format(
					Locale.ROOT,
					"Identifier attribute '%s' has too many generator annotations: %s",
					attributePath,
					combineUntyped( idGeneratorAnnotations, generatorAnnotations )
			) );
		}
		if ( !idGeneratorAnnotations.isEmpty() ) {
			idValue.setCustomIdGeneratorCreator( identifierGeneratorCreator(
					idGeneratorAnnotations.get(0),
					buildingContext.getModelsContext()
			) );
			return true;
		}
		else if ( !generatorAnnotations.isEmpty() ) {
			throw new AnnotationException( String.format(
					Locale.ROOT,
					"Identifier attribute '%s' is annotated '%s' which is not an '@IdGeneratorType'",
					attributePath,
					generatorAnnotations.get(0).annotationType().getName()
			) );
		}
		return false;
	}

	public static void applyIfNotEmpty(String name, String value, BiConsumer<String,String> consumer) {
		if ( isNotEmpty( value ) ) {
			consumer.accept( name, value );
		}
	}

	public static Generator createIdentifierGenerator(
			KeyValue identifierValue,
			Dialect dialect,
			RootClass rootClass,
			Property property,
			GeneratorSettings defaults,
			Database database,
			ServiceRegistry serviceRegistry,
			PropertyAccessStrategyResolver propertyAccessStrategyResolver) {
		if ( identifierValue instanceof Component component
				&& component.getCustomIdGeneratorCreator().isAssigned() ) {
			return buildIdentifierGenerator(
					component,
					dialect,
					rootClass,
					defaults,
					database,
					serviceRegistry,
					propertyAccessStrategyResolver
			);
		}
		if ( identifierValue instanceof SimpleValue simpleValue ) {
			return createIdentifierGenerator( simpleValue, rootClass, property, defaults, database, serviceRegistry );
		}
		throw new MappingException( "Identifier value is not a SimpleValue: " + identifierValue );
	}

	private static Generator createIdentifierGenerator(
			SimpleValue identifierValue,
			RootClass rootClass,
			Property property,
			GeneratorSettings defaults,
			Database database,
			ServiceRegistry serviceRegistry) {
		final var generatorCreator = identifierValue.getCustomIdGeneratorCreator();
		if ( generatorCreator == null ) {
			return null;
		}

		final var context = new IdGeneratorCreationContext(
				identifierValue,
				rootClass,
				property,
				defaults,
				database,
				serviceRegistry
		);
		final var generator = generatorCreator.createGenerator( context );
		GeneratorTypeHelper.checkGeneratorGeneratedType( generator, context );
		if ( generator.allowAssignedIdentifiers() && identifierValue.getNullValue() == null ) {
			identifierValue.setNullValueUndefined();
		}
		return generator;
	}

	private record IdGeneratorCreationContext(
			SimpleValue identifier,
			RootClass rootClass,
			Property property,
			GeneratorSettings defaults,
			Database database,
			ServiceRegistry serviceRegistry) implements GeneratorCreationContext {

		@Override
		public Database getDatabase() {
			return database;
		}

		@Override
		public ServiceRegistry getServiceRegistry() {
			return serviceRegistry;
		}

		@Override
		public SqlStringGenerationContext getSqlStringGenerationContext() {
			return defaults.getSqlStringGenerationContext();
		}

		@Override
		public String getDefaultCatalog() {
			return defaults.getDefaultCatalog();
		}

		@Override
		public String getDefaultSchema() {
			return defaults.getDefaultSchema();
		}

		@Override
		public RootClass getRootClass() {
			return rootClass;
		}

		@Override
		public PersistentClass getPersistentClass() {
			return rootClass;
		}

		@Override
		public Property getProperty() {
			return property;
		}

		@Override
		public Value getValue() {
			return identifier;
		}

		@Override
		public Type getType() {
			return identifier.getType();
		}

		@Override
		public MemberDetails getMemberDetails() {
			final MemberDetails identifierMember = identifier.getMemberDetails();
			return identifierMember != null || property == null
					? identifierMember
					: property.getMemberDetails();
		}
	}

	private static Setter injector(
			Property property,
			Class<?> attributeDeclarer,
			PropertyAccessStrategyResolver propertyAccessStrategyResolver) {
		return property.getPropertyAccessStrategy(
						attributeDeclarer,
						propertyAccessStrategyResolver
				)
				.buildPropertyAccess( attributeDeclarer, property.getName(), true )
				.getSetter();
	}

	/**
	 * Return the class that declares the composite pk attributes,
	 * which might be an {@code @IdClass}, an {@code @EmbeddedId},
	 * of the entity class itself.
	 */
	private static Class<?> getAttributeDeclarer(RootClass rootClass, Component component) {
		// See the javadoc discussion on CompositeNestedGeneratedValueGenerator
		// for the various scenarios we need to account for here
		if ( rootClass.getIdentifierMapper() != null ) {
			// we have the @IdClass / <composite-id mapped="true"/> case
			return resolveComponentClass( component );
		}
		else if ( rootClass.getIdentifierProperty() != null ) {
			// we have the "@EmbeddedId" / <composite-id name="idName"/> case
			return resolveComponentClass( component );
		}
		else {
			// we have the "straight up" embedded (again the Hibernate term)
			// component identifier: the entity class itself is the id class
			return rootClass.getMappedClass();
		}
	}

	private static Class<?> resolveComponentClass(Component component) {
		try {
			return component.getComponentClass();
		}
		catch ( Exception e ) {
			return null;
		}
	}

	public static Generator buildIdentifierGenerator(
			Component component,
			Dialect dialect,
			RootClass rootClass,
			GeneratorSettings defaults,
			Database database,
			ServiceRegistry serviceRegistry,
			PropertyAccessStrategyResolver propertyAccessStrategyResolver) {
		component.requireShapeComplete();
		final var properties = component.getProperties();
		final List<Generator> generators = new ArrayList<>( properties.size() );
		final int columnSpan = component.getColumnSpan();
		String[] columnValues = null;
		boolean[] columnInclusions = null;
		boolean[] generatedOnExecutionColumns = null;
		int columnIndex = 0;
		final List<GenerationPlan> generationPlans = new ArrayList<>();
		for ( int i = 0; i < properties.size(); i++ ) {
			final var property = properties.get( i );
			final var propertyGenerator =
					propertyGenerator(
							component,
							dialect,
							rootClass,
							defaults,
							property,
							generationPlans,
							i,
							database,
							serviceRegistry,
							propertyAccessStrategyResolver
					);
			generators.add( propertyGenerator );

			final int span = property.getColumnSpan();
			if ( propertyGenerator instanceof OnExecutionGenerator onExecutionGenerator
					&& propertyGenerator.generatedOnExecution() ) {
				if ( columnValues == null ) {
					columnValues = new String[columnSpan];
					columnInclusions = new boolean[columnSpan];
					generatedOnExecutionColumns = new boolean[columnSpan];
					for ( int j = 0; j < columnSpan; j++ ) {
						columnValues[j] = "?";
						columnInclusions[j] = true;
					}
				}
				for ( int j = 0; j < span; j++ ) {
					generatedOnExecutionColumns[columnIndex + j] = true;
				}
				if ( onExecutionGenerator.generatesOnInsert() ) {
					if ( !onExecutionGenerator.referenceColumnsInSql( dialect, EventType.INSERT ) ) {
						for ( int j = 0; j < span; j++ ) {
							columnInclusions[columnIndex + j] = false;
						}
					}
					else if ( onExecutionGenerator.writePropertyValue( EventType.INSERT ) ) {
						// leave default parameter markers in place
					}
					else {
						final String[] referencedColumnValues =
								onExecutionGenerator.getReferencedColumnValues( dialect, EventType.INSERT );
						if ( referencedColumnValues == null ) {
							throw new IdentifierGenerationException(
									"Generated column values were not provided for composite id property: "
											+ property.getName()
							);
						}
						if ( referencedColumnValues.length != span ) {
							throw new IdentifierGenerationException(
									"Mismatch between generated column values and column count for composite id property: "
											+ property.getName()
							);
						}
						System.arraycopy( referencedColumnValues, 0, columnValues, columnIndex, span );
					}
				}
				else if ( !onExecutionGenerator.allowMutation() ) {
					for ( int j = 0; j < span; j++ ) {
						columnInclusions[columnIndex + j] = false;
					}
				}
			}
			columnIndex += span;
		}

		final var generator =
				new CompositeNestedGeneratedValueGenerator(
						new Component.StandardGenerationContextLocator( rootClass.getEntityName() ),
						(ComponentType) component.getType(),
						generators,
						columnValues,
						columnInclusions,
						generatedOnExecutionColumns
				);
		for ( var plan : generationPlans ) {
			generator.addGeneratedValuePlan( plan );
		}
		return generator;
	}

	private static Generator propertyGenerator(
			Component component,
			Dialect dialect,
			RootClass rootClass,
			GeneratorSettings defaults,
			Property property,
			List<GenerationPlan> generationPlans,
			int propertyIndex,
			Database database,
			ServiceRegistry serviceRegistry,
			PropertyAccessStrategyResolver propertyAccessStrategyResolver) {
		final var value = property.getValue();
		if ( value instanceof SimpleValue simpleValue ) {
			if ( !simpleValue.getCustomIdGeneratorCreator().isAssigned() ) {
				// skip any 'assigned' generators, they would have been
				// handled by the StandardGenerationContextLocator
				final var propertyGenerator =
						createIdentifierGenerator( simpleValue, rootClass, property, defaults, database, serviceRegistry );
				if ( propertyGenerator instanceof BeforeExecutionGenerator beforeExecutionGenerator ) {
					generationPlans.add( new Component.ValueGenerationPlan(
							beforeExecutionGenerator,
							component.getType().isMutable()
									? injector( property, getAttributeDeclarer( rootClass, component ), propertyAccessStrategyResolver )
									: null,
							propertyIndex
					) );
				}
				return propertyGenerator;
			}
			else {
				return null;
			}
		}
		else {
			return null;
		}
	}
}
