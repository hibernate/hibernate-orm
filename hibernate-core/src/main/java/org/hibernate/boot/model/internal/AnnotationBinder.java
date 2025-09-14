/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Table;
import org.hibernate.AnnotationException;
import org.hibernate.MappingException;
import org.hibernate.annotations.CollectionTypeRegistration;
import org.hibernate.annotations.CompositeTypeRegistration;
import org.hibernate.annotations.ConverterRegistration;
import org.hibernate.annotations.EmbeddableInstantiatorRegistration;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.FetchProfile;
import org.hibernate.annotations.Imported;
import org.hibernate.annotations.JavaTypeRegistration;
import org.hibernate.annotations.JdbcTypeRegistration;
import org.hibernate.annotations.TypeRegistration;
import org.hibernate.boot.model.IdentifierGeneratorDefinition;
import org.hibernate.boot.model.NamedEntityGraphDefinition;
import org.hibernate.boot.model.convert.spi.RegisteredConversion;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.models.spi.AnnotationTarget;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.resource.beans.internal.FallbackBeanInstanceProducer;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hibernate.boot.model.internal.AnnotatedClassType.EMBEDDABLE;
import static org.hibernate.boot.model.internal.AnnotatedClassType.ENTITY;
import static org.hibernate.boot.model.internal.EntityBinder.bindEntityClass;
import static org.hibernate.boot.model.internal.FilterDefBinder.bindFilterDefs;
import static org.hibernate.boot.model.internal.GeneratorParameters.interpretSequenceGenerator;
import static org.hibernate.boot.model.internal.GeneratorParameters.interpretTableGenerator;
import static org.hibernate.boot.model.internal.InheritanceState.getInheritanceStateOfSuperEntity;
import static org.hibernate.boot.model.internal.InheritanceState.getSuperclassInheritanceState;
import static org.hibernate.internal.CoreMessageLogger.CORE_LOGGER;
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

	private AnnotationBinder() {}

	public static void bindDefaults(MetadataBuildingContext context) {
		final var metadataCollector = context.getMetadataCollector();
		final var globalRegistrations = metadataCollector.getGlobalRegistrations();


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// id generators

		globalRegistrations.getSequenceGeneratorRegistrations().forEach( (name, generatorRegistration) -> {
			final var definitionBuilder = new IdentifierGeneratorDefinition.Builder();
			interpretSequenceGenerator( generatorRegistration.configuration(), definitionBuilder );
			final IdentifierGeneratorDefinition idGenDef = definitionBuilder.build();
			if ( CORE_LOGGER.isTraceEnabled() ) {
				CORE_LOGGER.trace( "Adding global sequence generator with name: " + name );
			}
			metadataCollector.addDefaultIdentifierGenerator( idGenDef );
		} );

		globalRegistrations.getTableGeneratorRegistrations().forEach( (name, generatorRegistration) -> {
			final var definitionBuilder = new IdentifierGeneratorDefinition.Builder();
			interpretTableGenerator( generatorRegistration.configuration(), definitionBuilder );
			final IdentifierGeneratorDefinition idGenDef = definitionBuilder.build();
			if ( CORE_LOGGER.isTraceEnabled() ) {
				CORE_LOGGER.trace( "Adding global table generator with name: " + name );
			}
			metadataCollector.addDefaultIdentifierGenerator( idGenDef );
		} );


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// result-set-mappings

		globalRegistrations.getSqlResultSetMappingRegistrations().forEach( (name, mappingRegistration) -> {
			QueryBinder.bindSqlResultSetMapping( mappingRegistration.configuration(), context, true );
		} );


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// queries

		globalRegistrations.getNamedQueryRegistrations().forEach( (name, queryRegistration) -> {
			QueryBinder.bindQuery( queryRegistration.configuration(), context, true, null );
		} );

		globalRegistrations.getNamedNativeQueryRegistrations().forEach( (name, queryRegistration) -> {
			QueryBinder.bindNativeQuery( queryRegistration.configuration(), context, null, true );
		} );

		globalRegistrations.getNamedStoredProcedureQueryRegistrations().forEach( (name, queryRegistration) -> {
			QueryBinder.bindNamedStoredProcedureQuery( queryRegistration.configuration(), context, true );
		} );

	}

	private static ModelsContext modelsContext(MetadataBuildingContext context) {
		return context.getBootstrapContext().getModelsContext();
	}

	public static void bindPackage(ClassLoaderService cls, String packageName, MetadataBuildingContext context) {
		final Package pack = cls.packageForNameOrNull( packageName );
		if ( pack != null ) {
			final var packageInfo =
					modelsContext( context ).getClassDetailsRegistry()
							.resolveClassDetails( pack.getName() + ".package-info" );

			GeneratorBinder.registerGlobalGenerators( packageInfo, context );

			bindTypeDescriptorRegistrations( packageInfo, context );
			bindEmbeddableInstantiatorRegistrations( packageInfo, context );
			bindUserTypeRegistrations( packageInfo, context );
			bindCompositeUserTypeRegistrations( packageInfo, context );
			bindConverterRegistrations( packageInfo, context );

			bindQueries( packageInfo, context );
			bindFilterDefs( packageInfo, context );

			bindNamedEntityGraphs( packageInfo, context );
		}
	}

	private static void bindNamedEntityGraphs(ClassDetails packageInfoClassDetails, MetadataBuildingContext context) {
		final var collector = context.getMetadataCollector();
		packageInfoClassDetails.forEachRepeatedAnnotationUsages(
				HibernateAnnotations.NAMED_ENTITY_GRAPH,
				modelsContext( context ),
				(annotation) -> collector.addNamedEntityGraph( new NamedEntityGraphDefinition(
						annotation.name(), null,
						NamedEntityGraphDefinition.Source.PARSED,
						new NamedGraphCreatorParsed( annotation )
				) )
		);
	}

	public static void bindQueries(AnnotationTarget annotationTarget, MetadataBuildingContext context) {
		bindNamedJpaQueries( annotationTarget, context );
		bindNamedHibernateQueries( annotationTarget, context );
	}

	private static void bindNamedHibernateQueries(AnnotationTarget annotationTarget, MetadataBuildingContext context) {
		final var sourceModelContext = modelsContext( context );

		annotationTarget.forEachRepeatedAnnotationUsages(
				HibernateAnnotations.NAMED_QUERY,
				sourceModelContext,
				(usage) -> QueryBinder.bindQuery( usage, context, annotationTarget )
		);

		annotationTarget.forEachRepeatedAnnotationUsages(
				HibernateAnnotations.NAMED_NATIVE_QUERY,
				sourceModelContext,
				(usage) -> QueryBinder.bindNativeQuery( usage, context, annotationTarget )
		);
	}

	private static void bindNamedJpaQueries(AnnotationTarget annotationTarget, MetadataBuildingContext context) {
		final var sourceModelContext = modelsContext( context );

		annotationTarget.forEachRepeatedAnnotationUsages(
				JpaAnnotations.SQL_RESULT_SET_MAPPING,
				sourceModelContext,
				(usage) -> QueryBinder.bindSqlResultSetMapping( usage, context,false )
		);

		annotationTarget.forEachRepeatedAnnotationUsages(
				JpaAnnotations.NAMED_QUERY,
				sourceModelContext,
				(usage) -> QueryBinder.bindQuery( usage, context, false, annotationTarget )
		);

		annotationTarget.forEachRepeatedAnnotationUsages(
				JpaAnnotations.NAMED_NATIVE_QUERY,
				sourceModelContext,
				(usage) -> QueryBinder.bindNativeQuery( usage, context, annotationTarget, false )
		);

		annotationTarget.forEachRepeatedAnnotationUsages(
				JpaAnnotations.NAMED_STORED_PROCEDURE_QUERY,
				sourceModelContext,
				(usage) -> QueryBinder.bindNamedStoredProcedureQuery( usage, context, false )
		);
	}

	/**
	 * Bind an annotated class. A subclass must be bound <em>after</em> its superclass.
	 *
	 * @param classDetails entity to bind as {@code XClass} instance
	 * @param inheritanceStatePerClass Metadata about the inheritance relationships for all mapped classes
	 *
	 * @throws MappingException in case there is a configuration error
	 */
	public static void bindClass(
			ClassDetails classDetails,
			Map<ClassDetails, InheritanceState> inheritanceStatePerClass,
			MetadataBuildingContext context) throws MappingException {

		detectMappedSuperclassProblems( classDetails );

		bindQueries( classDetails, context );
		handleImport( classDetails, context );
		//bindFilterDefs( classDetails, context );
		bindTypeDescriptorRegistrations( classDetails, context );
		bindEmbeddableInstantiatorRegistrations( classDetails, context );
		bindUserTypeRegistrations( classDetails, context );
		bindCompositeUserTypeRegistrations( classDetails, context );
		bindConverterRegistrations( classDetails, context );

		// try to find class level generators
//		GeneratorBinder.registerGlobalGenerators( classDetails, context );
		if ( context.getMetadataCollector().getClassType( classDetails ) == ENTITY ) {
			bindEntityClass( classDetails, inheritanceStatePerClass, context );
		}
	}

	private static void handleImport(ClassDetails annotatedClass, MetadataBuildingContext context) {
		if ( annotatedClass.hasDirectAnnotationUsage( Imported.class ) ) {
			final String qualifiedName = annotatedClass.getName();
			final String name = unqualify( qualifiedName );
			final String rename = annotatedClass.getDirectAnnotationUsage( Imported.class ).rename();
			context.getMetadataCollector().addImport( rename.isBlank() ? name : rename, qualifiedName );
		}
	}

	private static void detectMappedSuperclassProblems(ClassDetails annotatedClass) {
		if ( annotatedClass.hasDirectAnnotationUsage( MappedSuperclass.class ) ) {
			//@Entity and @MappedSuperclass on the same class leads to a NPE down the road
			if ( annotatedClass.hasDirectAnnotationUsage( Entity.class ) ) {
				throw new AnnotationException( "Type '" + annotatedClass.getName()
						+ "' is annotated both '@Entity' and '@MappedSuperclass'" );
			}
			if ( annotatedClass.hasDirectAnnotationUsage( Table.class ) ) {
				throw new AnnotationException( "Mapped superclass '" + annotatedClass.getName()
						+ "' may not specify a '@Table'" );
			}
			if ( annotatedClass.hasDirectAnnotationUsage( Inheritance.class ) ) {
				throw new AnnotationException( "Mapped superclass '" + annotatedClass.getName()
						+ "' may not specify an '@Inheritance' mapping strategy" );
			}
		}
	}

	private static void bindTypeDescriptorRegistrations(
			AnnotationTarget annotatedElement,
			MetadataBuildingContext context) {
		final var managedBeanRegistry = context.getBootstrapContext().getManagedBeanRegistry();

		final var sourceModelContext = modelsContext( context );

		annotatedElement.forEachAnnotationUsage( JavaTypeRegistration.class, sourceModelContext, (usage) -> {
			handleJavaTypeRegistration( context, managedBeanRegistry, usage );
		} );

		annotatedElement.forEachAnnotationUsage( JdbcTypeRegistration.class, sourceModelContext, (usage) -> {
			handleJdbcTypeRegistration( context, managedBeanRegistry, usage );
		} );

		annotatedElement.forEachAnnotationUsage( CollectionTypeRegistration.class, sourceModelContext, (usage) -> {
			context.getMetadataCollector().addCollectionTypeRegistration( usage );
		} );
	}

	private static void handleJdbcTypeRegistration(
			MetadataBuildingContext context,
			ManagedBeanRegistry managedBeanRegistry,
			JdbcTypeRegistration annotation) {
		final JdbcType jdbcType = getBean( context, managedBeanRegistry, annotation.value() );
		context.getMetadataCollector()
				.addJdbcTypeRegistration( jdbcTypeCode( annotation, jdbcType ), jdbcType );
	}

	private static int jdbcTypeCode(JdbcTypeRegistration annotation, JdbcType jdbcType) {
		final int registrationCode = annotation.registrationCode();
		return registrationCode == Integer.MIN_VALUE
				? jdbcType.getDefaultSqlTypeCode()
				: registrationCode;
	}

	private static void handleJavaTypeRegistration(
			MetadataBuildingContext context,
			ManagedBeanRegistry managedBeanRegistry,
			JavaTypeRegistration annotation) {
		context.getMetadataCollector()
				.addJavaTypeRegistration( annotation.javaType(),
						getBean( context, managedBeanRegistry, annotation.descriptorClass() ) );
	}

	private static <T> T getBean(
			MetadataBuildingContext context,
			ManagedBeanRegistry managedBeanRegistry,
			Class<? extends T> jdbcTypeClass) {
		return context.getBuildingOptions().isAllowExtensionsInCdi()
				? managedBeanRegistry.getBean( jdbcTypeClass ).getBeanInstance()
				: FallbackBeanInstanceProducer.INSTANCE.produceBeanInstance( jdbcTypeClass );
	}

	private static void bindEmbeddableInstantiatorRegistrations(
			AnnotationTarget annotatedElement,
			MetadataBuildingContext context) {
		annotatedElement.forEachAnnotationUsage( EmbeddableInstantiatorRegistration.class, modelsContext( context ), (usage) -> {
			handleEmbeddableInstantiatorRegistration( context, usage );
		} );
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
			AnnotationTarget annotatedElement,
			MetadataBuildingContext context) {
		annotatedElement.forEachAnnotationUsage( CompositeTypeRegistration.class, modelsContext( context ), (usage) -> {
			handleCompositeUserTypeRegistration( context, usage );
		} );
	}

	private static void bindUserTypeRegistrations(
			AnnotationTarget annotatedElement,
			MetadataBuildingContext context) {
		annotatedElement.forEachAnnotationUsage( TypeRegistration.class, modelsContext( context ), (usage) -> {
			handleUserTypeRegistration( context, usage );
		} );
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

	private static void bindConverterRegistrations(AnnotationTarget container, MetadataBuildingContext context) {
		final ModelsContext sourceModelContext = modelsContext( context );
		container.forEachAnnotationUsage( ConverterRegistration.class, sourceModelContext, (usage) -> {
			handleConverterRegistration( usage, context );
		} );
	}

	private static void handleConverterRegistration(ConverterRegistration registration, MetadataBuildingContext context) {
		context.getMetadataCollector().getConverterRegistry()
				.addRegisteredConversion( new RegisteredConversion(
						registration.domainType(),
						registration.converter(),
						registration.autoApply(),
						context
				) );
	}

	public static void bindFetchProfilesForClass(AnnotationTarget annotatedClass, MetadataBuildingContext context) {
		bindFetchProfiles( annotatedClass, context );
	}

	public static void bindFetchProfilesForPackage(String packageName, MetadataBuildingContext context) {
		final var packageInfoClassDetails =
				context.getMetadataCollector().getClassDetailsRegistry()
						.findClassDetails( packageName + ".package-info" );
		if ( packageInfoClassDetails != null ) {
			bindFetchProfiles( packageInfoClassDetails, context );
		}
	}

	private static void bindFetchProfiles(AnnotationTarget annotatedElement, MetadataBuildingContext context) {
		annotatedElement.forEachAnnotationUsage( FetchProfile.class, modelsContext( context ), (usage) -> {
			bindFetchProfile( usage, context );
		} );
	}

	private static void bindFetchProfile(FetchProfile fetchProfile, MetadataBuildingContext context) {
		final String name = fetchProfile.name();
		if ( reuseOrCreateFetchProfile( context, name ) ) {
			for ( var fetchOverride : fetchProfile.fetchOverrides() ) {
				final FetchType type = fetchOverride.fetch();
				final FetchMode mode = fetchOverride.mode();
				if ( type == FetchType.LAZY && mode == FetchMode.JOIN ) {
					throw new AnnotationException(
							"Fetch profile '" + name
									+ "' has a '@FetchOverride' with 'fetch=LAZY' and 'mode=JOIN'"
									+ " (join fetching is eager by nature)"
					);
				}
				context.getMetadataCollector()
						.addSecondPass( new FetchOverrideSecondPass( name, fetchOverride, context ) );
			}
		}
		// otherwise, it's a fetch profile defined in XML, and it overrides
		// the annotations, so we simply ignore this annotation completely
	}

	private static boolean reuseOrCreateFetchProfile(MetadataBuildingContext context, String name) {
		// We tolerate multiple @FetchProfile annotations for same named profile
		final var collector = context.getMetadataCollector();
		var existing = collector.getFetchProfile( name );
		if ( existing == null ) {
			// no existing profile, so create a new one
			collector.addFetchProfile( new org.hibernate.mapping.FetchProfile( name, ANNOTATIONS ) );
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
	public static Map<ClassDetails, InheritanceState> buildInheritanceStates(
			List<ClassDetails> orderedClasses,
			MetadataBuildingContext buildingContext) {
		final Map<ClassDetails, InheritanceState> inheritanceStatePerClass = new HashMap<>( orderedClasses.size() );
		final var collector = buildingContext.getMetadataCollector();
		for ( ClassDetails clazz : orderedClasses ) {
			final var superclassState = getSuperclassInheritanceState( clazz, inheritanceStatePerClass );
			final var state = new InheritanceState( clazz, inheritanceStatePerClass, buildingContext );
			final var classType = collector.getClassType( clazz );
			if ( classType == EMBEDDABLE && !clazz.hasDirectAnnotationUsage( Imported.class ) ) {
				final String className = clazz.getName();
				collector.addImport( unqualify( className ), className );
			}
			if ( superclassState != null ) {
				//the classes are ordered thus preventing an NPE
				superclassState.setHasSiblings( true );
				final var superEntityState = getInheritanceStateOfSuperEntity( clazz, inheritanceStatePerClass );
				if ( superEntityState != null ) {
					state.setHasParents( true );
					if ( classType == EMBEDDABLE ) {
						collector.registerEmbeddableSubclass( superEntityState.getClassDetails(), clazz );
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

	private static void logMixedInheritance(ClassDetails classDetails, InheritanceState superclassState, InheritanceState state) {
		if ( state.getType() != null && superclassState.getType() != null ) {
			final boolean nonDefault = InheritanceType.SINGLE_TABLE != state.getType();
			final boolean mixingStrategy = state.getType() != superclassState.getType();
			if ( nonDefault && mixingStrategy ) {
				throw new AnnotationException( "Entity '" + classDetails.getName()
						+ "' may not override the inheritance mapping strategy '" + superclassState.getType()
						+ "' of its hierarchy"
						+ "' (each entity hierarchy has a single inheritance mapping strategy)" );
			}
		}
	}
}
