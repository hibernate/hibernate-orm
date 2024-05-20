/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.model.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.hibernate.AnnotationException;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.IdGeneratorType;
import org.hibernate.annotations.ValueGenerationType;
import org.hibernate.boot.internal.GenerationStrategyInterpreter;
import org.hibernate.boot.model.IdentifierGeneratorDefinition;
import org.hibernate.boot.model.relational.ExportableProducer;
import org.hibernate.boot.model.source.internal.hbm.MappingDocument;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.generator.AnnotationBasedGenerator;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.Generator;
import org.hibernate.generator.GeneratorCreationContext;
import org.hibernate.generator.OnExecutionGenerator;
import org.hibernate.id.Assigned;
import org.hibernate.id.Configurable;
import org.hibernate.id.ForeignGenerator;
import org.hibernate.id.GUIDGenerator;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.IdentityGenerator;
import org.hibernate.id.IncrementGenerator;
import org.hibernate.id.OptimizableGenerator;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.id.SelectGenerator;
import org.hibernate.id.UUIDGenerator;
import org.hibernate.id.UUIDHexGenerator;
import org.hibernate.id.enhanced.LegacyNamingStrategy;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.id.enhanced.SingleNamingStrategy;
import org.hibernate.generator.CustomIdGeneratorCreationContext;
import org.hibernate.internal.CoreLogging;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.GeneratorCreator;
import org.hibernate.mapping.IdentifierGeneratorCreator;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Table;
import org.hibernate.models.spi.AnnotationTarget;
import org.hibernate.models.spi.AnnotationUsage;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.mapping.Value;
import org.hibernate.resource.beans.container.spi.BeanContainer;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;

import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;
import org.jboss.logging.Logger;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.TableGenerator;
import jakarta.persistence.Version;

import static java.util.Collections.emptyMap;
import static org.hibernate.boot.model.internal.AnnotationHelper.extractParameterMap;
import static org.hibernate.boot.model.internal.BinderHelper.isCompositeId;
import static org.hibernate.boot.model.internal.BinderHelper.isGlobalGeneratorNameGlobal;
import static org.hibernate.internal.util.ReflectHelper.getDefaultConstructor;
import static org.hibernate.mapping.SimpleValue.DEFAULT_ID_GEN_STRATEGY;

/**
 * Responsible for configuring and instantiating {@link Generator}s.
 *
 * @author Gavin King
 */
public class GeneratorBinder {

	private static final Logger LOG = CoreLogging.logger( BinderHelper.class );

	/**
	 * Create an id generator for the given named {@code strategy} using the
	 * "old" (pre-Hibernate 6) approach.
	 */
	public static Generator createLegacyIdentifierGenerator(
			String strategy,
			SimpleValue idValue,
			Dialect dialect,
			RootClass rootClass,
			Map<String, Object> configuration) {
		final Generator identifierGenerator = instantiateGenerator( generatorClass( strategy, idValue ) );
		if ( identifierGenerator instanceof Configurable ) {
			final Configurable configurable = (Configurable) identifierGenerator;
			configurable.configure( idValue.getType(),
					collectParameters( idValue, dialect, rootClass, configuration ),
					idValue.getServiceRegistry() );
		}
		return identifierGenerator;
	}

	private static Generator instantiateGenerator(Class<? extends Generator> generatorClass) {
		final Constructor<? extends Generator> defaultConstructor = getDefaultConstructor( generatorClass );
		if ( defaultConstructor == null ) {
			throw new org.hibernate.InstantiationException( "No default constructor for id generator class", generatorClass);
		}
		try {
			return defaultConstructor.newInstance();
		}
		catch (Exception e) {
			throw new org.hibernate.InstantiationException( "Could not instantiate id generator", generatorClass, e );
		}
	}

	/**
	 * Interpret an "old" generator strategy name as a {@link Generator} class.
	 */
	private static Class<? extends Generator> generatorClass(String strategy, SimpleValue idValue) {
		if ( "native".equals(strategy) ) {
			strategy =
					idValue.getMetadata().getDatabase().getDialect()
							.getNativeIdentifierGeneratorStrategy();
		}
		switch (strategy) {
			case "assigned":
				return Assigned.class;
			case "enhanced-sequence":
			case "sequence":
				return SequenceStyleGenerator.class;
			case "enhanced-table":
			case "table":
				return org.hibernate.id.enhanced.TableGenerator.class;
			case "identity":
				return IdentityGenerator.class;
			case "increment":
				return IncrementGenerator.class;
			case "foreign":
				return ForeignGenerator.class;
			case "uuid":
			case "uuid.hex":
				return UUIDHexGenerator.class;
			case "uuid2":
				return UUIDGenerator.class;
			case "select":
				return SelectGenerator.class;
			case "guid":
				return GUIDGenerator.class;
		}
		final Class<? extends Generator> clazz =
				idValue.getServiceRegistry().requireService( ClassLoaderService.class )
						.classForName( strategy );
		if ( !Generator.class.isAssignableFrom( clazz ) ) {
			// in principle, this shouldn't happen, since @GenericGenerator
			// constrains the type to subtypes of Generator
			throw new MappingException( clazz.getName() + " does not implement 'Generator'" );
		}
		return clazz;
	}

	/**
	 * Collect the parameters which should be passed to
	 * {@link Configurable#configure(Type, Properties, ServiceRegistry)}.
	 */
	public static Properties collectParameters(
			SimpleValue simpleValue,
			Dialect dialect,
			RootClass rootClass,
			Map<String, Object> configuration) {
		final ConfigurationService configService =
				simpleValue.getMetadata().getMetadataBuildingOptions().getServiceRegistry()
						.requireService( ConfigurationService.class );

		final Properties params = new Properties();

		// default initial value and allocation size per-JPA defaults
		params.setProperty( OptimizableGenerator.INITIAL_PARAM,
				String.valueOf( OptimizableGenerator.DEFAULT_INITIAL_VALUE ) );

		params.setProperty( OptimizableGenerator.INCREMENT_PARAM,
				String.valueOf( defaultIncrement( configService ) ) );
		//init the table here instead of earlier, so that we can get a quoted table name
		//TODO: would it be better to simply pass the qualified table name, instead of
		//	  splitting it up into schema/catalog/table names
		final String tableName = simpleValue.getTable().getQuotedName( dialect );
		params.setProperty( PersistentIdentifierGenerator.TABLE, tableName );

		//pass the column name (a generated id almost always has a single column)
		final Column column = (Column) simpleValue.getSelectables().get(0);
		final String columnName = column.getQuotedName( dialect );
		params.setProperty( PersistentIdentifierGenerator.PK, columnName );

		//pass the entity-name, if not a collection-id
		if ( rootClass != null ) {
			params.setProperty( IdentifierGenerator.ENTITY_NAME, rootClass.getEntityName() );
			params.setProperty( IdentifierGenerator.JPA_ENTITY_NAME, rootClass.getJpaEntityName() );
			// The table name is not really a good default for subselect entities,
			// so use the JPA entity name which is short
			params.setProperty( OptimizableGenerator.IMPLICIT_NAME_BASE,
					simpleValue.getTable().isSubselect()
							? rootClass.getJpaEntityName()
							: simpleValue.getTable().getName() );

			params.setProperty( PersistentIdentifierGenerator.TABLES,
					identityTablesString( dialect, rootClass ) );
		}
		else {
			params.setProperty( PersistentIdentifierGenerator.TABLES, tableName );
			params.setProperty( OptimizableGenerator.IMPLICIT_NAME_BASE, tableName );
		}

		params.put( IdentifierGenerator.CONTRIBUTOR_NAME,
				simpleValue.getBuildingContext().getCurrentContributorName() );

		final Map<String, Object> settings = configService.getSettings();
		if ( settings.containsKey( AvailableSettings.PREFERRED_POOLED_OPTIMIZER ) ) {
			params.put( AvailableSettings.PREFERRED_POOLED_OPTIMIZER,
					settings.get( AvailableSettings.PREFERRED_POOLED_OPTIMIZER ) );
		}

		params.putAll( configuration );

		return params;
	}

	private static String identityTablesString(Dialect dialect, RootClass rootClass) {
		final StringBuilder tables = new StringBuilder();
		for ( Table table : rootClass.getIdentityTables() ) {
			tables.append( table.getQuotedName( dialect ) );
			if ( !tables.isEmpty() ) {
				tables.append( ", " );
			}
		}
		return tables.toString();
	}

	private static int defaultIncrement(ConfigurationService configService) {
		final String idNamingStrategy =
				configService.getSetting( AvailableSettings.ID_DB_STRUCTURE_NAMING_STRATEGY,
						StandardConverters.STRING, null );
		if ( LegacyNamingStrategy.STRATEGY_NAME.equals( idNamingStrategy )
				|| LegacyNamingStrategy.class.getName().equals( idNamingStrategy )
				|| SingleNamingStrategy.STRATEGY_NAME.equals( idNamingStrategy )
				|| SingleNamingStrategy.class.getName().equals( idNamingStrategy ) ) {
			return 1;
		}
		else {
			return OptimizableGenerator.DEFAULT_INCREMENT_SIZE;
		}
	}

	/**
	 * Apply an id generation strategy and parameters to the
	 * given {@link SimpleValue} which represents an identifier.
	 */
	public static void makeIdGenerator(
			SimpleValue id,
			MemberDetails idAttributeMember,
			String generatorType,
			String generatorName,
			MetadataBuildingContext buildingContext,
			Map<String, IdentifierGeneratorDefinition> localGenerators) {
		LOG.debugf( "#makeIdGenerator(%s, %s, %s, %s, ...)", id, idAttributeMember, generatorType, generatorName );

		//generator settings
		final Map<String,Object> parameters = new HashMap<>();

		//always settable
		parameters.put( PersistentIdentifierGenerator.TABLE, id.getTable().getName() );

		if ( id.getColumnSpan() == 1 ) {
			parameters.put( PersistentIdentifierGenerator.PK, id.getColumns().get(0).getName() );
		}
		// YUCK!  but cannot think of a clean way to do this given the string-config based scheme
		parameters.put( PersistentIdentifierGenerator.IDENTIFIER_NORMALIZER, buildingContext.getObjectNameNormalizer() );
		parameters.put( IdentifierGenerator.GENERATOR_NAME, generatorName );

		final String generatorStrategy;
		if ( !generatorName.isEmpty() ) {
			//we have a named generator
			final IdentifierGeneratorDefinition definition =
					makeIdentifierGeneratorDefinition( generatorName, idAttributeMember, localGenerators, buildingContext );
			if ( definition == null ) {
				throw new AnnotationException( "No id generator was declared with the name '" + generatorName
						+ "' specified by '@GeneratedValue'"
						+ " (define a named generator using '@SequenceGenerator', '@TableGenerator', or '@GenericGenerator')" );
			}
			//This is quite vague in the spec but a generator could override the generator choice
			generatorStrategy =
					generatorType == null
						//yuk! this is a hack not to override 'AUTO' even if generator is set
						|| !definition.getStrategy().equals( "identity" )
							? definition.getStrategy()
							: generatorType;
			//checkIfMatchingGenerator(definition, generatorType, generatorName);
			parameters.putAll( definition.getParameters() );
		}
		else {
			generatorStrategy = generatorType;
		}

		setGeneratorCreator( id, parameters, generatorStrategy );
	}

	/**
	 * apply an id generator to a SimpleValue
	 */
	public static void makeIdGenerator(
			SimpleValue id,
			MemberDetails idAttributeMember,
			String generatorType,
			String generatorName,
			MetadataBuildingContext buildingContext,
			IdentifierGeneratorDefinition foreignKGeneratorDefinition) {
		Map<String, IdentifierGeneratorDefinition> localIdentifiers = null;
		if ( foreignKGeneratorDefinition != null ) {
			localIdentifiers = new HashMap<>();
			localIdentifiers.put( foreignKGeneratorDefinition.getName(), foreignKGeneratorDefinition );
		}
		makeIdGenerator( id, idAttributeMember, generatorType, generatorName, buildingContext, localIdentifiers );
	}

	private static IdentifierGeneratorDefinition makeIdentifierGeneratorDefinition(
			String name,
			MemberDetails idAttributeMember,
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

		final AnnotationUsage<GeneratedValue> generatedValue = idAttributeMember.getAnnotationUsage( GeneratedValue.class );
		if ( generatedValue == null ) {
			// this should really never happen, but it's easy to protect against it...
			return new IdentifierGeneratorDefinition( DEFAULT_ID_GEN_STRATEGY, DEFAULT_ID_GEN_STRATEGY );
		}

		return IdentifierGeneratorDefinition.createImplicit(
				name,
				idAttributeMember.getType().determineRawClass().toJavaClass(),
				generatedValue.getString( "generator" ),
				interpretGenerationType( generatedValue )
		);
	}

	private static GenerationType interpretGenerationType(AnnotationUsage<GeneratedValue> generatedValueAnn) {
		// todo (jpa32) : when can this ever be null?
		final GenerationType strategy = generatedValueAnn.getEnum( "strategy" );
		return strategy == null ? GenerationType.AUTO : strategy;
	}

	public static Map<String, IdentifierGeneratorDefinition> buildGenerators(
			AnnotationTarget annotatedElement,
			MetadataBuildingContext context) {

		final InFlightMetadataCollector metadataCollector = context.getMetadataCollector();
		final Map<String, IdentifierGeneratorDefinition> generators = new HashMap<>();

		annotatedElement.forEachAnnotationUsage( TableGenerator.class, usage -> {
			IdentifierGeneratorDefinition idGenerator = buildTableIdGenerator( usage );
			generators.put( idGenerator.getName(), idGenerator );
			metadataCollector.addIdentifierGenerator( idGenerator );
		} );

		annotatedElement.forEachAnnotationUsage( SequenceGenerator.class, usage -> {
			IdentifierGeneratorDefinition idGenerator = buildSequenceIdGenerator( usage );
			generators.put( idGenerator.getName(), idGenerator );
			metadataCollector.addIdentifierGenerator( idGenerator );
		} );

		annotatedElement.forEachAnnotationUsage( GenericGenerator.class, usage -> {
			final IdentifierGeneratorDefinition idGenerator = buildIdGenerator( usage );
			generators.put( idGenerator.getName(), idGenerator );
			metadataCollector.addIdentifierGenerator( idGenerator );
		} );

		return generators;
	}

	static String generatorType(
			MetadataBuildingContext context,
			ClassDetails entityXClass,
			boolean isComponent,
			AnnotationUsage<GeneratedValue> generatedValue) {
		if ( isComponent ) {
			//a component must not have any generator
			return DEFAULT_ID_GEN_STRATEGY;
		}
		else {
			return generatedValue == null ? DEFAULT_ID_GEN_STRATEGY : generatorType( generatedValue, entityXClass, context );
		}
	}

	static String generatorType(
			AnnotationUsage<GeneratedValue> generatedValue,
			final ClassDetails javaClass,
			MetadataBuildingContext context) {
		return GenerationStrategyInterpreter.STRATEGY_INTERPRETER.determineGeneratorName(
				generatedValue.getEnum( "strategy" ),
				new GenerationStrategyInterpreter.GeneratorNameDeterminationContext() {
					Class<?> javaType = null;
					@Override
					public Class<?> getIdType() {
						if ( javaType == null ) {
							javaType = javaClass.toJavaClass();
						}
						return javaType;
					}
					@Override
					public String getGeneratedValueGeneratorName() {
						return generatedValue.getString( "generator" );
					}
				}
		);
	}

	static IdentifierGeneratorDefinition buildIdGenerator(AnnotationUsage<GenericGenerator> generatorAnnotation) {
		if ( generatorAnnotation == null ) {
			return null;
		}

		final IdentifierGeneratorDefinition.Builder definitionBuilder = new IdentifierGeneratorDefinition.Builder();
		definitionBuilder.setName( generatorAnnotation.getString( "name" ) );
		final Class<? extends Generator> generatorClass =
				generatorAnnotation.getClassDetails( "type" ).toJavaClass();
		final String strategy = generatorClass.equals(Generator.class)
				? generatorAnnotation.getString( "strategy" )
				: generatorClass.getName();
		definitionBuilder.setStrategy( strategy );
		definitionBuilder.addParams( extractParameterMap( generatorAnnotation.getList( "parameters" ) ) );
		if ( LOG.isTraceEnabled() ) {
			LOG.tracev( "Add generic generator with name: {0}", definitionBuilder.getName() );
		}
		return definitionBuilder.build();
	}

	static IdentifierGeneratorDefinition buildTableIdGenerator(AnnotationUsage<TableGenerator> generatorAnnotation) {
		if ( generatorAnnotation == null ) {
			return null;
		}

		final IdentifierGeneratorDefinition.Builder definitionBuilder = new IdentifierGeneratorDefinition.Builder();
		GenerationStrategyInterpreter.STRATEGY_INTERPRETER
				.interpretTableGenerator( generatorAnnotation, definitionBuilder );
		if ( LOG.isTraceEnabled() ) {
			LOG.tracev( "Add table generator with name: {0}", definitionBuilder.getName() );
		}
		return definitionBuilder.build();
	}

	static IdentifierGeneratorDefinition buildSequenceIdGenerator(AnnotationUsage<SequenceGenerator> generatorAnnotation) {
		if ( generatorAnnotation == null ) {
			return null;
		}

		final IdentifierGeneratorDefinition.Builder definitionBuilder = new IdentifierGeneratorDefinition.Builder();
		GenerationStrategyInterpreter.STRATEGY_INTERPRETER
				.interpretSequenceGenerator( generatorAnnotation, definitionBuilder );
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
	 * In case the given annotation is a value generator annotation, the corresponding value generation strategy to be
	 * applied to the given property is returned, {@code null} otherwise.
	 * Instantiates the given generator annotation type, initializing it with the given instance of the corresponding
	 * generator annotation and the property's type.
	 */
	static GeneratorCreator generatorCreator(
			MemberDetails memberDetails,
			AnnotationUsage<?> annotation,
			MetadataBuildingContext buildingContext) {
		final Class<? extends Annotation> annotationType = annotation.getAnnotationType();
		final ValueGenerationType generatorAnnotation = annotationType.getAnnotation( ValueGenerationType.class );
		if ( generatorAnnotation == null ) {
			return null;
		}
		final Class<? extends Generator> generatorClass = generatorAnnotation.generatedBy();
		checkGeneratorClass( generatorClass );
		checkGeneratorInterfaces( generatorClass );
		return creationContext -> {
			final Generator generator = instantiateGenerator(
					annotation,
					memberDetails,
					annotationType,
					GeneratorCreationContext.class,
					generatorClass,
					creationContext
			);
			callInitialize( annotation, memberDetails, creationContext, generator );
			checkVersionGenerationAlways( memberDetails, generator );
			return generator;
		};
	}

	static IdentifierGeneratorCreator identifierGeneratorCreator(
			MemberDetails idAttributeMember,
			AnnotationUsage<? extends Annotation> annotation,
			BeanContainer beanContainer) {
		final Class<? extends Annotation> annotationType = annotation.getAnnotationType();
		final IdGeneratorType idGeneratorType = annotationType.getAnnotation( IdGeneratorType.class );
		assert idGeneratorType != null;
		final Class<? extends Generator> generatorClass = idGeneratorType.value();
		return creationContext -> {
			checkGeneratorClass( generatorClass );
			final Generator generator = instantiateGenerator(
					annotation,
					beanContainer,
					creationContext,
					generatorClass,
					idAttributeMember,
					annotationType
			);
			callInitialize( annotation, idAttributeMember, creationContext, generator );
			callConfigure( creationContext, generator );
			checkIdGeneratorTiming( annotationType, generator );
			return generator;
		};
	}

	private static Generator instantiateGenerator(
			AnnotationUsage<? extends Annotation> annotation,
			BeanContainer beanContainer,
			CustomIdGeneratorCreationContext creationContext,
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
					CustomIdGeneratorCreationContext.class,
					generatorClass,
					creationContext
			);
		}
	}

	private static Generator instantiateGeneratorAsBean(
			AnnotationUsage<? extends Annotation> annotation,
			BeanContainer beanContainer,
			CustomIdGeneratorCreationContext creationContext,
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
								CustomIdGeneratorCreationContext.class,
								generatorClass,
								creationContext
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
			AnnotationUsage<?> annotation,
			MemberDetails memberDetails,
			Class<? extends Annotation> annotationType,
			Class<C> contextClass,
			Class<? extends G> generatorClass,
			C creationContext) {
		try {
			try {
				return generatorClass
						.getConstructor( annotationType, Member.class, contextClass )
						.newInstance( annotation.toAnnotation(), memberDetails.toJavaMember(), creationContext);
			}
			catch (NoSuchMethodException ignore) {
				try {
					return generatorClass
							.getConstructor( annotationType )
							.newInstance( annotation.toAnnotation() );
				}
				catch (NoSuchMethodException i) {
					return generatorClass.newInstance();
				}
			}
		}
		catch (InvocationTargetException | InstantiationException | IllegalAccessException | IllegalArgumentException e) {
			throw new HibernateException(
					"Could not instantiate generator of type '" + generatorClass.getName() + "'",
					e
			);
		}
	}

	private static <A extends Annotation> void callInitialize(
			AnnotationUsage<A> annotation,
			MemberDetails memberDetails,
			GeneratorCreationContext creationContext,
			Generator generator) {
		if ( generator instanceof AnnotationBasedGenerator ) {
			// This will cause a CCE in case the generation type doesn't match the annotation type; As this would be
			// a programming error of the generation type developer and thus should show up during testing, we don't
			// check this explicitly; If required, this could be done e.g. using ClassMate
			@SuppressWarnings("unchecked")
			final AnnotationBasedGenerator<A> generation = (AnnotationBasedGenerator<A>) generator;
			generation.initialize( annotation.toAnnotation(), memberDetails.toJavaMember(), creationContext );
		}
	}

	private static void checkVersionGenerationAlways(MemberDetails property, Generator generator) {
		if ( property.hasAnnotationUsage(Version.class) ) {
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
			Properties parameters = collectParameters(
					(SimpleValue) value,
					creationContext.getDatabase().getDialect(),
					creationContext.getPersistentClass().getRootClass(),
					emptyMap()
			);
			( (Configurable) generator ).configure( value.getType(), parameters, creationContext.getServiceRegistry() );
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
			ClassDetails entityClass,
			MemberDetails idAttributeMember) {
		//manage composite related metadata
		//guess if its a component and find id data access (property, field etc)
		final AnnotationUsage<GeneratedValue> generatedValue = idAttributeMember.getAnnotationUsage( GeneratedValue.class );
		final String generatorType = generatorType( context, entityClass, isCompositeId( entityClass, idAttributeMember ), generatedValue );
		final String generatorName = generatedValue == null ? "" : generatedValue.getString( "generator" );
		if ( isGlobalGeneratorNameGlobal( context ) ) {
			buildGenerators( idAttributeMember, context );
			context.getMetadataCollector().addSecondPass( new IdGeneratorResolverSecondPass(
					idValue,
					idAttributeMember,
					generatorType,
					generatorName,
					context
			) );
		}
		else {
			//clone classGenerator and override with local values
			final Map<String, IdentifierGeneratorDefinition> generators = new HashMap<>( classGenerators );
			generators.putAll( buildGenerators( idAttributeMember, context ) );
			makeIdGenerator( idValue, idAttributeMember, generatorType, generatorName, context, generators );
		}
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
			MetadataBuildingContext buildingContext) {

		if ( definition != null ) {
			final Map<String,Object> params = new HashMap<>();

			// see if the specified generator name matches a registered <identifier-generator/>
			final IdentifierGeneratorDefinition generatorDef =
					sourceDocument.getMetadataCollector().getIdentifierGenerator( definition.getName() );
			final String generatorStrategy;
			if ( generatorDef != null ) {
				generatorStrategy = generatorDef.getStrategy();
				params.putAll( generatorDef.getParameters() );
			}
			else {
				generatorStrategy = definition.getStrategy();
			}

			// YUCK!  but cannot think of a clean way to do this given the string-config based scheme
			params.put( PersistentIdentifierGenerator.IDENTIFIER_NORMALIZER,
					buildingContext.getObjectNameNormalizer() );

			params.putAll( definition.getParameters() );

			setGeneratorCreator( identifierValue, params, generatorStrategy );
		}
	}

	private static void setGeneratorCreator(
			SimpleValue identifierValue,
			Map<String, Object> parameters,
			String generatorStrategy) {
		identifierValue.setCustomIdGeneratorCreator( new IdentifierGeneratorCreator() {
			@Override
			public Generator createGenerator(CustomIdGeneratorCreationContext context) {
				final Generator generator =
						createLegacyIdentifierGenerator(
								generatorStrategy,
								identifierValue,
								context.getDatabase().getDialect(),
								context.getRootClass(),
								parameters
						);
				if ( generator instanceof IdentityGenerator) {
					identifierValue.setColumnToIdentity();
				}
				return generator;
			}

			@Override
			public boolean isAssigned() {
				return DEFAULT_ID_GEN_STRATEGY.equals( generatorStrategy );
			}
		} );
	}
}
