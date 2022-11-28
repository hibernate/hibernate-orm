/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.MappingException;
import org.hibernate.TimeZoneStorageStrategy;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CollectionTypeRegistration;
import org.hibernate.annotations.CollectionTypeRegistrations;
import org.hibernate.annotations.Columns;
import org.hibernate.annotations.CompositeType;
import org.hibernate.annotations.CompositeTypeRegistration;
import org.hibernate.annotations.CompositeTypeRegistrations;
import org.hibernate.annotations.ConverterRegistration;
import org.hibernate.annotations.ConverterRegistrations;
import org.hibernate.annotations.EmbeddableInstantiatorRegistration;
import org.hibernate.annotations.EmbeddableInstantiatorRegistrations;
import org.hibernate.annotations.FetchProfile;
import org.hibernate.annotations.FetchProfiles;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.FilterDefs;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.GenericGenerators;
import org.hibernate.annotations.IdGeneratorType;
import org.hibernate.annotations.Imported;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.JavaTypeRegistration;
import org.hibernate.annotations.JavaTypeRegistrations;
import org.hibernate.annotations.JdbcTypeRegistration;
import org.hibernate.annotations.JdbcTypeRegistrations;
import org.hibernate.annotations.LazyGroup;
import org.hibernate.annotations.ManyToAny;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.ParamDef;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Parent;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TypeRegistration;
import org.hibernate.annotations.TypeRegistrations;
import org.hibernate.annotations.ValueGenerationType;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XAnnotatedElement;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XMethod;
import org.hibernate.annotations.common.reflection.XPackage;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.boot.model.IdGeneratorStrategyInterpreter;
import org.hibernate.boot.model.IdentifierGeneratorDefinition;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.model.convert.spi.RegisteredConversion;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.annotations.CollectionBinder;
import org.hibernate.cfg.annotations.EntityBinder;
import org.hibernate.cfg.annotations.Nullability;
import org.hibernate.cfg.annotations.PropertyBinder;
import org.hibernate.cfg.annotations.QueryBinder;
import org.hibernate.dialect.TimeZoneSupport;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.GenericsHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Any;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.model.convert.internal.JpaAttributeConverterImpl;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.property.access.internal.PropertyAccessStrategyCompositeUserTypeImpl;
import org.hibernate.property.access.internal.PropertyAccessStrategyMixedImpl;
import org.hibernate.property.access.spi.PropertyAccessStrategy;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.generator.InMemoryGenerator;
import org.hibernate.type.BasicType;
import org.hibernate.type.CustomType;
import org.hibernate.type.descriptor.java.BasicJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.spi.JavaTypeRegistry;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.internal.ConvertedBasicTypeImpl;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.usertype.CompositeUserType;
import org.hibernate.usertype.UserType;
import org.hibernate.usertype.internal.OffsetDateTimeCompositeUserType;
import org.hibernate.usertype.internal.ZonedDateTimeCompositeUserType;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.MapsId;
import jakarta.persistence.NamedNativeQueries;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.NamedStoredProcedureQueries;
import jakarta.persistence.NamedStoredProcedureQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.SequenceGenerators;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.SqlResultSetMappings;
import jakarta.persistence.TableGenerator;
import jakarta.persistence.TableGenerators;
import jakarta.persistence.Version;

import static org.hibernate.cfg.BinderHelper.getMappedSuperclassOrNull;
import static org.hibernate.cfg.BinderHelper.getOverridableAnnotation;
import static org.hibernate.cfg.BinderHelper.getPath;
import static org.hibernate.cfg.BinderHelper.getPropertyOverriddenByMapperOrMapsId;
import static org.hibernate.cfg.BinderHelper.getRelativePath;
import static org.hibernate.cfg.BinderHelper.hasToOneAnnotation;
import static org.hibernate.cfg.BinderHelper.makeIdGenerator;
import static org.hibernate.cfg.InheritanceState.getInheritanceStateOfSuperEntity;
import static org.hibernate.cfg.InheritanceState.getSuperclassInheritanceState;
import static org.hibernate.cfg.PropertyHolderBuilder.buildPropertyHolder;
import static org.hibernate.cfg.annotations.HCANNHelper.findContainingAnnotation;
import static org.hibernate.cfg.annotations.PropertyBinder.identifierGeneratorCreator;
import static org.hibernate.internal.CoreLogging.messageLogger;
import static org.hibernate.mapping.Constraint.hashedName;
import static org.hibernate.mapping.SimpleValue.DEFAULT_ID_GEN_STRATEGY;

/**
 * Reads annotations from Java classes and produces the Hibernate configuration-time metamodel,
 * that is, the objects defined in the package {@link org.hibernate.mapping}.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public final class AnnotationBinder {
	private static final CoreMessageLogger LOG = messageLogger( AnnotationBinder.class );

	private static final String OFFSET_DATETIME_CLASS = OffsetDateTime.class.getName();
	private static final String ZONED_DATETIME_CLASS = ZonedDateTime.class.getName();

	private AnnotationBinder() {}

	public static void bindDefaults(MetadataBuildingContext context) {
		final Map<?,?> defaults = context.getBootstrapContext().getReflectionManager().getDefaults();

		// id generators ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		{
			@SuppressWarnings("unchecked")
			List<SequenceGenerator> generators = ( List<SequenceGenerator> ) defaults.get( SequenceGenerator.class );
			if ( generators != null ) {
				for ( SequenceGenerator sequenceGenerator : generators ) {
					final IdentifierGeneratorDefinition idGen = buildIdGenerator( sequenceGenerator, context );
					if ( idGen != null ) {
						context.getMetadataCollector().addDefaultIdentifierGenerator( idGen );
					}
				}
			}
		}
		{
			@SuppressWarnings("unchecked")
			List<TableGenerator> generators = ( List<TableGenerator> ) defaults.get( TableGenerator.class );
			if ( generators != null ) {
				for ( TableGenerator tableGenerator : generators ) {
					final IdentifierGeneratorDefinition idGen = buildIdGenerator( tableGenerator, context );
					if ( idGen != null ) {
						context.getMetadataCollector().addDefaultIdentifierGenerator( idGen );
					}
				}
			}
		}

		{
			@SuppressWarnings("unchecked")
			List<TableGenerators> generators = (List<TableGenerators>) defaults.get( TableGenerators.class );
			if ( generators != null ) {
				generators.forEach( tableGenerators -> {
					for ( TableGenerator tableGenerator : tableGenerators.value() ) {
						final IdentifierGeneratorDefinition idGen = buildIdGenerator( tableGenerator, context );
						if ( idGen != null ) {
							context.getMetadataCollector().addDefaultIdentifierGenerator( idGen );
						}
					}
				} );
			}
		}

		{
			@SuppressWarnings("unchecked")
			List<SequenceGenerators> generators = (List<SequenceGenerators>) defaults.get( SequenceGenerators.class );
			if ( generators != null ) {
				generators.forEach( sequenceGenerators -> {
					for ( SequenceGenerator sequenceGenerator : sequenceGenerators.value() ) {
						final IdentifierGeneratorDefinition idGen = buildIdGenerator( sequenceGenerator, context );
						if ( idGen != null ) {
							context.getMetadataCollector().addDefaultIdentifierGenerator( idGen );
						}
					}
				} );
			}
		}

		// queries ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		{
			@SuppressWarnings("unchecked")
			List<NamedQuery> queries = ( List<NamedQuery> ) defaults.get( NamedQuery.class );
			if ( queries != null ) {
				for ( NamedQuery ann : queries ) {
					QueryBinder.bindQuery( ann, context, true );
				}
			}
		}
		{
			@SuppressWarnings("unchecked")
			List<NamedNativeQuery> nativeQueries = ( List<NamedNativeQuery> ) defaults.get( NamedNativeQuery.class );
			if ( nativeQueries != null ) {
				for ( NamedNativeQuery ann : nativeQueries ) {
					QueryBinder.bindNativeQuery( ann, context, true );
				}
			}
		}

		// result-set-mappings ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		{
			@SuppressWarnings("unchecked")
			List<SqlResultSetMapping> mappings = ( List<SqlResultSetMapping> ) defaults.get( SqlResultSetMapping.class );
			if ( mappings != null ) {
				for ( SqlResultSetMapping ann : mappings ) {
					QueryBinder.bindSqlResultSetMapping( ann, context, true );
				}
			}
		}

		// stored procs ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		{
			@SuppressWarnings("unchecked")
			final List<NamedStoredProcedureQuery> storedProcedureQueries =
					(List<NamedStoredProcedureQuery>) defaults.get( NamedStoredProcedureQuery.class );
			if ( storedProcedureQueries != null ) {
				for ( NamedStoredProcedureQuery annotation : storedProcedureQueries ) {
					bindNamedStoredProcedureQuery( annotation, context, true );
				}
			}
		}
		{
			@SuppressWarnings("unchecked")
			final List<NamedStoredProcedureQueries> storedProcedureQueries =
					(List<NamedStoredProcedureQueries>) defaults.get( NamedStoredProcedureQueries.class );
			if ( storedProcedureQueries != null ) {
				for ( NamedStoredProcedureQueries annotation : storedProcedureQueries ) {
					bindNamedStoredProcedureQueries( annotation, context, true );
				}
			}
		}
	}

	public static void bindPackage(ClassLoaderService cls, String packageName, MetadataBuildingContext context) {
		final Package pack = cls.packageForNameOrNull( packageName );
		if ( pack == null ) {
			return;
		}
		final XPackage annotatedPackage = context.getBootstrapContext().getReflectionManager().toXPackage( pack );

		handleIdGenerators( annotatedPackage, context );

		handleTypeDescriptorRegistrations( annotatedPackage, context );
		bindEmbeddableInstantiatorRegistrations( annotatedPackage, context );
		bindUserTypeRegistrations( annotatedPackage, context );
		bindCompositeUserTypeRegistrations( annotatedPackage, context );
		handleConverterRegistrations( annotatedPackage, context );

		bindGenericGenerators( annotatedPackage, context );
		bindQueries( annotatedPackage, context );
		bindFilterDefs( annotatedPackage, context );
	}

	private static void handleIdGenerators(XPackage annotatedPackage, MetadataBuildingContext context) {
		if ( annotatedPackage.isAnnotationPresent( SequenceGenerator.class ) ) {
			final SequenceGenerator sequenceGenerator = annotatedPackage.getAnnotation( SequenceGenerator.class );
			IdentifierGeneratorDefinition idGen = buildIdGenerator( sequenceGenerator, context );
			context.getMetadataCollector().addIdentifierGenerator( idGen );
			if ( LOG.isTraceEnabled() ) {
				LOG.tracev( "Add sequence generator with name: {0}", idGen.getName() );
			}
		}
		if ( annotatedPackage.isAnnotationPresent( SequenceGenerators.class ) ) {
			final SequenceGenerators sequenceGenerators = annotatedPackage.getAnnotation( SequenceGenerators.class );
			for ( SequenceGenerator tableGenerator : sequenceGenerators.value() ) {
				context.getMetadataCollector().addIdentifierGenerator( buildIdGenerator( tableGenerator, context ) );
			}
		}

		if ( annotatedPackage.isAnnotationPresent( TableGenerator.class ) ) {
			final TableGenerator tableGenerator = annotatedPackage.getAnnotation( TableGenerator.class );
			IdentifierGeneratorDefinition idGen = buildIdGenerator( tableGenerator, context );
			context.getMetadataCollector().addIdentifierGenerator( idGen );
		}
		if ( annotatedPackage.isAnnotationPresent( TableGenerators.class ) ) {
			final TableGenerators tableGenerators = annotatedPackage.getAnnotation( TableGenerators.class );
			for ( TableGenerator tableGenerator : tableGenerators.value() ) {
				context.getMetadataCollector().addIdentifierGenerator( buildIdGenerator( tableGenerator, context ) );
			}
		}
	}

	private static void bindGenericGenerators(XAnnotatedElement annotatedElement, MetadataBuildingContext context) {
		final GenericGenerator genericGenerator = annotatedElement.getAnnotation( GenericGenerator.class );
		final GenericGenerators genericGenerators = annotatedElement.getAnnotation( GenericGenerators.class );
		if ( genericGenerator != null ) {
			bindGenericGenerator( genericGenerator, context );
		}
		if ( genericGenerators != null ) {
			for ( GenericGenerator generator : genericGenerators.value() ) {
				bindGenericGenerator( generator, context );
			}
		}
	}

	private static void bindGenericGenerator(GenericGenerator def, MetadataBuildingContext context) {
		context.getMetadataCollector().addIdentifierGenerator( buildIdGenerator( def, context ) );
	}

	private static void bindNamedJpaQueries(XAnnotatedElement annotatedElement, MetadataBuildingContext context) {
		QueryBinder.bindSqlResultSetMapping(
				annotatedElement.getAnnotation( SqlResultSetMapping.class ),
				context,
				false
		);

		final SqlResultSetMappings ann = annotatedElement.getAnnotation( SqlResultSetMappings.class );
		if ( ann != null ) {
			for ( SqlResultSetMapping current : ann.value() ) {
				QueryBinder.bindSqlResultSetMapping( current, context, false );
			}
		}

		QueryBinder.bindQuery(
				annotatedElement.getAnnotation( NamedQuery.class ),
				context,
				false
		);

		QueryBinder.bindQueries(
				annotatedElement.getAnnotation( NamedQueries.class ),
				context,
				false
		);

		QueryBinder.bindNativeQuery(
				annotatedElement.getAnnotation( NamedNativeQuery.class ),
				context,
				false
		);

		QueryBinder.bindNativeQueries(
				annotatedElement.getAnnotation( NamedNativeQueries.class ),
				context,
				false
		);
	}

	public static void bindQueries(XAnnotatedElement annotatedElement, MetadataBuildingContext context) {
		bindNamedJpaQueries( annotatedElement, context );

		QueryBinder.bindQuery(
				annotatedElement.getAnnotation( org.hibernate.annotations.NamedQuery.class ),
				context
		);

		QueryBinder.bindQueries(
				annotatedElement.getAnnotation( org.hibernate.annotations.NamedQueries.class ),
				context
		);

		QueryBinder.bindNativeQuery(
				annotatedElement.getAnnotation( org.hibernate.annotations.NamedNativeQuery.class ),
				context
		);

		QueryBinder.bindNativeQueries(
				annotatedElement.getAnnotation( org.hibernate.annotations.NamedNativeQueries.class ),
				context
		);

		// NamedStoredProcedureQuery handling ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		bindNamedStoredProcedureQuery(
				annotatedElement.getAnnotation( NamedStoredProcedureQuery.class ),
				context,
				false
		);

		// NamedStoredProcedureQueries handling ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		bindNamedStoredProcedureQueries(
				annotatedElement.getAnnotation( NamedStoredProcedureQueries.class ),
				context,
				false
		);
	}

	private static void bindNamedStoredProcedureQueries(
			NamedStoredProcedureQueries annotation,
			MetadataBuildingContext context,
			boolean isDefault) {
		if ( annotation != null ) {
			for ( NamedStoredProcedureQuery queryAnnotation : annotation.value() ) {
				bindNamedStoredProcedureQuery( queryAnnotation, context, isDefault );
			}
		}
	}

	private static void bindNamedStoredProcedureQuery(
			NamedStoredProcedureQuery annotation,
			MetadataBuildingContext context,
			boolean isDefault) {
		if ( annotation != null ) {
			QueryBinder.bindNamedStoredProcedureQuery( annotation, context, isDefault );
		}
	}

	private static IdentifierGeneratorDefinition buildIdGenerator(
			Annotation generatorAnnotation,
			MetadataBuildingContext context) {
		if ( generatorAnnotation == null ) {
			return null;
		}

		final IdentifierGeneratorDefinition.Builder definitionBuilder = new IdentifierGeneratorDefinition.Builder();
		if ( generatorAnnotation instanceof TableGenerator ) {
			context.getBuildingOptions().getIdGenerationTypeInterpreter().interpretTableGenerator(
					(TableGenerator) generatorAnnotation,
					definitionBuilder
			);
			if ( LOG.isTraceEnabled() ) {
				LOG.tracev( "Add table generator with name: {0}", definitionBuilder.getName() );
			}
		}
		else if ( generatorAnnotation instanceof SequenceGenerator ) {
			context.getBuildingOptions().getIdGenerationTypeInterpreter().interpretSequenceGenerator(
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
			final String strategy = genericGenerator.type().equals(InMemoryGenerator.class)
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

	/**
	 * Bind an annotated class. A subclass must be bound <em>after</em> its superclass.
	 *
	 * @param annotatedClass entity to bind as {@code XClass} instance
	 * @param inheritanceStatePerClass Metadata about the inheritance relationships for all mapped classes
	 *
	 * @throws MappingException in case there is a configuration error
	 */
	public static void bindClass(
			XClass annotatedClass,
			Map<XClass, InheritanceState> inheritanceStatePerClass,
			MetadataBuildingContext context) throws MappingException {

		detectMappedSuperclassProblems( annotatedClass );

		switch ( context.getMetadataCollector().getClassType( annotatedClass ) ) {
			case MAPPED_SUPERCLASS:
				// Allow queries and filters to be declared by a @MappedSuperclass
				bindQueries( annotatedClass, context );
				bindFilterDefs( annotatedClass, context );
				//fall through:
			case IMPORTED:
				handleImport( annotatedClass, context );
			case EMBEDDABLE:
			case NONE:
				return;
		}

		// try to find class level generators
		final Map<String, IdentifierGeneratorDefinition> generators = buildGenerators( annotatedClass, context );
		handleTypeDescriptorRegistrations( annotatedClass, context );
		bindEmbeddableInstantiatorRegistrations( annotatedClass, context );
		bindUserTypeRegistrations( annotatedClass, context );
		bindCompositeUserTypeRegistrations( annotatedClass, context );
		handleConverterRegistrations( annotatedClass, context );

		bindQueries( annotatedClass, context );
		bindFilterDefs( annotatedClass, context );

		EntityBinder.bindEntityClass( annotatedClass, inheritanceStatePerClass, generators, context );
	}

	private static void handleImport(XClass annotatedClass, MetadataBuildingContext context) {
		if ( annotatedClass.isAnnotationPresent( Imported.class ) ) {
			String qualifiedName = annotatedClass.getName();
			String name = StringHelper.unqualify( qualifiedName );
			String rename = annotatedClass.getAnnotation( Imported.class ).rename();
			context.getMetadataCollector().addImport( rename.isEmpty() ? name : rename, qualifiedName );
		}
	}

	private static void detectMappedSuperclassProblems(XClass annotatedClass) {
		//@Entity and @MappedSuperclass on the same class leads to a NPE down the road
		if ( annotatedClass.isAnnotationPresent( Entity.class )
				&&  annotatedClass.isAnnotationPresent( MappedSuperclass.class ) ) {
			throw new AnnotationException( "Type '"+ annotatedClass.getName()
					+ "' is annotated both '@Entity' and '@MappedSuperclass'" );
		}

		if ( annotatedClass.isAnnotationPresent( Inheritance.class )
				&&  annotatedClass.isAnnotationPresent( MappedSuperclass.class ) ) {
			LOG.unsupportedMappedSuperclassWithEntityInheritance( annotatedClass.getName() );
		}
	}

	private static void handleTypeDescriptorRegistrations(XAnnotatedElement annotatedElement, MetadataBuildingContext context) {
		final ManagedBeanRegistry managedBeanRegistry = context.getBootstrapContext()
				.getServiceRegistry()
				.getService( ManagedBeanRegistry.class );

		final JavaTypeRegistration javaTypeRegistration = annotatedElement.getAnnotation( JavaTypeRegistration.class );
		if ( javaTypeRegistration != null ) {
			handleJavaTypeRegistration( context, managedBeanRegistry, javaTypeRegistration );
		}
		else {
			final JavaTypeRegistrations javaTypeRegistrations = annotatedElement.getAnnotation( JavaTypeRegistrations.class );
			if ( javaTypeRegistrations != null ) {
				final JavaTypeRegistration[] registrations = javaTypeRegistrations.value();
				for ( JavaTypeRegistration registration : registrations ) {
					handleJavaTypeRegistration( context, managedBeanRegistry, registration );
				}
			}
		}

		final JdbcTypeRegistration jdbcTypeRegistration = annotatedElement.getAnnotation( JdbcTypeRegistration.class );
		if ( jdbcTypeRegistration != null ) {
			handleJdbcTypeRegistration( context, managedBeanRegistry, jdbcTypeRegistration );
		}
		else {
			final JdbcTypeRegistrations jdbcTypeRegistrations = annotatedElement.getAnnotation( JdbcTypeRegistrations.class );
			if ( jdbcTypeRegistrations != null ) {
				final JdbcTypeRegistration[] registrations = jdbcTypeRegistrations.value();
				for ( JdbcTypeRegistration registration : registrations ) {
					handleJdbcTypeRegistration( context, managedBeanRegistry, registration );
				}
			}
		}

		final CollectionTypeRegistration collectionTypeRegistration =
				annotatedElement.getAnnotation( CollectionTypeRegistration.class );
		if ( collectionTypeRegistration != null ) {
			context.getMetadataCollector().addCollectionTypeRegistration( collectionTypeRegistration );
		}

		final CollectionTypeRegistrations collectionTypeRegistrations =
				annotatedElement.getAnnotation( CollectionTypeRegistrations.class );
		if ( collectionTypeRegistrations != null ) {
			for ( CollectionTypeRegistration registration : collectionTypeRegistrations.value() ) {
				context.getMetadataCollector().addCollectionTypeRegistration( registration );
			}
		}
	}

	private static void handleJdbcTypeRegistration(
			MetadataBuildingContext context,
			ManagedBeanRegistry managedBeanRegistry,
			JdbcTypeRegistration annotation) {
		final Class<? extends JdbcType> jdbcTypeClass = annotation.value();
		final JdbcType jdbcType = managedBeanRegistry.getBean( jdbcTypeClass ).getBeanInstance();
		final int typeCode = annotation.registrationCode() == Integer.MIN_VALUE
				? jdbcType.getJdbcTypeCode()
				: annotation.registrationCode();
		context.getMetadataCollector().addJdbcTypeRegistration( typeCode, jdbcType );
	}

	private static void handleJavaTypeRegistration(
			MetadataBuildingContext context,
			ManagedBeanRegistry managedBeanRegistry,
			JavaTypeRegistration annotation) {
		final Class<? extends BasicJavaType<?>> jtdClass = annotation.descriptorClass();
		final BasicJavaType<?> jtd = managedBeanRegistry.getBean( jtdClass ).getBeanInstance();
		context.getMetadataCollector().addJavaTypeRegistration( annotation.javaType(), jtd );
	}

	private static void bindEmbeddableInstantiatorRegistrations(
			XAnnotatedElement annotatedElement,
			MetadataBuildingContext context) {
		final EmbeddableInstantiatorRegistration embeddableInstantiatorRegistration =
				annotatedElement.getAnnotation( EmbeddableInstantiatorRegistration.class );
		if ( embeddableInstantiatorRegistration != null ) {
			handleEmbeddableInstantiatorRegistration( context, embeddableInstantiatorRegistration );
		}
		else {
			final EmbeddableInstantiatorRegistrations embeddableInstantiatorRegistrations =
					annotatedElement.getAnnotation( EmbeddableInstantiatorRegistrations.class );
			if ( embeddableInstantiatorRegistrations != null ) {
				final EmbeddableInstantiatorRegistration[] registrations = embeddableInstantiatorRegistrations.value();
				for ( EmbeddableInstantiatorRegistration registration : registrations ) {
					handleEmbeddableInstantiatorRegistration( context, registration );
				}
			}
		}
	}

	private static void handleEmbeddableInstantiatorRegistration(
			MetadataBuildingContext context,
			EmbeddableInstantiatorRegistration annotation) {
		context.getMetadataCollector().registerEmbeddableInstantiator(
				annotation.embeddableClass(),
				annotation.instantiator()
		);
	}

	private static void bindCompositeUserTypeRegistrations(
			XAnnotatedElement annotatedElement,
			MetadataBuildingContext context) {
		final CompositeTypeRegistration compositeTypeRegistration =
				annotatedElement.getAnnotation( CompositeTypeRegistration.class );
		if ( compositeTypeRegistration != null ) {
			handleCompositeUserTypeRegistration( context, compositeTypeRegistration );
		}
		else {
			final CompositeTypeRegistrations compositeTypeRegistrations =
					annotatedElement.getAnnotation( CompositeTypeRegistrations.class );
			if ( compositeTypeRegistrations != null ) {
				final CompositeTypeRegistration[] registrations = compositeTypeRegistrations.value();
				for ( CompositeTypeRegistration registration : registrations ) {
					handleCompositeUserTypeRegistration( context, registration );
				}
			}
		}
	}

	private static void bindUserTypeRegistrations(
			XAnnotatedElement annotatedElement,
			MetadataBuildingContext context) {
		final TypeRegistration typeRegistration =
				annotatedElement.getAnnotation( TypeRegistration.class );
		if ( typeRegistration != null ) {
			handleUserTypeRegistration( context, typeRegistration );
		}
		else {
			final TypeRegistrations typeRegistrations =
					annotatedElement.getAnnotation( TypeRegistrations.class );
			if ( typeRegistrations != null ) {
				final TypeRegistration[] registrations = typeRegistrations.value();
				for ( TypeRegistration registration : registrations ) {
					handleUserTypeRegistration( context, registration );
				}
			}
		}
	}

	private static void handleUserTypeRegistration(
			MetadataBuildingContext context,
			TypeRegistration compositeTypeRegistration) {
		context.getMetadataCollector().registerUserType(
				compositeTypeRegistration.basicClass(),
				compositeTypeRegistration.userType()
		);
	}

	private static void handleCompositeUserTypeRegistration(
			MetadataBuildingContext context,
			CompositeTypeRegistration compositeTypeRegistration) {
		context.getMetadataCollector().registerCompositeUserType(
				compositeTypeRegistration.embeddableClass(),
				compositeTypeRegistration.userType()
		);
	}

	private static void handleConverterRegistrations(XAnnotatedElement container, MetadataBuildingContext context) {
		final ConverterRegistration converterRegistration = container.getAnnotation( ConverterRegistration.class );
		if ( converterRegistration != null ) {
			handleConverterRegistration( converterRegistration, context );
			return;
		}

		final ConverterRegistrations converterRegistrations = container.getAnnotation( ConverterRegistrations.class );
		if ( converterRegistrations != null ) {
			final ConverterRegistration[] registrations = converterRegistrations.value();
			for ( ConverterRegistration registration : registrations ) {
				handleConverterRegistration( registration, context );
			}
		}
	}

	private static void handleConverterRegistration(ConverterRegistration registration, MetadataBuildingContext context) {
		final InFlightMetadataCollector metadataCollector = context.getMetadataCollector();
		metadataCollector.getConverterRegistry().addRegisteredConversion(
				new RegisteredConversion(
						registration.domainType(),
						registration.converter(),
						registration.autoApply(),
						context
				)
		);
	}

	public static void bindFilterDefs(XAnnotatedElement annotatedElement, MetadataBuildingContext context) {
		final FilterDef filterDef = annotatedElement.getAnnotation( FilterDef.class );
		final FilterDefs filterDefs = getOverridableAnnotation( annotatedElement, FilterDefs.class, context );
		if ( filterDef != null ) {
			bindFilterDef( filterDef, context );
		}
		if ( filterDefs != null ) {
			for ( FilterDef def : filterDefs.value() ) {
				bindFilterDef( def, context );
			}
		}
	}

	private static void bindFilterDef(FilterDef filterDef, MetadataBuildingContext context) {
		final Map<String, JdbcMapping> explicitParamJaMappings = filterDef.parameters().length == 0 ? null : new HashMap<>();
		for ( ParamDef paramDef : filterDef.parameters() ) {
			final JdbcMapping jdbcMapping = resolveFilterParamType( paramDef.type(), context );
			if ( jdbcMapping == null ) {
				throw new MappingException(
						String.format(
								Locale.ROOT,
								"Unable to resolve type specified for parameter (%s) defined for @FilterDef (%s)",
								paramDef.name(),
								filterDef.name()
						)
				);
			}
			explicitParamJaMappings.put( paramDef.name(), jdbcMapping );
		}
		final FilterDefinition filterDefinition =
				new FilterDefinition( filterDef.name(), filterDef.defaultCondition(), explicitParamJaMappings );
		LOG.debugf( "Binding filter definition: %s", filterDefinition.getFilterName() );
		context.getMetadataCollector().addFilterDefinition( filterDefinition );
	}

	@SuppressWarnings("unchecked")
	private static JdbcMapping resolveFilterParamType(Class<?> type, MetadataBuildingContext context) {
		if ( UserType.class.isAssignableFrom( type ) ) {
			return resolveUserType( (Class<UserType<?>>) type, context );
		}
		else if ( AttributeConverter.class.isAssignableFrom( type ) ) {
			return resolveAttributeConverter( (Class<AttributeConverter<?,?>>) type, context );
		}
		else if ( JavaType.class.isAssignableFrom( type ) ) {
			return resolveJavaType( (Class<JavaType<?>>) type, context );
		}
		else {
			return resolveBasicType( type, context );
		}
	}

	private static BasicType<Object> resolveBasicType(Class<?> type, MetadataBuildingContext context) {
		final TypeConfiguration typeConfiguration = context.getBootstrapContext().getTypeConfiguration();
		final JavaType<Object> jtd = typeConfiguration.getJavaTypeRegistry().findDescriptor( type );
		if ( jtd != null ) {
			final JdbcType jdbcType = jtd.getRecommendedJdbcType(
					new JdbcTypeIndicators() {
						@Override
						public TypeConfiguration getTypeConfiguration() {
							return typeConfiguration;
						}

						@Override
						public int getPreferredSqlTypeCodeForBoolean() {
							return context.getPreferredSqlTypeCodeForBoolean();
						}

						@Override
						public int getPreferredSqlTypeCodeForDuration() {
							return context.getPreferredSqlTypeCodeForDuration();
						}

						@Override
						public int getPreferredSqlTypeCodeForUuid() {
							return context.getPreferredSqlTypeCodeForUuid();
						}

						@Override
						public int getPreferredSqlTypeCodeForInstant() {
							return context.getPreferredSqlTypeCodeForInstant();
						}

						@Override
						public int getPreferredSqlTypeCodeForArray() {
							return context.getPreferredSqlTypeCodeForArray();
						}
					}
			);
			return typeConfiguration.getBasicTypeRegistry().resolve( jtd, jdbcType );
		}
		else {
			return null;
		}
	}

	private static JdbcMapping resolveUserType(Class<UserType<?>> type, MetadataBuildingContext context) {
		final StandardServiceRegistry serviceRegistry = context.getBootstrapContext().getServiceRegistry();
		final ManagedBeanRegistry beanRegistry = serviceRegistry.getService( ManagedBeanRegistry.class );
		final ManagedBean<UserType<?>> bean = beanRegistry.getBean( type );
		return new CustomType<>( bean.getBeanInstance(), context.getBootstrapContext().getTypeConfiguration() );
	}

	private static JdbcMapping resolveAttributeConverter(Class<AttributeConverter<?, ?>> type, MetadataBuildingContext context) {
		final StandardServiceRegistry serviceRegistry = context.getBootstrapContext().getServiceRegistry();
		final ManagedBeanRegistry beanRegistry = serviceRegistry.getService( ManagedBeanRegistry.class );
		final ManagedBean<AttributeConverter<?, ?>> bean = beanRegistry.getBean( type );

		final TypeConfiguration typeConfiguration = context.getBootstrapContext().getTypeConfiguration();
		final JavaTypeRegistry jtdRegistry = typeConfiguration.getJavaTypeRegistry();
		final JavaType<? extends AttributeConverter<?,?>> converterJtd = jtdRegistry.resolveDescriptor( bean.getBeanClass() );

		final ParameterizedType converterParameterizedType = GenericsHelper.extractParameterizedType( bean.getBeanClass() );
		final Class<?> domainJavaClass = GenericsHelper.extractClass( converterParameterizedType.getActualTypeArguments()[0] );
		final Class<?> relationalJavaClass = GenericsHelper.extractClass( converterParameterizedType.getActualTypeArguments()[1] );

		final JavaType<?> domainJtd = jtdRegistry.resolveDescriptor( domainJavaClass );
		final JavaType<?> relationalJtd = jtdRegistry.resolveDescriptor( relationalJavaClass );

		@SuppressWarnings({"rawtypes", "unchecked"})
		final JpaAttributeConverterImpl<?,?> valueConverter =
				new JpaAttributeConverterImpl( bean, converterJtd, domainJtd, relationalJtd );
		return new ConvertedBasicTypeImpl<>(
				ConverterDescriptor.TYPE_NAME_PREFIX
						+ valueConverter.getConverterJavaType().getJavaType().getTypeName(),
				String.format(
						"BasicType adapter for AttributeConverter<%s,%s>",
						domainJtd.getJavaType().getTypeName(),
						relationalJtd.getJavaType().getTypeName()
				),
				relationalJtd.getRecommendedJdbcType( typeConfiguration.getCurrentBaseSqlTypeIndicators() ),
				valueConverter
		);
	}

	private static JdbcMapping resolveJavaType(Class<JavaType<?>> type, MetadataBuildingContext context) {
		final TypeConfiguration typeConfiguration = context.getBootstrapContext().getTypeConfiguration();
		final JavaType<?> jtd = getJavaType( type, context, typeConfiguration );
		final JdbcType jdbcType = jtd.getRecommendedJdbcType( typeConfiguration.getCurrentBaseSqlTypeIndicators() );
		return typeConfiguration.getBasicTypeRegistry().resolve( jtd, jdbcType );
	}

	private static JavaType<?> getJavaType(
			Class<JavaType<?>> type,
			MetadataBuildingContext context,
			TypeConfiguration typeConfiguration) {
		final JavaType<?> registeredJtd = typeConfiguration.getJavaTypeRegistry().findDescriptor( type );
		if ( registeredJtd != null ) {
			return registeredJtd;
		}
		else {
			final StandardServiceRegistry serviceRegistry = context.getBootstrapContext().getServiceRegistry();
			final ManagedBeanRegistry beanRegistry = serviceRegistry.getService( ManagedBeanRegistry.class );
			return beanRegistry.getBean(type).getBeanInstance();
		}
	}

	public static void bindFetchProfilesForClass(XClass annotatedClass, MetadataBuildingContext context) {
		bindFetchProfiles( annotatedClass, context );
	}

	public static void bindFetchProfilesForPackage(ClassLoaderService cls, String packageName, MetadataBuildingContext context) {
		final Package pack = cls.packageForNameOrNull( packageName );
		if ( pack != null ) {
			final ReflectionManager reflectionManager = context.getBootstrapContext().getReflectionManager();
			bindFetchProfiles( reflectionManager.toXPackage( pack ), context );
		}
	}

	private static void bindFetchProfiles(XAnnotatedElement annotatedElement, MetadataBuildingContext context) {
		final FetchProfile fetchProfileAnnotation = annotatedElement.getAnnotation( FetchProfile.class );
		final FetchProfiles fetchProfileAnnotations = annotatedElement.getAnnotation( FetchProfiles.class );
		if ( fetchProfileAnnotation != null ) {
			bindFetchProfile( fetchProfileAnnotation, context );
		}
		if ( fetchProfileAnnotations != null ) {
			for ( FetchProfile profile : fetchProfileAnnotations.value() ) {
				bindFetchProfile( profile, context );
			}
		}
	}

	private static void bindFetchProfile(FetchProfile fetchProfileAnnotation, MetadataBuildingContext context) {
		for ( FetchProfile.FetchOverride fetch : fetchProfileAnnotation.fetchOverrides() ) {
			org.hibernate.annotations.FetchMode mode = fetch.mode();
			if ( !mode.equals( org.hibernate.annotations.FetchMode.JOIN ) ) {
				throw new MappingException( "Only FetchMode.JOIN is currently supported" );
			}
			context.getMetadataCollector().addSecondPass(
					new VerifyFetchProfileReferenceSecondPass( fetchProfileAnnotation.name(), fetch, context )
			);
		}
	}

	/**
	 * @param elements List of {@code ProperyData} instances
	 * @param propertyContainer Metadata about a class and its properties
	 *
	 * @return the number of id properties found while iterating the elements of {@code annotatedClass} using
	 *         the determined access strategy, {@code false} otherwise.
	 */
	static int addElementsOfClass(
			List<PropertyData> elements,
			PropertyContainer propertyContainer,
			MetadataBuildingContext context) {
		int idPropertyCounter = 0;
		for ( XProperty property : propertyContainer.propertyIterator() ) {
			idPropertyCounter += addProperty( propertyContainer, property, elements, context );
		}
		return idPropertyCounter;
	}

	private static int addProperty(
			PropertyContainer propertyContainer,
			XProperty property,
			List<PropertyData> inFlightPropertyDataList,
			MetadataBuildingContext context) {
		// see if inFlightPropertyDataList already contains a PropertyData for this name,
		// and if so, skip it..
		for ( PropertyData propertyData : inFlightPropertyDataList ) {
			if ( propertyData.getPropertyName().equals( property.getName() ) ) {
				checkIdProperty( property, propertyData );
				// EARLY EXIT!!!
				return 0;
			}
		}

		final XClass declaringClass = propertyContainer.getDeclaringClass();
		final XClass entity = propertyContainer.getEntityAtStake();
		int idPropertyCounter = 0;
		final PropertyData propertyAnnotatedElement = new PropertyInferredData(
				declaringClass,
				property,
				propertyContainer.getClassLevelAccessType().getType(),
				context.getBootstrapContext().getReflectionManager()
		);

		// put element annotated by @Id in front, since it has to be parsed
		// before any association by Hibernate
		final XAnnotatedElement element = propertyAnnotatedElement.getProperty();
		if ( hasIdAnnotation( element ) ) {
			inFlightPropertyDataList.add( 0, propertyAnnotatedElement );
			handleIdProperty( propertyContainer, context, declaringClass, entity, element );
			if ( hasToOneAnnotation( element ) ) {
				context.getMetadataCollector().addToOneAndIdProperty( entity, propertyAnnotatedElement );
			}
			idPropertyCounter++;
		}
		else {
			inFlightPropertyDataList.add( propertyAnnotatedElement );
		}
		if ( element.isAnnotationPresent( MapsId.class ) ) {
			context.getMetadataCollector().addPropertyAnnotatedWithMapsId( entity, propertyAnnotatedElement );
		}

		return idPropertyCounter;
	}

	private static void checkIdProperty(XProperty property, PropertyData propertyData) {
		final Id incomingIdProperty = property.getAnnotation( Id.class );
		final Id existingIdProperty = propertyData.getProperty().getAnnotation( Id.class );
		if ( incomingIdProperty != null && existingIdProperty == null ) {
			throw new MappingException(
					String.format(
							"You cannot override the [%s] non-identifier property from the [%s] base class or @MappedSuperclass and make it an identifier in the [%s] subclass",
							propertyData.getProperty().getName(),
							propertyData.getProperty().getDeclaringClass().getName(),
							property.getDeclaringClass().getName()
					)
			);
		}
	}

	private static void handleIdProperty(
			PropertyContainer propertyContainer,
			MetadataBuildingContext context,
			XClass declaringClass,
			XClass entity,
			XAnnotatedElement element) {
		// The property must be put in hibernate.properties as it's a system wide property. Fixable?
		//TODO support true/false/default on the property instead of present / not present
		//TODO is @Column mandatory?
		//TODO add method support
		if ( context.getBuildingOptions().isSpecjProprietarySyntaxEnabled() ) {
			if ( element.isAnnotationPresent( Id.class ) && element.isAnnotationPresent( Column.class ) ) {
				final String columnName = element.getAnnotation( Column.class ).name();
				for ( XProperty property : declaringClass.getDeclaredProperties( AccessType.FIELD.getType() ) ) {
					if ( !property.isAnnotationPresent( MapsId.class ) && isJoinColumnPresent( columnName, property ) ) {
						//create a PropertyData for the specJ property holding the mapping
						context.getMetadataCollector().addPropertyAnnotatedWithMapsIdSpecj(
								entity,
								new PropertyInferredData(
										declaringClass,
										//same dec
										property,
										// the actual @XToOne property
										propertyContainer.getClassLevelAccessType().getType(),
										//TODO we should get the right accessor but the same as id would do
										context.getBootstrapContext().getReflectionManager()
								),
								element.toString()
						);
					}
				}
			}
		}
	}

	private static boolean isJoinColumnPresent(String columnName, XProperty property) {
		//The detection of a configured individual JoinColumn differs between Annotation
		//and XML configuration processing.
		if ( property.isAnnotationPresent( JoinColumn.class )
				&& property.getAnnotation( JoinColumn.class ).name().equals(columnName) ) {
			return true;
		}
		else if ( property.isAnnotationPresent( JoinColumns.class ) ) {
			for ( JoinColumn columnAnnotation : property.getAnnotation( JoinColumns.class ).value() ) {
				if ( columnName.equals( columnAnnotation.name() ) ) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean hasIdAnnotation(XAnnotatedElement element) {
		return element.isAnnotationPresent(Id.class)
			|| element.isAnnotationPresent(EmbeddedId.class);
	}

	/**
	 * Process annotation of a particular property or field.
	 */
	public static void processElementAnnotations(
			PropertyHolder propertyHolder,
			Nullability nullability,
			PropertyData inferredData,
			Map<String, IdentifierGeneratorDefinition> classGenerators,
			EntityBinder entityBinder,
			boolean isIdentifierMapper,
			boolean isComponentEmbedded,
			boolean inSecondPass,
			MetadataBuildingContext context,
			Map<XClass, InheritanceState> inheritanceStatePerClass) throws MappingException {

		if ( alreadyProcessedBySuper( propertyHolder, inferredData, entityBinder ) ) {
			LOG.debugf(
					"Skipping attribute [%s : %s] as it was already processed as part of super hierarchy",
					inferredData.getClassOrElementName(),
					inferredData.getPropertyName()
			);
		}
		else {
			// inSecondPass can only be used to apply right away the second pass of a composite-element
			// Because it's a value type, there is no bidirectional association, hence second pass
			// ordering does not matter

			if ( LOG.isTraceEnabled() ) {
				LOG.tracev(
						"Processing annotations of {0}.{1}" ,
						propertyHolder.getEntityName(),
						inferredData.getPropertyName()
				);
			}

			final XProperty property = inferredData.getProperty();
			if ( property.isAnnotationPresent( Parent.class ) ) {
				handleParentProperty( propertyHolder, inferredData, property );
			}
			else {
				//prepare PropertyBinder
				buildProperty(
						propertyHolder,
						nullability,
						inferredData,
						classGenerators,
						entityBinder,
						isIdentifierMapper,
						isComponentEmbedded,
						inSecondPass,
						context,
						inheritanceStatePerClass,
						property,
						inferredData.getClassOrElement()
				);
			}
		}
	}

	private static boolean alreadyProcessedBySuper(
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			EntityBinder entityBinder) {
		return !propertyHolder.isComponent()
			&& entityBinder.isPropertyDefinedInSuperHierarchy( inferredData.getPropertyName() );
	}

	private static void handleParentProperty(PropertyHolder propertyHolder, PropertyData inferredData, XProperty property) {
		if ( propertyHolder.isComponent() ) {
			propertyHolder.setParentProperty( property.getName() );
		}
		else {
			throw new AnnotationException(
					"Property '" + getPath( propertyHolder, inferredData )
							+ "' is annotated '@Parent' but is not a member of an embeddable class"

			);
		}
	}

	private static void buildProperty(
			PropertyHolder propertyHolder,
			Nullability nullability,
			PropertyData inferredData,
			Map<String, IdentifierGeneratorDefinition> classGenerators,
			EntityBinder entityBinder,
			boolean isIdentifierMapper,
			boolean isComponentEmbedded,
			boolean inSecondPass,
			MetadataBuildingContext context,
			Map<XClass, InheritanceState> inheritanceStatePerClass,
			XProperty property,
			XClass returnedClass) {

		final ColumnsBuilder columnsBuilder = new ColumnsBuilder(
				propertyHolder,
				nullability,
				property,
				inferredData,
				entityBinder,
				context
		).extractMetadata();

		final PropertyBinder propertyBinder = new PropertyBinder();
		propertyBinder.setName( inferredData.getPropertyName() );
		propertyBinder.setReturnedClassName( inferredData.getTypeName() );
		propertyBinder.setAccessType( inferredData.getDefaultAccess() );
		propertyBinder.setHolder( propertyHolder );
		propertyBinder.setProperty( property );
		propertyBinder.setReturnedClass( inferredData.getPropertyClass() );
		propertyBinder.setBuildingContext( context );
		if ( isIdentifierMapper ) {
			propertyBinder.setInsertable( false );
			propertyBinder.setUpdatable( false );
		}
		propertyBinder.setDeclaringClass( inferredData.getDeclaringClass() );
		propertyBinder.setEntityBinder( entityBinder );
		propertyBinder.setInheritanceStatePerClass( inheritanceStatePerClass );
		propertyBinder.setId( !entityBinder.isIgnoreIdAnnotations() && hasIdAnnotation( property ) );

		final LazyGroup lazyGroupAnnotation = property.getAnnotation( LazyGroup.class );
		if ( lazyGroupAnnotation != null ) {
			propertyBinder.setLazyGroup( lazyGroupAnnotation.value() );
		}

		final AnnotatedJoinColumns joinColumns = columnsBuilder.getJoinColumns();
		final AnnotatedColumns columns = bindProperty(
				propertyHolder,
				nullability,
				inferredData,
				classGenerators,
				entityBinder,
				isIdentifierMapper,
				isComponentEmbedded,
				inSecondPass,
				context,
				inheritanceStatePerClass,
				property,
				returnedClass,
				columnsBuilder,
				propertyBinder
		);
		addIndexes( inSecondPass, property, columns, joinColumns );
		addNaturalIds( inSecondPass, property, columns, joinColumns );
	}

	private static AnnotatedColumns bindProperty(
			PropertyHolder propertyHolder,
			Nullability nullability,
			PropertyData inferredData,
			Map<String, IdentifierGeneratorDefinition> classGenerators,
			EntityBinder entityBinder,
			boolean isIdentifierMapper,
			boolean isComponentEmbedded,
			boolean inSecondPass,
			MetadataBuildingContext context,
			Map<XClass, InheritanceState> inheritanceStatePerClass,
			XProperty property,
			XClass returnedClass,
			ColumnsBuilder columnsBuilder,
			PropertyBinder propertyBinder) {
		if ( property.isAnnotationPresent( Version.class ) ) {
			bindVersionProperty(
					propertyHolder,
					inferredData,
					isIdentifierMapper,
					context,
					inheritanceStatePerClass,
					property,
					columnsBuilder.getColumns(),
					propertyBinder
			);
		}
		else {
			if ( property.isAnnotationPresent( ManyToOne.class ) ) {
				ToOneBinder.bindManyToOne(
						propertyHolder,
						inferredData,
						isIdentifierMapper,
						inSecondPass,
						context,
						property,
						columnsBuilder.getJoinColumns(),
						propertyBinder,
						isForcePersist( property )
				);
			}
			else if ( property.isAnnotationPresent( OneToOne.class ) ) {
				ToOneBinder.bindOneToOne(
						propertyHolder,
						inferredData,
						isIdentifierMapper,
						inSecondPass,
						context,
						property,
						columnsBuilder.getJoinColumns(),
						propertyBinder,
						isForcePersist( property )
				);
			}
			else if ( property.isAnnotationPresent( org.hibernate.annotations.Any.class ) ) {
				bindAny(
						propertyHolder,
						nullability,
						inferredData,
						entityBinder,
						isIdentifierMapper,
						context,
						property,
						columnsBuilder.getJoinColumns(),
						isForcePersist( property )
				);
			}
			else if ( property.isAnnotationPresent( OneToMany.class )
					|| property.isAnnotationPresent( ManyToMany.class )
					|| property.isAnnotationPresent( ElementCollection.class )
					|| property.isAnnotationPresent( ManyToAny.class ) ) {
				CollectionBinder.bindCollection(
						propertyHolder,
						nullability,
						inferredData,
						classGenerators,
						entityBinder,
						isIdentifierMapper,
						context,
						inheritanceStatePerClass,
						property,
						columnsBuilder.getJoinColumns()
				);
			}
			//Either a regular property or a basic @Id or @EmbeddedId while not ignoring id annotations
			else if ( !propertyBinder.isId() || !entityBinder.isIgnoreIdAnnotations() ) {
				// returns overridden columns
				return bindBasic(
						propertyHolder,
						nullability,
						inferredData,
						classGenerators,
						entityBinder,
						isIdentifierMapper,
						isComponentEmbedded,
						context,
						inheritanceStatePerClass,
						property,
						columnsBuilder,
						columnsBuilder.getColumns(),
						returnedClass,
						propertyBinder
				);
			}
		}
		return columnsBuilder.getColumns();
	}

	private static boolean isForcePersist(XProperty property) {
		return property.isAnnotationPresent(MapsId.class)
			|| property.isAnnotationPresent(Id.class);
	}

	private static void bindVersionProperty(
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			boolean isIdentifierMapper,
			MetadataBuildingContext context,
			Map<XClass, InheritanceState> inheritanceStatePerClass,
			XProperty annotatedProperty,
			AnnotatedColumns columns,
			PropertyBinder propertyBinder) {
		checkVersionProperty( propertyHolder, isIdentifierMapper );
		if ( LOG.isTraceEnabled() ) {
			LOG.tracev( "{0} is a version property", inferredData.getPropertyName() );
		}
		final RootClass rootClass = (RootClass) propertyHolder.getPersistentClass();
		propertyBinder.setColumns( columns );
		final Property property = propertyBinder.makePropertyValueAndBind();
		propertyBinder.getBasicValueBinder().setVersion( true );
		rootClass.setVersion( property );

		//If version is on a mapped superclass, update the mapping
		final org.hibernate.mapping.MappedSuperclass superclass = getMappedSuperclassOrNull(
				inferredData.getDeclaringClass(),
				inheritanceStatePerClass,
				context
		);
		if ( superclass != null ) {
			superclass.setDeclaredVersion( property );
		}
		else {
			//we know the property is on the actual entity
			rootClass.setDeclaredVersion( property );
		}

		( (SimpleValue) property.getValue() ).setNullValue( "undefined" );
		rootClass.setOptimisticLockStyle( OptimisticLockStyle.VERSION );
		if ( LOG.isTraceEnabled() ) {
			LOG.tracev( "Version name: {0}, unsavedValue: {1}", rootClass.getVersion().getName(),
					( (SimpleValue) rootClass.getVersion().getValue() ).getNullValue() );
		}
	}

	private static void checkVersionProperty(PropertyHolder propertyHolder, boolean isIdentifierMapper) {
		if (isIdentifierMapper) {
			throw new AnnotationException( "Class '" + propertyHolder.getEntityName()
					+ "' is annotated '@IdClass' and may not have a property annotated '@Version'"
			);
		}
		if ( !( propertyHolder.getPersistentClass() instanceof RootClass ) ) {
			throw new AnnotationException( "Entity '" + propertyHolder.getEntityName()
					+ "' is a subclass in an entity class hierarchy and may not have a property annotated '@Version'" );
		}
		if ( !propertyHolder.isEntity() ) {
			throw new AnnotationException( "Embedded class '" + propertyHolder.getEntityName()
					+ "' may not have a property annotated '@Version'" );
		}
	}

	private static AnnotatedColumns bindBasic(
			PropertyHolder propertyHolder,
			Nullability nullability,
			PropertyData inferredData,
			Map<String, IdentifierGeneratorDefinition> classGenerators,
			EntityBinder entityBinder,
			boolean isIdentifierMapper,
			boolean isComponentEmbedded,
			MetadataBuildingContext context,
			Map<XClass, InheritanceState> inheritanceStatePerClass,
			XProperty property,
			ColumnsBuilder columnsBuilder,
			AnnotatedColumns columns,
			XClass returnedClass,
			PropertyBinder propertyBinder) {

		// overrides from @MapsId or @IdClass if needed
		final boolean isComposite;
		final boolean isOverridden;
		final AnnotatedColumns actualColumns;
		if ( propertyBinder.isId() || propertyHolder.isOrWithinEmbeddedId() || propertyHolder.isInIdClass() ) {
			// the associated entity could be using an @IdClass making the overridden property a component
			final PropertyData overridingProperty = getPropertyOverriddenByMapperOrMapsId(
					propertyBinder.isId(),
					propertyHolder,
					property.getName(),
					context
			);
			if ( overridingProperty != null ) {
				isOverridden = true;
				final InheritanceState state = inheritanceStatePerClass.get( overridingProperty.getClassOrElement() );
				isComposite = state != null ? state.hasIdClassOrEmbeddedId() : isEmbedded( property, returnedClass );
				//Get the new column
				actualColumns = columnsBuilder.overrideColumnFromMapperOrMapsIdProperty( propertyBinder.isId() );
			}
			else {
				isOverridden = false;
				isComposite = isEmbedded( property, returnedClass );
				actualColumns = columns;
			}
		}
		else {
			isOverridden = false;
			isComposite = isEmbedded( property, returnedClass );
			actualColumns = columns;
		}

		final Class<? extends CompositeUserType<?>> compositeUserType = resolveCompositeUserType( inferredData, context );

		if ( isComposite || compositeUserType != null ) {
			propertyBinder = createCompositeBinder(
					propertyHolder,
					inferredData,
					entityBinder,
					isIdentifierMapper,
					isComponentEmbedded,
					context,
					inheritanceStatePerClass,
					property,
					actualColumns,
					returnedClass,
					propertyBinder,
					isOverridden,
					compositeUserType
			);
		}
		else if ( property.isCollection() && property.getElementClass() != null
				&& isEmbedded( property, property.getElementClass() ) ) {
			// This is a special kind of basic aggregate component array type
			// todo: see HHH-15830
			throw new AnnotationException(
					"Property '" + BinderHelper.getPath( propertyHolder, inferredData )
							+ "' is mapped as basic aggregate component array, but this is not yet supported."
			);
		}
		else {
			createBasicBinder(
					propertyHolder,
					inferredData,
					nullability,
					context,
					property,
					actualColumns,
					propertyBinder,
					isOverridden
			);
		}
		if ( isOverridden ) {
			handleGeneratorsForOverriddenId(
					propertyHolder,
					classGenerators,
					context,
					property,
					propertyBinder
			);
		}
		else if ( propertyBinder.isId() ) {
			//components and regular basic types create SimpleValue objects
			processId(
					propertyHolder,
					inferredData,
					(SimpleValue) propertyBinder.getValue(),
					classGenerators,
					isIdentifierMapper,
					context
			);
		}
		return actualColumns;
	}

	private static void handleGeneratorsForOverriddenId(
			PropertyHolder propertyHolder,
			Map<String, IdentifierGeneratorDefinition> classGenerators,
			MetadataBuildingContext context,
			XProperty property,
			PropertyBinder propertyBinder) {
		final PropertyData mapsIdProperty = getPropertyOverriddenByMapperOrMapsId(
				propertyBinder.isId(),
				propertyHolder,
				property.getName(),
				context
		);
		final IdentifierGeneratorDefinition foreignGenerator = createForeignGenerator( mapsIdProperty );
		if ( isGlobalGeneratorNameGlobal( context ) ) {
			context.getMetadataCollector().addSecondPass( new IdGeneratorResolverSecondPass(
					(SimpleValue) propertyBinder.getValue(),
					property,
					foreignGenerator.getStrategy(),
					foreignGenerator.getName(),
					context,
					foreignGenerator
			) );
		}
		else {
			final Map<String, IdentifierGeneratorDefinition> generators = new HashMap<>( classGenerators );
			generators.put( foreignGenerator.getName(), foreignGenerator );
			makeIdGenerator(
					(SimpleValue) propertyBinder.getValue(),
					property,
					foreignGenerator.getStrategy(),
					foreignGenerator.getName(),
					context,
					generators
			);
		}
	}

	private static IdentifierGeneratorDefinition createForeignGenerator(PropertyData mapsIdProperty) {
		final IdentifierGeneratorDefinition.Builder foreignGeneratorBuilder =
				new IdentifierGeneratorDefinition.Builder();
		foreignGeneratorBuilder.setName( "Hibernate-local--foreign generator" );
		foreignGeneratorBuilder.setStrategy( "foreign" );
		foreignGeneratorBuilder.addParam( "property", mapsIdProperty.getPropertyName() );
		return foreignGeneratorBuilder.build();
	}

	private static void createBasicBinder(
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			Nullability nullability,
			MetadataBuildingContext context,
			XProperty property,
			AnnotatedColumns columns,
			PropertyBinder propertyBinder,
			boolean isOverridden) {
		//provide the basic property mapping
		final boolean optional;
		final boolean lazy;
		if ( property.isAnnotationPresent( Basic.class ) ) {
			final Basic basic = property.getAnnotation( Basic.class );
			optional = basic.optional();
			lazy = basic.fetch() == FetchType.LAZY;
		}
		else {
			optional = true;
			lazy = false;
		}

		//implicit type will check basic types and Serializable classes
		if ( propertyBinder.isId() || !optional && nullability != Nullability.FORCED_NULL ) {
			//force columns to not null
			for ( AnnotatedColumn column : columns.getColumns() ) {
				if ( propertyBinder.isId() && column.isFormula() ) {
					throw new CannotForceNonNullableException( "Identifier property '"
							+ getPath( propertyHolder, inferredData ) + "' cannot map to a '@Formula'" );
				}
				column.forceNotNull();
			}
		}

		propertyBinder.setLazy( lazy );
		propertyBinder.setColumns( columns );
		if ( isOverridden ) {
			final PropertyData mapsIdProperty = getPropertyOverriddenByMapperOrMapsId(
					propertyBinder.isId(),
					propertyHolder,
					property.getName(),
					context
			);
			propertyBinder.setReferencedEntityName( mapsIdProperty.getClassOrElementName() );
		}

		propertyBinder.makePropertyValueAndBind();
	}

	private static PropertyBinder createCompositeBinder(
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			EntityBinder entityBinder,
			boolean isIdentifierMapper,
			boolean isComponentEmbedded,
			MetadataBuildingContext context,
			Map<XClass, InheritanceState> inheritanceStatePerClass,
			XProperty property,
			AnnotatedColumns columns,
			XClass returnedClass,
			PropertyBinder propertyBinder,
			boolean isOverridden,
			Class<? extends CompositeUserType<?>> compositeUserType) {
		final String referencedEntityName;
		final String propertyName;
		final AnnotatedJoinColumns actualColumns;
		if ( isOverridden ) {
			// careful: not always a @MapsId property, sometimes it's from an @IdClass
			final PropertyData mapsIdProperty = getPropertyOverriddenByMapperOrMapsId(
					propertyBinder.isId(),
					propertyHolder,
					property.getName(),
					context
			);
			referencedEntityName = mapsIdProperty.getClassOrElementName();
			propertyName = mapsIdProperty.getPropertyName();
			final AnnotatedJoinColumns parent = new AnnotatedJoinColumns();
			parent.setBuildingContext( context );
			parent.setPropertyHolder( propertyHolder );
			parent.setPropertyName( getRelativePath( propertyHolder, propertyName ) );
			//TODO: resetting the parent here looks like a dangerous thing to do
			//      should we be cloning them first (the legacy code did not)
			for ( AnnotatedColumn column : columns.getColumns() ) {
				column.setParent( parent );
			}
			actualColumns = parent;
		}
		else {
			referencedEntityName = null;
			propertyName = null;
			actualColumns = null;
		}

		return bindComponent(
				inferredData,
				propertyHolder,
				entityBinder.getPropertyAccessor( property ),
				entityBinder,
				isIdentifierMapper,
				context,
				isComponentEmbedded,
				propertyBinder.isId(),
				inheritanceStatePerClass,
				referencedEntityName,
				propertyName,
				determineCustomInstantiator( property, returnedClass, context ),
				compositeUserType,
				actualColumns,
				columns
		);
	}

	private static boolean isEmbedded(XProperty property, XClass returnedClass) {
		return property.isAnnotationPresent( Embedded.class )
			|| property.isAnnotationPresent( EmbeddedId.class )
			|| returnedClass.isAnnotationPresent( Embeddable.class );
	}

	private static void bindAny(
			PropertyHolder propertyHolder,
			Nullability nullability,
			PropertyData inferredData,
			EntityBinder entityBinder,
			boolean isIdentifierMapper,
			MetadataBuildingContext context,
			XProperty property,
			AnnotatedJoinColumns joinColumns,
			boolean forcePersist) {

		//check validity
		if (  property.isAnnotationPresent( Columns.class ) ) {
			throw new AnnotationException(
					String.format(
							Locale.ROOT,
							"Property '%s' is annotated '@Any' and may not have a '@Columns' annotation "
									+ "(a single '@Column' or '@Formula' must be used to map the discriminator, and '@JoinColumn's must be used to map the foreign key) ",
							getPath( propertyHolder, inferredData )
					)
			);
		}

		final Cascade hibernateCascade = property.getAnnotation( Cascade.class );
		final OnDelete onDeleteAnn = property.getAnnotation( OnDelete.class );
		final JoinTable assocTable = propertyHolder.getJoinTable(property);
		if ( assocTable != null ) {
			final Join join = propertyHolder.addJoin( assocTable, false );
			for ( AnnotatedJoinColumn joinColumn : joinColumns.getJoinColumns() ) {
				joinColumn.setExplicitTableName( join.getTable().getName() );
			}
		}
		bindAny(
				BinderHelper.getCascadeStrategy( null, hibernateCascade, false, forcePersist ),
				//@Any has no cascade attribute
				joinColumns,
				onDeleteAnn == null ? null : onDeleteAnn.action(),
				nullability,
				propertyHolder,
				inferredData,
				entityBinder,
				isIdentifierMapper,
				context
		);
	}

	private static void addIndexes(
			boolean inSecondPass,
			XProperty property,
			AnnotatedColumns columns,
			AnnotatedJoinColumns joinColumns) {
		//process indexes after everything: in second pass, many to one has to be done before indexes
		final Index index = property.getAnnotation( Index.class );
		if ( index != null ) {
			if ( joinColumns != null ) {
				for ( AnnotatedColumn column : joinColumns.getColumns() ) {
					column.addIndex( index, inSecondPass);
				}
			}
			else {
				if ( columns != null ) {
					for ( AnnotatedColumn column : columns.getColumns() ) {
						column.addIndex( index, inSecondPass );
					}
				}
			}
		}
	}

	private static void addNaturalIds(
			boolean inSecondPass,
			XProperty property,
			AnnotatedColumns columns,
			AnnotatedJoinColumns joinColumns) {
		// Natural ID columns must reside in one single UniqueKey within the Table.
		// For now, simply ensure consistent naming.
		// TODO: AFAIK, there really isn't a reason for these UKs to be created
		// on the SecondPass. This whole area should go away...
		final NaturalId naturalId = property.getAnnotation( NaturalId.class );
		if ( naturalId != null ) {
			if ( joinColumns != null ) {
				final String keyName = "UK_" + hashedName( joinColumns.getTable().getName() + "_NaturalID" );
				for ( AnnotatedColumn column : joinColumns.getColumns() ) {
					column.addUniqueKey( keyName, inSecondPass );
				}
			}
			else {
				final String keyName = "UK_" + hashedName( columns.getTable().getName() + "_NaturalID" );
				for ( AnnotatedColumn column : columns.getColumns() ) {
					column.addUniqueKey( keyName, inSecondPass );
				}
			}
		}
	}

	private static Class<? extends EmbeddableInstantiator> determineCustomInstantiator(
			XProperty property,
			XClass returnedClass,
			MetadataBuildingContext context) {
		if ( property.isAnnotationPresent( EmbeddedId.class ) ) {
			// we don't allow custom instantiators for composite ids
			return null;
		}

		final org.hibernate.annotations.EmbeddableInstantiator propertyAnnotation =
				property.getAnnotation( org.hibernate.annotations.EmbeddableInstantiator.class );
		if ( propertyAnnotation != null ) {
			return propertyAnnotation.value();
		}

		final org.hibernate.annotations.EmbeddableInstantiator classAnnotation =
				returnedClass.getAnnotation( org.hibernate.annotations.EmbeddableInstantiator.class );
		if ( classAnnotation != null ) {
			return classAnnotation.value();
		}

		final Class<?> embeddableClass = context.getBootstrapContext().getReflectionManager().toClass( returnedClass );
		if ( embeddableClass != null ) {
			return context.getMetadataCollector().findRegisteredEmbeddableInstantiator( embeddableClass );
		}

		return null;
	}

	private static Class<? extends CompositeUserType<?>> resolveCompositeUserType(
			PropertyData inferredData,
			MetadataBuildingContext context) {
		final XProperty property = inferredData.getProperty();
		final XClass returnedClass = inferredData.getClassOrElement();

		if ( property != null ) {
			final CompositeType compositeType = property.getAnnotation( CompositeType.class );
			if ( compositeType != null ) {
				return compositeType.value();
			}
			final Class<? extends CompositeUserType<?>> compositeUserType =
					resolveTimeZoneStorageCompositeUserType( property, returnedClass, context );
			if ( compositeUserType != null ) {
				return compositeUserType;
			}
		}

		if ( returnedClass != null ) {
			final Class<?> embeddableClass = context.getBootstrapContext()
					.getReflectionManager()
					.toClass( returnedClass );
			if ( embeddableClass != null ) {
				return context.getMetadataCollector().findRegisteredCompositeUserType( embeddableClass );
			}
		}

		return null;
	}

	private static boolean isGlobalGeneratorNameGlobal(MetadataBuildingContext context) {
		return context.getBootstrapContext().getJpaCompliance().isGlobalGeneratorScopeEnabled();
	}

	private static void processId(
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			SimpleValue idValue,
			Map<String, IdentifierGeneratorDefinition> classGenerators,
			boolean isIdentifierMapper,
			MetadataBuildingContext context) {
		if ( isIdentifierMapper ) {
			throw new AnnotationException( "Property '"+ getPath( propertyHolder, inferredData )
					+ "' belongs to an '@IdClass' and may not be annotated '@Id' or '@EmbeddedId'" );
		}
		final XProperty idProperty = inferredData.getProperty();
		final Annotation idGeneratorAnnotation = findContainingAnnotation( idProperty, IdGeneratorType.class );
		final Annotation generatorAnnotation = findContainingAnnotation( idProperty, ValueGenerationType.class );
		//TODO: validate that we don't have too many generator annotations and throw
		if ( idGeneratorAnnotation != null ) {
			idValue.setCustomIdGeneratorCreator( identifierGeneratorCreator( idProperty, idGeneratorAnnotation ) );
		}
		else if ( generatorAnnotation != null ) {
//			idValue.setCustomGeneratorCreator( generatorCreator( idProperty, generatorAnnotation ) );
			throw new AnnotationException( "Property '"+ getPath( propertyHolder, inferredData )
					+ "' is annotated '" + generatorAnnotation.annotationType() + "' which is not an '@IdGeneratorType'" );
		}
		else {
			final XClass entityClass = inferredData.getClassOrElement();
			createIdGenerator( idValue, classGenerators, context, entityClass, idProperty );
			if ( LOG.isTraceEnabled() ) {
				LOG.tracev(
						"Bind {0} on {1}",
						isCompositeId( entityClass, idProperty ) ? "@EmbeddedId" : "@Id",
						inferredData.getPropertyName()
				);
			}
		}
	}

	private static void createIdGenerator(
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

	private static boolean isCompositeId(XClass entityClass, XProperty idProperty) {
		return entityClass.isAnnotationPresent(Embeddable.class)
				|| idProperty.isAnnotationPresent(EmbeddedId.class);
	}

	private static String generatorType(
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

	public static String generatorType(GeneratedValue generatedValue, final XClass javaClass, MetadataBuildingContext context) {
		return context.getBuildingOptions().getIdGenerationTypeInterpreter()
				.determineGeneratorName(
						generatedValue.strategy(),
						new IdGeneratorStrategyInterpreter.GeneratorNameDeterminationContext() {
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

	private static PropertyBinder bindComponent(
			PropertyData inferredData,
			PropertyHolder propertyHolder,
			AccessType propertyAccessor,
			EntityBinder entityBinder,
			boolean isIdentifierMapper,
			MetadataBuildingContext context,
			boolean isComponentEmbedded,
			boolean isId, //is an identifier
			Map<XClass, InheritanceState> inheritanceStatePerClass,
			String referencedEntityName, //is a component who is overridden by a @MapsId
			String propertyName,
			Class<? extends EmbeddableInstantiator> customInstantiatorImpl,
			Class<? extends CompositeUserType<?>> compositeUserTypeClass,
			AnnotatedJoinColumns columns,
			AnnotatedColumns annotatedColumns) {
		final Component component;
		if ( referencedEntityName != null ) {
			component = createComponent(
					propertyHolder,
					inferredData,
					isComponentEmbedded,
					isIdentifierMapper,
					customInstantiatorImpl,
					context
			);
			context.getMetadataCollector().addSecondPass( new CopyIdentifierComponentSecondPass(
					component,
					referencedEntityName,
					propertyName,
					columns,
					context
			) );
		}
		else {
			component = fillComponent(
					propertyHolder,
					inferredData,
					propertyAccessor,
					!isId,
					entityBinder,
					isComponentEmbedded,
					isIdentifierMapper,
					false,
					customInstantiatorImpl,
					compositeUserTypeClass,
					annotatedColumns,
					context,
					inheritanceStatePerClass
			);
		}
		if ( isId ) {
			component.setKey( true );
			checkEmbeddedId( inferredData, propertyHolder, referencedEntityName, component );
		}
		final PropertyBinder binder = new PropertyBinder();
		binder.setDeclaringClass( inferredData.getDeclaringClass() );
		binder.setName( inferredData.getPropertyName() );
		binder.setValue( component );
		binder.setProperty( inferredData.getProperty() );
		binder.setAccessType( inferredData.getDefaultAccess() );
		binder.setEmbedded( isComponentEmbedded );
		binder.setHolder( propertyHolder );
		binder.setId( isId );
		binder.setEntityBinder( entityBinder );
		binder.setInheritanceStatePerClass( inheritanceStatePerClass );
		binder.setBuildingContext( context );
		binder.makePropertyAndBind();
		return binder;
	}

	private static void checkEmbeddedId(
			PropertyData inferredData,
			PropertyHolder propertyHolder,
			String referencedEntityName,
			Component component) {
		if ( propertyHolder.getPersistentClass().getIdentifier() != null ) {
			throw new AnnotationException(
					"Embeddable class '" + component.getComponentClassName()
							+ "' may not have a property annotated '@Id' since it is used by '"
							+ getPath(propertyHolder, inferredData)
							+ "' as an '@EmbeddedId'"
			);
		}
		if ( referencedEntityName == null && component.getPropertySpan() == 0 ) {
			throw new AnnotationException(
					"Embeddable class '" + component.getComponentClassName()
							+ "' may not be used as an '@EmbeddedId' by '"
							+ getPath(propertyHolder, inferredData)
							+ "' because it has no properties"
			);
		}
	}

	public static Component fillComponent(
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			AccessType propertyAccessor,
			boolean isNullable,
			EntityBinder entityBinder,
			boolean isComponentEmbedded,
			boolean isIdentifierMapper,
			boolean inSecondPass,
			Class<? extends EmbeddableInstantiator> customInstantiatorImpl,
			Class<? extends CompositeUserType<?>> compositeUserTypeClass,
			AnnotatedColumns columns,
			MetadataBuildingContext context,
			Map<XClass, InheritanceState> inheritanceStatePerClass) {
		return fillComponent(
				propertyHolder,
				inferredData,
				null,
				propertyAccessor,
				isNullable,
				entityBinder,
				isComponentEmbedded,
				isIdentifierMapper,
				inSecondPass,
				customInstantiatorImpl,
				compositeUserTypeClass,
				columns,
				context,
				inheritanceStatePerClass
		);
	}

	public static Component fillComponent(
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			PropertyData baseInferredData, //base inferred data correspond to the entity reproducing inferredData's properties (ie IdClass)
			AccessType propertyAccessor,
			boolean isNullable,
			EntityBinder entityBinder,
			boolean isComponentEmbedded,
			boolean isIdentifierMapper,
			boolean inSecondPass,
			Class<? extends EmbeddableInstantiator> customInstantiatorImpl,
			Class<? extends CompositeUserType<?>> compositeUserTypeClass,
			AnnotatedColumns columns,
			MetadataBuildingContext context,
			Map<XClass, InheritanceState> inheritanceStatePerClass) {
		// inSecondPass can only be used to apply right away the second pass of a composite-element
		// Because it's a value type, there is no bidirectional association, hence second pass
		// ordering does not matter
		final Component component = createComponent(
				propertyHolder,
				inferredData,
				isComponentEmbedded,
				isIdentifierMapper,
				customInstantiatorImpl,
				context
		);

		final String subpath = getPath( propertyHolder, inferredData );
		LOG.tracev( "Binding component with path: {0}", subpath );
		final PropertyHolder subholder = buildPropertyHolder(
				component,
				subpath,
				inferredData,
				propertyHolder,
				context
		);

		// propertyHolder here is the owner of the component property.
		// Tell it we are about to start the component...
		propertyHolder.startingProperty( inferredData.getProperty() );

		final CompositeUserType<?> compositeUserType;
		final XClass returnedClassOrElement;
		if ( compositeUserTypeClass == null ) {
			compositeUserType = null;
			returnedClassOrElement = inferredData.getClassOrElement();
		}
		else {
			compositeUserType = compositeUserType( compositeUserTypeClass, context );
			component.setTypeName( compositeUserTypeClass.getName() );
			returnedClassOrElement = context.getBootstrapContext().getReflectionManager()
					.toXClass( compositeUserType.embeddable() );
		}

		final XClass annotatedClass = inferredData.getPropertyClass();
		final List<PropertyData> classElements =
				collectClassElements( propertyAccessor, context, returnedClassOrElement, annotatedClass );
		final List<PropertyData> baseClassElements =
				collectBaseClassElements( baseInferredData, propertyAccessor, context, annotatedClass );
		if ( baseClassElements != null
				//useful to avoid breaking pre JPA 2 mappings
				&& !hasAnnotationsOnIdClass( annotatedClass ) ) {
			processIdClassElememts( propertyHolder, baseInferredData, classElements, baseClassElements );
		}
		for ( PropertyData propertyAnnotatedElement : classElements ) {
			processElementAnnotations(
					subholder,
					isNullable ? Nullability.NO_CONSTRAINT : Nullability.FORCED_NOT_NULL,
					propertyAnnotatedElement,
					new HashMap<>(),
					entityBinder,
					isIdentifierMapper,
					isComponentEmbedded,
					inSecondPass,
					context,
					inheritanceStatePerClass
			);

			final XProperty property = propertyAnnotatedElement.getProperty();
			if ( isGeneratedId( property ) ) {
				processGeneratedId( context, component, property );
			}
		}

		if ( compositeUserType != null ) {
			processCompositeUserType( component, compositeUserType );
		}
		AggregateComponentBinder.processAggregate(
				component,
				propertyHolder,
				inferredData,
				returnedClassOrElement,
				columns,
				context
		);
		return component;
	}

	private static CompositeUserType<?> compositeUserType(
			Class<? extends CompositeUserType<?>> compositeUserTypeClass,
			MetadataBuildingContext context) {
		return context.getBootstrapContext().getServiceRegistry()
				.getService( ManagedBeanRegistry.class )
				.getBean( compositeUserTypeClass )
				.getBeanInstance();
	}

	private static List<PropertyData> collectClassElements(
			AccessType propertyAccessor,
			MetadataBuildingContext context,
			XClass returnedClassOrElement,
			XClass annotatedClass) {
		final List<PropertyData> classElements = new ArrayList<>();
		//embeddable elements can have type defs
		final PropertyContainer container =
				new PropertyContainer( returnedClassOrElement, annotatedClass, propertyAccessor );
		addElementsOfClass( classElements, container, context);
		//add elements of the embeddable's mapped superclasses
		XClass superClass = annotatedClass.getSuperclass();
		while ( superClass != null && superClass.isAnnotationPresent( MappedSuperclass.class ) ) {
			//FIXME: proper support of type variables incl var resolved at upper levels
			final PropertyContainer superContainer =
					new PropertyContainer( superClass, annotatedClass, propertyAccessor );
			addElementsOfClass( classElements, superContainer, context );
			superClass = superClass.getSuperclass();
		}
		return classElements;
	}

	private static List<PropertyData> collectBaseClassElements(
			PropertyData baseInferredData,
			AccessType propertyAccessor,
			MetadataBuildingContext context,
			XClass annotatedClass) {
		if ( baseInferredData != null ) {
			final List<PropertyData> baseClassElements = new ArrayList<>();
			// iterate from base returned class up hierarchy to handle cases where the @Id attributes
			// might be spread across the subclasses and super classes.
			XClass baseReturnedClassOrElement = baseInferredData.getClassOrElement();
			while ( !Object.class.getName().equals( baseReturnedClassOrElement.getName() ) ) {
				final PropertyContainer container =
						new PropertyContainer( baseReturnedClassOrElement, annotatedClass, propertyAccessor );
				addElementsOfClass( baseClassElements, container, context );
				baseReturnedClassOrElement = baseReturnedClassOrElement.getSuperclass();
			}
			return baseClassElements;
		}
		else {
			return null;
		}
	}

	private static boolean isGeneratedId(XProperty property) {
		return property.isAnnotationPresent( GeneratedValue.class )
			&& property.isAnnotationPresent( Id.class );
	}

	private static void processCompositeUserType(Component component, CompositeUserType<?> compositeUserType) {
		component.sortProperties();
		final List<String> sortedPropertyNames = new ArrayList<>( component.getPropertySpan() );
		final List<Type> sortedPropertyTypes = new ArrayList<>( component.getPropertySpan() );
		final PropertyAccessStrategy strategy = new PropertyAccessStrategyCompositeUserTypeImpl(
				compositeUserType,
				sortedPropertyNames,
				sortedPropertyTypes
		);
		for ( Property property : component.getProperties() ) {
			sortedPropertyNames.add( property.getName() );
			sortedPropertyTypes.add(
					PropertyAccessStrategyMixedImpl.INSTANCE.buildPropertyAccess(
							compositeUserType.embeddable(),
							property.getName(),
							false
					).getGetter().getReturnType()
			);
			property.setPropertyAccessStrategy( strategy );
		}
	}

	private static void processGeneratedId(MetadataBuildingContext context, Component component, XProperty property) {
		final GeneratedValue generatedValue = property.getAnnotation( GeneratedValue.class );
		final String generatorType = generatedValue != null
				? generatorType( generatedValue, property.getType(), context)
				: DEFAULT_ID_GEN_STRATEGY;
		final String generator = generatedValue != null ? generatedValue.generator() : "";

		if ( isGlobalGeneratorNameGlobal( context ) ) {
			buildGenerators( property, context );
			context.getMetadataCollector().addSecondPass( new IdGeneratorResolverSecondPass(
					(SimpleValue) component.getProperty( property.getName() ).getValue(),
					property,
					generatorType,
					generator,
					context
			) );

//			handleTypeDescriptorRegistrations( property, context );
//			bindEmbeddableInstantiatorRegistrations( property, context );
//			bindCompositeUserTypeRegistrations( property, context );
//			handleConverterRegistrations( property, context );
		}
		else {
			makeIdGenerator(
					(SimpleValue) component.getProperty( property.getName() ).getValue(),
					property,
					generatorType,
					generator,
					context,
					new HashMap<>( buildGenerators( property, context ) )
			);
		}
	}

	private static void processIdClassElememts(
			PropertyHolder propertyHolder,
			PropertyData baseInferredData,
			List<PropertyData> classElements,
			List<PropertyData> baseClassElements) {
		final Map<String, PropertyData> baseClassElementsByName = new HashMap<>();
		for ( PropertyData element : baseClassElements ) {
			baseClassElementsByName.put( element.getPropertyName(), element );
		}

		for ( int i = 0; i < classElements.size(); i++ ) {
			final PropertyData idClassPropertyData = classElements.get( i );
			final PropertyData entityPropertyData =
					baseClassElementsByName.get( idClassPropertyData.getPropertyName() );
			if ( propertyHolder.isInIdClass() ) {
				if ( entityPropertyData == null ) {
					throw new AnnotationException(
							"Property '" + getPath(propertyHolder, idClassPropertyData )
									+ "' belongs to an '@IdClass' but has no matching property in entity class '"
									+ baseInferredData.getPropertyClass().getName()
									+ "' (every property of the '@IdClass' must have a corresponding persistent property in the '@Entity' class)"
					);
				}
				if ( hasToOneAnnotation( entityPropertyData.getProperty() )
						&& !entityPropertyData.getClassOrElement().equals( idClassPropertyData.getClassOrElement() ) ) {
					//don't replace here as we need to use the actual original return type
					//the annotation overriding will be dealt with by a mechanism similar to @MapsId
					continue;
				}
			}
			classElements.set( i, entityPropertyData );  //this works since they are in the same order
		}
	}

	public static Component createComponent(
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			boolean isComponentEmbedded,
			boolean isIdentifierMapper,
			Class<? extends EmbeddableInstantiator> customInstantiatorImpl,
			MetadataBuildingContext context) {
		final Component component = new Component( context, propertyHolder.getPersistentClass() );
		component.setEmbedded( isComponentEmbedded );
		//yuk
		component.setTable( propertyHolder.getTable() );
		//FIXME shouldn't identifier mapper use getClassOrElementName? Need to be checked.
		if ( isIdentifierMapper
				|| isComponentEmbedded && inferredData.getPropertyName() == null ) {
			component.setComponentClassName( component.getOwner().getClassName() );
		}
		else {
			component.setComponentClassName( inferredData.getClassOrElementName() );
		}
		component.setCustomInstantiator( customInstantiatorImpl );
		return component;
	}

	public static PropertyData getUniqueIdPropertyFromBaseClass(
			PropertyData inferredData,
			PropertyData baseInferredData,
			AccessType propertyAccessor,
			MetadataBuildingContext context) {
		final List<PropertyData> baseClassElements = new ArrayList<>();
		final PropertyContainer propContainer = new PropertyContainer(
				baseInferredData.getClassOrElement(),
				inferredData.getPropertyClass(),
				propertyAccessor
		);
		addElementsOfClass( baseClassElements, propContainer, context );
		//Id properties are on top and there is only one
		return baseClassElements.get( 0 );
	}

	private static void bindAny(
			String cascadeStrategy,
			AnnotatedJoinColumns columns,
			OnDeleteAction onDeleteAction,
			Nullability nullability,
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			EntityBinder entityBinder,
			boolean isIdentifierMapper,
			MetadataBuildingContext context) {
		final XProperty property = inferredData.getProperty();
		final org.hibernate.annotations.Any any = property.getAnnotation( org.hibernate.annotations.Any.class );
		if ( any == null ) {
			throw new AssertionFailure( "Missing @Any annotation: " + getPath( propertyHolder, inferredData ) );
		}

		final boolean lazy = any.fetch() == FetchType.LAZY;
		final Any value = BinderHelper.buildAnyValue(
				property.getAnnotation( Column.class ),
				getOverridableAnnotation( property, Formula.class, context ),
				columns,
				inferredData,
				onDeleteAction,
				lazy,
				nullability,
				propertyHolder,
				entityBinder,
				any.optional(),
				context
		);

		final PropertyBinder binder = new PropertyBinder();
		binder.setName( inferredData.getPropertyName() );
		binder.setValue( value );
		binder.setLazy( lazy );
		//binder.setCascade(cascadeStrategy);
		if ( isIdentifierMapper ) {
			binder.setInsertable( false );
			binder.setUpdatable( false );
		}
		binder.setAccessType( inferredData.getDefaultAccess() );
		binder.setCascade( cascadeStrategy );
		Property prop = binder.makeProperty();
		//composite FK columns are in the same table, so it's OK
		propertyHolder.addProperty( prop, columns, inferredData.getDeclaringClass() );
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
			IdentifierGeneratorDefinition idGenerator = buildIdGenerator( genericGenerator, context );
			generators.put( idGenerator.getName(), idGenerator );
			metadataCollector.addIdentifierGenerator( idGenerator );
		}

		return generators;
	}

	public static boolean isDefault(XClass clazz, MetadataBuildingContext context) {
		return context.getBootstrapContext().getReflectionManager().equals( clazz, void.class );
	}

	/**
	 * For the mapped entities build some temporary data-structure containing information about the
	 * inheritance status of a class.
	 *
	 * @param orderedClasses Order list of all annotated entities and their mapped superclasses
	 *
	 * @return A map of {@code InheritanceState}s keyed against their {@code XClass}.
	 */
	public static Map<XClass, InheritanceState> buildInheritanceStates(
			List<XClass> orderedClasses,
			MetadataBuildingContext buildingContext) {
		final Map<XClass, InheritanceState> inheritanceStatePerClass = new HashMap<>( orderedClasses.size() );
		for ( XClass clazz : orderedClasses ) {
			final InheritanceState superclassState = getSuperclassInheritanceState( clazz, inheritanceStatePerClass );
			final InheritanceState state = new InheritanceState( clazz, inheritanceStatePerClass, buildingContext );
			if ( superclassState != null ) {
				//the classes are ordered thus preventing an NPE
				//FIXME if an entity has subclasses annotated @MappedSuperclass wo sub @Entity this is wrong
				superclassState.setHasSiblings( true );
				final InheritanceState superEntityState = getInheritanceStateOfSuperEntity( clazz, inheritanceStatePerClass );
				state.setHasParents( superEntityState != null );
				logMixedInheritance( clazz, superclassState, state );
				if ( superclassState.getType() != null ) {
					state.setType( superclassState.getType() );
				}
			}
			inheritanceStatePerClass.put( clazz, state );
		}
		return inheritanceStatePerClass;
	}

	private static void logMixedInheritance(XClass clazz, InheritanceState superclassState, InheritanceState state) {
		if ( state.getType() != null && superclassState.getType() != null ) {
			final boolean nonDefault = InheritanceType.SINGLE_TABLE != state.getType();
			final boolean mixingStrategy = state.getType() != superclassState.getType();
			if ( nonDefault && mixingStrategy ) {
				//TODO: why on earth is this not an error!
				LOG.invalidSubStrategy( clazz.getName() );
			}
		}
	}

	private static boolean hasAnnotationsOnIdClass(XClass idClass) {
		for ( XProperty property : idClass.getDeclaredProperties( XClass.ACCESS_FIELD ) ) {
			if ( hasTriggeringAnnotation( property ) ) {
				return true;
			}
		}
		for ( XMethod method : idClass.getDeclaredMethods() ) {
			if ( hasTriggeringAnnotation( method ) ) {
				return true;
			}
		}
		return false;
	}

	private static boolean hasTriggeringAnnotation(XAnnotatedElement property) {
		return property.isAnnotationPresent(Column.class)
			|| property.isAnnotationPresent(OneToMany.class)
			|| property.isAnnotationPresent(ManyToOne.class)
			|| property.isAnnotationPresent(Id.class)
			|| property.isAnnotationPresent(GeneratedValue.class)
			|| property.isAnnotationPresent(OneToOne.class)
			|| property.isAnnotationPresent(ManyToMany.class);
	}

	static void matchIgnoreNotFoundWithFetchType(
			String entity,
			String association,
			NotFoundAction notFoundAction,
			FetchType fetchType) {
		if ( notFoundAction != null && fetchType == FetchType.LAZY ) {
			LOG.ignoreNotFoundWithFetchTypeLazy( entity, association );
		}
	}

	private static Class<? extends CompositeUserType<?>> resolveTimeZoneStorageCompositeUserType(
			XProperty property,
			XClass returnedClass,
			MetadataBuildingContext context) {
		if ( useColumnForTimeZoneStorage( property, context ) ) {
			String returnedClassName = returnedClass.getName();
			if ( OFFSET_DATETIME_CLASS.equals( returnedClassName ) ) {
				return OffsetDateTimeCompositeUserType.class;
			}
			else if ( ZONED_DATETIME_CLASS.equals( returnedClassName ) ) {
				return ZonedDateTimeCompositeUserType.class;
			}
		}
		return null;
	}

	private static boolean isZonedDateTimeClass(String returnedClassName) {
		return OFFSET_DATETIME_CLASS.equals( returnedClassName )
			|| ZONED_DATETIME_CLASS.equals( returnedClassName );
	}

	static boolean useColumnForTimeZoneStorage(XAnnotatedElement element, MetadataBuildingContext context) {
		final TimeZoneStorage timeZoneStorage = element.getAnnotation( TimeZoneStorage.class );
		if ( timeZoneStorage == null ) {
			if ( element instanceof XProperty ) {
				XProperty property = (XProperty) element;
				return isZonedDateTimeClass( property.getType().getName() )
					//no @TimeZoneStorage annotation, so we need to use the default storage strategy
					&& context.getBuildingOptions().getDefaultTimeZoneStorage() == TimeZoneStorageStrategy.COLUMN;
			}
			else {
				return false;
			}
		}
		else {
			switch ( timeZoneStorage.value() ) {
				case COLUMN:
					return true;
				case AUTO:
					// if the db has native support for timezones, we use that, not a column
					return context.getBuildingOptions().getTimeZoneSupport() != TimeZoneSupport.NATIVE;
				default:
					return false;
			}
		}
	}
}
