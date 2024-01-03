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

import org.hibernate.annotations.FilterDef;
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
import org.hibernate.boot.jaxb.mapping.spi.JaxbSequenceGeneratorImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbTableGeneratorImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbUserTypeRegistrationImpl;
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
import org.hibernate.boot.models.categorize.spi.SequenceGeneratorRegistration;
import org.hibernate.boot.models.categorize.spi.TableGeneratorRegistration;
import org.hibernate.boot.models.categorize.spi.UserTypeRegistration;
import org.hibernate.boot.models.xml.internal.XmlAnnotationHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.models.internal.MutableAnnotationUsage;
import org.hibernate.models.internal.dynamic.DynamicAnnotationUsage;
import org.hibernate.models.spi.AnnotationDescriptor;
import org.hibernate.models.spi.AnnotationDescriptorRegistry;
import org.hibernate.models.spi.AnnotationTarget;
import org.hibernate.models.spi.AnnotationUsage;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.SourceModelContext;

import jakarta.persistence.SequenceGenerator;
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

	public void collectEntityListenerRegistrations(List<JaxbEntityListenerImpl> listeners) {
		if ( CollectionHelper.isEmpty( listeners ) ) {
			return;
		}

		listeners.forEach( (jaxbEntityListener) -> {
			final ClassDetails classDetails = classDetailsRegistry.resolveClassDetails( jaxbEntityListener.getClazz() );
			final JpaEventListener listener = JpaEventListener.from(
					JpaEventListenerStyle.LISTENER,
					classDetails,
					jaxbEntityListener
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
			final MutableAnnotationUsage<SequenceGenerator> annotationUsage = makeAnnotation( SequenceGenerator.class );
			annotationUsage.setAttributeValue( "name", generator.getName() );
			annotationUsage.setAttributeValue( "sequenceName", generator.getSequenceName() );
			annotationUsage.setAttributeValue( "catalog", generator.getCatalog() );
			annotationUsage.setAttributeValue( "schema", generator.getSchema() );
			annotationUsage.setAttributeValue( "initialValue", generator.getInitialValue() );
			annotationUsage.setAttributeValue( "allocationSize", generator.getAllocationSize() );

			collectSequenceGenerator( new SequenceGeneratorRegistration( generator.getName(), annotationUsage ) );
		} );
	}

	private <A extends Annotation> MutableAnnotationUsage<A> makeAnnotation(Class<A> annotationType) {
		final AnnotationDescriptor<A> descriptor = descriptorRegistry.getDescriptor( annotationType );
		return new DynamicAnnotationUsage<>( descriptor );
	}

	public void collectSequenceGenerator(AnnotationUsage<SequenceGenerator> usage) {
		collectSequenceGenerator( new SequenceGeneratorRegistration( usage.getAttributeValue( "name" ), usage ) );
	}

	public void collectSequenceGenerator(SequenceGeneratorRegistration generatorRegistration) {
		if ( sequenceGeneratorRegistrations == null ) {
			sequenceGeneratorRegistrations = new HashMap<>();
		}

		sequenceGeneratorRegistrations.put( generatorRegistration.getName(), generatorRegistration );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Table generator

	public void collectTableGenerators(List<JaxbTableGeneratorImpl> tableGenerators) {
		if ( CollectionHelper.isEmpty( tableGenerators ) ) {
			return;
		}

		tableGenerators.forEach( (generator) -> {
			final MutableAnnotationUsage<TableGenerator> annotationUsage = makeAnnotation( TableGenerator.class );
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

		tableGeneratorRegistrations.put( generatorRegistration.getName(), generatorRegistration );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Generic generators

	private void collectGenericGenerators(List<JaxbGenericIdGeneratorImpl> genericGenerators) {
		if ( CollectionHelper.isEmpty( genericGenerators ) ) {
			return;
		}

		genericGenerators.forEach( (generator) -> {
			final MutableAnnotationUsage<GenericGenerator> annotationUsage = makeAnnotation( GenericGenerator.class );
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

		genericGeneratorRegistrations.put( generatorRegistration.getName(), generatorRegistration );
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
}
