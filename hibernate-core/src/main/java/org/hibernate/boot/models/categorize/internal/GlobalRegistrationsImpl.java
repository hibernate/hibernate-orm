/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.internal;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Imported;
import org.hibernate.annotations.ParamDef;
import org.hibernate.annotations.Parameter;
import org.hibernate.boot.jaxb.mapping.spi.JaxbCollectionUserTypeRegistrationImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbColumnResultImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbCompositeUserTypeRegistrationImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbConfigurationParameterImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbConstructorResultImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbConverterImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbConverterRegistrationImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEmbeddableInstantiatorRegistrationImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityListenerImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityResultImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbFieldResultImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbFilterDefImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbGenericIdGeneratorImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbJavaTypeRegistrationImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbJdbcTypeRegistrationImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbNamedNativeQueryImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbNamedQueryBase;
import org.hibernate.boot.jaxb.mapping.spi.JaxbNamedQueryImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbNamedStoredProcedureQueryImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbQueryHint;
import org.hibernate.boot.jaxb.mapping.spi.JaxbSequenceGeneratorImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbSqlResultSetMappingImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbStoredProcedureParameterImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbTableGeneratorImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbUserTypeRegistrationImpl;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.categorize.spi.CollectionTypeRegistration;
import org.hibernate.boot.models.categorize.spi.CompositeUserTypeRegistration;
import org.hibernate.boot.models.categorize.spi.ConversionRegistration;
import org.hibernate.boot.models.categorize.spi.ConverterRegistration;
import org.hibernate.boot.models.categorize.spi.EmbeddableInstantiatorRegistration;
import org.hibernate.boot.models.categorize.spi.FilterDefRegistration;
import org.hibernate.boot.models.categorize.spi.GenericGeneratorRegistration;
import org.hibernate.boot.models.categorize.spi.GlobalRegistrations;
import org.hibernate.boot.models.categorize.spi.JavaTypeRegistration;
import org.hibernate.boot.models.categorize.spi.JdbcTypeRegistration;
import org.hibernate.boot.models.categorize.spi.JpaEventListener;
import org.hibernate.boot.models.categorize.spi.JpaEventListenerStyle;
import org.hibernate.boot.models.categorize.spi.NamedNativeQueryRegistration;
import org.hibernate.boot.models.categorize.spi.NamedQueryRegistration;
import org.hibernate.boot.models.categorize.spi.NamedStoredProcedureQueryRegistration;
import org.hibernate.boot.models.categorize.spi.SequenceGeneratorRegistration;
import org.hibernate.boot.models.categorize.spi.SqlResultSetMappingRegistration;
import org.hibernate.boot.models.categorize.spi.TableGeneratorRegistration;
import org.hibernate.boot.models.categorize.spi.UserTypeRegistration;
import org.hibernate.boot.models.xml.internal.XmlAnnotationHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.jpa.AvailableHints;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.models.spi.AnnotationDescriptor;
import org.hibernate.models.spi.AnnotationDescriptorRegistry;
import org.hibernate.models.spi.AnnotationTarget;
import org.hibernate.models.spi.AnnotationUsage;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.MutableAnnotationUsage;
import org.hibernate.models.spi.SourceModelBuildingContext;
import org.hibernate.models.spi.SourceModelContext;

import jakarta.persistence.ColumnResult;
import jakarta.persistence.ConstructorResult;
import jakarta.persistence.EntityResult;
import jakarta.persistence.FieldResult;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.NamedStoredProcedureQuery;
import jakarta.persistence.QueryHint;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.StoredProcedureParameter;
import jakarta.persistence.TableGenerator;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.hibernate.boot.models.HibernateAnnotations.COLLECTION_TYPE_REG;
import static org.hibernate.boot.models.HibernateAnnotations.COMPOSITE_TYPE_REG;
import static org.hibernate.boot.models.HibernateAnnotations.CONVERTER_REG;
import static org.hibernate.boot.models.HibernateAnnotations.EMBEDDABLE_INSTANTIATOR_REG;
import static org.hibernate.boot.models.HibernateAnnotations.FILTER_DEF;
import static org.hibernate.boot.models.HibernateAnnotations.JAVA_TYPE_REG;
import static org.hibernate.boot.models.HibernateAnnotations.JDBC_TYPE_REG;
import static org.hibernate.boot.models.HibernateAnnotations.TYPE_REG;
import static org.hibernate.internal.util.collections.CollectionHelper.arrayList;
import static org.hibernate.internal.util.collections.CollectionHelper.isEmpty;

/**
 * @author Steve Ebersole
 */
public class GlobalRegistrationsImpl implements GlobalRegistrations {
	private final ClassDetailsRegistry classDetailsRegistry;
	private final AnnotationDescriptorRegistry descriptorRegistry;

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

	public GlobalRegistrationsImpl(SourceModelContext sourceModelContext) {
		this( sourceModelContext.getClassDetailsRegistry(), sourceModelContext.getAnnotationDescriptorRegistry() );
	}

	public GlobalRegistrationsImpl(ClassDetailsRegistry classDetailsRegistry, AnnotationDescriptorRegistry descriptorRegistry) {
		this.classDetailsRegistry = classDetailsRegistry;
		this.descriptorRegistry = descriptorRegistry;
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
		annotationTarget.forEachAnnotationUsage( JAVA_TYPE_REG, (usage) -> collectJavaTypeRegistration(
				usage.getAttributeValue( "javaType" ),
				usage.getAttributeValue( "descriptorClass" )
		) );
	}

	public void collectJavaTypeRegistrations(List<JaxbJavaTypeRegistrationImpl> registrations) {
		if ( CollectionHelper.isEmpty( registrations ) ) {
			return;
		}

		registrations.forEach( (reg) -> collectJavaTypeRegistration(
				classDetailsRegistry.resolveClassDetails( reg.getClazz() ),
				classDetailsRegistry.resolveClassDetails( reg.getDescriptor() )
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
		annotationTarget.forEachAnnotationUsage( JDBC_TYPE_REG, (usage) -> collectJdbcTypeRegistration(
				usage.getAttributeValue( "registrationCode" ),
				usage.getAttributeValue( "value" )
		) );
	}

	public void collectJdbcTypeRegistrations(List<JaxbJdbcTypeRegistrationImpl> registrations) {
		if ( CollectionHelper.isEmpty( registrations ) ) {
			return;
		}

		registrations.forEach( (reg) -> collectJdbcTypeRegistration(
				reg.getCode(),
				classDetailsRegistry.resolveClassDetails( reg.getDescriptor() )
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
		annotationTarget.forEachAnnotationUsage( CONVERTER_REG, (usage) -> {
			final ClassDetails domainType = usage.getAttributeValue( "domainType" );
			final ClassDetails converterType = usage.getAttributeValue( "converter" );
			final boolean autoApply = usage.getAttributeValue( "autoApply" );
			collectConverterRegistration( new ConversionRegistration( domainType, converterType, autoApply, CONVERTER_REG ) );
		} );
	}

	public void collectConverterRegistrations(List<JaxbConverterRegistrationImpl> registrations) {
		if ( CollectionHelper.isEmpty( registrations ) ) {
			return;
		}

		registrations.forEach( (registration) -> {
			final ClassDetails explicitDomainType;
			final String explicitDomainTypeName = registration.getClazz();
			if ( StringHelper.isNotEmpty( explicitDomainTypeName ) ) {
				explicitDomainType = classDetailsRegistry.resolveClassDetails( explicitDomainTypeName );
			}
			else {
				explicitDomainType = null;
			}
			final ClassDetails converterType = classDetailsRegistry.resolveClassDetails( registration.getConverter() );
			final boolean autoApply = registration.isAutoApply();
			collectConverterRegistration( new ConversionRegistration( explicitDomainType, converterType, autoApply, CONVERTER_REG ) );
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
		annotationTarget.forEachAnnotationUsage( TYPE_REG, (usage) -> collectUserTypeRegistration(
				usage.getAttributeValue( "basicClass" ),
				usage.getAttributeValue( "userType" )
		) );
	}

	public void collectUserTypeRegistrations(List<JaxbUserTypeRegistrationImpl> registrations) {
		if ( CollectionHelper.isEmpty( registrations ) ) {
			return;
		}

		registrations.forEach( (reg) -> {
			final ClassDetails domainTypeDetails = classDetailsRegistry.resolveClassDetails( reg.getClazz() );
			final ClassDetails descriptorDetails = classDetailsRegistry.resolveClassDetails( reg.getDescriptor() );
			collectUserTypeRegistration( domainTypeDetails, descriptorDetails );
		} );
	}

	public void collectUserTypeRegistration(ClassDetails domainClass, ClassDetails userTypeClass) {
		if ( userTypeRegistrations == null ) {
			userTypeRegistrations = new ArrayList<>();
		}
		userTypeRegistrations.add( new UserTypeRegistration( domainClass, userTypeClass ) );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// CompositeUserTypeRegistration

	public void collectCompositeUserTypeRegistrations(AnnotationTarget annotationTarget) {
		annotationTarget.forEachAnnotationUsage( COMPOSITE_TYPE_REG, (usage) -> collectCompositeUserTypeRegistration(
				usage.getAttributeValue( "embeddableClass" ),
				usage.getAttributeValue( "userType" )
		) );
	}

	public void collectCompositeUserTypeRegistrations(List<JaxbCompositeUserTypeRegistrationImpl> registrations) {
		if ( CollectionHelper.isEmpty( registrations ) ) {
			return;
		}

		registrations.forEach( (reg) -> collectCompositeUserTypeRegistration(
				classDetailsRegistry.resolveClassDetails( reg.getClazz() ),
				classDetailsRegistry.resolveClassDetails( reg.getDescriptor() )
		) );
	}

	public void collectCompositeUserTypeRegistration(ClassDetails domainClass, ClassDetails userTypeClass) {
		if ( compositeUserTypeRegistrations == null ) {
			compositeUserTypeRegistrations = new ArrayList<>();
		}
		compositeUserTypeRegistrations.add( new CompositeUserTypeRegistration( domainClass, userTypeClass ) );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// CollectionTypeRegistration

	public void collectCollectionTypeRegistrations(AnnotationTarget annotationTarget) {
		annotationTarget.forEachAnnotationUsage( COLLECTION_TYPE_REG, (usage) -> collectCollectionTypeRegistration(
				usage.getAttributeValue( "classification" ),
				usage.getAttributeValue( "type" ),
				extractParameterMap( usage )
		) );
	}

	private Map<String,String> extractParameterMap(AnnotationUsage<? extends Annotation> source) {
		final List<AnnotationUsage<Parameter>> parameters = source.getAttributeValue( "parameters" );

		final Map<String,String> result = new HashMap<>();
		for ( AnnotationUsage<Parameter> parameter : parameters ) {
			result.put(
					parameter.getAttributeValue( "name" ),
					parameter.getAttributeValue( "value" )
			);
		}
		return result;
	}

	public void collectCollectionTypeRegistrations(List<JaxbCollectionUserTypeRegistrationImpl> registrations) {
		if ( CollectionHelper.isEmpty( registrations ) ) {
			return;
		}

		registrations.forEach( (reg) -> collectCollectionTypeRegistration(
				reg.getClassification(),
				classDetailsRegistry.resolveClassDetails( reg.getDescriptor() ),
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


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// EmbeddableInstantiatorRegistration

	public void collectEmbeddableInstantiatorRegistrations(AnnotationTarget annotationTarget) {
		annotationTarget.forEachAnnotationUsage( EMBEDDABLE_INSTANTIATOR_REG, (usage) -> collectEmbeddableInstantiatorRegistration(
				usage.getAttributeValue( "embeddableClass" ),
				usage.getAttributeValue( "instantiator" )
		) );
	}

	public void collectEmbeddableInstantiatorRegistrations(List<JaxbEmbeddableInstantiatorRegistrationImpl> registrations) {
		if ( CollectionHelper.isEmpty( registrations ) ) {
			return;
		}

		registrations.forEach( (reg) -> collectEmbeddableInstantiatorRegistration(
				classDetailsRegistry.resolveClassDetails( reg.getEmbeddableClass() ),
				classDetailsRegistry.resolveClassDetails( reg.getInstantiator() )
		) );
	}

	public void collectEmbeddableInstantiatorRegistration(ClassDetails embeddableClass, ClassDetails instantiator) {
		if ( embeddableInstantiatorRegistrations == null ) {
			embeddableInstantiatorRegistrations = new ArrayList<>();
		}
		embeddableInstantiatorRegistrations.add( new EmbeddableInstantiatorRegistration( embeddableClass, instantiator ) );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Filter-defs

	public void collectFilterDefinitions(AnnotationTarget annotationTarget) {
		annotationTarget.forEachAnnotationUsage( FILTER_DEF, (usage) -> collectFilterDefinition(
				usage.getAttributeValue( "name" ),
				usage.getAttributeValue( "defaultCondition" ),
				extractFilterParameters( usage )
		) );
	}

	private Map<String, ClassDetails> extractFilterParameters(AnnotationUsage<FilterDef> source) {
		final List<AnnotationUsage<ParamDef>> parameters = source.getAttributeValue( "parameters" );
		if ( isEmpty( parameters ) ) {
			return null;
		}

		final Map<String, ClassDetails> result = new HashMap<>( parameters.size() );
		for ( AnnotationUsage<ParamDef> parameter : parameters ) {
			result.put( parameter.getAttributeValue( "name" ), parameter.getAttributeValue( "type" ) );
		}
		return result;
	}

	public void collectFilterDefinitions(List<JaxbFilterDefImpl> filterDefinitions) {
		if ( CollectionHelper.isEmpty( filterDefinitions ) ) {
			return;
		}

		filterDefinitions.forEach( (filterDefinition) -> collectFilterDefinition(
				filterDefinition.getName(),
				filterDefinition.getDefaultCondition(),
				extractFilterParameters( filterDefinition )
		) );
	}

	private Map<String, ClassDetails> extractFilterParameters(JaxbFilterDefImpl source) {
		final List<JaxbFilterDefImpl.JaxbFilterParamImpl> parameters = source.getFilterParams();
		if ( isEmpty( parameters ) ) {
			return null;
		}

		final Map<String, ClassDetails> result = new HashMap<>( parameters.size() );
		for ( JaxbFilterDefImpl.JaxbFilterParamImpl parameter : parameters ) {
			// for now, don't check whether nothing was specified; this situation
			// should resolve to Object - let's see how that reacts
			final ClassDetails targetClassDetails = XmlAnnotationHelper.resolveJavaType(
					parameter.getType(),
					classDetailsRegistry
			);
			result.put( parameter.getName(), targetClassDetails );
		}
		return result;
	}

	public void collectFilterDefinition(String name, String defaultCondition, Map<String, ClassDetails> parameters) {
		if ( filterDefRegistrations == null ) {
			filterDefRegistrations = new HashMap<>();
		}

		final FilterDefRegistration previousEntry = filterDefRegistrations.put(
				name,
				new FilterDefRegistration( name, defaultCondition, parameters )
		);
		if ( previousEntry != null ) {
			// legacy code simply allows the collision overwriting the previous
			// todo (jpa32) : re-enable this, especially if the conditions differ
			//throw new AnnotationException( "Multiple '@FilterDef' annotations define a filter named '" + name + "'" );
		}
	}

	public void collectImportRename(ClassDetails classDetails) {
		final AnnotationUsage<Imported> importedUsage = classDetails.getAnnotationUsage( Imported.class );

		if ( importedUsage == null ) {
			return;
		}

		final String explicitRename = importedUsage.getString( "rename" );
		final String rename = StringHelper.isNotEmpty( explicitRename )
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

	public void collectEntityListenerRegistrations(List<JaxbEntityListenerImpl> listeners, SourceModelBuildingContext modelsContext) {
		if ( CollectionHelper.isEmpty( listeners ) ) {
			return;
		}

		listeners.forEach( (jaxbEntityListener) -> {
			final ClassDetails classDetails = classDetailsRegistry.resolveClassDetails( jaxbEntityListener.getClazz() );
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
		classDetails.forEachAnnotationUsage( SequenceGenerator.class, this::collectSequenceGenerator );
		classDetails.forEachAnnotationUsage( TableGenerator.class, this::collectTableGenerator );
		classDetails.forEachAnnotationUsage( GenericGenerator.class, this::collectGenericGenerator );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Sequence generator

	public void collectSequenceGenerators(List<JaxbSequenceGeneratorImpl> sequenceGenerators) {
		if ( CollectionHelper.isEmpty( sequenceGenerators ) ) {
			return;
		}

		sequenceGenerators.forEach( (generator) -> {
			final MutableAnnotationUsage<SequenceGenerator> annotationUsage = makeAnnotation( JpaAnnotations.SEQUENCE_GENERATOR );
			annotationUsage.setAttributeValue( "name", generator.getName() );
			annotationUsage.setAttributeValue( "sequenceName", generator.getSequenceName() );
			annotationUsage.setAttributeValue( "catalog", generator.getCatalog() );
			annotationUsage.setAttributeValue( "schema", generator.getSchema() );
			annotationUsage.setAttributeValue( "initialValue", generator.getInitialValue() );
			annotationUsage.setAttributeValue( "allocationSize", generator.getAllocationSize() );

			collectSequenceGenerator( new SequenceGeneratorRegistration( generator.getName(), annotationUsage ) );
		} );
	}

	private <A extends Annotation> MutableAnnotationUsage<A> makeAnnotation(AnnotationDescriptor<A> annotationDescriptor) {
		return annotationDescriptor.createUsage( null, null );
	}

	public void collectSequenceGenerator(AnnotationUsage<SequenceGenerator> usage) {
		collectSequenceGenerator( new SequenceGeneratorRegistration( usage.getAttributeValue( "name" ), usage ) );
	}

	public void collectSequenceGenerator(SequenceGeneratorRegistration generatorRegistration) {
		if ( sequenceGeneratorRegistrations == null ) {
			sequenceGeneratorRegistrations = new HashMap<>();
		}

		sequenceGeneratorRegistrations.put( generatorRegistration.name(), generatorRegistration );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Table generator

	public void collectTableGenerators(List<JaxbTableGeneratorImpl> tableGenerators) {
		if ( CollectionHelper.isEmpty( tableGenerators ) ) {
			return;
		}

		tableGenerators.forEach( (generator) -> {
			final MutableAnnotationUsage<TableGenerator> annotationUsage = makeAnnotation( JpaAnnotations.TABLE_GENERATOR );
			annotationUsage.setAttributeValue( "name", generator.getName() );
			annotationUsage.setAttributeValue( "table", generator.getTable() );
			annotationUsage.setAttributeValue( "catalog", generator.getCatalog() );
			annotationUsage.setAttributeValue( "schema", generator.getSchema() );
			annotationUsage.setAttributeValue( "pkColumnName", generator.getPkColumnName() );
			annotationUsage.setAttributeValue( "valueColumnName", generator.getValueColumnName() );
			annotationUsage.setAttributeValue( "pkColumnValue", generator.getPkColumnValue() );
			annotationUsage.setAttributeValue( "initialValue", generator.getInitialValue() );
			annotationUsage.setAttributeValue( "allocationSize", generator.getAllocationSize() );

			collectTableGenerator( new TableGeneratorRegistration( generator.getName(), annotationUsage ) );
		} );
	}

	public void collectTableGenerator(AnnotationUsage<TableGenerator> usage) {
		collectTableGenerator( new TableGeneratorRegistration( usage.getAttributeValue( "name" ), usage ) );
	}

	public void collectTableGenerator(TableGeneratorRegistration generatorRegistration) {
		if ( tableGeneratorRegistrations == null ) {
			tableGeneratorRegistrations = new HashMap<>();
		}

		tableGeneratorRegistrations.put( generatorRegistration.name(), generatorRegistration );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Generic generators

	private void collectGenericGenerators(List<JaxbGenericIdGeneratorImpl> genericGenerators) {
		if ( CollectionHelper.isEmpty( genericGenerators ) ) {
			return;
		}

		genericGenerators.forEach( (generator) -> {
			final MutableAnnotationUsage<GenericGenerator> annotationUsage = makeAnnotation( HibernateAnnotations.GENERIC_GENERATOR );
			annotationUsage.setAttributeValue( "name", generator.getName() );
			annotationUsage.setAttributeValue( "strategy", generator.getClazz() );

			// todo : update the mapping.xsd to account for new @GenericGenerator definition

			collectGenericGenerator( new GenericGeneratorRegistration( generator.getName(), annotationUsage ) );
		} );
	}

	public void collectGenericGenerator(AnnotationUsage<GenericGenerator> usage) {
		collectGenericGenerator( new GenericGeneratorRegistration( usage.getAttributeValue( "name" ), usage ) );
	}

	public void collectGenericGenerator(GenericGeneratorRegistration generatorRegistration) {
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
			final ClassDetails converterType = classDetailsRegistry.resolveClassDetails( converterClassName );
			final boolean autoApply = jaxbConverter.isAutoApply();

			jpaConverters.add( new ConverterRegistration( converterType, autoApply ) );
		} );
	}

	public void collectQueryReferences(JaxbEntityMappingsImpl jaxbRoot) {
		collectNamedSqlResultSetMappings( jaxbRoot.getSqlResultSetMappings() );
		collectNamedQueries( jaxbRoot.getNamedQueries() );
		collectNamedNativeQueries( jaxbRoot.getNamedNativeQueries() );
		collectStoredProcedureQueries( jaxbRoot.getNamedProcedureQueries() );
	}

	private void collectNamedSqlResultSetMappings(List<JaxbSqlResultSetMappingImpl> jaxbSqlResultSetMappings) {
		if ( isEmpty( jaxbSqlResultSetMappings ) ) {
			return;
		}

		if ( sqlResultSetMappingRegistrations == null ) {
			sqlResultSetMappingRegistrations = new HashMap<>();
		}

		jaxbSqlResultSetMappings.forEach( (jaxbMapping) -> {
			final MutableAnnotationUsage<SqlResultSetMapping> mappingAnnotation = JpaAnnotations.SQL_RESULT_SET_MAPPING.createUsage( null, null );
			mappingAnnotation.setAttributeValue( "name", jaxbMapping.getName() );

			sqlResultSetMappingRegistrations.put(
					jaxbMapping.getName(),
					new SqlResultSetMappingRegistration( jaxbMapping.getName(), mappingAnnotation )
			);

			applyEntityResults(
					jaxbMapping.getEntityResult(),
					(results) -> mappingAnnotation.setAttributeValue( "entities", results )
			);

			applyConstructorResults(
					jaxbMapping.getConstructorResult(),
					(results) -> mappingAnnotation.setAttributeValue( "classes", results )
			);

			applyColumnResults(
					jaxbMapping.getColumnResult(),
					(columnResults) -> mappingAnnotation.setAttributeValue( "columns", columnResults )
			);
		} );
	}

	private void applyEntityResults(
			List<JaxbEntityResultImpl> jaxbEntityResults,
			Consumer<List<AnnotationUsage<EntityResult>>> annotationListConsumer) {
		if ( jaxbEntityResults.isEmpty() ) {
			return;
		}

		final List<AnnotationUsage<EntityResult>> entityResults = arrayList( jaxbEntityResults.size() );

		for ( JaxbEntityResultImpl jaxbEntityResult : jaxbEntityResults ) {
			final MutableAnnotationUsage<EntityResult> entityResultAnnotation = makeAnnotation( JpaAnnotations.ENTITY_RESULT );
			entityResults.add( entityResultAnnotation );

			entityResultAnnotation.setAttributeValue( "entityClass", classDetailsRegistry.resolveClassDetails( jaxbEntityResult.getEntityClass() ) );
			entityResultAnnotation.setAttributeValue( "lockMode", jaxbEntityResult.getLockMode() );
			entityResultAnnotation.setAttributeValue( "discriminatorColumn", jaxbEntityResult.getDiscriminatorColumn() );

			if ( !jaxbEntityResult.getFieldResult().isEmpty() ) {
				final List<AnnotationUsage<FieldResult>> fieldResults = arrayList( jaxbEntityResult.getFieldResult().size() );
				entityResultAnnotation.setAttributeValue( "fields", fieldResults );

				for ( JaxbFieldResultImpl jaxbFieldResult : jaxbEntityResult.getFieldResult() ) {
					final MutableAnnotationUsage<FieldResult> fieldResultAnnotation = makeAnnotation( JpaAnnotations.FIELD_RESULT );
					fieldResultAnnotation.setAttributeValue( "name", jaxbFieldResult.getName() );
					fieldResultAnnotation.setAttributeValue( "column", jaxbFieldResult.getColumn() );
				}
			}
		}
		annotationListConsumer.accept( entityResults );
	}

	private void applyConstructorResults(
			List<JaxbConstructorResultImpl> jaxbConstructorResults,
			Consumer<List<AnnotationUsage<ConstructorResult>>> annotationListConsumer) {
		if ( isEmpty( jaxbConstructorResults ) ) {
			return;
		}

		final List<AnnotationUsage<ConstructorResult>> results = arrayList( jaxbConstructorResults.size() );
		for ( JaxbConstructorResultImpl jaxbConstructorResult : jaxbConstructorResults ) {
			final MutableAnnotationUsage<ConstructorResult> result = makeAnnotation( JpaAnnotations.CONSTRUCTOR_RESULT );
			results.add( result );

			result.setAttributeValue(
					"entityClass",
					classDetailsRegistry.resolveClassDetails( jaxbConstructorResult.getTargetClass() )
			);

			if ( !jaxbConstructorResult.getColumns().isEmpty() ) {
				applyColumnResults(
						jaxbConstructorResult.getColumns(),
						(columnResults) -> result.setAttributeValue( "columns", columnResults )
				);
			}
		}
	}

	private void applyColumnResults(
			List<JaxbColumnResultImpl> jaxbColumnResults,
			Consumer<List<AnnotationUsage<ColumnResult>>> annotationListConsumer) {
		if ( isEmpty( jaxbColumnResults ) ) {
			return;
		}

		final List<AnnotationUsage<ColumnResult>> columnResults = arrayList( jaxbColumnResults.size() );
		for ( JaxbColumnResultImpl jaxbColumn : jaxbColumnResults ) {
			final MutableAnnotationUsage<ColumnResult> columnResultAnnotation = makeAnnotation( JpaAnnotations.COLUMN_RESULT );
			columnResults.add( columnResultAnnotation );

			columnResultAnnotation.setAttributeValue( "name", jaxbColumn.getName() );
			columnResultAnnotation.setAttributeValue( "type", classDetailsRegistry.resolveClassDetails( jaxbColumn.getClazz() ) );
		}
		annotationListConsumer.accept( columnResults );
	}

	private void collectNamedQueries(List<JaxbNamedQueryImpl> jaxbNamedQueries) {
		if ( isEmpty( jaxbNamedQueries ) ) {
			return;
		}

		if ( namedQueryRegistrations == null ) {
			namedQueryRegistrations = new HashMap<>();
		}

		for ( JaxbNamedQueryImpl jaxbNamedQuery : jaxbNamedQueries ) {
			final MutableAnnotationUsage<NamedQuery> queryAnnotation = makeAnnotation( JpaAnnotations.NAMED_QUERY );
			namedQueryRegistrations.put(
					jaxbNamedQuery.getName(),
					new NamedQueryRegistration( jaxbNamedQuery.getName(), queryAnnotation )
			);

			queryAnnotation.setAttributeValue( "name", jaxbNamedQuery.getName() );
			queryAnnotation.setAttributeValue( "query", jaxbNamedQuery.getQuery() );
			queryAnnotation.setAttributeValue( "lockMode", jaxbNamedQuery.getLockMode() );

			final List<AnnotationUsage<QueryHint>> hints = extractQueryHints( jaxbNamedQuery );
			queryAnnotation.setAttributeValue( "hints", hints );

			if ( jaxbNamedQuery.isCacheable() == Boolean.TRUE ) {
				final MutableAnnotationUsage<QueryHint> cacheableHint = makeAnnotation( JpaAnnotations.QUERY_HINT );
				cacheableHint.setAttributeValue( "name", AvailableHints.HINT_CACHEABLE );
				cacheableHint.setAttributeValue( "value", Boolean.TRUE.toString() );

				if ( jaxbNamedQuery.getCacheMode() != null ) {
					final MutableAnnotationUsage<QueryHint> hint = makeAnnotation( JpaAnnotations.QUERY_HINT );
					hint.setAttributeValue( "name", AvailableHints.HINT_CACHE_MODE );
					hint.setAttributeValue( "value", jaxbNamedQuery.getCacheMode().name() );
				}

				if ( StringHelper.isNotEmpty( jaxbNamedQuery.getCacheRegion() ) ) {
					final MutableAnnotationUsage<QueryHint> hint = makeAnnotation( JpaAnnotations.QUERY_HINT );
					hint.setAttributeValue( "name", AvailableHints.HINT_CACHE_REGION );
					hint.setAttributeValue( "value", jaxbNamedQuery.getCacheRegion() );
				}
			}

			if ( StringHelper.isNotEmpty( jaxbNamedQuery.getComment() ) ) {
				final MutableAnnotationUsage<QueryHint> hint = makeAnnotation( JpaAnnotations.QUERY_HINT );
				hint.setAttributeValue( "name", AvailableHints.HINT_COMMENT );
				hint.setAttributeValue( "value", jaxbNamedQuery.getComment() );
			}

			if ( jaxbNamedQuery.getFetchSize() != null ) {
				final MutableAnnotationUsage<QueryHint> hint = makeAnnotation( JpaAnnotations.QUERY_HINT );
				hint.setAttributeValue( "name", AvailableHints.HINT_FETCH_SIZE );
				hint.setAttributeValue( "value", jaxbNamedQuery.getFetchSize().toString() );
			}

			if ( jaxbNamedQuery.isReadOnly() == Boolean.TRUE ) {
				final MutableAnnotationUsage<QueryHint> hint = makeAnnotation( JpaAnnotations.QUERY_HINT );
				hint.setAttributeValue( "name", AvailableHints.HINT_READ_ONLY );
				hint.setAttributeValue( "value", Boolean.TRUE.toString() );
			}

			if ( jaxbNamedQuery.getFlushMode() != null ) {
				final MutableAnnotationUsage<QueryHint> hint = makeAnnotation( JpaAnnotations.QUERY_HINT );
				hint.setAttributeValue( "name", AvailableHints.HINT_FLUSH_MODE );
				hint.setAttributeValue( "value", jaxbNamedQuery.getFlushMode().name() );
			}
		}
	}

	private void collectNamedNativeQueries(List<JaxbNamedNativeQueryImpl> namedNativeQueries) {
		if ( isEmpty( namedNativeQueries ) ) {
			return;
		}

		if ( namedNativeQueryRegistrations == null ) {
			namedNativeQueryRegistrations = new HashMap<>();
		}

		for ( JaxbNamedNativeQueryImpl jaxbNamedQuery : namedNativeQueries ) {
			final MutableAnnotationUsage<NamedNativeQuery> queryAnnotation = makeAnnotation( JpaAnnotations.NAMED_NATIVE_QUERY );
			namedNativeQueryRegistrations.put(
					jaxbNamedQuery.getName(),
					new NamedNativeQueryRegistration( jaxbNamedQuery.getName(), queryAnnotation )
			);

			queryAnnotation.setAttributeValue( "name", jaxbNamedQuery.getName() );
			queryAnnotation.setAttributeValue( "query", jaxbNamedQuery.getQuery() );

			if ( StringHelper.isNotEmpty( jaxbNamedQuery.getResultClass() ) ) {
				queryAnnotation.setAttributeValue( "resultClass", classDetailsRegistry.resolveClassDetails( jaxbNamedQuery.getResultClass() ) );
			}

			if ( StringHelper.isNotEmpty( jaxbNamedQuery.getResultSetMapping() ) ) {
				queryAnnotation.setAttributeValue( "resultSetMapping", jaxbNamedQuery.getResultSetMapping() );
			}

			applyEntityResults(
					jaxbNamedQuery.getEntityResult(),
					(results) -> queryAnnotation.setAttributeValue( "entities", results )
			);

			applyConstructorResults(
					jaxbNamedQuery.getConstructorResult(),
					(results) -> queryAnnotation.setAttributeValue( "classes", results )
			);

			applyColumnResults(
					jaxbNamedQuery.getColumnResult(),
					(columnResults) -> queryAnnotation.setAttributeValue( "columns", columnResults )
			);

			final List<AnnotationUsage<QueryHint>> hints = extractQueryHints( jaxbNamedQuery );
			queryAnnotation.setAttributeValue( "hints", hints );

			if ( jaxbNamedQuery.isCacheable() == Boolean.TRUE ) {
				final MutableAnnotationUsage<QueryHint> cacheableHint = makeAnnotation( JpaAnnotations.QUERY_HINT );
				cacheableHint.setAttributeValue( "name", AvailableHints.HINT_CACHEABLE );
				cacheableHint.setAttributeValue( "value", Boolean.TRUE.toString() );

				if ( jaxbNamedQuery.getCacheMode() != null ) {
					final MutableAnnotationUsage<QueryHint> hint = makeAnnotation( JpaAnnotations.QUERY_HINT );
					hint.setAttributeValue( "name", AvailableHints.HINT_CACHE_MODE );
					hint.setAttributeValue( "value", jaxbNamedQuery.getCacheMode().name() );
				}

				if ( StringHelper.isNotEmpty( jaxbNamedQuery.getCacheRegion() ) ) {
					final MutableAnnotationUsage<QueryHint> hint = makeAnnotation( JpaAnnotations.QUERY_HINT );
					hint.setAttributeValue( "name", AvailableHints.HINT_CACHE_REGION );
					hint.setAttributeValue( "value", jaxbNamedQuery.getCacheRegion() );
				}
			}

			if ( jaxbNamedQuery.isReadOnly() == Boolean.TRUE ) {
				final MutableAnnotationUsage<QueryHint> hint = makeAnnotation( JpaAnnotations.QUERY_HINT );
				hint.setAttributeValue( "name", AvailableHints.HINT_READ_ONLY );
				hint.setAttributeValue( "value", Boolean.TRUE.toString() );
			}

			if ( StringHelper.isNotEmpty( jaxbNamedQuery.getComment() ) ) {
				final MutableAnnotationUsage<QueryHint> hint = makeAnnotation( JpaAnnotations.QUERY_HINT );
				hint.setAttributeValue( "name", AvailableHints.HINT_COMMENT );
				hint.setAttributeValue( "value", jaxbNamedQuery.getComment() );
			}
		}
	}

	private void collectStoredProcedureQueries(List<JaxbNamedStoredProcedureQueryImpl> namedProcedureQueries) {
		if ( isEmpty( namedProcedureQueries ) ) {
			return;
		}

		if ( namedStoredProcedureQueryRegistrations == null ) {
			namedStoredProcedureQueryRegistrations = new HashMap<>();
		}

		for ( JaxbNamedStoredProcedureQueryImpl jaxbQuery : namedProcedureQueries ) {
			final MutableAnnotationUsage<NamedStoredProcedureQuery> queryAnnotation = makeAnnotation( JpaAnnotations.NAMED_STORED_PROCEDURE_QUERY );
			namedStoredProcedureQueryRegistrations.put(
					jaxbQuery.getName(),
					new NamedStoredProcedureQueryRegistration( jaxbQuery.getName(), queryAnnotation )
			);

			queryAnnotation.setAttributeValue( "name", jaxbQuery.getName() );
			queryAnnotation.setAttributeValue( "procedureName", jaxbQuery.getProcedureName() );

			final ArrayList<ClassDetails> resultClasses = arrayList( jaxbQuery.getResultClasses().size() );
			queryAnnotation.setAttributeValue( "resultClasses", resultClasses );
			for ( String resultClassName : jaxbQuery.getResultClasses() ) {
				resultClasses.add( classDetailsRegistry.resolveClassDetails( resultClassName ) );
			}

			queryAnnotation.setAttributeValue( "resultSetMappings", jaxbQuery.getResultSetMappings() );

			queryAnnotation.setAttributeValue( "hints", extractQueryHints( jaxbQuery ) );

			final ArrayList<AnnotationUsage<StoredProcedureParameter>> parameters = arrayList( jaxbQuery.getProcedureParameters().size() );
			queryAnnotation.setAttributeValue( "parameters", parameters );
			for ( JaxbStoredProcedureParameterImpl jaxbProcedureParameter : jaxbQuery.getProcedureParameters() ) {
				final MutableAnnotationUsage<StoredProcedureParameter> parameterAnnotation = makeAnnotation( JpaAnnotations.STORED_PROCEDURE_PARAMETER );
				parameters.add( parameterAnnotation );

				parameterAnnotation.setAttributeValue( "name", jaxbProcedureParameter.getName() );
				parameterAnnotation.setAttributeValue( "mode", jaxbProcedureParameter.getMode() );
				parameterAnnotation.setAttributeValue( "type", classDetailsRegistry.resolveClassDetails( jaxbProcedureParameter.getClazz() ) );
			}
		}
	}

	private List<AnnotationUsage<QueryHint>> extractQueryHints(JaxbNamedQueryBase jaxbQuery) {
		final List<AnnotationUsage<QueryHint>> hints = new ArrayList<>();
		for ( JaxbQueryHint jaxbHint : jaxbQuery.getHints() ) {
			final MutableAnnotationUsage<QueryHint> hint = makeAnnotation( JpaAnnotations.QUERY_HINT );
			hint.setAttributeValue( "name", jaxbHint.getName() );
			hint.setAttributeValue( "value", jaxbHint.getValue() );
		}
		return hints;
	}
}
