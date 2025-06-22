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
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.hibernate.MappingException;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Imported;
import org.hibernate.annotations.ParamDef;
import org.hibernate.annotations.Parameter;
import org.hibernate.boot.jaxb.mapping.spi.JaxbCollectionUserTypeRegistrationImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbCompositeUserTypeRegistrationImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbConfigurationParameterImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbConverterImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbConverterRegistrationImpl;
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
import org.hibernate.boot.jaxb.mapping.spi.JaxbQueryHint;
import org.hibernate.boot.jaxb.mapping.spi.JaxbSequenceGeneratorImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbSqlResultSetMappingImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbTableGeneratorImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbUserTypeRegistrationImpl;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.internal.GenericGeneratorAnnotation;
import org.hibernate.boot.models.annotations.internal.NamedNativeQueryJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.NamedQueryJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.NamedStoredProcedureQueryJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.QueryHintJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.SequenceGeneratorJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.SqlResultSetMappingJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.TableGeneratorJpaAnnotation;
import org.hibernate.boot.models.spi.CollectionTypeRegistration;
import org.hibernate.boot.models.spi.CompositeUserTypeRegistration;
import org.hibernate.boot.models.spi.ConversionRegistration;
import org.hibernate.boot.models.spi.ConverterRegistration;
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
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.jpa.AvailableHints;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.models.spi.AnnotationTarget;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.usertype.CompositeUserType;
import org.hibernate.usertype.UserCollectionType;
import org.hibernate.usertype.UserType;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Entity;
import jakarta.persistence.GenerationType;
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
import static org.hibernate.boot.models.JpaAnnotations.ENTITY;
import static org.hibernate.boot.models.JpaAnnotations.NAMED_STORED_PROCEDURE_QUERY;
import static org.hibernate.boot.models.JpaAnnotations.SEQUENCE_GENERATOR;
import static org.hibernate.boot.models.JpaAnnotations.TABLE_GENERATOR;
import static org.hibernate.boot.models.HibernateAnnotations.TYPE_REGISTRATION;
import static org.hibernate.boot.models.xml.internal.QueryProcessing.collectResultClasses;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;
import static org.hibernate.internal.util.collections.CollectionHelper.isEmpty;

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

	public GlobalRegistrationsImpl(ModelsContext sourceModelContext, BootstrapContext bootstrapContext) {
		this.sourceModelContext = sourceModelContext;
		this.bootstrapContext = bootstrapContext;
	}

	@Override
	public <T> T as(Class<T> type) {
		//noinspection unchecked
		return (T) this;
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
		return namedStoredProcedureQueryRegistrations== null ? emptyMap() : namedStoredProcedureQueryRegistrations;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JavaTypeRegistration

	public void collectJavaTypeRegistrations(AnnotationTarget annotationTarget) {
		annotationTarget.forEachAnnotationUsage( JAVA_TYPE_REGISTRATION, sourceModelContext, (usage) -> collectJavaTypeRegistration(
				toClassDetails( usage.javaType().getName() ),
				toClassDetails( usage.descriptorClass().getName() )
		) );
	}

	public void collectJavaTypeRegistrations(List<JaxbJavaTypeRegistrationImpl> registrations) {
		if ( CollectionHelper.isEmpty( registrations ) ) {
			return;
		}

		registrations.forEach( (reg) -> collectJavaTypeRegistration(
				toClassDetails( reg.getClazz() ),
				toClassDetails( reg.getDescriptor() )
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
		annotationTarget.forEachAnnotationUsage( JDBC_TYPE_REGISTRATION, sourceModelContext, (usage) -> collectJdbcTypeRegistration(
				usage.registrationCode(),
				sourceModelContext.getClassDetailsRegistry().resolveClassDetails( usage.value().getName() )
		) );
	}

	public void collectJdbcTypeRegistrations(List<JaxbJdbcTypeRegistrationImpl> registrations) {
		if ( CollectionHelper.isEmpty( registrations ) ) {
			return;
		}

		registrations.forEach( (reg) -> collectJdbcTypeRegistration(
				reg.getCode(),
				toClassDetails( reg.getDescriptor() )
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
		annotationTarget.forEachAnnotationUsage( CONVERTER_REGISTRATION, sourceModelContext, (usage) -> {
			collectConverterRegistration( new ConversionRegistration(
					usage.domainType(),
					usage.converter(),
					usage.autoApply(),
					CONVERTER_REGISTRATION
			) );
		} );
	}

	public void collectConverterRegistrations(List<JaxbConverterRegistrationImpl> registrations) {
		if ( CollectionHelper.isEmpty( registrations ) ) {
			return;
		}

		registrations.forEach( (registration) -> {
			final Class<?> explicitDomainType;
			final String explicitDomainTypeName = registration.getClazz();
			if ( isNotEmpty( explicitDomainTypeName ) ) {
				explicitDomainType = sourceModelContext.getClassDetailsRegistry().resolveClassDetails( explicitDomainTypeName ).toJavaClass();
			}
			else {
				explicitDomainType = null;
			}
			final Class<? extends AttributeConverter<?,?>> converterType = sourceModelContext.getClassDetailsRegistry().resolveClassDetails( registration.getConverter() ).toJavaClass();
			final boolean autoApply = registration.isAutoApply();
			collectConverterRegistration( new ConversionRegistration(
					explicitDomainType,
					converterType,
					autoApply,
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
		annotationTarget.forEachAnnotationUsage( TYPE_REGISTRATION, sourceModelContext, (usage) -> collectUserTypeRegistration(
				usage.basicClass(),
				usage.userType()
		) );
	}

	public void collectUserTypeRegistrations(List<JaxbUserTypeRegistrationImpl> registrations) {
		if ( CollectionHelper.isEmpty( registrations ) ) {
			return;
		}

		registrations.forEach( (reg) -> {
			final ClassDetails domainTypeDetails = toClassDetails( reg.getClazz() );
			final ClassDetails descriptorDetails = toClassDetails( reg.getDescriptor() );
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
		annotationTarget.forEachAnnotationUsage( COMPOSITE_TYPE_REGISTRATION, sourceModelContext, (usage) -> collectCompositeUserTypeRegistration(
				usage.embeddableClass(),
				usage.userType()
		) );
	}

	public void collectCompositeUserTypeRegistrations(List<JaxbCompositeUserTypeRegistrationImpl> registrations) {
		if ( CollectionHelper.isEmpty( registrations ) ) {
			return;
		}

		registrations.forEach( (reg) -> collectCompositeUserTypeRegistration(
				toClassDetails( reg.getClazz() ),
				toClassDetails( reg.getDescriptor() )
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
		for ( Parameter parameter : parameters ) {
			result.put( parameter.name(), parameter.value() );
		}
		return result;
	}

	public void collectCollectionTypeRegistrations(List<JaxbCollectionUserTypeRegistrationImpl> registrations) {
		if ( CollectionHelper.isEmpty( registrations ) ) {
			return;
		}

		registrations.forEach( (reg) -> collectCollectionTypeRegistration(
				reg.getClassification(),
				toClassDetails( reg.getDescriptor() ),
				extractParameterMap( reg.getParameters() )
		) );
	}

	private Map<String, String> extractParameterMap(List<JaxbConfigurationParameterImpl> parameters) {
		if ( CollectionHelper.isEmpty( parameters ) ) {
			return Collections.emptyMap();
		}

		final Map<String,String> result = new HashMap<>();
		parameters.forEach( parameter -> result.put( parameter.getName(), parameter.getValue() ) );
		return result;
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
		annotationTarget.forEachAnnotationUsage( EMBEDDABLE_INSTANTIATOR_REGISTRATION, sourceModelContext, (usage) -> collectEmbeddableInstantiatorRegistration(
				usage.embeddableClass(),
				usage.instantiator()
		) );
	}

	public void collectEmbeddableInstantiatorRegistrations(List<JaxbEmbeddableInstantiatorRegistrationImpl> registrations) {
		if ( CollectionHelper.isEmpty( registrations ) ) {
			return;
		}

		registrations.forEach( (reg) -> collectEmbeddableInstantiatorRegistration(
				sourceModelContext.getClassDetailsRegistry().resolveClassDetails( reg.getEmbeddableClass() ),
				sourceModelContext.getClassDetailsRegistry().resolveClassDetails( reg.getInstantiator() )
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
		annotationTarget.forEachAnnotationUsage( FILTER_DEF, sourceModelContext, (usage) -> {
			final Map<String, ClassDetails> paramJdbcMappings;
			final Map<String, ClassDetails> parameterResolvers;
			final ParamDef[] parameters = usage.parameters();
			if ( CollectionHelper.isEmpty( parameters ) ) {
				paramJdbcMappings = emptyMap();
				parameterResolvers = emptyMap();
			}
			else {
				paramJdbcMappings = new HashMap<>();
				parameterResolvers = new HashMap<>();

				for ( ParamDef parameter : parameters ) {
					paramJdbcMappings.put( parameter.name(), toClassDetails( parameter.type() ) );
					final ClassDetails resolverClassDetails = toClassDetails( parameter.resolver() );
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
		return sourceModelContext.getClassDetailsRegistry().resolveClassDetails( type.getName() );
	}

	private ClassDetails toClassDetails(String typeName) {
		return sourceModelContext.getClassDetailsRegistry().resolveClassDetails( typeName );
	}

	public void collectFilterDefinitions(List<JaxbFilterDefImpl> filterDefinitions) {
		if ( CollectionHelper.isEmpty( filterDefinitions ) ) {
			return;
		}

		filterDefinitions.forEach( (filterDefinition) -> {
			final Map<String, ClassDetails> paramJdbcMappings;
			final Map<String, ClassDetails> parameterResolvers;
			final List<JaxbFilterDefImpl.JaxbFilterParamImpl> jaxbParameters = filterDefinition.getFilterParams();
			if ( jaxbParameters.isEmpty() ) {
				paramJdbcMappings = emptyMap();
				parameterResolvers = emptyMap();
			}
			else {
				paramJdbcMappings = new HashMap<>();
				parameterResolvers = new HashMap<>();

				for ( JaxbFilterDefImpl.JaxbFilterParamImpl jaxbParameter : jaxbParameters ) {
					final ClassDetails targetClassDetails = XmlAnnotationHelper.resolveSimpleJavaType(
							jaxbParameter.getType(),
							sourceModelContext.getClassDetailsRegistry()
					);
					paramJdbcMappings.put( jaxbParameter.getName(), targetClassDetails );

					if ( isNotEmpty( jaxbParameter.getResolver() ) ) {
						parameterResolvers.put(
								jaxbParameter.getName(),
								sourceModelContext.getClassDetailsRegistry().resolveClassDetails( jaxbParameter.getResolver() )
						);
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

		final FilterDefRegistration previousEntry = filterDefRegistrations.put(
				name,
				new FilterDefRegistration( name, defaultCondition, autoEnabled, applyToLoadByKey, parameterTypes, parameterResolvers )
		);
		if ( previousEntry != null ) {
			// legacy code simply allows the collision overwriting the previous
			// todo (jpa32) : re-enable this, especially if the conditions differ
			//throw new AnnotationException( "Multiple '@FilterDef' annotations define a filter named '" + name + "'" );
		}
	}

	public void collectImportRename(ClassDetails classDetails) {
		final Imported importedUsage = classDetails.getDirectAnnotationUsage( Imported.class );

		if ( importedUsage == null ) {
			return;
		}

		final String explicitRename = importedUsage.rename();
		final String rename = isNotEmpty( explicitRename )
				? explicitRename
				: StringHelper.unqualify( classDetails.getName() );

		collectImportRename( rename, classDetails.getName() );
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
		if ( CollectionHelper.isEmpty( listeners ) ) {
			return;
		}

		listeners.forEach( (jaxbEntityListener) -> {
			final ClassDetails classDetails = sourceModelContext.getClassDetailsRegistry().resolveClassDetails( jaxbEntityListener.getClazz() );
			final JpaEventListener listener = JpaEventListener.from(
					JpaEventListenerStyle.LISTENER,
					classDetails,
					jaxbEntityListener,
					modelsContext
			);
			addJpaEventListener( listener );
		} );
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
				(sequenceGenerator) -> collectSequenceGenerator( classDetails, sequenceGenerator )
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
		if ( !bootstrapContext.getJpaCompliance().isGlobalGeneratorScopeEnabled() ) {
			return;
		}

		memberDetails.forEachRepeatedAnnotationUsages(
				SEQUENCE_GENERATOR,
				sourceModelContext,
				(sequenceGenerator) -> collectSequenceGenerator( memberDetails, sequenceGenerator )
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

	/**
	 * Account for implicit naming of sequence and table generators when applied to an entity class per JPA 3.2
	 */
	private String determineImplicitGeneratorNameBase(ClassDetails classDetails, GenerationType generationType) {
		if ( classDetails.getName().endsWith( ".package-info" ) ) {
			throw new MappingException( String.format(
					Locale.ROOT,
					"@%s placed on package (%s) specified no name",
					generationType == GenerationType.SEQUENCE ? SequenceGenerator.class.getSimpleName() : TableGenerator.class.getSimpleName(),
					StringHelper.qualifier( classDetails.getName() )
			) );
		}
		final Entity entityAnnotation = classDetails.getDirectAnnotationUsage( ENTITY );
		if ( entityAnnotation != null ) {
			final String explicitEntityName = entityAnnotation.name();
			return StringHelper.isNotEmpty( explicitEntityName )
					? explicitEntityName
					: StringHelper.unqualify( classDetails.getName() );
		}
		return null;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Sequence generator

	public void collectSequenceGenerators(List<JaxbSequenceGeneratorImpl> sequenceGenerators) {
		if ( CollectionHelper.isEmpty( sequenceGenerators ) ) {
			return;
		}

		sequenceGenerators.forEach( (jaxbGenerator) -> {
			final SequenceGeneratorJpaAnnotation sequenceAnn = SEQUENCE_GENERATOR.createUsage( sourceModelContext );

			if ( StringHelper.isNotEmpty( jaxbGenerator.getName() ) ) {
				sequenceAnn.name( jaxbGenerator.getName() );
			}

			if ( jaxbGenerator.getSequenceName() != null ) {
				sequenceAnn.sequenceName( jaxbGenerator.getSequenceName() );
			}

			if ( StringHelper.isNotEmpty( jaxbGenerator.getCatalog() ) ) {
				sequenceAnn.catalog( jaxbGenerator.getCatalog() );
			}

			if ( StringHelper.isNotEmpty( jaxbGenerator.getSchema() ) ) {
				sequenceAnn.schema( jaxbGenerator.getSchema() );
			}

			if ( jaxbGenerator.getInitialValue() != null ) {
				sequenceAnn.initialValue( jaxbGenerator.getInitialValue() );
			}

			if ( jaxbGenerator.getAllocationSize() != null ) {
				sequenceAnn.allocationSize( jaxbGenerator.getAllocationSize() );
			}

			if ( StringHelper.isNotEmpty( jaxbGenerator.getOptions() ) ) {
				sequenceAnn.options( jaxbGenerator.getOptions() );
			}

			collectSequenceGenerator( new SequenceGeneratorRegistration( jaxbGenerator.getName(), sequenceAnn ) );
		} );
	}

	public void collectSequenceGenerator(MemberDetails memberDetails, SequenceGenerator usage) {
		collectSequenceGenerator( memberDetails.getDeclaringType(), usage );
	}


	public void collectSequenceGenerator(ClassDetails classDetails, SequenceGenerator usage) {
		final String registrationName;
		if ( !usage.name().isEmpty() ) {
			registrationName = usage.name();
		}
		else {
			final Entity entityAnnotation = classDetails.getDirectAnnotationUsage( Entity.class );
			if ( entityAnnotation != null && !entityAnnotation.name().isEmpty() ) {
				registrationName = entityAnnotation.name();
			}
			else {
				registrationName = StringHelper.unqualify( classDetails.getName() );
			}
		}
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
		if ( generatorMap == null ) {
			return;
		}
		if ( generatorMap.containsKey( name ) ) {
			if ( bootstrapContext.getJpaCompliance().isGlobalGeneratorScopeEnabled() ) {
				throw new IllegalArgumentException( "Duplicate generator name " + name + "; you will likely want to set the property " + AvailableSettings.JPA_ID_GENERATOR_GLOBAL_SCOPE_COMPLIANCE + " to false " );
			}
			else {
				CoreLogging.messageLogger( GlobalRegistrationsImpl.class ).duplicateGeneratorName( name );
			}
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Table generator

	public void collectTableGenerators(List<JaxbTableGeneratorImpl> jaxbGenerators) {
		if ( CollectionHelper.isEmpty( jaxbGenerators ) ) {
			return;
		}

		jaxbGenerators.forEach( (jaxbGenerator) -> {
			final TableGeneratorJpaAnnotation annotation = TABLE_GENERATOR.createUsage( sourceModelContext );
			if ( StringHelper.isNotEmpty( jaxbGenerator.getName() ) ) {
				annotation.name( jaxbGenerator.getName() );
			}
			if ( StringHelper.isNotEmpty( jaxbGenerator.getTable() ) ) {
				annotation.table( jaxbGenerator.getTable() );
			}
			if ( StringHelper.isNotEmpty( jaxbGenerator.getCatalog() ) ) {
				annotation.catalog( jaxbGenerator.getCatalog() );
			}
			if ( StringHelper.isNotEmpty( jaxbGenerator.getSchema() ) ) {
				annotation.schema( jaxbGenerator.getSchema() );
			}
			if ( StringHelper.isNotEmpty( jaxbGenerator.getPkColumnName() ) ) {
				annotation.pkColumnName( jaxbGenerator.getPkColumnName() );
			}
			if ( StringHelper.isNotEmpty( jaxbGenerator.getPkColumnValue() ) ) {
				annotation.pkColumnValue( jaxbGenerator.getPkColumnValue() );
			}
			if ( StringHelper.isNotEmpty( jaxbGenerator.getValueColumnName() ) ) {
				annotation.valueColumnName( jaxbGenerator.getValueColumnName() );
			}
			if ( jaxbGenerator.getInitialValue() != null ) {
				annotation.initialValue( jaxbGenerator.getInitialValue() );
			}
			if ( jaxbGenerator.getAllocationSize() != null ) {
				annotation.allocationSize( jaxbGenerator.getAllocationSize() );
			}
			if ( StringHelper.isNotEmpty( jaxbGenerator.getOptions() ) ) {
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

	public void collectTableGenerator(ClassDetails classDetails, TableGenerator usage) {
		final TableGeneratorJpaAnnotation generatorAnnotation = new TableGeneratorJpaAnnotation(
				usage,
				sourceModelContext
		);

		final Entity entityAnnotation = classDetails.getDirectAnnotationUsage( Entity.class );
		final String simpleName;
		if ( entityAnnotation != null && !entityAnnotation.name().isEmpty() ) {
			simpleName = entityAnnotation.name();
		}
		else {
			simpleName = StringHelper.unqualify( classDetails.getName() );
		}

		final String registrationName;
		if ( !usage.name().isEmpty() ) {
			registrationName = usage.name();
		}
		else {
			registrationName = simpleName;
		}
		generatorAnnotation.name( registrationName );
		collectTableGenerator( new TableGeneratorRegistration( registrationName, usage ) );

	}

	public void collectTableGenerator(MemberDetails memberDetails, TableGenerator usage) {
		final String registrationName;
		if ( !usage.name().isEmpty() ) {
			registrationName = usage.name();
		}
		else {
			final Entity entityAnnotation = memberDetails.getDeclaringType().getDirectAnnotationUsage( Entity.class );
			if ( entityAnnotation != null && !entityAnnotation.name().isEmpty() ) {
				registrationName = entityAnnotation.name();
			}
			else {
				registrationName = StringHelper.unqualify( memberDetails.getDeclaringType().getName() );
			}
		}
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
		if ( CollectionHelper.isEmpty( jaxbGenerators ) ) {
			return;
		}

		jaxbGenerators.forEach( (jaxbGenerator) -> {
			final GenericGeneratorAnnotation annotation = GENERIC_GENERATOR.createUsage( sourceModelContext );
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
		if ( usage.name().isEmpty() ) {
			return;
		}
		collectGenericGenerator( new GenericGeneratorRegistration( usage.name(), usage ) );
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
		if ( CollectionHelper.isEmpty( converters ) ) {
			return;
		}

		if ( jpaConverters == null ) {
			jpaConverters = CollectionHelper.setOfSize( converters.size() );
		}

		converters.forEach( (jaxbConverter) -> {
			final String converterClassName = jaxbConverter.getClazz();
			assert converterClassName != null;
			final ClassDetails converterType = sourceModelContext.getClassDetailsRegistry().resolveClassDetails( converterClassName );
			final boolean autoApply = jaxbConverter.isAutoApply();

			jpaConverters.add( new ConverterRegistration( converterType, autoApply ) );
		} );
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
		if ( isEmpty( jaxbSqlResultSetMappings ) ) {
			return;
		}

		if ( sqlResultSetMappingRegistrations == null ) {
			sqlResultSetMappingRegistrations = new HashMap<>();
		}

		jaxbSqlResultSetMappings.forEach( (jaxbMapping) -> {
			final SqlResultSetMappingJpaAnnotation annotation = JpaAnnotations.SQL_RESULT_SET_MAPPING.createUsage( sourceModelContext );
			annotation.name( jaxbMapping.getName() );

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

			sqlResultSetMappingRegistrations.put(
					jaxbMapping.getName(),
					new SqlResultSetMappingRegistration( jaxbMapping.getName(), annotation )
			);
		} );
	}

	private void collectNamedQueries(List<JaxbNamedHqlQueryImpl> jaxbNamedQueries, XmlDocumentContext xmlDocumentContext) {
		if ( isEmpty( jaxbNamedQueries ) ) {
			return;
		}

		if ( namedQueryRegistrations == null ) {
			namedQueryRegistrations = new HashMap<>();
		}

		for ( JaxbNamedHqlQueryImpl jaxbNamedQuery : jaxbNamedQueries ) {
			final NamedQueryJpaAnnotation queryAnnotation = JpaAnnotations.NAMED_QUERY.createUsage( xmlDocumentContext.getModelBuildingContext() );
			namedQueryRegistrations.put(
					jaxbNamedQuery.getName(),
					new NamedQueryRegistration( jaxbNamedQuery.getName(), queryAnnotation )
			);

			queryAnnotation.name( jaxbNamedQuery.getName() );
			queryAnnotation.query( jaxbNamedQuery.getQuery() );

			if ( jaxbNamedQuery.getLockMode() != null ) {
				queryAnnotation.lockMode( jaxbNamedQuery.getLockMode() );
			}

			queryAnnotation.hints( collectQueryHints(
					jaxbNamedQuery,
					xmlDocumentContext
			) );
		}
	}

	private QueryHint[] collectQueryHints(JaxbNamedHqlQueryImpl jaxbNamedQuery, XmlDocumentContext xmlDocumentContext) {
		final List<QueryHint> hints = extractQueryHints( jaxbNamedQuery );

		if ( jaxbNamedQuery.isCacheable() == Boolean.TRUE ) {
			final QueryHintJpaAnnotation cacheableHint = JpaAnnotations.QUERY_HINT.createUsage( xmlDocumentContext.getModelBuildingContext() );
			cacheableHint.name( AvailableHints.HINT_CACHEABLE );
			cacheableHint.value( Boolean.TRUE.toString() );
			hints.add( cacheableHint );

			if ( jaxbNamedQuery.getCacheMode() != null ) {
				final QueryHintJpaAnnotation cacheModeHint = JpaAnnotations.QUERY_HINT.createUsage( xmlDocumentContext.getModelBuildingContext() );
				cacheModeHint.name( AvailableHints.HINT_CACHE_MODE );
				cacheModeHint.value( jaxbNamedQuery.getCacheMode().name() );
				hints.add( cacheModeHint );
			}

			if ( isNotEmpty( jaxbNamedQuery.getCacheRegion() ) ) {
				final QueryHintJpaAnnotation cacheRegionHint = JpaAnnotations.QUERY_HINT.createUsage( xmlDocumentContext.getModelBuildingContext() );
				cacheRegionHint.name( AvailableHints.HINT_CACHE_REGION );
				cacheRegionHint.value( jaxbNamedQuery.getCacheRegion() );
				hints.add( cacheRegionHint );
			}
		}

		if ( isNotEmpty( jaxbNamedQuery.getComment() ) ) {
			final QueryHintJpaAnnotation hint = JpaAnnotations.QUERY_HINT.createUsage( xmlDocumentContext.getModelBuildingContext() );
			hint.name( AvailableHints.HINT_COMMENT );
			hint.value( jaxbNamedQuery.getComment() );
			hints.add( hint );
		}

		if ( jaxbNamedQuery.getFetchSize() != null ) {
			final QueryHintJpaAnnotation hint = JpaAnnotations.QUERY_HINT.createUsage( xmlDocumentContext.getModelBuildingContext() );
			hint.name( AvailableHints.HINT_FETCH_SIZE );
			hint.value( jaxbNamedQuery.getFetchSize().toString() );
			hints.add( hint );
		}

		if ( jaxbNamedQuery.isReadOnly() == Boolean.TRUE ) {
			final QueryHintJpaAnnotation hint = JpaAnnotations.QUERY_HINT.createUsage( xmlDocumentContext.getModelBuildingContext() );
			hint.name( AvailableHints.HINT_READ_ONLY );
			hint.value( Boolean.TRUE.toString() );
			hints.add( hint );
		}

		if ( jaxbNamedQuery.getFlushMode() != null ) {
			final QueryHintJpaAnnotation hint = JpaAnnotations.QUERY_HINT.createUsage( xmlDocumentContext.getModelBuildingContext() );
			hint.name( AvailableHints.HINT_FLUSH_MODE );
			hint.value( jaxbNamedQuery.getFlushMode().name() );
			hints.add( hint );
		}

		return hints.toArray(QueryHint[]::new);
	}

	private void collectNamedNativeQueries(List<JaxbNamedNativeQueryImpl> namedNativeQueries, XmlDocumentContext xmlDocumentContext) {
		if ( isEmpty( namedNativeQueries ) ) {
			return;
		}

		if ( namedNativeQueryRegistrations == null ) {
			namedNativeQueryRegistrations = new HashMap<>();
		}

		for ( JaxbNamedNativeQueryImpl jaxbNamedQuery : namedNativeQueries ) {
			final NamedNativeQueryJpaAnnotation queryAnnotation = JpaAnnotations.NAMED_NATIVE_QUERY.createUsage( xmlDocumentContext.getModelBuildingContext() );
			namedNativeQueryRegistrations.put(
					jaxbNamedQuery.getName(),
					new NamedNativeQueryRegistration( jaxbNamedQuery.getName(), queryAnnotation )
			);

			queryAnnotation.name( jaxbNamedQuery.getName() );
			queryAnnotation.query( jaxbNamedQuery.getQuery() );

			if ( isNotEmpty( jaxbNamedQuery.getResultClass() ) ) {
				final ClassDetails resultClassDetails = toClassDetails( jaxbNamedQuery.getResultClass() );
				queryAnnotation.resultClass( resultClassDetails.toJavaClass() );
			}

			applyResultSetMapping( jaxbNamedQuery, queryAnnotation, xmlDocumentContext );

			queryAnnotation.hints( collectNativeQueryHints( jaxbNamedQuery ) );
		}
	}

	private void applyResultSetMapping(
			JaxbNamedNativeQueryImpl jaxbNamedQuery,
			NamedNativeQueryJpaAnnotation queryAnnotation,
			XmlDocumentContext xmlDocumentContext) {
		if ( isNotEmpty( jaxbNamedQuery.getResultSetMapping() ) ) {
			queryAnnotation.resultSetMapping( jaxbNamedQuery.getResultSetMapping() );
			return;
		}

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

	private QueryHint[] collectNativeQueryHints(JaxbNamedNativeQueryImpl jaxbNamedQuery) {
		final List<QueryHint> hints = extractQueryHints( jaxbNamedQuery );

		if ( jaxbNamedQuery.isCacheable() == Boolean.TRUE ) {
			final QueryHintJpaAnnotation cacheableHint = JpaAnnotations.QUERY_HINT.createUsage( sourceModelContext );
			cacheableHint.name( AvailableHints.HINT_CACHEABLE );
			cacheableHint.value( Boolean.TRUE.toString() );
			hints.add( cacheableHint );

			if ( jaxbNamedQuery.getCacheMode() != null ) {
				final QueryHintJpaAnnotation cacheModeHint = JpaAnnotations.QUERY_HINT.createUsage( sourceModelContext );
				cacheModeHint.name( AvailableHints.HINT_CACHE_MODE );
				cacheModeHint.value( jaxbNamedQuery.getCacheMode().name() );
				hints.add( cacheModeHint );
			}

			if ( isNotEmpty( jaxbNamedQuery.getCacheRegion() ) ) {
				final QueryHintJpaAnnotation cacheRegionHint = JpaAnnotations.QUERY_HINT.createUsage( sourceModelContext );
				cacheRegionHint.name( AvailableHints.HINT_CACHE_REGION );
				cacheRegionHint.value( jaxbNamedQuery.getCacheRegion() );
				hints.add( cacheRegionHint );
			}
		}

		if ( jaxbNamedQuery.isReadOnly() == Boolean.TRUE ) {
			final QueryHintJpaAnnotation hint = JpaAnnotations.QUERY_HINT.createUsage( sourceModelContext );
			hint.name( AvailableHints.HINT_READ_ONLY );
			hint.value( Boolean.TRUE.toString() );
			hints.add( hint );
		}

		if ( isNotEmpty( jaxbNamedQuery.getComment() ) ) {
			final QueryHintJpaAnnotation hint = JpaAnnotations.QUERY_HINT.createUsage( sourceModelContext );
			hint.name( AvailableHints.HINT_COMMENT );
			hint.value( jaxbNamedQuery.getComment() );
			hints.add( hint );
		}

		return hints.toArray(QueryHint[]::new);
	}

	private void collectStoredProcedureQueries(
			List<JaxbNamedStoredProcedureQueryImpl> namedProcedureQueries,
			XmlDocumentContext xmlDocumentContext) {
		if ( isEmpty( namedProcedureQueries ) ) {
			return;
		}

		if ( namedStoredProcedureQueryRegistrations == null ) {
			namedStoredProcedureQueryRegistrations = new HashMap<>();
		}

		for ( JaxbNamedStoredProcedureQueryImpl jaxbQuery : namedProcedureQueries ) {
			final NamedStoredProcedureQueryJpaAnnotation queryAnnotation = NAMED_STORED_PROCEDURE_QUERY.createUsage( sourceModelContext );
			namedStoredProcedureQueryRegistrations.put(
					jaxbQuery.getName(),
					new NamedStoredProcedureQueryRegistration( jaxbQuery.getName(), queryAnnotation )
			);

			queryAnnotation.name( jaxbQuery.getName() );
			queryAnnotation.procedureName( jaxbQuery.getProcedureName() );

			queryAnnotation.resultClasses( collectResultClasses(
					jaxbQuery.getResultClasses(),
					xmlDocumentContext
			) );

			if ( CollectionHelper.isNotEmpty( jaxbQuery.getResultSetMappings() ) ) {
				queryAnnotation.resultSetMappings( jaxbQuery.getResultSetMappings().toArray(String[]::new) );
			}

			queryAnnotation.hints( extractQueryHints( jaxbQuery ).toArray(QueryHint[]::new) );

			queryAnnotation.parameters( QueryProcessing.collectParameters(
					jaxbQuery.getProcedureParameters(),
					xmlDocumentContext
			) );
		}
	}

	private List<QueryHint> extractQueryHints(JaxbQueryHintContainer jaxbQuery) {
		final List<QueryHint> hints = new ArrayList<>();
		for ( JaxbQueryHint jaxbHint : jaxbQuery.getHints() ) {
			final QueryHintJpaAnnotation hint = JpaAnnotations.QUERY_HINT.createUsage( sourceModelContext );
			hint.name( jaxbHint.getName() );
			hint.value( jaxbHint.getValue() );
		}
		return hints;
	}
}
