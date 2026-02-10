/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Imported;
import org.hibernate.annotations.Parameter;
import org.hibernate.boot.jaxb.mapping.spi.JaxbCollectionUserTypeRegistrationImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbCompositeUserTypeRegistrationImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbConfigurationParameterImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbConverterImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbConverterRegistrationImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbDatabaseObjectImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEmbeddableInstantiatorRegistrationImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityListenerImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbFilterDefImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbGenericIdGeneratorImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbJavaTypeRegistrationImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbJdbcTypeRegistrationImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbNamedNativeQueryImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbQueryHintContainer;
import org.hibernate.boot.jaxb.mapping.spi.JaxbNamedHqlQueryImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbNamedStoredProcedureQueryImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbSequenceGeneratorImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbSqlResultSetMappingImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbTableGeneratorImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbUserTypeRegistrationImpl;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.internal.NamedNativeQueryJpaAnnotation;
import org.hibernate.boot.models.spi.CollectionTypeRegistration;
import org.hibernate.boot.models.spi.CompositeUserTypeRegistration;
import org.hibernate.boot.models.spi.ConversionRegistration;
import org.hibernate.boot.models.spi.ConverterRegistration;
import org.hibernate.boot.models.spi.DatabaseObjectRegistration;
import org.hibernate.boot.models.spi.DialectScopeRegistration;
import org.hibernate.boot.models.spi.EmbeddableInstantiatorRegistration;
import org.hibernate.boot.models.spi.FilterDefRegistration;
import org.hibernate.boot.models.spi.GenericGeneratorRegistration;
import org.hibernate.boot.models.spi.GlobalRegistrar;
import org.hibernate.boot.models.spi.GlobalRegistrations;
import org.hibernate.boot.models.spi.JavaTypeRegistration;
import org.hibernate.boot.models.spi.JdbcTypeRegistration;
import org.hibernate.boot.models.spi.JpaEventListener;
import org.hibernate.boot.models.JpaEventListenerStyle;
import org.hibernate.boot.models.spi.NamedNativeQueryRegistration;
import org.hibernate.boot.models.spi.NamedQueryRegistration;
import org.hibernate.boot.models.spi.NamedStoredProcedureQueryRegistration;
import org.hibernate.boot.models.spi.SequenceGeneratorRegistration;
import org.hibernate.boot.models.spi.SqlResultSetMappingRegistration;
import org.hibernate.boot.models.spi.TableGeneratorRegistration;
import org.hibernate.boot.models.spi.UserTypeRegistration;
import org.hibernate.boot.models.xml.internal.QueryProcessing;
import org.hibernate.boot.models.xml.internal.XmlAnnotationHelper;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.AvailableHints;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.models.spi.AnnotationTarget;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.usertype.CompositeUserType;
import org.hibernate.usertype.UserCollectionType;
import org.hibernate.usertype.UserType;

import jakarta.persistence.Entity;
import jakarta.persistence.QueryHint;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.TableGenerator;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.hibernate.boot.models.HibernateAnnotations.COLLECTION_TYPE_REGISTRATION;
import static org.hibernate.boot.models.HibernateAnnotations.COMPOSITE_TYPE_REGISTRATION;
import static org.hibernate.boot.models.HibernateAnnotations.CONVERTER_REGISTRATION;
import static org.hibernate.boot.models.HibernateAnnotations.EMBEDDABLE_INSTANTIATOR_REGISTRATION;
import static org.hibernate.boot.models.HibernateAnnotations.FILTER_DEF;
import static org.hibernate.boot.models.HibernateAnnotations.GENERIC_GENERATOR;
import static org.hibernate.boot.models.HibernateAnnotations.JAVA_TYPE_REGISTRATION;
import static org.hibernate.boot.models.HibernateAnnotations.JDBC_TYPE_REGISTRATION;
import static org.hibernate.boot.models.JpaAnnotations.NAMED_STORED_PROCEDURE_QUERY;
import static org.hibernate.boot.models.JpaAnnotations.SEQUENCE_GENERATOR;
import static org.hibernate.boot.models.JpaAnnotations.TABLE_GENERATOR;
import static org.hibernate.boot.models.HibernateAnnotations.TYPE_REGISTRATION;
import static org.hibernate.boot.models.xml.internal.QueryProcessing.collectResultClasses;
import static org.hibernate.boot.BootLogging.BOOT_LOGGER;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;
import static org.hibernate.internal.util.StringHelper.unqualify;
import static org.hibernate.internal.util.collections.CollectionHelper.isEmpty;
import static org.hibernate.internal.util.collections.CollectionHelper.isNotEmpty;
import static org.hibernate.internal.util.collections.CollectionHelper.setOfSize;

/**
 * @author Steve Ebersole
 */
public class GlobalRegistrationsImpl implements GlobalRegistrations, GlobalRegistrar {
	private final ModelsContext sourceModelContext;
	private final BootstrapContext bootstrapContext;

	private List<JpaEventListener> jpaEventListeners;
	private List<ConversionRegistration> converterRegistrations;
	private List<JavaTypeRegistration> javaTypeRegistrations;
	private List<JdbcTypeRegistration> jdbcTypeRegistrations;
	private List<UserTypeRegistration> userTypeRegistrations;
	private List<CompositeUserTypeRegistration> compositeUserTypeRegistrations;
	private List<CollectionTypeRegistration> collectionTypeRegistrations;
	private List<EmbeddableInstantiatorRegistration> embeddableInstantiatorRegistrations;
	private Map<String, FilterDefRegistration> filterDefRegistrations;
	private Map<String,String> importedRenameMap;

	private Map<String, SequenceGeneratorRegistration> sequenceGeneratorRegistrations;
	private Map<String, TableGeneratorRegistration> tableGeneratorRegistrations;
	private Map<String, GenericGeneratorRegistration> genericGeneratorRegistrations;

	private Set<ConverterRegistration> jpaConverters;

	private Map<String, SqlResultSetMappingRegistration> sqlResultSetMappingRegistrations;
	private Map<String, NamedQueryRegistration> namedQueryRegistrations;
	private Map<String, NamedNativeQueryRegistration> namedNativeQueryRegistrations;
	private Map<String, NamedStoredProcedureQueryRegistration> namedStoredProcedureQueryRegistrations;

	private List<DatabaseObjectRegistration> databaseObjectRegistrations;

	public GlobalRegistrationsImpl(ModelsContext sourceModelContext, BootstrapContext bootstrapContext) {
		this.sourceModelContext = sourceModelContext;
		this.bootstrapContext = bootstrapContext;
	}

	@Override
	public <T> T as(Class<T> type) {
		return type.cast( this );
	}

	@Override
	public List<JpaEventListener> getEntityListenerRegistrations() {
		return jpaEventListeners == null ? emptyList() : jpaEventListeners;
	}

	@Override
	public List<ConversionRegistration> getConverterRegistrations() {
		return converterRegistrations == null ? emptyList() : converterRegistrations;
	}

	@Override
	public List<JavaTypeRegistration> getJavaTypeRegistrations() {
		return javaTypeRegistrations == null ? emptyList() : javaTypeRegistrations;
	}

	@Override
	public List<JdbcTypeRegistration> getJdbcTypeRegistrations() {
		return jdbcTypeRegistrations == null ? emptyList() : jdbcTypeRegistrations;
	}

	@Override
	public List<UserTypeRegistration> getUserTypeRegistrations() {
		return userTypeRegistrations == null ? emptyList() : userTypeRegistrations;
	}

	@Override
	public List<CompositeUserTypeRegistration> getCompositeUserTypeRegistrations() {
		return compositeUserTypeRegistrations == null ? emptyList() : compositeUserTypeRegistrations;
	}

	@Override
	public List<CollectionTypeRegistration> getCollectionTypeRegistrations() {
		return collectionTypeRegistrations == null ? emptyList() : collectionTypeRegistrations;
	}

	@Override
	public List<EmbeddableInstantiatorRegistration> getEmbeddableInstantiatorRegistrations() {
		return embeddableInstantiatorRegistrations == null ? emptyList() : embeddableInstantiatorRegistrations;
	}

	@Override
	public Map<String, FilterDefRegistration> getFilterDefRegistrations() {
		return filterDefRegistrations == null ? emptyMap() : filterDefRegistrations;
	}

	@Override
	public Map<String, String> getImportedRenames() {
		return importedRenameMap == null ? emptyMap() : importedRenameMap;
	}

	@Override
	public Map<String, SequenceGeneratorRegistration> getSequenceGeneratorRegistrations() {
		return sequenceGeneratorRegistrations == null ? emptyMap() : sequenceGeneratorRegistrations;
	}

	@Override
	public Map<String, TableGeneratorRegistration> getTableGeneratorRegistrations() {
		return tableGeneratorRegistrations == null ? emptyMap() : tableGeneratorRegistrations;
	}

	@Override
	public Map<String, GenericGeneratorRegistration> getGenericGeneratorRegistrations() {
		return genericGeneratorRegistrations == null ? emptyMap() : genericGeneratorRegistrations;
	}

	@Override
	public Set<ConverterRegistration> getJpaConverters() {
		return jpaConverters == null ? emptySet() : jpaConverters;
	}

	@Override
	public Map<String, SqlResultSetMappingRegistration> getSqlResultSetMappingRegistrations() {
		return sqlResultSetMappingRegistrations == null ? emptyMap() : sqlResultSetMappingRegistrations;
	}

	@Override
	public Map<String, NamedQueryRegistration > getNamedQueryRegistrations() {
		return namedQueryRegistrations == null ? emptyMap() : namedQueryRegistrations;
	}

	@Override
	public Map<String, NamedNativeQueryRegistration> getNamedNativeQueryRegistrations() {
		return namedNativeQueryRegistrations == null ? emptyMap() : namedNativeQueryRegistrations;
	}

	@Override
	public Map<String, NamedStoredProcedureQueryRegistration> getNamedStoredProcedureQueryRegistrations() {
		return namedStoredProcedureQueryRegistrations == null ? emptyMap() : namedStoredProcedureQueryRegistrations;
	}

	@Override
	public List<DatabaseObjectRegistration> getDatabaseObjectRegistrations() {
		return databaseObjectRegistrations == null ? emptyList() : databaseObjectRegistrations;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JavaTypeRegistration

	private ClassDetailsRegistry getClassDetailsRegistry() {
		return sourceModelContext.getClassDetailsRegistry();
	}

	public void collectJavaTypeRegistrations(AnnotationTarget annotationTarget) {
		annotationTarget.forEachAnnotationUsage( JAVA_TYPE_REGISTRATION, sourceModelContext,
				usage -> collectJavaTypeRegistration(
						toClassDetails( usage.javaType().getName() ),
						toClassDetails( usage.descriptorClass().getName() )
				) );
	}

	public void collectJavaTypeRegistrations(List<JaxbJavaTypeRegistrationImpl> registrations) {
		registrations.forEach( registration -> collectJavaTypeRegistration(
				toClassDetails( registration.getClazz() ),
				toClassDetails( registration.getDescriptor() )
		) );
	}

	public  void collectJavaTypeRegistration(ClassDetails javaType, ClassDetails descriptor) {
		collectJavaTypeRegistration( new JavaTypeRegistration( javaType, descriptor ) );
	}

	public  void collectJavaTypeRegistration(JavaTypeRegistration registration) {
		if ( javaTypeRegistrations == null ) {
			javaTypeRegistrations = new ArrayList<>();
		}
		javaTypeRegistrations.add( registration );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JdbcTypeRegistration

	public void collectJdbcTypeRegistrations(AnnotationTarget annotationTarget) {
		annotationTarget.forEachAnnotationUsage( JDBC_TYPE_REGISTRATION, sourceModelContext,
				usage -> collectJdbcTypeRegistration(
						usage.registrationCode(),
						getClassDetailsRegistry()
								.resolveClassDetails( usage.value().getName() )
				) );
	}

	public void collectJdbcTypeRegistrations(List<JaxbJdbcTypeRegistrationImpl> registrations) {
		registrations.forEach( registration -> collectJdbcTypeRegistration(
				registration.getCode(),
				toClassDetails( registration.getDescriptor() )
		) );
	}

	public void collectJdbcTypeRegistration(Integer registrationCode, ClassDetails descriptor) {
		if ( jdbcTypeRegistrations == null ) {
			jdbcTypeRegistrations = new ArrayList<>();
		}
		jdbcTypeRegistrations.add( new JdbcTypeRegistration( registrationCode, descriptor ) );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// ConversionRegistration

	public void collectConverterRegistrations(AnnotationTarget annotationTarget) {
		annotationTarget.forEachAnnotationUsage( CONVERTER_REGISTRATION, sourceModelContext,
				usage -> collectConverterRegistration( new ConversionRegistration(
						usage.domainType(),
						usage.converter(),
						usage.autoApply(),
						CONVERTER_REGISTRATION
				) ) );
	}

	public void collectConverterRegistrations(List<JaxbConverterRegistrationImpl> registrations) {
		registrations.forEach( registration -> {
			final String explicitDomainTypeName = registration.getClazz();
			collectConverterRegistration( new ConversionRegistration(
					isNotEmpty( explicitDomainTypeName )
							? getClassDetailsRegistry()
									.resolveClassDetails( explicitDomainTypeName )
									.toJavaClass()
							: null,
					getClassDetailsRegistry()
							.resolveClassDetails( registration.getConverter() )
							.toJavaClass(),
					registration.isAutoApply(),
					CONVERTER_REGISTRATION
			) );
		} );
	}

	public void collectConverterRegistration(ConversionRegistration conversion) {
		if ( converterRegistrations == null ) {
			converterRegistrations = new ArrayList<>();
		}
		converterRegistrations.add( conversion );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// UserTypeRegistration

	public void collectUserTypeRegistrations(AnnotationTarget annotationTarget) {
		annotationTarget.forEachAnnotationUsage( TYPE_REGISTRATION, sourceModelContext,
				usage -> collectUserTypeRegistration( usage.basicClass(), usage.userType() ) );
	}

	public void collectUserTypeRegistrations(List<JaxbUserTypeRegistrationImpl> registrations) {
		registrations.forEach( registration -> {
			final var domainTypeDetails = toClassDetails( registration.getClazz() );
			final var descriptorDetails = toClassDetails( registration.getDescriptor() );
			collectUserTypeRegistration( domainTypeDetails, descriptorDetails );
		} );
	}

	public void collectUserTypeRegistration(ClassDetails domainClass, ClassDetails userTypeClass) {
		if ( userTypeRegistrations == null ) {
			userTypeRegistrations = new ArrayList<>();
		}
		userTypeRegistrations.add( new UserTypeRegistration( domainClass, userTypeClass ) );
	}

	public void collectUserTypeRegistration(Class<?> domainClass, Class<? extends UserType<?>> userTypeClass) {
		collectUserTypeRegistration(
				toClassDetails( domainClass.getName() ),
				toClassDetails( userTypeClass.getName() )
		);
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// CompositeUserTypeRegistration

	public void collectCompositeUserTypeRegistrations(AnnotationTarget annotationTarget) {
		annotationTarget.forEachAnnotationUsage( COMPOSITE_TYPE_REGISTRATION, sourceModelContext,
				usage -> collectCompositeUserTypeRegistration( usage.embeddableClass(), usage.userType() ) );
	}

	public void collectCompositeUserTypeRegistrations(List<JaxbCompositeUserTypeRegistrationImpl> registrations) {
		registrations.forEach( registration -> collectCompositeUserTypeRegistration(
				toClassDetails( registration.getClazz() ),
				toClassDetails( registration.getDescriptor() )
		) );
	}

	public void collectCompositeUserTypeRegistration(ClassDetails domainClass, ClassDetails userTypeClass) {
		if ( compositeUserTypeRegistrations == null ) {
			compositeUserTypeRegistrations = new ArrayList<>();
		}
		compositeUserTypeRegistrations.add( new CompositeUserTypeRegistration( domainClass, userTypeClass ) );
	}

	public void collectCompositeUserTypeRegistration(Class<?> domainClass, Class<? extends CompositeUserType<?>> userTypeClass) {
		collectCompositeUserTypeRegistration(
				toClassDetails( domainClass.getName() ),
				toClassDetails( userTypeClass.getName() )
		);
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// CollectionTypeRegistration

	public void collectCollectionTypeRegistrations(AnnotationTarget annotationTarget) {
		annotationTarget.forEachAnnotationUsage( COLLECTION_TYPE_REGISTRATION, sourceModelContext, (usage) -> collectCollectionTypeRegistration(
				usage.classification(),
				usage.type(),
				extractParameterMap( usage.parameters() )
		) );
	}

	private Map<String,String> extractParameterMap(Parameter[] parameters) {
		final Map<String,String> result = new HashMap<>();
		for ( var parameter : parameters ) {
			result.put( parameter.name(), parameter.value() );
		}
		return result;
	}

	public void collectCollectionTypeRegistrations(List<JaxbCollectionUserTypeRegistrationImpl> registrations) {
		registrations.forEach( registration -> collectCollectionTypeRegistration(
				registration.getClassification(),
				toClassDetails( registration.getDescriptor() ),
				extractParameterMap( registration.getParameters() )
		) );
	}

	private Map<String, String> extractParameterMap(List<JaxbConfigurationParameterImpl> parameters) {
		if ( isEmpty( parameters ) ) {
			return Collections.emptyMap();
		}
		else {
			final Map<String, String> result = new HashMap<>();
			parameters.forEach( parameter -> result.put( parameter.getName(), parameter.getValue() ) );
			return result;
		}
	}

	public void collectCollectionTypeRegistration(
			CollectionClassification classification,
			ClassDetails userTypeClass,
			Map<String,String> parameters) {
		if ( collectionTypeRegistrations == null ) {
			collectionTypeRegistrations = new ArrayList<>();
		}
		collectionTypeRegistrations.add( new CollectionTypeRegistration( classification, userTypeClass, parameters ) );
	}

	public void collectCollectionTypeRegistration(
			CollectionClassification classification,
			Class<? extends UserCollectionType> userTypeClass,
			Map<String,String> parameters) {
		collectCollectionTypeRegistration(
				classification,
				toClassDetails( userTypeClass.getName() ),
				parameters
		);
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// EmbeddableInstantiatorRegistration

	public void collectEmbeddableInstantiatorRegistrations(AnnotationTarget annotationTarget) {
		annotationTarget.forEachAnnotationUsage( EMBEDDABLE_INSTANTIATOR_REGISTRATION, sourceModelContext,
				usage -> collectEmbeddableInstantiatorRegistration( usage.embeddableClass(), usage.instantiator() ) );
	}

	public void collectEmbeddableInstantiatorRegistrations(List<JaxbEmbeddableInstantiatorRegistrationImpl> registrations) {
		final var classDetailsRegistry = getClassDetailsRegistry();
		registrations.forEach( registration -> collectEmbeddableInstantiatorRegistration(
				classDetailsRegistry.resolveClassDetails( registration.getEmbeddableClass() ),
				classDetailsRegistry.resolveClassDetails( registration.getInstantiator() )
		) );
	}

	public void collectEmbeddableInstantiatorRegistration(ClassDetails embeddableClass, ClassDetails instantiator) {
		if ( embeddableInstantiatorRegistrations == null ) {
			embeddableInstantiatorRegistrations = new ArrayList<>();
		}
		embeddableInstantiatorRegistrations.add( new EmbeddableInstantiatorRegistration( embeddableClass, instantiator ) );
	}

	public void collectEmbeddableInstantiatorRegistration(Class<?> embeddableClass, Class<? extends EmbeddableInstantiator> instantiator) {
		collectEmbeddableInstantiatorRegistration(
				toClassDetails( embeddableClass.getName() ),
				toClassDetails( instantiator.getName() )
		);
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Filter-defs

	public void collectFilterDefinitions(AnnotationTarget annotationTarget) {
		annotationTarget.forEachAnnotationUsage( FILTER_DEF, sourceModelContext, usage -> {
			final Map<String, ClassDetails> paramJdbcMappings;
			final Map<String, ClassDetails> parameterResolvers;
			final var parameters = usage.parameters();
			if ( isEmpty( parameters ) ) {
				paramJdbcMappings = emptyMap();
				parameterResolvers = emptyMap();
			}
			else {
				paramJdbcMappings = new HashMap<>();
				parameterResolvers = new HashMap<>();

				for ( var parameter : parameters ) {
					paramJdbcMappings.put( parameter.name(), toClassDetails( parameter.type() ) );
					final var resolverClassDetails = toClassDetails( parameter.resolver() );
					if ( !resolverClassDetails.getName().equals( Supplier.class.getName() ) ) {
						parameterResolvers.put( parameter.name(), resolverClassDetails );
					}
				}
			}

			collectFilterDefinition(
					usage.name(),
					usage.defaultCondition(),
					usage.autoEnabled(),
					usage.applyToLoadByKey(),
					paramJdbcMappings,
					parameterResolvers
			);
		} );
	}

	private ClassDetails toClassDetails(Class<?> type) {
		return getClassDetailsRegistry().resolveClassDetails( type.getName() );
	}

	private ClassDetails toClassDetails(String typeName) {
		return getClassDetailsRegistry().resolveClassDetails( typeName );
	}

	public void collectFilterDefinitions(List<JaxbFilterDefImpl> filterDefinitions) {
		final var classDetailsRegistry = getClassDetailsRegistry();
		filterDefinitions.forEach( filterDefinition -> {
			final Map<String, ClassDetails> paramJdbcMappings;
			final Map<String, ClassDetails> parameterResolvers;
			final var jaxbParameters = filterDefinition.getFilterParams();
			if ( jaxbParameters.isEmpty() ) {
				paramJdbcMappings = emptyMap();
				parameterResolvers = emptyMap();
			}
			else {
				paramJdbcMappings = new HashMap<>();
				parameterResolvers = new HashMap<>();

				for ( var jaxbParameter : jaxbParameters ) {
					paramJdbcMappings.put( jaxbParameter.getName(),
							XmlAnnotationHelper.resolveSimpleJavaType(
									jaxbParameter.getType(),
									classDetailsRegistry
							) );

					final String resolver = jaxbParameter.getResolver();
					if ( isNotEmpty( resolver ) ) {
						parameterResolvers.put( jaxbParameter.getName(),
								classDetailsRegistry.resolveClassDetails( resolver ) );
					}
				}
			}

			collectFilterDefinition(
					filterDefinition.getName(),
					filterDefinition.getDefaultCondition(),
					filterDefinition.isAutoEnabled(),
					filterDefinition.isApplyToLoadByKey(),
					paramJdbcMappings,
					parameterResolvers
			);
		} );
	}

	public void collectFilterDefinition(
			String name,
			String defaultCondition,
			boolean autoEnabled,
			boolean applyToLoadByKey,
			Map<String, ClassDetails> parameterTypes,
			Map<String, ClassDetails> parameterResolvers) {
		if ( filterDefRegistrations == null ) {
			filterDefRegistrations = new HashMap<>();
		}

		final var previousEntry =
				filterDefRegistrations.put( name,
						new FilterDefRegistration( name, defaultCondition, autoEnabled,
								applyToLoadByKey, parameterTypes, parameterResolvers ) );
		if ( previousEntry != null ) {
			// legacy code simply allows the collision overwriting the previous
			// todo (jpa32) : re-enable this, especially if the conditions differ
			//throw new AnnotationException( "Multiple '@FilterDef' annotations define a filter named '" + name + "'" );
		}
	}

	public void collectImportRename(ClassDetails classDetails) {
		final var importedUsage = classDetails.getDirectAnnotationUsage( Imported.class );
		if ( importedUsage != null ) {
			final String explicitRename = importedUsage.rename();
			final String rename =
					isNotEmpty( explicitRename )
							? explicitRename
							: unqualify( classDetails.getName() );
			collectImportRename( rename, classDetails.getName() );
		}
	}

	public void collectImportRename(String rename, String name) {
		if ( importedRenameMap == null ) {
			importedRenameMap = new HashMap<>();
		}

		importedRenameMap.put( rename, name );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// EntityListenerRegistration

	public void collectEntityListenerRegistrations(List<JaxbEntityListenerImpl> listeners, ModelsContext modelsContext) {
		final var classDetailsRegistry = getClassDetailsRegistry();
		listeners.forEach( jaxbEntityListener ->
				addJpaEventListener( JpaEventListener.from(
						JpaEventListenerStyle.LISTENER,
						classDetailsRegistry.resolveClassDetails( jaxbEntityListener.getClazz() ),
						jaxbEntityListener,
						modelsContext
				) ) );
	}

	public void addJpaEventListener(JpaEventListener listener) {
		if ( jpaEventListeners == null ) {
			jpaEventListeners = new ArrayList<>();
		}

		jpaEventListeners.add( listener );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Id generators

	public void collectIdGenerators(JaxbEntityMappingsImpl jaxbRoot) {
		collectSequenceGenerators( jaxbRoot.getSequenceGenerators() );
		collectTableGenerators( jaxbRoot.getTableGenerators() );
		collectGenericGenerators( jaxbRoot.getGenericGenerators() );

		// todo : add support for @IdGeneratorType in mapping.xsd?
	}

	public void collectIdGenerators(ClassDetails classDetails) {
		if ( !classDetails.getName().endsWith( ".package-info" )
				&& !bootstrapContext.getJpaCompliance().isGlobalGeneratorScopeEnabled() ) {
			return;
		}

		classDetails.forEachRepeatedAnnotationUsages(
				SEQUENCE_GENERATOR,
				sourceModelContext,
				sequenceGenerator -> collectSequenceGenerator( classDetails, sequenceGenerator )
		);
		classDetails.forEachAnnotationUsage(
				TABLE_GENERATOR,
				sourceModelContext,
				tableGenerator -> collectTableGenerator( classDetails, tableGenerator )
		);
		classDetails.forEachAnnotationUsage(
				GENERIC_GENERATOR,
				sourceModelContext,
				this::collectGenericGenerator
		);
	}

	@Override
	public void collectIdGenerators(MemberDetails memberDetails) {
		if ( bootstrapContext.getJpaCompliance().isGlobalGeneratorScopeEnabled() ) {
			memberDetails.forEachRepeatedAnnotationUsages(
					SEQUENCE_GENERATOR,
					sourceModelContext,
					sequenceGenerator -> collectSequenceGenerator( memberDetails, sequenceGenerator )
			);
			memberDetails.forEachAnnotationUsage(
					TABLE_GENERATOR,
					sourceModelContext,
					tableGenerator -> collectTableGenerator( memberDetails, tableGenerator )
			);
			memberDetails.forEachAnnotationUsage(
					GENERIC_GENERATOR,
					sourceModelContext,
					this::collectGenericGenerator
			);
		}
	}

//	/**
//	 * Account for implicit naming of sequence and table generators when applied to an entity class per JPA 3.2
//	 */
//	private String determineImplicitGeneratorNameBase(ClassDetails classDetails, GenerationType generationType) {
//		if ( classDetails.getName().endsWith( ".package-info" ) ) {
//			throw new MappingException( String.format(
//					Locale.ROOT,
//					"@%s placed on package (%s) specified no name",
//					generationType == GenerationType.SEQUENCE ? SequenceGenerator.class.getSimpleName() : TableGenerator.class.getSimpleName(),
//					StringHelper.qualifier( classDetails.getName() )
//			) );
//		}
//		final var entityAnnotation = classDetails.getDirectAnnotationUsage( ENTITY );
//		if ( entityAnnotation != null ) {
//			final String explicitEntityName = entityAnnotation.name();
//			return StringHelper.isNotEmpty( explicitEntityName )
//					? explicitEntityName
//					: unqualify( classDetails.getName() );
//		}
//		return null;
//	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Sequence generator

	public void collectSequenceGenerators(List<JaxbSequenceGeneratorImpl> sequenceGenerators) {
		sequenceGenerators.forEach( jaxbGenerator -> {
			final var sequenceAnn = SEQUENCE_GENERATOR.createUsage( sourceModelContext );

			if ( isNotEmpty( jaxbGenerator.getName() ) ) {
				sequenceAnn.name( jaxbGenerator.getName() );
			}

			if ( jaxbGenerator.getSequenceName() != null ) {
				sequenceAnn.sequenceName( jaxbGenerator.getSequenceName() );
			}

			if ( isNotEmpty( jaxbGenerator.getCatalog() ) ) {
				sequenceAnn.catalog( jaxbGenerator.getCatalog() );
			}

			if ( isNotEmpty( jaxbGenerator.getSchema() ) ) {
				sequenceAnn.schema( jaxbGenerator.getSchema() );
			}

			if ( jaxbGenerator.getInitialValue() != null ) {
				sequenceAnn.initialValue( jaxbGenerator.getInitialValue() );
			}

			if ( jaxbGenerator.getAllocationSize() != null ) {
				sequenceAnn.allocationSize( jaxbGenerator.getAllocationSize() );
			}

			if ( isNotEmpty( jaxbGenerator.getOptions() ) ) {
				sequenceAnn.options( jaxbGenerator.getOptions() );
			}

			collectSequenceGenerator( new SequenceGeneratorRegistration( jaxbGenerator.getName(), sequenceAnn ) );
		} );
	}

	public void collectSequenceGenerator(MemberDetails memberDetails, SequenceGenerator usage) {
		collectSequenceGenerator( memberDetails.getDeclaringType(), usage );
	}

	public void collectSequenceGenerator(ClassDetails classDetails, SequenceGenerator usage) {
		final String registrationName = registrationName( classDetails, usage.name() );
		collectSequenceGenerator( new SequenceGeneratorRegistration( registrationName, usage ) );
	}

	public void collectSequenceGenerator(SequenceGeneratorRegistration generatorRegistration) {
		checkGeneratorName( generatorRegistration.name() );

		if ( sequenceGeneratorRegistrations == null ) {
			sequenceGeneratorRegistrations = new HashMap<>();
		}
		sequenceGeneratorRegistrations.put( generatorRegistration.name(), generatorRegistration );
	}

	private void checkGeneratorName(String name) {
		checkGeneratorName( name, sequenceGeneratorRegistrations );
		checkGeneratorName( name, tableGeneratorRegistrations );
		checkGeneratorName( name, genericGeneratorRegistrations );
	}

	private void checkGeneratorName(String name, Map<String, ?> generatorMap) {
		if ( generatorMap != null && generatorMap.containsKey( name ) ) {
			if ( bootstrapContext.getJpaCompliance().isGlobalGeneratorScopeEnabled() ) {
				throw new IllegalArgumentException(
						"Duplicate generator name " + name + "; you will likely want to set the property " + AvailableSettings.JPA_ID_GENERATOR_GLOBAL_SCOPE_COMPLIANCE + " to false " );
			}
			else {
				BOOT_LOGGER.duplicateGeneratorName( name );
			}
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Table generator

	public void collectTableGenerators(List<JaxbTableGeneratorImpl> jaxbGenerators) {
		jaxbGenerators.forEach( jaxbGenerator -> {
			final var annotation = TABLE_GENERATOR.createUsage( sourceModelContext );
			if ( isNotEmpty( jaxbGenerator.getName() ) ) {
				annotation.name( jaxbGenerator.getName() );
			}
			if ( isNotEmpty( jaxbGenerator.getTable() ) ) {
				annotation.table( jaxbGenerator.getTable() );
			}
			if ( isNotEmpty( jaxbGenerator.getCatalog() ) ) {
				annotation.catalog( jaxbGenerator.getCatalog() );
			}
			if ( isNotEmpty( jaxbGenerator.getSchema() ) ) {
				annotation.schema( jaxbGenerator.getSchema() );
			}
			if ( isNotEmpty( jaxbGenerator.getPkColumnName() ) ) {
				annotation.pkColumnName( jaxbGenerator.getPkColumnName() );
			}
			if ( isNotEmpty( jaxbGenerator.getPkColumnValue() ) ) {
				annotation.pkColumnValue( jaxbGenerator.getPkColumnValue() );
			}
			if ( isNotEmpty( jaxbGenerator.getValueColumnName() ) ) {
				annotation.valueColumnName( jaxbGenerator.getValueColumnName() );
			}
			if ( jaxbGenerator.getInitialValue() != null ) {
				annotation.initialValue( jaxbGenerator.getInitialValue() );
			}
			if ( jaxbGenerator.getAllocationSize() != null ) {
				annotation.allocationSize( jaxbGenerator.getAllocationSize() );
			}
			if ( isNotEmpty( jaxbGenerator.getOptions() ) ) {
				annotation.options( jaxbGenerator.getOptions() );
			}

			annotation.uniqueConstraints( XmlAnnotationHelper.collectUniqueConstraints(
					jaxbGenerator.getUniqueConstraints(),
					sourceModelContext
			) );

			annotation.indexes( XmlAnnotationHelper.collectIndexes(
					jaxbGenerator.getIndexes(),
					sourceModelContext
			) );

			collectTableGenerator( new TableGeneratorRegistration( jaxbGenerator.getName(), annotation ) );
		} );
	}

	public void collectTableGenerator(MemberDetails memberDetails, TableGenerator usage) {
		collectTableGenerator(  memberDetails.getDeclaringType(), usage );
	}

	public void collectTableGenerator(ClassDetails classDetails, TableGenerator usage) {
		final String registrationName = registrationName( classDetails, usage.name() );
		collectTableGenerator( new TableGeneratorRegistration( registrationName, usage ) );
	}

	public void collectTableGenerator(TableGeneratorRegistration generatorRegistration) {
		checkGeneratorName( generatorRegistration.name() );

		if ( tableGeneratorRegistrations == null ) {
			tableGeneratorRegistrations = new HashMap<>();
		}
		tableGeneratorRegistrations.put( generatorRegistration.name(), generatorRegistration );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Generic generators

	private void collectGenericGenerators(List<JaxbGenericIdGeneratorImpl> jaxbGenerators) {
		jaxbGenerators.forEach( jaxbGenerator -> {
			final var annotation = GENERIC_GENERATOR.createUsage( sourceModelContext );
			annotation.name( jaxbGenerator.getName() );
			annotation.strategy( jaxbGenerator.getClazz() );
			annotation.parameters( XmlAnnotationHelper.collectParameters(
					jaxbGenerator.getParameters(),
					sourceModelContext
			) );

			collectGenericGenerator( new GenericGeneratorRegistration( jaxbGenerator.getName(), annotation ) );
		} );
	}

	public void collectGenericGenerator(GenericGenerator usage) {
		if ( !usage.name().isEmpty() ) {
			collectGenericGenerator( new GenericGeneratorRegistration( usage.name(), usage ) );
		}
	}

	public void collectGenericGenerator(GenericGeneratorRegistration generatorRegistration) {
		checkGeneratorName( generatorRegistration.name() );

		if ( genericGeneratorRegistrations == null ) {
			genericGeneratorRegistrations = new HashMap<>();
		}
		genericGeneratorRegistrations.put( generatorRegistration.name(), generatorRegistration );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Converters

	public void collectConverter(ClassDetails converterClassDetails) {
		if ( jpaConverters == null ) {
			jpaConverters = new HashSet<>();
		}

		jpaConverters.add( new ConverterRegistration( converterClassDetails, null ) );
	}

	public void collectConverters(List<JaxbConverterImpl> converters) {
		if ( !isEmpty( converters ) ) {
			if ( jpaConverters == null ) {
				jpaConverters = setOfSize( converters.size() );
			}

			converters.forEach( jaxbConverter -> {
				final String converterClassName = jaxbConverter.getClazz();
				assert converterClassName != null;
				final var converterType =
						getClassDetailsRegistry()
								.resolveClassDetails( converterClassName );
				jpaConverters.add( new ConverterRegistration( converterType, jaxbConverter.isAutoApply() ) );
			} );
		}
	}

	public void collectQueryReferences(JaxbEntityMappingsImpl jaxbRoot, XmlDocumentContext xmlDocumentContext) {
		collectNamedSqlResultSetMappings( jaxbRoot.getSqlResultSetMappings(), xmlDocumentContext );
		collectNamedQueries( jaxbRoot.getNamedQueries(), xmlDocumentContext );
		collectNamedNativeQueries( jaxbRoot.getNamedNativeQueries(), xmlDocumentContext );
		collectStoredProcedureQueries( jaxbRoot.getNamedProcedureQueries(), xmlDocumentContext );
	}

	private void collectNamedSqlResultSetMappings(
			List<JaxbSqlResultSetMappingImpl> jaxbSqlResultSetMappings,
			XmlDocumentContext xmlDocumentContext) {
		if ( !isEmpty( jaxbSqlResultSetMappings ) ) {
			if ( sqlResultSetMappingRegistrations == null ) {
				sqlResultSetMappingRegistrations = new HashMap<>();
			}

			jaxbSqlResultSetMappings.forEach( (jaxbMapping) -> {
				final var annotation = JpaAnnotations.SQL_RESULT_SET_MAPPING.createUsage( sourceModelContext );
				final String name = jaxbMapping.getName();

				annotation.name( name );

				annotation.columns( QueryProcessing.extractColumnResults(
						jaxbMapping.getColumnResult(),
						xmlDocumentContext
				) );
				annotation.classes( QueryProcessing.extractConstructorResults(
						jaxbMapping.getConstructorResult(),
						xmlDocumentContext
				) );
				annotation.entities( QueryProcessing.extractEntityResults(
						jaxbMapping.getEntityResult(),
						xmlDocumentContext
				) );

				sqlResultSetMappingRegistrations.put( name,
						new SqlResultSetMappingRegistration( name, annotation ) );
			} );
		}
	}

	private void collectNamedQueries(List<JaxbNamedHqlQueryImpl> jaxbNamedQueries, XmlDocumentContext xmlDocumentContext) {
		if ( !isEmpty( jaxbNamedQueries ) ) {
			if ( namedQueryRegistrations == null ) {
				namedQueryRegistrations = new HashMap<>();
			}

			for ( var jaxbNamedQuery : jaxbNamedQueries ) {
				final var queryAnnotation = JpaAnnotations.NAMED_QUERY.createUsage( sourceModelContext );
				final String name = jaxbNamedQuery.getName();

				namedQueryRegistrations.put( name,
						new NamedQueryRegistration( name, queryAnnotation ) );

				queryAnnotation.name( name );
				queryAnnotation.query( jaxbNamedQuery.getQuery() );

				final var lockMode = jaxbNamedQuery.getLockMode();
				if ( lockMode != null ) {
					queryAnnotation.lockMode( lockMode );
				}

				queryAnnotation.hints( collectQueryHints( jaxbNamedQuery, xmlDocumentContext ) );
			}
		}
	}

	private QueryHint[] collectQueryHints(JaxbNamedHqlQueryImpl jaxbNamedQuery, XmlDocumentContext xmlDocumentContext) {
		final var hints = extractQueryHints( jaxbNamedQuery );

		final var modelBuildingContext = xmlDocumentContext.getModelBuildingContext();

		if ( jaxbNamedQuery.isCacheable() == Boolean.TRUE ) {
			final var cacheableHint = JpaAnnotations.QUERY_HINT.createUsage( modelBuildingContext );
			cacheableHint.name( AvailableHints.HINT_CACHEABLE );
			cacheableHint.value( Boolean.TRUE.toString() );
			hints.add( cacheableHint );

			if ( jaxbNamedQuery.getCacheMode() != null ) {
				final var cacheModeHint = JpaAnnotations.QUERY_HINT.createUsage( modelBuildingContext );
				cacheModeHint.name( AvailableHints.HINT_CACHE_MODE );
				cacheModeHint.value( jaxbNamedQuery.getCacheMode().name() );
				hints.add( cacheModeHint );
			}

			if ( isNotEmpty( jaxbNamedQuery.getCacheRegion() ) ) {
				final var cacheRegionHint = JpaAnnotations.QUERY_HINT.createUsage( modelBuildingContext );
				cacheRegionHint.name( AvailableHints.HINT_CACHE_REGION );
				cacheRegionHint.value( jaxbNamedQuery.getCacheRegion() );
				hints.add( cacheRegionHint );
			}
		}

		if ( isNotEmpty( jaxbNamedQuery.getComment() ) ) {
			final var hint = JpaAnnotations.QUERY_HINT.createUsage( modelBuildingContext );
			hint.name( AvailableHints.HINT_COMMENT );
			hint.value( jaxbNamedQuery.getComment() );
			hints.add( hint );
		}

		if ( jaxbNamedQuery.getFetchSize() != null ) {
			final var hint = JpaAnnotations.QUERY_HINT.createUsage( modelBuildingContext );
			hint.name( AvailableHints.HINT_FETCH_SIZE );
			hint.value( jaxbNamedQuery.getFetchSize().toString() );
			hints.add( hint );
		}

		if ( jaxbNamedQuery.isReadOnly() == Boolean.TRUE ) {
			final var hint = JpaAnnotations.QUERY_HINT.createUsage( modelBuildingContext );
			hint.name( AvailableHints.HINT_READ_ONLY );
			hint.value( Boolean.TRUE.toString() );
			hints.add( hint );
		}

		if ( jaxbNamedQuery.getFlushMode() != null ) {
			final var hint = JpaAnnotations.QUERY_HINT.createUsage( modelBuildingContext );
			hint.name( AvailableHints.HINT_FLUSH_MODE );
			hint.value( jaxbNamedQuery.getFlushMode().name() );
			hints.add( hint );
		}

		return hints.toArray(QueryHint[]::new);
	}

	private void collectNamedNativeQueries(List<JaxbNamedNativeQueryImpl> namedNativeQueries, XmlDocumentContext xmlDocumentContext) {
		if ( !isEmpty( namedNativeQueries ) ) {
			if ( namedNativeQueryRegistrations == null ) {
				namedNativeQueryRegistrations = new HashMap<>();
			}

			for ( var jaxbNamedQuery : namedNativeQueries ) {
				final var queryAnnotation = JpaAnnotations.NAMED_NATIVE_QUERY.createUsage( sourceModelContext );
				final String name = jaxbNamedQuery.getName();

				namedNativeQueryRegistrations.put( name,
						new NamedNativeQueryRegistration( name, queryAnnotation ) );

				queryAnnotation.name( name );
				queryAnnotation.query( jaxbNamedQuery.getQuery() );

				final String resultClass = jaxbNamedQuery.getResultClass();
				if ( isNotEmpty( resultClass ) ) {
					queryAnnotation.resultClass( toClassDetails( resultClass ).toJavaClass() );
				}

				applyResultSetMapping( jaxbNamedQuery, queryAnnotation, xmlDocumentContext );

				queryAnnotation.hints( collectNativeQueryHints( jaxbNamedQuery ) );
			}
		}
	}

	private void applyResultSetMapping(
			JaxbNamedNativeQueryImpl jaxbNamedQuery,
			NamedNativeQueryJpaAnnotation queryAnnotation,
			XmlDocumentContext xmlDocumentContext) {
		final String resultSetMapping = jaxbNamedQuery.getResultSetMapping();
		if ( isNotEmpty( resultSetMapping ) ) {
			queryAnnotation.resultSetMapping( jaxbNamedQuery.getResultSetMapping() );
		}
		else {
			queryAnnotation.columns( QueryProcessing.extractColumnResults(
					jaxbNamedQuery.getColumnResult(),
					xmlDocumentContext
			) );
			queryAnnotation.classes( QueryProcessing.extractConstructorResults(
					jaxbNamedQuery.getConstructorResult(),
					xmlDocumentContext
			) );
			queryAnnotation.entities( QueryProcessing.extractEntityResults(
					jaxbNamedQuery.getEntityResult(),
					xmlDocumentContext
			) );
		}
	}

	private QueryHint[] collectNativeQueryHints(JaxbNamedNativeQueryImpl jaxbNamedQuery) {
		final var hints = extractQueryHints( jaxbNamedQuery );

		if ( jaxbNamedQuery.isCacheable() == Boolean.TRUE ) {
			final var cacheableHint = JpaAnnotations.QUERY_HINT.createUsage( sourceModelContext );
			cacheableHint.name( AvailableHints.HINT_CACHEABLE );
			cacheableHint.value( Boolean.TRUE.toString() );
			hints.add( cacheableHint );

			if ( jaxbNamedQuery.getCacheMode() != null ) {
				final var cacheModeHint = JpaAnnotations.QUERY_HINT.createUsage( sourceModelContext );
				cacheModeHint.name( AvailableHints.HINT_CACHE_MODE );
				cacheModeHint.value( jaxbNamedQuery.getCacheMode().name() );
				hints.add( cacheModeHint );
			}

			if ( isNotEmpty( jaxbNamedQuery.getCacheRegion() ) ) {
				final var cacheRegionHint = JpaAnnotations.QUERY_HINT.createUsage( sourceModelContext );
				cacheRegionHint.name( AvailableHints.HINT_CACHE_REGION );
				cacheRegionHint.value( jaxbNamedQuery.getCacheRegion() );
				hints.add( cacheRegionHint );
			}
		}

		if ( jaxbNamedQuery.isReadOnly() == Boolean.TRUE ) {
			final var hint = JpaAnnotations.QUERY_HINT.createUsage( sourceModelContext );
			hint.name( AvailableHints.HINT_READ_ONLY );
			hint.value( Boolean.TRUE.toString() );
			hints.add( hint );
		}

		if ( isNotEmpty( jaxbNamedQuery.getComment() ) ) {
			final var hint = JpaAnnotations.QUERY_HINT.createUsage( sourceModelContext );
			hint.name( AvailableHints.HINT_COMMENT );
			hint.value( jaxbNamedQuery.getComment() );
			hints.add( hint );
		}

		return hints.toArray(QueryHint[]::new);
	}

	private void collectStoredProcedureQueries(
			List<JaxbNamedStoredProcedureQueryImpl> namedProcedureQueries,
			XmlDocumentContext xmlDocumentContext) {
		if ( !isEmpty( namedProcedureQueries ) ) {
			if ( namedStoredProcedureQueryRegistrations == null ) {
				namedStoredProcedureQueryRegistrations = new HashMap<>();
			}

			for ( var jaxbQuery : namedProcedureQueries ) {
				final var queryAnnotation = NAMED_STORED_PROCEDURE_QUERY.createUsage( sourceModelContext );
				final String name = jaxbQuery.getName();

				namedStoredProcedureQueryRegistrations.put( name,
						new NamedStoredProcedureQueryRegistration( name, queryAnnotation ) );

				queryAnnotation.name( name );
				queryAnnotation.procedureName( jaxbQuery.getProcedureName() );

				queryAnnotation.resultClasses( collectResultClasses(
						jaxbQuery.getResultClasses(),
						xmlDocumentContext
				) );

				final var resultSetMappings = jaxbQuery.getResultSetMappings();
				if ( isNotEmpty( resultSetMappings ) ) {
					queryAnnotation.resultSetMappings( resultSetMappings.toArray( String[]::new ) );
				}

				queryAnnotation.hints( extractQueryHints( jaxbQuery ).toArray( QueryHint[]::new ) );

				queryAnnotation.parameters( QueryProcessing.collectParameters(
						jaxbQuery.getProcedureParameters(),
						xmlDocumentContext
				) );
			}
		}
	}

	private List<QueryHint> extractQueryHints(JaxbQueryHintContainer jaxbQuery) {
		final var jaxbQueryHints = jaxbQuery.getHints();
		final List<QueryHint> hints = new ArrayList<>();
		for ( var jaxbHint : jaxbQueryHints ) {
			final var hint = JpaAnnotations.QUERY_HINT.createUsage( sourceModelContext );
			hint.name( jaxbHint.getName() );
			hint.value( jaxbHint.getValue() );
		}
		return hints;
	}

	public void collectDataBaseObject(List<JaxbDatabaseObjectImpl> databaseObjects) {
		if ( !isEmpty( databaseObjects ) ) {
			if ( databaseObjectRegistrations == null ) {
				databaseObjectRegistrations = new ArrayList<>();
			}

			for ( var jaxbDatabaseObject : databaseObjects ) {
				final var definition = jaxbDatabaseObject.getDefinition();
				final var dialectScopes = jaxbDatabaseObject.getDialectScopes();
				final List<DialectScopeRegistration> scopeRegistrations =
						new ArrayList<>( dialectScopes.size() );
				for ( var dialectScope : dialectScopes ) {
					scopeRegistrations.add(
							new DialectScopeRegistration(
									dialectScope.getName(),
									dialectScope.getContent(),
									dialectScope.getMinimumVersion(),
									dialectScope.getMaximumVersion() )
					);
				}
				databaseObjectRegistrations.add( new DatabaseObjectRegistration(
						jaxbDatabaseObject.getCreate(),
						jaxbDatabaseObject.getDrop(),
						definition != null ? definition.getClazz() : null,
						scopeRegistrations ) );
			}
		}
	}

	private static String registrationName(ClassDetails classDetails, String name) {
		if ( !name.isBlank() ) {
			return name;
		}
		else {
			final var entityAnnotation = classDetails.getDirectAnnotationUsage( Entity.class );
			return entityAnnotation != null && !entityAnnotation.name().isEmpty()
					? entityAnnotation.name()
					: unqualify( classDetails.getName() );
		}
	}
}
