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
import java.util.Properties;

import org.hibernate.AnnotationException;
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
import org.hibernate.generator.Assigned;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.Generator;
import org.hibernate.generator.GeneratorCreationContext;
import org.hibernate.generator.OnExecutionGenerator;
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
import org.hibernate.internal.CoreLogging;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.GeneratorCreator;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Table;
import org.hibernate.models.spi.AnnotationTarget;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.SourceModelBuildingContext;
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
import static org.hibernate.boot.internal.GenerationStrategyInterpreter.STRATEGY_INTERPRETER;
import static org.hibernate.boot.model.internal.AnnotationHelper.extractParameterMap;
import static org.hibernate.boot.model.internal.BinderHelper.isCompositeId;
import static org.hibernate.boot.model.internal.BinderHelper.isGlobalGeneratorNameGlobal;
import static org.hibernate.resource.beans.internal.Helper.allowExtensionsInCdi;

/**
 * Responsible for configuring and instantiating {@link Generator}s.
 *
 * @author Gavin King
 */
public class GeneratorBinder {

	private static final Logger LOG = CoreLogging.logger( GeneratorBinder.class );

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
				return org.hibernate.id.Assigned.class;
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
			SimpleValue identifierValue,
			Dialect dialect,
			RootClass rootClass,
			Map<String, Object> configuration) {
		final ConfigurationService configService =
				identifierValue.getMetadata().getMetadataBuildingOptions().getServiceRegistry()
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
		final String tableName = identifierValue.getTable().getQuotedName( dialect );
		params.setProperty( PersistentIdentifierGenerator.TABLE, tableName );

		//pass the column name (a generated id almost always has a single column)
		final Column column = (Column) identifierValue.getSelectables().get(0);
		final String columnName = column.getQuotedName( dialect );
		params.setProperty( PersistentIdentifierGenerator.PK, columnName );

		//pass the entity-name, if not a collection-id
		if ( rootClass != null ) {
			params.setProperty( IdentifierGenerator.ENTITY_NAME, rootClass.getEntityName() );
			params.setProperty( IdentifierGenerator.JPA_ENTITY_NAME, rootClass.getJpaEntityName() );
			// The table name is not really a good default for subselect entities,
			// so use the JPA entity name which is short
			params.setProperty( OptimizableGenerator.IMPLICIT_NAME_BASE,
					identifierValue.getTable().isSubselect()
							? rootClass.getJpaEntityName()
							: identifierValue.getTable().getName() );

			params.setProperty( PersistentIdentifierGenerator.TABLES,
					identityTablesString( dialect, rootClass ) );
		}
		else {
			params.setProperty( PersistentIdentifierGenerator.TABLES, tableName );
			params.setProperty( OptimizableGenerator.IMPLICIT_NAME_BASE, tableName );
		}

		params.put( IdentifierGenerator.CONTRIBUTOR_NAME,
				identifierValue.getBuildingContext().getCurrentContributorName() );

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
			MetadataBuildingContext context,
			Map<String, ? extends IdentifierGeneratorDefinition> localGenerators) {

		//generator settings
		final Map<String,Object> configuration = new HashMap<>();

		//always settable
		configuration.put( PersistentIdentifierGenerator.TABLE, id.getTable().getName() );

		if ( id.getColumnSpan() == 1 ) {
			configuration.put( PersistentIdentifierGenerator.PK, id.getColumns().get(0).getName() );
		}
		// YUCK!  but cannot think of a clean way to do this given the string-config based scheme
		configuration.put( PersistentIdentifierGenerator.IDENTIFIER_NORMALIZER, context.getObjectNameNormalizer() );
		configuration.put( IdentifierGenerator.GENERATOR_NAME, generatorName );

		final String generatorStrategy =
				determineStrategy( idAttributeMember, generatorType, generatorName, context, localGenerators, configuration );
		setGeneratorCreator( id, configuration, generatorStrategy, beanContainer( context ) );
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
		makeIdGenerator( id, idAttributeMember, generatorType, generatorName, buildingContext,
				foreignKGeneratorDefinition != null
						? Map.of( foreignKGeneratorDefinition.getName(), foreignKGeneratorDefinition )
						: null );
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
			// this should really never happen, but it's easy to protect against it...
			return new IdentifierGeneratorDefinition(ASSIGNED_GENERATOR_NAME, ASSIGNED_GENERATOR_NAME);
		}

		return IdentifierGeneratorDefinition.createImplicit(
				name,
				idAttributeMember.getType().determineRawClass().toJavaClass(),
				generatedValue.generator(),
				interpretGenerationType( generatedValue )
		);
	}

	private static GenerationType interpretGenerationType(GeneratedValue generatedValueAnn) {
		// todo (jpa32) : when can this ever be null?
		final GenerationType strategy = generatedValueAnn.strategy();
		return strategy == null ? GenerationType.AUTO : strategy;
	}

	public static Map<String, IdentifierGeneratorDefinition> buildGenerators(
			AnnotationTarget annotatedElement,
			MetadataBuildingContext context) {
		final InFlightMetadataCollector metadataCollector = context.getMetadataCollector();
		final SourceModelBuildingContext sourceModelContext = metadataCollector.getSourceModelBuildingContext();
		final Map<String, IdentifierGeneratorDefinition> generators = new HashMap<>();

		annotatedElement.forEachAnnotationUsage( TableGenerator.class, sourceModelContext, (usage) -> {
			IdentifierGeneratorDefinition idGenerator = buildIdGenerator( usage, context );
			generators.put( idGenerator.getName(), idGenerator );
			metadataCollector.addIdentifierGenerator( idGenerator );
		} );

		annotatedElement.forEachAnnotationUsage( SequenceGenerator.class, sourceModelContext, (usage) -> {
			IdentifierGeneratorDefinition idGenerator = buildIdGenerator( usage, context );
			generators.put( idGenerator.getName(), idGenerator );
			metadataCollector.addIdentifierGenerator( idGenerator );
		} );

		annotatedElement.forEachAnnotationUsage( GenericGenerator.class, sourceModelContext, (usage) -> {
			final IdentifierGeneratorDefinition idGenerator = buildIdGenerator( usage, context );
			generators.put( idGenerator.getName(), idGenerator );
			metadataCollector.addIdentifierGenerator( idGenerator );
		} );

		return generators;
	}

	static String generatorType(
			MetadataBuildingContext context,
			ClassDetails entityXClass,
			boolean isComponent,
			GeneratedValue generatedValue) {
		if ( isComponent ) {
			//a component must not have any generator
			return ASSIGNED_GENERATOR_NAME;
		}
		else {
			return generatedValue == null ? ASSIGNED_GENERATOR_NAME : generatorType( generatedValue, entityXClass, context );
		}
	}

	static String generatorType(
			GeneratedValue generatedValue,
			final ClassDetails javaClass,
			MetadataBuildingContext context) {
		return STRATEGY_INTERPRETER.determineGeneratorName(
				generatedValue.strategy(),
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
		if ( generatorAnnotation instanceof TableGenerator tableGenerator ) {
			STRATEGY_INTERPRETER.interpretTableGenerator( tableGenerator, definitionBuilder );
			if ( LOG.isTraceEnabled() ) {
				LOG.tracev( "Add table generator with name: {0}", definitionBuilder.getName() );
			}
		}
		else if ( generatorAnnotation instanceof SequenceGenerator sequenceGenerator ) {
			STRATEGY_INTERPRETER.interpretSequenceGenerator( sequenceGenerator, definitionBuilder );
			if ( LOG.isTraceEnabled() ) {
				LOG.tracev( "Add sequence generator with name: {0}", definitionBuilder.getName() );
			}
		}
		else if ( generatorAnnotation instanceof GenericGenerator genericGenerator ) {
			definitionBuilder.setName( genericGenerator.name() );
			final Class<? extends Generator> generatorClass = genericGenerator.type();
			final String strategy =
					generatorClass.equals( Generator.class )
							? genericGenerator.strategy()
							: generatorClass.getName();
			definitionBuilder.setStrategy( strategy );
			definitionBuilder.addParams( extractParameterMap( genericGenerator.parameters() ) );
			if ( LOG.isTraceEnabled() ) {
				LOG.tracev( "Add generic generator with name: {0}", definitionBuilder.getName() );
			}
		}

		return definitionBuilder.build();
	}

	static IdentifierGeneratorDefinition buildSequenceIdGenerator(SequenceGenerator generatorAnnotation) {
		if ( generatorAnnotation == null ) {
			return null;
		}

		final IdentifierGeneratorDefinition.Builder definitionBuilder = new IdentifierGeneratorDefinition.Builder();
		STRATEGY_INTERPRETER.interpretSequenceGenerator( generatorAnnotation, definitionBuilder );
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
			Annotation annotation,
			BeanContainer beanContainer) {
		final Class<? extends Annotation> annotationType = annotation.annotationType();
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

	static GeneratorCreator identifierGeneratorCreator(
			MemberDetails idAttributeMember,
			Annotation annotation,
			SimpleValue identifierValue,
			BeanContainer beanContainer) {
		final Class<? extends Annotation> annotationType = annotation.annotationType();
		final IdGeneratorType idGeneratorType = annotationType.getAnnotation( IdGeneratorType.class );
		assert idGeneratorType != null;
		final Class<? extends Generator> generatorClass = idGeneratorType.value();
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
	private static <C> Generator instantiateGenerator(
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
	private static Generator instantiateGenerator(
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
	 * call its {@link Configurable#configure(Type, Properties, ServiceRegistry)
	 * configure()} method.
	 */
	private static void callConfigure(
			GeneratorCreationContext creationContext,
			Generator generator,
			Map<String, Object> configuration,
			SimpleValue identifierValue) {
		if ( generator instanceof Configurable ) {
			final Configurable configurable = (Configurable) generator;
			final Properties parameters = collectParameters(
					identifierValue,
					creationContext.getDatabase().getDialect(),
					creationContext.getRootClass(),
					configuration
			);
			configurable.configure( identifierValue.getType(), parameters, creationContext.getServiceRegistry() );
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
		final GeneratedValue generatedValue = idAttributeMember.getDirectAnnotationUsage( GeneratedValue.class );
		final String generatorType = generatorType( context, entityClass, isCompositeId( entityClass, idAttributeMember ), generatedValue );
		final String generatorName = generatedValue == null ? "" : generatedValue.generator();
		if ( isGlobalGeneratorNameGlobal( context ) ) {
			buildGenerators( idAttributeMember, context );
			context.getMetadataCollector()
					.addSecondPass( new IdGeneratorResolverSecondPass(
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
			final Map<String,Object> configuration = new HashMap<>();

			// see if the specified generator name matches a registered <identifier-generator/>
			final IdentifierGeneratorDefinition generatorDef =
					sourceDocument.getMetadataCollector().getIdentifierGenerator( definition.getName() );
			final String generatorStrategy;
			if ( generatorDef != null ) {
				generatorStrategy = generatorDef.getStrategy();
				configuration.putAll( generatorDef.getParameters() );
			}
			else {
				generatorStrategy = definition.getStrategy();
			}

			// YUCK!  but cannot think of a clean way to do this given the string-config based scheme
			configuration.put( PersistentIdentifierGenerator.IDENTIFIER_NORMALIZER,
					buildingContext.getObjectNameNormalizer() );

			configuration.putAll( definition.getParameters() );

			setGeneratorCreator( identifierValue, configuration, generatorStrategy, beanContainer( buildingContext ) );
		}
	}

	/**
	 * Obtain a {@link BeanContainer} to be used for instantiating generators.
	 */
	static BeanContainer beanContainer(MetadataBuildingContext buildingContext) {
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
			BeanContainer beanContainer) {
		if ( ASSIGNED_GENERATOR_NAME.equals( generatorStrategy )
				|| org.hibernate.id.Assigned.class.getName().equals( generatorStrategy ) ) {
			identifierValue.setCustomIdGeneratorCreator( ASSIGNED_IDENTIFIER_GENERATOR_CREATOR );
		}
		else {
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
}
