/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.model.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.AnnotationException;
import org.hibernate.MappingException;
import org.hibernate.annotations.CollectionTypeRegistration;
import org.hibernate.annotations.CollectionTypeRegistrations;
import org.hibernate.annotations.CompositeTypeRegistration;
import org.hibernate.annotations.CompositeTypeRegistrations;
import org.hibernate.annotations.ConverterRegistration;
import org.hibernate.annotations.ConverterRegistrations;
import org.hibernate.annotations.EmbeddableInstantiatorRegistration;
import org.hibernate.annotations.EmbeddableInstantiatorRegistrations;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.FetchProfile;
import org.hibernate.annotations.FetchProfile.FetchOverride;
import org.hibernate.annotations.FetchProfiles;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.GenericGenerators;
import org.hibernate.annotations.Imported;
import org.hibernate.annotations.JavaTypeRegistration;
import org.hibernate.annotations.JavaTypeRegistrations;
import org.hibernate.annotations.JdbcTypeRegistration;
import org.hibernate.annotations.JdbcTypeRegistrations;
import org.hibernate.annotations.TypeRegistration;
import org.hibernate.annotations.TypeRegistrations;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XAnnotatedElement;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XPackage;
import org.hibernate.boot.model.IdentifierGeneratorDefinition;
import org.hibernate.boot.model.convert.spi.RegisteredConversion;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.resource.beans.internal.FallbackBeanInstanceProducer;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.type.descriptor.java.BasicJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.NamedNativeQueries;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.NamedStoredProcedureQueries;
import jakarta.persistence.NamedStoredProcedureQuery;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.SequenceGenerators;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.SqlResultSetMappings;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;
import jakarta.persistence.TableGenerators;

import static org.hibernate.boot.model.internal.AnnotatedClassType.EMBEDDABLE;
import static org.hibernate.boot.model.internal.AnnotatedClassType.ENTITY;
import static org.hibernate.boot.model.internal.GeneratorBinder.buildGenerators;
import static org.hibernate.boot.model.internal.GeneratorBinder.buildIdGenerator;
import static org.hibernate.boot.model.internal.InheritanceState.getInheritanceStateOfSuperEntity;
import static org.hibernate.boot.model.internal.InheritanceState.getSuperclassInheritanceState;
import static org.hibernate.internal.CoreLogging.messageLogger;
import static org.hibernate.internal.util.StringHelper.unqualify;
import static org.hibernate.mapping.MetadataSource.ANNOTATIONS;

/**
 * Reads annotations from Java classes and produces the Hibernate configuration-time metamodel,
 * that is, the objects defined in the package {@link org.hibernate.mapping}.
 *
 * @implNote This class is stateless, unlike most of the other "binders".
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public final class AnnotationBinder {
	private static final CoreMessageLogger LOG = messageLogger( AnnotationBinder.class );

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
				for ( SqlResultSetMapping annotation : mappings ) {
					QueryBinder.bindSqlResultSetMapping( annotation, context, true );
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

		bindTypeDescriptorRegistrations( annotatedPackage, context );
		bindEmbeddableInstantiatorRegistrations( annotatedPackage, context );
		bindUserTypeRegistrations( annotatedPackage, context );
		bindCompositeUserTypeRegistrations( annotatedPackage, context );
		bindConverterRegistrations( annotatedPackage, context );

		bindGenericGenerators( annotatedPackage, context );
		bindQueries( annotatedPackage, context );
		FilterDefBinder.bindFilterDefs( annotatedPackage, context );
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

		QueryBinder.bindSqlResultSetMappings(
				annotatedElement.getAnnotation( SqlResultSetMappings.class ),
				context,
				false
		);

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

	public static void bindFilterDefs(
			XClass annotatedClass,
			MetadataBuildingContext context) throws MappingException {
		FilterDefBinder.bindFilterDefs( annotatedClass, context );
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

		bindQueries( annotatedClass, context );
		handleImport( annotatedClass, context );
		bindTypeDescriptorRegistrations( annotatedClass, context );
		bindEmbeddableInstantiatorRegistrations( annotatedClass, context );
		bindUserTypeRegistrations( annotatedClass, context );
		bindCompositeUserTypeRegistrations( annotatedClass, context );
		bindConverterRegistrations( annotatedClass, context );

		// try to find class level generators
		final Map<String, IdentifierGeneratorDefinition> generators = buildGenerators( annotatedClass, context );
		if ( context.getMetadataCollector().getClassType( annotatedClass ) == ENTITY ) {
			EntityBinder.bindEntityClass( annotatedClass, inheritanceStatePerClass, generators, context );
		}
	}

	private static void handleImport(XClass annotatedClass, MetadataBuildingContext context) {
		if ( annotatedClass.isAnnotationPresent( Imported.class ) ) {
			String qualifiedName = annotatedClass.getName();
			String name = unqualify( qualifiedName );
			String rename = annotatedClass.getAnnotation( Imported.class ).rename();
			context.getMetadataCollector().addImport( rename.isEmpty() ? name : rename, qualifiedName );
		}
	}

	private static void detectMappedSuperclassProblems(XClass annotatedClass) {
		if ( annotatedClass.isAnnotationPresent( MappedSuperclass.class ) ) {
			//@Entity and @MappedSuperclass on the same class leads to a NPE down the road
			if ( annotatedClass.isAnnotationPresent( Entity.class ) ) {
				throw new AnnotationException( "Type '" + annotatedClass.getName()
						+ "' is annotated both '@Entity' and '@MappedSuperclass'" );
			}
			if ( annotatedClass.isAnnotationPresent( Table.class ) ) {
				throw new AnnotationException( "Mapped superclass '" + annotatedClass.getName()
						+ "' may not specify a '@Table'" );
			}
			if ( annotatedClass.isAnnotationPresent( Inheritance.class ) ) {
				throw new AnnotationException( "Mapped superclass '" + annotatedClass.getName()
						+ "' may not specify an '@Inheritance' mapping strategy" );
			}
		}
	}

	private static void bindTypeDescriptorRegistrations(XAnnotatedElement annotatedElement, MetadataBuildingContext context) {
		final ManagedBeanRegistry managedBeanRegistry = context.getBootstrapContext()
				.getServiceRegistry()
				.getService( ManagedBeanRegistry.class );

		final JavaTypeRegistration javaTypeRegistration =
				annotatedElement.getAnnotation( JavaTypeRegistration.class );
		if ( javaTypeRegistration != null ) {
			handleJavaTypeRegistration( context, managedBeanRegistry, javaTypeRegistration );
		}
		else {
			final JavaTypeRegistrations javaTypeRegistrations =
					annotatedElement.getAnnotation( JavaTypeRegistrations.class );
			if ( javaTypeRegistrations != null ) {
				for ( JavaTypeRegistration registration : javaTypeRegistrations.value() ) {
					handleJavaTypeRegistration( context, managedBeanRegistry, registration );
				}
			}
		}

		final JdbcTypeRegistration jdbcTypeRegistration =
				annotatedElement.getAnnotation( JdbcTypeRegistration.class );
		if ( jdbcTypeRegistration != null ) {
			handleJdbcTypeRegistration( context, managedBeanRegistry, jdbcTypeRegistration );
		}
		else {
			final JdbcTypeRegistrations jdbcTypeRegistrations =
					annotatedElement.getAnnotation( JdbcTypeRegistrations.class );
			if ( jdbcTypeRegistrations != null ) {
				for ( JdbcTypeRegistration registration : jdbcTypeRegistrations.value() ) {
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
		final JdbcType jdbcType = !context.getBuildingOptions().isAllowExtensionsInCdi()
				? FallbackBeanInstanceProducer.INSTANCE.produceBeanInstance( jdbcTypeClass )
				: managedBeanRegistry.getBean( jdbcTypeClass ).getBeanInstance();
		final int typeCode = annotation.registrationCode() == Integer.MIN_VALUE
				? jdbcType.getDefaultSqlTypeCode()
				: annotation.registrationCode();
		context.getMetadataCollector().addJdbcTypeRegistration( typeCode, jdbcType );
	}

	private static void handleJavaTypeRegistration(
			MetadataBuildingContext context,
			ManagedBeanRegistry managedBeanRegistry,
			JavaTypeRegistration annotation) {
		final Class<? extends BasicJavaType<?>> javaTypeClass = annotation.descriptorClass();
		final BasicJavaType<?> javaType =
				!context.getBuildingOptions().isAllowExtensionsInCdi()
						? FallbackBeanInstanceProducer.INSTANCE.produceBeanInstance( javaTypeClass )
						: managedBeanRegistry.getBean( javaTypeClass ).getBeanInstance();
		context.getMetadataCollector().addJavaTypeRegistration( annotation.javaType(), javaType );
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
				for ( EmbeddableInstantiatorRegistration registration : embeddableInstantiatorRegistrations.value() ) {
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
				for ( CompositeTypeRegistration registration : compositeTypeRegistrations.value() ) {
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
				for ( TypeRegistration registration : typeRegistrations.value() ) {
					handleUserTypeRegistration( context, registration );
				}
			}
		}
	}

	private static void handleUserTypeRegistration(
			MetadataBuildingContext context,
			TypeRegistration compositeTypeRegistration) {
		// TODO: check that the two classes agree, i.e. that
		//       the user type knows how to handle the type
		context.getMetadataCollector().registerUserType(
				compositeTypeRegistration.basicClass(),
				compositeTypeRegistration.userType()
		);
	}

	private static void handleCompositeUserTypeRegistration(
			MetadataBuildingContext context,
			CompositeTypeRegistration compositeTypeRegistration) {
		// TODO: check that the two classes agree, i.e. that
		//       the user type knows how to handle the type
		context.getMetadataCollector().registerCompositeUserType(
				compositeTypeRegistration.embeddableClass(),
				compositeTypeRegistration.userType()
		);
	}

	private static void bindConverterRegistrations(XAnnotatedElement container, MetadataBuildingContext context) {
		final ConverterRegistration converterRegistration = container.getAnnotation( ConverterRegistration.class );
		if ( converterRegistration != null ) {
			handleConverterRegistration( converterRegistration, context );
		}
		else {
			final ConverterRegistrations converterRegistrations = container.getAnnotation( ConverterRegistrations.class );
			if ( converterRegistrations != null ) {
				for ( ConverterRegistration registration : converterRegistrations.value() ) {
					handleConverterRegistration( registration, context );
				}
			}
		}
	}

	private static void handleConverterRegistration(ConverterRegistration registration, MetadataBuildingContext context) {
		context.getMetadataCollector().getConverterRegistry()
				.addRegisteredConversion(
						new RegisteredConversion(
								registration.domainType(),
								registration.converter(),
								registration.autoApply(),
								context
						)
				);
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
		final FetchProfile fetchProfile = annotatedElement.getAnnotation( FetchProfile.class );
		final FetchProfiles fetchProfiles = annotatedElement.getAnnotation( FetchProfiles.class );
		if ( fetchProfile != null ) {
			bindFetchProfile( fetchProfile, context );
		}
		if ( fetchProfiles != null ) {
			for ( FetchProfile profile : fetchProfiles.value() ) {
				bindFetchProfile( profile, context );
			}
		}
	}

	private static void bindFetchProfile(FetchProfile fetchProfile, MetadataBuildingContext context) {
		final String name = fetchProfile.name();
		if ( reuseOrCreateFetchProfile( context, name ) ) {
			for ( FetchOverride fetch : fetchProfile.fetchOverrides() ) {
				if ( fetch.fetch() == FetchType.LAZY && fetch.mode() == FetchMode.JOIN ) {
					throw new AnnotationException( "Fetch profile '" + name
							+ "' has a '@FetchOverride' with 'fetch=LAZY' and 'mode=JOIN'"
							+ " (join fetching is eager by nature)");
				}
				context.getMetadataCollector()
						.addSecondPass( new FetchOverrideSecondPass( name, fetch, context ) );
			}
		}
		// otherwise, it's a fetch profile defined in XML, and it overrides
		// the annotations, so we simply ignore this annotation completely
	}

	private static boolean reuseOrCreateFetchProfile(MetadataBuildingContext context, String name) {
		// We tolerate multiple @FetchProfile annotations for same named profile
		org.hibernate.mapping.FetchProfile existing = context.getMetadataCollector().getFetchProfile( name );
		if ( existing == null ) {
			// no existing profile, so create a new one
			org.hibernate.mapping.FetchProfile profile =
					new org.hibernate.mapping.FetchProfile( name, ANNOTATIONS );
			context.getMetadataCollector().addFetchProfile( profile );
			return true;
		}
		else {
			return existing.getSource() == ANNOTATIONS;
		}
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
			final InheritanceState superclassState =
					getSuperclassInheritanceState( clazz, inheritanceStatePerClass );
			final InheritanceState state =
					new InheritanceState( clazz, inheritanceStatePerClass, buildingContext );
			final AnnotatedClassType classType = buildingContext.getMetadataCollector().getClassType( clazz );
			if ( classType == EMBEDDABLE && !clazz.isAnnotationPresent( Imported.class ) ) {
				final String className = clazz.getName();
				buildingContext.getMetadataCollector().addImport( unqualify( className ), className );
			}
			if ( superclassState != null ) {
				//the classes are ordered thus preventing an NPE
				superclassState.setHasSiblings( true );
				final InheritanceState superEntityState =
						getInheritanceStateOfSuperEntity( clazz, inheritanceStatePerClass );
				if ( superEntityState != null ) {
					state.setHasParents( true );
					if ( classType == EMBEDDABLE ) {
						buildingContext.getMetadataCollector().registerEmbeddableSubclass(
								superEntityState.getClazz(),
								clazz
						);
					}
				}
				logMixedInheritance( clazz, superclassState, state );
				if ( superclassState.getType() != null ) {
					state.setType( superclassState.getType() );
				}
			}
			switch ( classType ) {
				case ENTITY:
				case MAPPED_SUPERCLASS:
				case EMBEDDABLE:
					inheritanceStatePerClass.put( clazz, state );
			}
		}
		return inheritanceStatePerClass;
	}

	private static void logMixedInheritance(XClass clazz, InheritanceState superclassState, InheritanceState state) {
		if ( state.getType() != null && superclassState.getType() != null ) {
			final boolean nonDefault = InheritanceType.SINGLE_TABLE != state.getType();
			final boolean mixingStrategy = state.getType() != superclassState.getType();
			if ( nonDefault && mixingStrategy ) {
				throw new AnnotationException( "Entity '" + clazz.getName()
						+ "' may not override the inheritance mapping strategy '" + superclassState.getType()
						+ "' of its hierarchy"
						+ "' (each entity hierarchy has a single inheritance mapping strategy)" );
			}
		}
	}
}
