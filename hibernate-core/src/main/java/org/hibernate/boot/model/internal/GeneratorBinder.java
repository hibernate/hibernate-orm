/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.model.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.IdGeneratorType;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.ValueGenerationType;
import org.hibernate.annotations.common.reflection.XAnnotatedElement;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.boot.internal.GenerationStrategyInterpreter;
import org.hibernate.boot.model.IdentifierGeneratorDefinition;
import org.hibernate.boot.model.relational.ExportableProducer;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.PropertyData;
import org.hibernate.generator.AnnotationBasedGenerator;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.Generator;
import org.hibernate.generator.GeneratorCreationContext;
import org.hibernate.generator.OnExecutionGenerator;
import org.hibernate.id.Configurable;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.id.factory.spi.CustomIdGeneratorCreationContext;
import org.hibernate.internal.CoreLogging;
import org.hibernate.mapping.GeneratorCreator;
import org.hibernate.mapping.IdentifierGeneratorCreator;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Value;
import org.hibernate.resource.beans.container.spi.BeanContainer;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;

import org.jboss.logging.Logger;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.SequenceGenerators;
import jakarta.persistence.TableGenerator;
import jakarta.persistence.TableGenerators;
import jakarta.persistence.Version;

import static org.hibernate.boot.model.internal.BinderHelper.isCompositeId;
import static org.hibernate.boot.model.internal.BinderHelper.isGlobalGeneratorNameGlobal;
import static org.hibernate.id.factory.internal.IdentifierGeneratorUtil.collectParameters;
import static org.hibernate.mapping.SimpleValue.DEFAULT_ID_GEN_STRATEGY;

public class GeneratorBinder {

	private static final Logger LOG = CoreLogging.logger( BinderHelper.class );

	/**
	 * Apply an id generation strategy and parameters to the
	 * given {@link SimpleValue} which represents an identifier.
	 */
	public static void makeIdGenerator(
			SimpleValue id,
			XProperty property,
			String generatorType,
			String generatorName,
			MetadataBuildingContext buildingContext,
			Map<String, IdentifierGeneratorDefinition> localGenerators) {
		LOG.debugf( "#makeIdGenerator(%s, %s, %s, %s, ...)", id, property, generatorType, generatorName );

		//generator settings
		id.setIdentifierGeneratorStrategy( generatorType );

		final Map<String,Object> parameters = new HashMap<>();

		//always settable
		parameters.put( PersistentIdentifierGenerator.TABLE, id.getTable().getName() );

		if ( id.getColumnSpan() == 1 ) {
			parameters.put( PersistentIdentifierGenerator.PK, id.getColumns().get(0).getName() );
		}
		// YUCK!  but cannot think of a clean way to do this given the string-config based scheme
		parameters.put( PersistentIdentifierGenerator.IDENTIFIER_NORMALIZER, buildingContext.getObjectNameNormalizer() );
		parameters.put( IdentifierGenerator.GENERATOR_NAME, generatorName );

		if ( !generatorName.isEmpty() ) {
			//we have a named generator
			final IdentifierGeneratorDefinition definition = makeIdentifierGeneratorDefinition(
					generatorName,
					property,
					localGenerators,
					buildingContext
			);
			if ( definition == null ) {
				throw new AnnotationException( "No id generator was declared with the name '" + generatorName
						+ "' specified by '@GeneratedValue'"
						+ " (define a named generator using '@SequenceGenerator', '@TableGenerator', or '@GenericGenerator')" );
			}
			//This is quite vague in the spec but a generator could override the generator choice
			final String identifierGeneratorStrategy = definition.getStrategy();
			//yuk! this is a hack not to override 'AUTO' even if generator is set
			final boolean avoidOverriding = identifierGeneratorStrategy.equals( "identity" )
					|| identifierGeneratorStrategy.equals( "seqhilo" );
			if ( generatorType == null || !avoidOverriding ) {
				id.setIdentifierGeneratorStrategy( identifierGeneratorStrategy );
				if ( DEFAULT_ID_GEN_STRATEGY.equals( identifierGeneratorStrategy ) ) {
					id.setNullValue( "undefined" );
				}
			}
			//checkIfMatchingGenerator(definition, generatorType, generatorName);
			parameters.putAll( definition.getParameters() );
		}
		if ( DEFAULT_ID_GEN_STRATEGY.equals( generatorType ) ) {
			id.setNullValue( "undefined" );
		}
		id.setIdentifierGeneratorParameters( parameters );
	}

	/**
	 * apply an id generator to a SimpleValue
	 */
	public static void makeIdGenerator(
			SimpleValue id,
			XProperty idXProperty,
			String generatorType,
			String generatorName,
			MetadataBuildingContext buildingContext,
			IdentifierGeneratorDefinition foreignKGeneratorDefinition) {
		Map<String, IdentifierGeneratorDefinition> localIdentifiers = null;
		if ( foreignKGeneratorDefinition != null ) {
			localIdentifiers = new HashMap<>();
			localIdentifiers.put( foreignKGeneratorDefinition.getName(), foreignKGeneratorDefinition );
		}
		makeIdGenerator( id, idXProperty, generatorType, generatorName, buildingContext, localIdentifiers );
	}

	private static IdentifierGeneratorDefinition makeIdentifierGeneratorDefinition(
			String name,
			XProperty idProperty,
			Map<String, IdentifierGeneratorDefinition> localGenerators,
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

		final GeneratedValue generatedValue = idProperty.getAnnotation( GeneratedValue.class );
		if ( generatedValue == null ) {
			// this should really never happen, but it's easy to protect against it...
			return new IdentifierGeneratorDefinition( DEFAULT_ID_GEN_STRATEGY, DEFAULT_ID_GEN_STRATEGY );
		}

		return IdentifierGeneratorDefinition.createImplicit(
				name,
				buildingContext
						.getBootstrapContext()
						.getReflectionManager()
						.toClass( idProperty.getType() ),
				generatedValue.generator(),
				interpretGenerationType( generatedValue )
		);
	}

	private static GenerationType interpretGenerationType(GeneratedValue generatedValueAnn) {
		return generatedValueAnn.strategy() == null ? GenerationType.AUTO : generatedValueAnn.strategy();
	}

	public static Map<String, IdentifierGeneratorDefinition> buildGenerators(
			XAnnotatedElement annotatedElement,
			MetadataBuildingContext context) {

		final InFlightMetadataCollector metadataCollector = context.getMetadataCollector();
		final Map<String, IdentifierGeneratorDefinition> generators = new HashMap<>();

		final TableGenerators tableGenerators = annotatedElement.getAnnotation( TableGenerators.class );
		if ( tableGenerators != null ) {
			for ( TableGenerator tableGenerator : tableGenerators.value() ) {
				IdentifierGeneratorDefinition idGenerator = buildIdGenerator( tableGenerator, context );
				generators.put(
						idGenerator.getName(),
						idGenerator
				);
				metadataCollector.addIdentifierGenerator( idGenerator );
			}
		}

		final SequenceGenerators sequenceGenerators = annotatedElement.getAnnotation( SequenceGenerators.class );
		if ( sequenceGenerators != null ) {
			for ( SequenceGenerator sequenceGenerator : sequenceGenerators.value() ) {
				IdentifierGeneratorDefinition idGenerator = buildIdGenerator( sequenceGenerator, context );
				generators.put(
						idGenerator.getName(),
						idGenerator
				);
				metadataCollector.addIdentifierGenerator( idGenerator );
			}
		}

		final TableGenerator tableGenerator = annotatedElement.getAnnotation( TableGenerator.class );
		if ( tableGenerator != null ) {
			IdentifierGeneratorDefinition idGenerator = buildIdGenerator( tableGenerator, context );
			generators.put( idGenerator.getName(), idGenerator );
			metadataCollector.addIdentifierGenerator( idGenerator );

		}

		final SequenceGenerator sequenceGenerator = annotatedElement.getAnnotation( SequenceGenerator.class );
		if ( sequenceGenerator != null ) {
			IdentifierGeneratorDefinition idGenerator = buildIdGenerator( sequenceGenerator, context );
			generators.put( idGenerator.getName(), idGenerator );
			metadataCollector.addIdentifierGenerator( idGenerator );
		}

		final GenericGenerator genericGenerator = annotatedElement.getAnnotation( GenericGenerator.class );
		if ( genericGenerator != null ) {
			final IdentifierGeneratorDefinition idGenerator = buildIdGenerator( genericGenerator, context );
			generators.put( idGenerator.getName(), idGenerator );
			metadataCollector.addIdentifierGenerator( idGenerator );
		}

		return generators;
	}

	static String generatorType(
			MetadataBuildingContext context,
			XClass entityXClass,
			boolean isComponent,
			GeneratedValue generatedValue) {
		if ( isComponent ) {
			//a component must not have any generator
			return DEFAULT_ID_GEN_STRATEGY;
		}
		else {
			return generatedValue == null ? DEFAULT_ID_GEN_STRATEGY : generatorType( generatedValue, entityXClass, context );
		}
	}

	static String generatorType(GeneratedValue generatedValue, final XClass javaClass, MetadataBuildingContext context) {
		return GenerationStrategyInterpreter.STRATEGY_INTERPRETER.determineGeneratorName(
				generatedValue.strategy(),
				new GenerationStrategyInterpreter.GeneratorNameDeterminationContext() {
					Class<?> javaType = null;
					@Override
					public Class<?> getIdType() {
						if ( javaType == null ) {
							javaType = context.getBootstrapContext().getReflectionManager().toClass( javaClass );
						}
						return javaType;
					}
					@Override
					public String getGeneratedValueGeneratorName() {
						return generatedValue.generator();
					}
				}
		);
	}

	static IdentifierGeneratorDefinition buildIdGenerator(
			Annotation generatorAnnotation,
			MetadataBuildingContext context) {
		if ( generatorAnnotation == null ) {
			return null;
		}

		final IdentifierGeneratorDefinition.Builder definitionBuilder = new IdentifierGeneratorDefinition.Builder();
		if ( generatorAnnotation instanceof TableGenerator ) {
			GenerationStrategyInterpreter.STRATEGY_INTERPRETER.interpretTableGenerator(
					(TableGenerator) generatorAnnotation,
					definitionBuilder
			);
			if ( LOG.isTraceEnabled() ) {
				LOG.tracev( "Add table generator with name: {0}", definitionBuilder.getName() );
			}
		}
		else if ( generatorAnnotation instanceof SequenceGenerator ) {
			GenerationStrategyInterpreter.STRATEGY_INTERPRETER.interpretSequenceGenerator(
					(SequenceGenerator) generatorAnnotation,
					definitionBuilder
			);
			if ( LOG.isTraceEnabled() ) {
				LOG.tracev( "Add sequence generator with name: {0}", definitionBuilder.getName() );
			}
		}
		else if ( generatorAnnotation instanceof GenericGenerator ) {
			final GenericGenerator genericGenerator = (GenericGenerator) generatorAnnotation;
			definitionBuilder.setName( genericGenerator.name() );
			final String strategy = genericGenerator.type().equals(Generator.class)
					? genericGenerator.strategy()
					: genericGenerator.type().getName();
			definitionBuilder.setStrategy( strategy );
			for ( Parameter parameter : genericGenerator.parameters() ) {
				definitionBuilder.addParam( parameter.name(), parameter.value() );
			}
			if ( LOG.isTraceEnabled() ) {
				LOG.tracev( "Add generic generator with name: {0}", definitionBuilder.getName() );
			}
		}
		else {
			throw new AssertionFailure( "Unknown Generator annotation: " + generatorAnnotation );
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
	 * In case the given annotation is a value generator annotation, the corresponding value generation strategy to be
	 * applied to the given property is returned, {@code null} otherwise.
	 * Instantiates the given generator annotation type, initializing it with the given instance of the corresponding
	 * generator annotation and the property's type.
	 */
	static GeneratorCreator generatorCreator(XProperty property, Annotation annotation) {
		final Member member = HCANNHelper.getUnderlyingMember( property );
		final Class<? extends Annotation> annotationType = annotation.annotationType();
		final ValueGenerationType generatorAnnotation = annotationType.getAnnotation( ValueGenerationType.class );
		if ( generatorAnnotation == null ) {
			return null;
		}
		final Class<? extends Generator> generatorClass = generatorAnnotation.generatedBy();
		checkGeneratorClass( generatorClass );
		checkGeneratorInterfaces( generatorClass );
		return creationContext -> {
			final Generator generator =
					instantiateGenerator(
							annotation,
							member,
							annotationType,
							creationContext,
							GeneratorCreationContext.class,
							generatorClass
					);
			callInitialize( annotation, member, creationContext, generator );
			checkVersionGenerationAlways( property, generator );
			return generator;
		};
	}

	static IdentifierGeneratorCreator identifierGeneratorCreator(
			XProperty idProperty,
			Annotation annotation,
			BeanContainer beanContainer) {
		final Member member = HCANNHelper.getUnderlyingMember( idProperty );
		final Class<? extends Annotation> annotationType = annotation.annotationType();
		final IdGeneratorType idGeneratorType = annotationType.getAnnotation( IdGeneratorType.class );
		assert idGeneratorType != null;
		return creationContext -> {
			final Class<? extends Generator> generatorClass = idGeneratorType.value();
			checkGeneratorClass( generatorClass );
			final Generator generator =
					instantiateGenerator(
							annotation,
							beanContainer,
							creationContext,
							generatorClass,
							member,
							annotationType
					);
			callInitialize( annotation, member, creationContext, generator );
			callConfigure( creationContext, generator );
			checkIdGeneratorTiming( annotationType, generator );
			return generator;
		};
	}

	private static Generator instantiateGenerator(
			Annotation annotation,
			BeanContainer beanContainer,
			CustomIdGeneratorCreationContext creationContext,
			Class<? extends Generator> generatorClass,
			Member member,
			Class<? extends Annotation> annotationType) {
		if ( beanContainer != null ) {
			return instantiateGeneratorAsBean(
					annotation,
					beanContainer,
					creationContext,
					generatorClass,
					member,
					annotationType
			);
		}
		else {
			return instantiateGenerator(
					annotation,
					member,
					annotationType,
					creationContext,
					CustomIdGeneratorCreationContext.class,
					generatorClass
			);
		}
	}

	private static Generator instantiateGeneratorAsBean(
			Annotation annotation,
			BeanContainer beanContainer,
			CustomIdGeneratorCreationContext creationContext,
			Class<? extends Generator> generatorClass,
			Member member,
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
								member,
								annotationType,
								creationContext,
								CustomIdGeneratorCreationContext.class,
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

	private static <C, G extends Generator> G instantiateGenerator(
			Annotation annotation,
			Member member,
			Class<? extends Annotation> annotationType,
			C creationContext,
			Class<C> contextClass,
			Class<? extends G> generatorClass) {
		try {
			try {
				return generatorClass
						.getConstructor( annotationType, Member.class, contextClass )
						.newInstance( annotation, member, creationContext);
			}
			catch (NoSuchMethodException ignore) {
				try {
					return generatorClass
							.getConstructor( annotationType )
							.newInstance( annotation );
				}
				catch (NoSuchMethodException i) {
					return generatorClass.newInstance();
				}
			}
		}
		catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
			throw new HibernateException(
					"Could not instantiate generator of type '" + generatorClass.getName() + "'",
					e
			);
		}
	}

	private static <A extends Annotation> void callInitialize(
			A annotation,
			Member member,
			GeneratorCreationContext creationContext,
			Generator generator) {
		if ( generator instanceof AnnotationBasedGenerator ) {
			// This will cause a CCE in case the generation type doesn't match the annotation type; As this would be
			// a programming error of the generation type developer and thus should show up during testing, we don't
			// check this explicitly; If required, this could be done e.g. using ClassMate
			@SuppressWarnings("unchecked")
			final AnnotationBasedGenerator<A> generation = (AnnotationBasedGenerator<A>) generator;
			generation.initialize( annotation, member, creationContext );
		}
	}

	private static void checkVersionGenerationAlways(XProperty property, Generator generator) {
		if ( property.isAnnotationPresent(Version.class) ) {
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

	private static void callConfigure(GeneratorCreationContext creationContext, Generator generator) {
		if ( generator instanceof Configurable ) {
			final Value value = creationContext.getProperty().getValue();
			( (Configurable) generator ).configure( value.getType(), collectParameters(
					(SimpleValue) value,
					creationContext.getDatabase().getDialect(),
					creationContext.getDefaultCatalog(),
					creationContext.getDefaultSchema(),
					creationContext.getPersistentClass().getRootClass()
			), creationContext.getServiceRegistry() );
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

	static void createIdGenerator(
			SimpleValue idValue,
			Map<String, IdentifierGeneratorDefinition> classGenerators,
			MetadataBuildingContext context,
			XClass entityClass,
			XProperty idProperty) {
		//manage composite related metadata
		//guess if its a component and find id data access (property, field etc)
		final GeneratedValue generatedValue = idProperty.getAnnotation( GeneratedValue.class );
		final String generatorType = generatorType( context, entityClass, isCompositeId( entityClass, idProperty ), generatedValue );
		final String generatorName = generatedValue == null ? "" : generatedValue.generator();
		if ( isGlobalGeneratorNameGlobal( context ) ) {
			buildGenerators( idProperty, context );
			context.getMetadataCollector().addSecondPass( new IdGeneratorResolverSecondPass(
					idValue,
					idProperty,
					generatorType,
					generatorName,
					context
			) );
		}
		else {
			//clone classGenerator and override with local values
			final Map<String, IdentifierGeneratorDefinition> generators = new HashMap<>( classGenerators );
			generators.putAll( buildGenerators( idProperty, context ) );
			makeIdGenerator( idValue, idProperty, generatorType, generatorName, context, generators );
		}
	}

	static IdentifierGeneratorDefinition createForeignGenerator(PropertyData mapsIdProperty) {
		final IdentifierGeneratorDefinition.Builder foreignGeneratorBuilder =
				new IdentifierGeneratorDefinition.Builder();
		foreignGeneratorBuilder.setName( "Hibernate-local--foreign generator" );
		foreignGeneratorBuilder.setStrategy( "foreign" );
		foreignGeneratorBuilder.addParam( "property", mapsIdProperty.getPropertyName() );
		return foreignGeneratorBuilder.build();
	}
}
