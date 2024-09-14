/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.internal;

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
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.PropertyData;
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
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.mapping.GeneratorCreator;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.models.spi.AnnotationTarget;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.SourceModelBuildingContext;
import org.hibernate.resource.beans.container.spi.BeanContainer;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.service.ServiceRegistry;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.TableGenerator;
import jakarta.persistence.Version;

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
import static org.hibernate.internal.util.NullnessUtil.castNonNull;
import static org.hibernate.internal.util.StringHelper.qualify;
import static org.hibernate.resource.beans.internal.Helper.allowExtensionsInCdi;

/**
 * Responsible for configuring and instantiating {@link Generator}s.
 *
 * @author Gavin King
 */
public class GeneratorBinder {

	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( GeneratorBinder.class );

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
			final GeneratedValue generatedValue = idAttributeMember.getDirectAnnotationUsage( GeneratedValue.class );
			if ( generatedValue != null ) {
				// The mapping used @GeneratedValue but specified no name.  This is a special case added in JPA 3.2.
				// Look for a matching "implied generator" based on the GenerationType

				final GenerationType strategy = generatedValue.strategy();
				final String strategyGeneratorClassName = correspondingGeneratorName( strategy );

				final IdentifierGeneratorDefinition impliedGenerator = determineImpliedGenerator(
						strategy,
						strategyGeneratorClassName,
						localGenerators
				);

				if ( impliedGenerator != null ) {
					configuration.putAll( impliedGenerator.getParameters() );

					final BeanContainer beanContainer = beanContainer( context );
					identifierValue.setCustomIdGeneratorCreator( creationContext -> {
						final Generator identifierGenerator = instantiateGenerator(
								beanContainer,
								generatorClass( strategyGeneratorClassName, identifierValue )
						);
						callConfigure( creationContext, identifierGenerator, configuration, identifierValue );
						if ( identifierGenerator instanceof IdentityGenerator) {
							identifierValue.setColumnToIdentity();
						}
						return identifierGenerator;
					} );

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
			final IdentifierGeneratorDefinition generatorDefinition = localGenerators.entrySet().iterator().next().getValue();
			// NOTE : a little bit of a special rule here for the case of just one -
			// 		we consider it a match, based on strategy, if the strategy is AUTO or matches...
			if ( strategy == AUTO
					|| isImpliedGenerator( strategy, strategyGeneratorClassName, generatorDefinition ) ) {
				return generatorDefinition;
			}
		}

		IdentifierGeneratorDefinition matching = null;
		for ( Map.Entry<String, ? extends IdentifierGeneratorDefinition> localGeneratorEntry : localGenerators.entrySet() ) {
			if ( isImpliedGenerator( strategy, strategyGeneratorClassName, localGeneratorEntry.getValue() ) ) {
				if ( matching != null ) {
					// we found multiple matching generators
					return null;
				}
				matching = localGeneratorEntry.getValue();
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
			final IdentifierGeneratorDefinition definition =
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
			final IdentifierGeneratorDefinition result = localGenerators.get( name );
			if ( result != null ) {
				return result;
			}
		}

		final IdentifierGeneratorDefinition globalDefinition =
				buildingContext.getMetadataCollector().getIdentifierGenerator( name );
		if ( globalDefinition != null ) {
			return globalDefinition;
		}

		LOG.debugf( "Could not resolve explicit IdentifierGeneratorDefinition - using implicit interpretation (%s)", name );

		final GeneratedValue generatedValue = idAttributeMember.getDirectAnnotationUsage( GeneratedValue.class );
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

	private static GenerationType interpretGenerationType(GeneratedValue generatedValueAnn) {
		// todo (jpa32) : when can this ever be null?
		final GenerationType strategy = generatedValueAnn.strategy();
		return strategy == null ? AUTO : strategy;
	}

	/**
	 * Collects definition objects for all generators defined using any of {@link TableGenerator},
	 * {@link SequenceGenerator}, and {@link GenericGenerator} on the given annotated element.
	 */
	public static List<IdentifierGeneratorDefinition> collectIdGeneratorDefinitions(
			AnnotationTarget annotatedElement,
			MetadataBuildingContext context) {
		final ArrayList<IdentifierGeneratorDefinition> definitions = new ArrayList<>();
		visitIdGeneratorDefinitions(
				annotatedElement,
				definitions::add,
				context
		);
		return definitions;
	}

	public static void visitIdGeneratorDefinitions(
			AnnotationTarget annotatedElement,
			Consumer<IdentifierGeneratorDefinition> consumer,
			MetadataBuildingContext context) {
		final InFlightMetadataCollector metadataCollector = context.getMetadataCollector();
		final SourceModelBuildingContext sourceModelContext = metadataCollector.getSourceModelBuildingContext();

		annotatedElement.forEachAnnotationUsage( TableGenerator.class, sourceModelContext, (usage) -> {
			final IdentifierGeneratorDefinition idGenerator = buildTableIdGenerator( usage );
			consumer.accept( idGenerator );
		} );

		annotatedElement.forEachAnnotationUsage( SequenceGenerator.class, sourceModelContext, (usage) -> {
			final IdentifierGeneratorDefinition idGenerator = buildSequenceIdGenerator( usage );
			consumer.accept( idGenerator );
		} );

		annotatedElement.forEachAnnotationUsage( GenericGenerator.class, sourceModelContext, (usage) -> {
			final IdentifierGeneratorDefinition idGenerator = buildIdGenerator( usage );
			consumer.accept( idGenerator );
		} );

	}

	public static void registerGlobalGenerators(
			AnnotationTarget annotatedElement,
			MetadataBuildingContext context) {
		if ( !context.getBootstrapContext().getJpaCompliance().isGlobalGeneratorScopeEnabled() ) {
			return;
		}

		final InFlightMetadataCollector metadataCollector = context.getMetadataCollector();
		visitIdGeneratorDefinitions(
				annotatedElement,
				(definition) -> {
					if ( !definition.getName().isEmpty() ) {
						metadataCollector.addIdentifierGenerator( definition );
					}
				},
				context
		);
	}

	private static IdentifierGeneratorDefinition buildIdGenerator(GenericGenerator generatorAnnotation) {
		final IdentifierGeneratorDefinition.Builder definitionBuilder =
				new IdentifierGeneratorDefinition.Builder();
		definitionBuilder.setName( generatorAnnotation.name() );
		final Class<? extends Generator> generatorClass = generatorAnnotation.type();
		final String strategy =
				generatorClass.equals( Generator.class )
						? generatorAnnotation.strategy()
						: generatorClass.getName();
		definitionBuilder.setStrategy( strategy );
		definitionBuilder.addParams( extractParameterMap( generatorAnnotation.parameters() ) );

		if ( LOG.isTraceEnabled() ) {
			LOG.tracev( "Added generator with name: {0}, strategy: {0}",
					definitionBuilder.getName(), definitionBuilder.getStrategy() );
		}

		return definitionBuilder.build();
	}

	private static IdentifierGeneratorDefinition buildSequenceIdGenerator(SequenceGenerator generatorAnnotation) {
		final IdentifierGeneratorDefinition.Builder definitionBuilder = new IdentifierGeneratorDefinition.Builder();
		interpretSequenceGenerator( generatorAnnotation, definitionBuilder );
		if ( LOG.isTraceEnabled() ) {
			LOG.tracev( "Add sequence generator with name: {0}", definitionBuilder.getName() );
		}
		return definitionBuilder.build();
	}

	private static IdentifierGeneratorDefinition buildTableIdGenerator(TableGenerator generatorAnnotation) {
		final IdentifierGeneratorDefinition.Builder definitionBuilder = new IdentifierGeneratorDefinition.Builder();
		interpretTableGenerator( generatorAnnotation, definitionBuilder );
		if ( LOG.isTraceEnabled() ) {
			LOG.tracev( "Add sequence generator with name: {0}", definitionBuilder.getName() );
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
		// we don't yet support the additional "fancy" operations of
		// IdentifierGenerator with regular generators, though this
		// would be extremely easy to add if anyone asks for it
		if ( IdentifierGenerator.class.isAssignableFrom( generatorClass ) ) {
			throw new AnnotationException("Generator class '" + generatorClass.getName()
					+ "' implements 'IdentifierGenerator' and may not be used with '@ValueGenerationType'");
		}
		if ( ExportableProducer.class.isAssignableFrom( generatorClass ) ) {
			throw new AnnotationException("Generator class '" + generatorClass.getName()
					+ "' implements 'ExportableProducer' and may not be used with '@ValueGenerationType'");
		}
	}

	/**
	 * Return a {@link GeneratorCreator} for an attribute annotated
	 * with a {@linkplain ValueGenerationType generator annotation}.
	 */
	private static GeneratorCreator generatorCreator(
			MemberDetails memberDetails,
			Annotation annotation,
			BeanContainer beanContainer) {
		final Class<? extends Annotation> annotationType = annotation.annotationType();
		final ValueGenerationType generatorAnnotation = annotationType.getAnnotation( ValueGenerationType.class );
		assert generatorAnnotation != null;
		final Class<? extends Generator> generatorClass = generatorAnnotation.generatedBy();
		checkGeneratorClass( generatorClass );
		checkGeneratorInterfaces( generatorClass );
		return creationContext -> {
			final Generator generator = instantiateGenerator(
					annotation,
					beanContainer,
					creationContext,
					generatorClass,
					memberDetails,
					annotationType
			);
			callInitialize( annotation, memberDetails, creationContext, generator );
			//TODO: callConfigure( creationContext, generator, emptyMap(), identifierValue );
			checkVersionGenerationAlways( memberDetails, generator );
			return generator;
		};
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
		final Class<? extends Annotation> annotationType = annotation.annotationType();
		final IdGeneratorType idGeneratorAnnotation = annotationType.getAnnotation( IdGeneratorType.class );
		assert idGeneratorAnnotation != null;
		final Class<? extends Generator> generatorClass = idGeneratorAnnotation.value();
		checkGeneratorClass( generatorClass );
		return creationContext -> {
			final Generator generator =
					instantiateGenerator(
							annotation,
							beanContainer,
							creationContext,
							generatorClass,
							idAttributeMember,
							annotationType
					);
			callInitialize( annotation, idAttributeMember, creationContext, generator );
			callConfigure( creationContext, generator, emptyMap(), identifierValue );
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
	private static Generator instantiateGeneratorAsBean(
			Annotation annotation,
			BeanContainer beanContainer,
			GeneratorCreationContext creationContext,
			Class<? extends Generator> generatorClass,
			MemberDetails memberDetails,
			Class<? extends Annotation> annotationType) {
		return beanContainer.getBean( generatorClass,
				new BeanContainer.LifecycleOptions() {
					@Override
					public boolean canUseCachedReferences() {
						return false;
					}
					@Override
					public boolean useJpaCompliantCreation() {
						return true;
					}
				},
				new BeanInstanceProducer() {
					@SuppressWarnings( "unchecked" )
					@Override
					public <B> B produceBeanInstance(Class<B> beanType) {
						return (B) instantiateGenerator(
								annotation,
								memberDetails,
								annotationType,
								creationContext,
								generatorClass
						);
					}
					@Override
					public <B> B produceBeanInstance(String name, Class<B> beanType) {
						return produceBeanInstance( beanType );
					}
				} )
				.getBeanInstance();
	}

	/**
	 * Instantiate a {@link Generator}, using the given {@link BeanContainer},
	 * for the case where no generator annotation is available.
	 *
	 * @param beanContainer an optional {@code BeanContainer}
	 * @param generatorClass a class which implements {@code Generator}
	 */
	private static Generator instantiateGeneratorAsBean(
			BeanContainer beanContainer,
			Class<? extends Generator> generatorClass) {
		return beanContainer.getBean( generatorClass,
				new BeanContainer.LifecycleOptions() {
					@Override
					public boolean canUseCachedReferences() {
						return false;
					}
					@Override
					public boolean useJpaCompliantCreation() {
						return true;
					}
				},
				new BeanInstanceProducer() {
					@SuppressWarnings( "unchecked" )
					@Override
					public <B> B produceBeanInstance(Class<B> beanType) {
						return (B) instantiateGeneratorViaDefaultConstructor( generatorClass );
					}
					@Override
					public <B> B produceBeanInstance(String name, Class<B> beanType) {
						return produceBeanInstance( beanType );
					}
				} )
				.getBeanInstance();
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
	public static Generator instantiateGenerator(
			BeanContainer beanContainer,
			Class<? extends Generator> generatorClass) {
		if ( beanContainer != null ) {
			return instantiateGeneratorAsBean( beanContainer, generatorClass );
		}
		else {
			return instantiateGeneratorViaDefaultConstructor( generatorClass );
		}
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

	private static <A extends Annotation> void callInitialize(
			A annotation,
			MemberDetails memberDetails,
			GeneratorCreationContext creationContext,
			Generator generator) {
		if ( generator instanceof AnnotationBasedGenerator ) {
			// This will cause a CCE in case the generation type doesn't match the annotation type; As this would be
			// a programming error of the generation type developer and thus should show up during testing, we don't
			// check this explicitly; If required, this could be done e.g. using ClassMate
			@SuppressWarnings("unchecked")
			final AnnotationBasedGenerator<A> generation = (AnnotationBasedGenerator<A>) generator;
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
			SimpleValue identifierValue) {
		if ( generator instanceof final Configurable configurable ) {
			final Properties parameters = collectParameters(
					identifierValue,
					creationContext.getDatabase().getDialect(),
					creationContext.getRootClass(),
					configuration
			);
			configurable.configure( creationContext, parameters );
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
		final GeneratedValue generatedValue = castNonNull( idMember.getDirectAnnotationUsage( GeneratedValue.class ) );

		if ( isGlobalGeneratorNameGlobal( context ) ) {
			// process and register any generators defined on the member.
			// according to JPA these are also global.
			context.getMetadataCollector().getGlobalRegistrations().as( GlobalRegistrar.class ).collectIdGenerators( idMember );
			context.getMetadataCollector().addSecondPass( new StrictIdGeneratorResolverSecondPass(
					persistentClass,
					idValue,
					idMember,
					context
			) );
		}
		else {
			context.getMetadataCollector().addSecondPass( new IdGeneratorResolverSecondPass(
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
			MemberDetails idMember,
			SimpleValue idValue,
			Map<String, Object> configuration,
			MetadataBuildingContext context) {
		configuration.putAll( defaultedGenerator.getParameters() );

		final BeanContainer beanContainer = beanContainer( context );
		idValue.setCustomIdGeneratorCreator( creationContext -> {
			final Generator identifierGenerator = instantiateGenerator(
					beanContainer,
					generatorClass( defaultedGenerator.getStrategy(), idValue )
			);
			callConfigure( creationContext, identifierGenerator, configuration, idValue );
			if ( identifierGenerator instanceof IdentityGenerator) {
				idValue.setColumnToIdentity();
			}

			if ( identifierGenerator instanceof ExportableProducer exportableProducer ) {
				exportableProducer.registerExportables( creationContext.getDatabase() );
			}

			return identifierGenerator;
		} );
	}


	public static void createGeneratorFrom(
			IdentifierGeneratorDefinition defaultedGenerator,
			MemberDetails idMember,
			SimpleValue idValue,
			PersistentClass persistentClass,
			MetadataBuildingContext context) {
		createGeneratorFrom(
				defaultedGenerator,
				idMember,
				idValue,
				buildConfigurationMap( defaultedGenerator, idValue, persistentClass ),
				context
		);
	}

	public static Map<String, Object> buildConfigurationMap(
			IdentifierGeneratorDefinition defaultedGenerator,
			SimpleValue idValue,
			PersistentClass persistentClass) {
		final Map<String,Object> configuration = new HashMap<>();

		configuration.put( PersistentIdentifierGenerator.TABLE, idValue.getTable().getName() );

		if ( idValue.getColumnSpan() == 1 ) {
			configuration.put( PersistentIdentifierGenerator.PK, idValue.getColumns().get( 0).getName() );
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
			final IdentifierGeneratorDefinition generatorDef =
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
		final ServiceRegistry serviceRegistry = buildingContext.getBootstrapContext().getServiceRegistry();
		return allowExtensionsInCdi( serviceRegistry )
				? serviceRegistry.requireService( ManagedBeanRegistry.class ).getBeanContainer()
				: null;
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
			final BeanContainer beanContainer = beanContainer( context );
			identifierValue.setCustomIdGeneratorCreator( creationContext -> {
				final Generator identifierGenerator =
						instantiateGenerator( beanContainer, generatorClass( generatorStrategy, identifierValue ) );
				callConfigure( creationContext, identifierGenerator, configuration, identifierValue );
				if ( identifierGenerator instanceof IdentityGenerator) {
					identifierValue.setColumnToIdentity();
				}
				return identifierGenerator;
			} );
		}
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
			MetadataBuildingContext context) {

		final SourceModelBuildingContext sourceModelContext =
				context.getMetadataCollector().getSourceModelBuildingContext();
		final MemberDetails idAttributeMember = inferredData.getAttributeMember();
		final List<? extends Annotation> idGeneratorAnnotations =
				idAttributeMember.getMetaAnnotated( IdGeneratorType.class, sourceModelContext );
		final List<? extends Annotation> generatorAnnotations =
				idAttributeMember.getMetaAnnotated( ValueGenerationType.class, sourceModelContext );
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
					CollectionHelper.combineUntyped( idGeneratorAnnotations, generatorAnnotations )
			) );
		}
		if ( !idGeneratorAnnotations.isEmpty() ) {
			idValue.setCustomIdGeneratorCreator( identifierGeneratorCreator(
					idAttributeMember,
					idGeneratorAnnotations.get(0),
					idValue,
					beanContainer( context )
			) );
		}
		else if ( !generatorAnnotations.isEmpty() ) {
//			idValue.setCustomGeneratorCreator( generatorCreator( idAttributeMember, generatorAnnotation ) );
			throw new AnnotationException( String.format(
					Locale.ROOT,
					"Identifier attribute '%s' is annotated '%s' which is not an '@IdGeneratorType'",
					getPath( propertyHolder, inferredData ),
					generatorAnnotations.get(0).annotationType()
			) );
		}
		else if ( idAttributeMember.hasDirectAnnotationUsage( GeneratedValue.class ) ) {
			createIdGenerator( idAttributeMember, idValue, propertyHolder.getPersistentClass(), context );
		}
	}

	/**
	 * Returns the value generation strategy for the given property, if any, by
	 * considering {@linkplain ValueGenerationType generator type annotations}.
	 */
	static GeneratorCreator createValueGeneratorFromAnnotations(
			PropertyHolder holder, String propertyName,
			MemberDetails property, MetadataBuildingContext context) {
		final List<? extends Annotation> generatorAnnotations =
				property.getMetaAnnotated( ValueGenerationType.class,
						context.getMetadataCollector().getSourceModelBuildingContext() );
		switch ( generatorAnnotations.size() ) {
			case 0:
				return null;
			case 1:
				return generatorCreator( property, generatorAnnotations.get(0), beanContainer( context ) );
			default:
				throw new AnnotationException( "Property '" + qualify( holder.getPath(), propertyName )
						+ "' has too many generator annotations: " + generatorAnnotations );
		}
	}

	public static void applyIfNotEmpty(String name, String value, BiConsumer<String,String> consumer) {
		if ( StringHelper.isNotEmpty( value ) ) {
			consumer.accept( name, value );
		}
	}
}
