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
import org.hibernate.boot.model.relational.ExportableProducer;
import org.hibernate.boot.model.source.internal.hbm.MappingDocument;
import org.hibernate.boot.models.spi.GlobalRegistrar;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.PropertyData;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.generator.AnnotationBasedGenerator;
import org.hibernate.generator.Assigned;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.Generator;
import org.hibernate.generator.GeneratorCreationContext;
import org.hibernate.generator.OnExecutionGenerator;
import org.hibernate.id.Configurable;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.IdentityGenerator;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.id.uuid.UuidValueGenerator;
import org.hibernate.mapping.GeneratorCreator;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Value;
import org.hibernate.models.spi.AnnotationTarget;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.resource.beans.container.spi.BeanContainer;
import org.hibernate.resource.beans.internal.Helper;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static jakarta.persistence.GenerationType.AUTO;
import static java.util.Collections.emptyMap;
import static org.hibernate.boot.model.internal.AnnotationHelper.extractParameterMap;
import static org.hibernate.boot.model.internal.BinderHelper.getPath;
import static org.hibernate.boot.model.internal.BinderHelper.isGlobalGeneratorNameGlobal;
import static org.hibernate.boot.model.internal.GeneratorParameters.collectParameters;
import static org.hibernate.boot.model.internal.GeneratorParameters.interpretSequenceGenerator;
import static org.hibernate.boot.model.internal.GeneratorParameters.interpretTableGenerator;
import static org.hibernate.boot.model.internal.GeneratorStrategies.generatorClass;
import static org.hibernate.id.IdentifierGenerator.GENERATOR_NAME;
import static org.hibernate.internal.CoreMessageLogger.CORE_LOGGER;
import static org.hibernate.internal.util.NullnessUtil.castNonNull;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;
import static org.hibernate.internal.util.StringHelper.qualify;
import static org.hibernate.internal.util.collections.CollectionHelper.combineUntyped;

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
			MemberDetails idAttributeMember,
			String generatorType,
			String generatorName,
			MetadataBuildingContext context,
			Map<String, ? extends IdentifierGeneratorDefinition> localGenerators) {

		//generator settings
		final Map<String,Object> configuration = new HashMap<>();
		configuration.put( GENERATOR_NAME, generatorName );
		configuration.put( PersistentIdentifierGenerator.TABLE, identifierValue.getTable().getName() );
		if ( identifierValue.getColumnSpan() == 1 ) {
			configuration.put( PersistentIdentifierGenerator.PK, identifierValue.getColumns().get(0).getName() );
		}

		if ( generatorName.isEmpty() ) {
			final var generatedValue = idAttributeMember.getDirectAnnotationUsage( GeneratedValue.class );
			if ( generatedValue != null ) {
				// The mapping used @GeneratedValue but specified no name.  This is a special case added in JPA 3.2.
				// Look for a matching "implied generator" based on the GenerationType
				final var strategy = generatedValue.strategy();
				final String strategyGeneratorClassName = correspondingGeneratorName( strategy );
				final var impliedGenerator =
						determineImpliedGenerator( strategy, strategyGeneratorClassName, localGenerators );
				if ( impliedGenerator != null ) {
					configuration.putAll( impliedGenerator.getParameters() );
					instantiateNamedStrategyGenerator( identifierValue, strategyGeneratorClassName, configuration, context );
					return;
				}
			}
		}

		final String generatorStrategy = determineStrategy(
				idAttributeMember,
				generatorType,
				generatorName,
				context,
				localGenerators,
				configuration
		);
		setGeneratorCreator( identifierValue, configuration, generatorStrategy, context );
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
			// 		 we consider it a match, based on strategy, if the strategy is AUTO or matches
			if ( strategy == AUTO
					|| isImpliedGenerator( strategy, strategyGeneratorClassName, generatorDefinition ) ) {
				return generatorDefinition;
			}
		}

		IdentifierGeneratorDefinition matching = null;
		for ( var localGenerator : localGenerators.values() ) {
			if ( isImpliedGenerator( strategy, strategyGeneratorClassName, localGenerator ) ) {
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
			GenerationType strategy,
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
		if ( !generatorName.isEmpty() ) {
			//we have a named generator
			final var definition =
					makeIdentifierGeneratorDefinition( generatorName, idAttributeMember, localGenerators, context );
			if ( definition == null ) {
				throw new AnnotationException( "No id generator was declared with the name '" + generatorName
						+ "' specified by '@GeneratedValue'"
						+ " (define a named generator using '@SequenceGenerator', '@TableGenerator', or '@GenericGenerator')" );
			}
			//This is quite vague in the spec but a generator could override the generator choice
			final String generatorStrategy =
					generatorType == null
						//yuk! this is a hack not to override 'AUTO' even if generator is set
						|| !definition.getStrategy().equals( "identity" )
							? definition.getStrategy()
							: generatorType;
			//checkIfMatchingGenerator(definition, generatorType, generatorName);
			configuration.putAll( definition.getParameters() );
			return generatorStrategy;
		}
		else {
			return generatorType;
		}
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
		final var modelsContext = buildingContext.getBootstrapContext().getModelsContext();
		annotatedElement.forEachAnnotationUsage( TableGenerator.class, modelsContext,
				usage -> consumer.accept( buildTableIdGenerator( usage ) ) );
		annotatedElement.forEachAnnotationUsage( SequenceGenerator.class, modelsContext,
				usage -> consumer.accept( buildSequenceIdGenerator( usage ) ) );
		annotatedElement.forEachAnnotationUsage( GenericGenerator.class, modelsContext,
				usage -> consumer.accept( buildIdGenerator( usage ) ) );
	}

	public static void registerGlobalGenerators(
			AnnotationTarget annotatedElement,
			MetadataBuildingContext context) {
		if ( context.getBootstrapContext().getJpaCompliance().isGlobalGeneratorScopeEnabled() ) {
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

	private static IdentifierGeneratorDefinition buildIdGenerator(GenericGenerator generatorAnnotation) {
		final var definitionBuilder = new IdentifierGeneratorDefinition.Builder();
		definitionBuilder.setName( generatorAnnotation.name() );
		final var generatorClass = generatorAnnotation.type();
		final String strategy =
				generatorClass.equals( Generator.class )
						? generatorAnnotation.strategy()
						: generatorClass.getName();
		definitionBuilder.setStrategy( strategy );
		definitionBuilder.addParams( extractParameterMap( generatorAnnotation.parameters() ) );

		if ( CORE_LOGGER.isTraceEnabled() ) {
			CORE_LOGGER.tracev( "Added generator with name: {0}, strategy: {0}",
					definitionBuilder.getName(), definitionBuilder.getStrategy() );
		}
		return definitionBuilder.build();
	}

	private static IdentifierGeneratorDefinition buildSequenceIdGenerator(SequenceGenerator generatorAnnotation) {
		final var definitionBuilder = new IdentifierGeneratorDefinition.Builder();
		interpretSequenceGenerator( generatorAnnotation, definitionBuilder );
		if ( CORE_LOGGER.isTraceEnabled() ) {
			CORE_LOGGER.tracev( "Added sequence generator with name: {0}", definitionBuilder.getName() );
		}
		return definitionBuilder.build();
	}

	private static IdentifierGeneratorDefinition buildTableIdGenerator(TableGenerator generatorAnnotation) {
		final var definitionBuilder = new IdentifierGeneratorDefinition.Builder();
		interpretTableGenerator( generatorAnnotation, definitionBuilder );
		if ( CORE_LOGGER.isTraceEnabled() ) {
			CORE_LOGGER.tracev( "Added sequence generator with name: {0}", definitionBuilder.getName() );
		}
		return definitionBuilder.build();
	}

	private static void checkGeneratorClass(Class<? extends Generator> generatorClass) {
		if ( !BeforeExecutionGenerator.class.isAssignableFrom( generatorClass )
				&& !OnExecutionGenerator.class.isAssignableFrom( generatorClass ) ) {
			throw new MappingException("Generator class '" + generatorClass.getName()
					+ "' must implement either 'BeforeExecutionGenerator' or 'OnExecutionGenerator'");
		}
	}

	private static void checkGeneratorInterfaces(Class<? extends Generator> generatorClass) {
		// A regular value generator should not implement legacy IdentifierGenerator
		if ( IdentifierGenerator.class.isAssignableFrom( generatorClass ) ) {
			throw new AnnotationException("Generator class '" + generatorClass.getName()
					+ "' implements 'IdentifierGenerator' and may not be used with '@ValueGenerationType'");
		}
	}

	/**
	 * Return a {@link GeneratorCreator} for an attribute annotated
	 * with a {@linkplain ValueGenerationType generator annotation}.
	 */
	private static GeneratorCreator generatorCreator(
			MemberDetails memberDetails,
			Value value,
			Annotation annotation,
			BeanContainer beanContainer) {
		final var annotationType = annotation.annotationType();
		final var generatorAnnotation = annotationType.getAnnotation( ValueGenerationType.class );
		assert generatorAnnotation != null;
		final var generatorClass = generatorAnnotation.generatedBy();
		checkGeneratorClass( generatorClass );
		checkGeneratorInterfaces( generatorClass );
		return creationContext -> {
			final Generator generator =
					instantiateAndInitializeGenerator(
							value,
							annotation,
							beanContainer,
							creationContext,
							generatorClass,
							memberDetails,
							annotationType
					);
			checkVersionGenerationAlways( memberDetails, generator );
			return generator;
		};
	}

	private static Generator instantiateAndInitializeGenerator(
			Value value,
			Annotation annotation,
			BeanContainer beanContainer,
			GeneratorCreationContext creationContext,
			Class<? extends Generator> generatorClass,
			MemberDetails memberDetails,
			Class<? extends Annotation> annotationType) {
		final Generator generator = instantiateGenerator(
				annotation,
				beanContainer,
				creationContext,
				generatorClass,
				memberDetails,
				annotationType
		);
		callInitialize( annotation, memberDetails, creationContext, generator );
		callConfigure( creationContext, generator, emptyMap(), value );
		return generator;
	}

	/**
	 * Return a {@link GeneratorCreator} for an id attribute annotated
	 * with an {@linkplain IdGeneratorType id generator annotation}.
	 */
	private static GeneratorCreator identifierGeneratorCreator(
			MemberDetails idAttributeMember,
			Annotation annotation,
			SimpleValue identifierValue,
			BeanContainer beanContainer) {
		final var annotationType = annotation.annotationType();
		final var idGeneratorAnnotation = annotationType.getAnnotation( IdGeneratorType.class );
		assert idGeneratorAnnotation != null;
		final var generatorClass = idGeneratorAnnotation.value();
		checkGeneratorClass( generatorClass );
		return creationContext -> {
			final Generator generator =
					instantiateAndInitializeGenerator(
							identifierValue,
							annotation,
							beanContainer,
							creationContext,
							generatorClass,
							idAttributeMember,
							annotationType
					);
			checkIdGeneratorTiming( annotationType, generator );
			return generator;
		};
	}

	/**
	 * Instantiate a {@link Generator}, using the given {@link BeanContainer} if any,
	 * for the case where the generator was specified using a generator annotation.
	 *
	 * @param annotation the generator annotation
	 * @param beanContainer an optional {@code BeanContainer}
	 * @param generatorClass a class which implements {@code Generator}
	 */
	private static Generator instantiateGenerator(
			Annotation annotation,
			BeanContainer beanContainer,
			GeneratorCreationContext creationContext,
			Class<? extends Generator> generatorClass,
			MemberDetails memberDetails,
			Class<? extends Annotation> annotationType) {
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
	private static <T extends Generator> Generator instantiateGeneratorAsBean(
			Annotation annotation,
			BeanContainer beanContainer,
			GeneratorCreationContext creationContext,
			Class<T> generatorClass,
			MemberDetails memberDetails,
			Class<? extends Annotation> annotationType) {
		return Helper.getBean(
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
		return Helper.getBean(
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
	private static <G extends Generator> G instantiateGenerator(
			Annotation annotation,
			MemberDetails memberDetails,
			Class<? extends Annotation> annotationType,
			GeneratorCreationContext creationContext,
			Class<? extends G> generatorClass) {
		try {
			try {
				return generatorClass.getConstructor( annotationType, Member.class, GeneratorCreationContext.class )
						.newInstance( annotation, memberDetails.toJavaMember(), creationContext);
			}
			catch (NoSuchMethodException ignore) {
				try {
					return generatorClass.getConstructor( annotationType )
							.newInstance( annotation );
				}
				catch (NoSuchMethodException i) {
					return instantiateGeneratorViaDefaultConstructor( generatorClass );
				}
			}
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
			return generatorClass.getDeclaredConstructor().newInstance();
		}
		catch (NoSuchMethodException e) {
			throw new org.hibernate.InstantiationException( "No appropriate constructor for id generator class", generatorClass);
		}
		catch (Exception e) {
			throw new org.hibernate.InstantiationException( "Could not instantiate id generator", generatorClass, e );
		}
	}

	public static <A extends Annotation> void callInitialize(
			A annotation,
			MemberDetails memberDetails,
			GeneratorCreationContext creationContext,
			Generator generator) {
		if ( generator instanceof AnnotationBasedGenerator ) {
			// This will cause a CCE in case the generation type doesn't match the annotation type; As this would be
			// a programming error of the generation type developer and thus should show up during testing, we don't
			// check this explicitly; If required, this could be done e.g. using ClassMate
			@SuppressWarnings("unchecked")
			final var generation = (AnnotationBasedGenerator<A>) generator;
			generation.initialize( annotation, memberDetails.toJavaMember(), creationContext );
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
	public static void callConfigure(
			GeneratorCreationContext creationContext,
			Generator generator,
			Map<String, Object> configuration,
			Value value) {
		if ( generator instanceof Configurable configurable ) {
			final Properties parameters = collectParameters(
					value,
					creationContext.getDatabase().getDialect(),
					creationContext.getRootClass(),
					configuration,
					creationContext.getServiceRegistry().requireService( ConfigurationService.class )
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
					+ "' is annotated 'IdGeneratorType' but the given 'Generator' does not generate on inserts");
		}
		if ( generator.generatesOnUpdate() ) {
			throw new MappingException( "Annotation '" + annotationType
					+ "' is annotated 'IdGeneratorType' but the given 'Generator' generates on updates (it must generate only on inserts)");
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
		// NOTE: `generatedValue` is never null here
		final var generatedValue = castNonNull( idMember.getDirectAnnotationUsage( GeneratedValue.class ) );
		final var metadataCollector = context.getMetadataCollector();
		if ( isGlobalGeneratorNameGlobal( context ) ) {
			// process and register any generators defined on the member.
			// according to JPA these are also global.
			metadataCollector.getGlobalRegistrations().as( GlobalRegistrar.class ).collectIdGenerators( idMember );
			metadataCollector.addSecondPass( new StrictIdGeneratorResolverSecondPass(
					persistentClass,
					idValue,
					idMember,
					generatedValue,
					context
			) );
		}
		else {
			metadataCollector.addSecondPass( new IdGeneratorResolverSecondPass(
					persistentClass,
					idValue,
					idMember,
					generatedValue,
					context
			) );
		}
	}

	public static void createGeneratorFrom(
			IdentifierGeneratorDefinition defaultedGenerator,
			SimpleValue idValue,
			Map<String, Object> configuration,
			MetadataBuildingContext context) {
		configuration.putAll( defaultedGenerator.getParameters() );
		instantiateNamedStrategyGenerator( idValue, defaultedGenerator.getStrategy(), configuration, context );
	}


	public static void createGeneratorFrom(
			IdentifierGeneratorDefinition defaultedGenerator,
			SimpleValue idValue,
			MetadataBuildingContext context) {
		createGeneratorFrom( defaultedGenerator, idValue, buildConfigurationMap( idValue ), context );
	}

	private static Map<String, Object> buildConfigurationMap(KeyValue idValue) {
		final Map<String,Object> configuration = new HashMap<>();
		configuration.put( PersistentIdentifierGenerator.TABLE, idValue.getTable().getName() );
		if ( idValue.getColumnSpan() == 1 ) {
			configuration.put( PersistentIdentifierGenerator.PK, idValue.getColumns().get(0).getName() );
		}
		return configuration;
	}

	/**
	 * Set up the identifier generator for an id defined in a {@code hbm.xml} mapping.
	 *
	 * @see org.hibernate.boot.model.source.internal.hbm.ModelBinder
	 */
	public static void makeIdGenerator(
			final MappingDocument sourceDocument,
			IdentifierGeneratorDefinition definition,
			SimpleValue identifierValue,
			MetadataBuildingContext context) {

		if ( definition != null ) {
			// see if the specified generator name matches a registered <identifier-generator/>
			final var generatorDef =
					sourceDocument.getMetadataCollector()
							.getIdentifierGenerator( definition.getName() );
			final Map<String,Object> configuration = new HashMap<>();
			final String generatorStrategy;
			if ( generatorDef != null ) {
				generatorStrategy = generatorDef.getStrategy();
				configuration.putAll( generatorDef.getParameters() );
			}
			else {
				generatorStrategy = definition.getStrategy();
			}

			configuration.putAll( definition.getParameters() );

			setGeneratorCreator( identifierValue, configuration, generatorStrategy, context );
		}
	}

	/**
	 * Obtain a {@link BeanContainer} to be used for instantiating generators.
	 */
	public static BeanContainer beanContainer(MetadataBuildingContext buildingContext) {
		return Helper.getBeanContainer( buildingContext.getBootstrapContext().getServiceRegistry() );
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
			instantiateNamedStrategyGenerator( identifierValue, generatorStrategy, configuration, context );
		}
	}

	private static void instantiateNamedStrategyGenerator(
			SimpleValue identifierValue,
			String generatorStrategy,
			Map<String, Object> configuration,
			MetadataBuildingContext context) {
		final BeanContainer beanContainer = beanContainer( context );
		identifierValue.setCustomIdGeneratorCreator( creationContext -> {
			final Generator identifierGenerator =
					instantiateGenerator( beanContainer, generatorClass( generatorStrategy, identifierValue ) );
			// in this code path, there's no generator annotation,
			// and therefore no need to call initialize()
			callConfigure( creationContext, identifierGenerator, configuration, identifierValue );
			if ( identifierGenerator instanceof IdentityGenerator) {
				identifierValue.setColumnToIdentity();
			}
			return identifierGenerator;
		} );
	}

	/**
	 * Set up the id generator by considering all annotations of the identifier
	 * field, including {@linkplain IdGeneratorType id generator annotations},
	 * and {@link GeneratedValue}.
	 */
	static void createIdGeneratorsFromGeneratorAnnotations(
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			SimpleValue idValue,
			MetadataBuildingContext buildingContext) {
		final var modelsContext = buildingContext.getBootstrapContext().getModelsContext();
		final var idMemberDetails = inferredData.getAttributeMember();
		final var idGeneratorAnnotations = idMemberDetails.getMetaAnnotated( IdGeneratorType.class, modelsContext );
		final var generatorAnnotations = idMemberDetails.getMetaAnnotated( ValueGenerationType.class, modelsContext );
		// Since these collections may contain Proxies created by common-annotations module we cannot reliably use simple remove/removeAll
		// collection methods as those proxies do not implement hashcode/equals and even a simple `a.equals(a)` will return `false`.
		// Instead, we will check the annotation types, since generator annotations should not be "repeatable" we should have only
		// at most one annotation for a generator:
		for ( Annotation id : idGeneratorAnnotations ) {
			generatorAnnotations.removeIf( gen -> gen.annotationType().equals( id.annotationType() ) );
		}

		if ( idGeneratorAnnotations.size() + generatorAnnotations.size() > 1 ) {
			throw new AnnotationException( String.format(
					Locale.ROOT,
					"Identifier attribute '%s' has too many generator annotations: %s",
					getPath( propertyHolder, inferredData ),
					combineUntyped( idGeneratorAnnotations, generatorAnnotations )
			) );
		}
		if ( !idGeneratorAnnotations.isEmpty() ) {
			idValue.setCustomIdGeneratorCreator( identifierGeneratorCreator(
					idMemberDetails,
					idGeneratorAnnotations.get(0),
					idValue,
					beanContainer( buildingContext )
			) );
		}
		else if ( !generatorAnnotations.isEmpty() ) {
//			idValue.setCustomGeneratorCreator( generatorCreator( idMemberDetails, generatorAnnotation ) );
			throw new AnnotationException( String.format(
					Locale.ROOT,
					"Identifier attribute '%s' is annotated '%s' which is not an '@IdGeneratorType'",
					getPath( propertyHolder, inferredData ),
					generatorAnnotations.get(0).annotationType().getName()
			) );
		}
		else if ( idMemberDetails.hasDirectAnnotationUsage( GeneratedValue.class ) ) {
			createIdGenerator( idMemberDetails, idValue, propertyHolder.getPersistentClass(), buildingContext );
		}
	}

	/**
	 * Returns the value generation strategy for the given property, if any, by
	 * considering {@linkplain ValueGenerationType generator type annotations}.
	 */
	static GeneratorCreator createValueGeneratorFromAnnotations(
			PropertyHolder holder, String propertyName,
			Value value, MemberDetails property, MetadataBuildingContext buildingContext) {
		final var generatorAnnotations =
				property.getMetaAnnotated( ValueGenerationType.class,
						buildingContext.getBootstrapContext().getModelsContext() );
		return switch ( generatorAnnotations.size() ) {
			case 0 -> null;
			case 1 -> generatorCreator( property, value, generatorAnnotations.get(0), beanContainer( buildingContext ) );
			default -> throw new AnnotationException( "Property '" + qualify( holder.getPath(), propertyName )
					+ "' has too many generator annotations: " + generatorAnnotations );
		};
	}

	public static void applyIfNotEmpty(String name, String value, BiConsumer<String,String> consumer) {
		if ( isNotEmpty( value ) ) {
			consumer.accept( name, value );
		}
	}
}
