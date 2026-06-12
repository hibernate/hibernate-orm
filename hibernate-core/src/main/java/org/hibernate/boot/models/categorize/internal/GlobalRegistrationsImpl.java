/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.AnnotationException;
import org.hibernate.annotations.FetchProfile;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Imported;
import org.hibernate.annotations.ParamDef;
import org.hibernate.annotations.Parameter;
import org.hibernate.boot.model.NamedEntityGraphDefinition;
import org.hibernate.boot.jaxb.mapping.spi.JaxbCollectionUserTypeRegistrationImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbCompositeUserTypeRegistrationImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbConfigurationParameterImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbConverterImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbConverterRegistrationImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbDatabaseObjectImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEmbeddableInstantiatorRegistrationImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityListenerImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbFetchProfileImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbFilterDefImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbGenericIdGeneratorImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbHqlImportImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbJavaTypeRegistrationImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbJdbcTypeRegistrationImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbNamedHqlQueryImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbNamedNativeQueryImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbNamedStoredProcedureQueryImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbQueryHintContainer;
import org.hibernate.boot.jaxb.mapping.spi.JaxbSequenceGeneratorImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbSqlResultSetMappingImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbTableGeneratorImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbUserTypeRegistrationImpl;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.internal.NamedNativeQueryJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.SqlResultSetMappingJpaAnnotation;
import org.hibernate.boot.models.categorize.spi.CollectionTypeRegistration;
import org.hibernate.boot.models.categorize.spi.CompositeUserTypeRegistration;
import org.hibernate.boot.models.categorize.spi.ConversionRegistration;
import org.hibernate.boot.models.categorize.spi.DatabaseObjectRegistration;
import org.hibernate.boot.models.categorize.spi.EmbeddableInstantiatorRegistration;
import org.hibernate.boot.models.categorize.spi.FetchProfileRegistration;
import org.hibernate.boot.models.categorize.spi.FilterDefRegistration;
import org.hibernate.boot.models.categorize.spi.GenericGeneratorRegistration;
import org.hibernate.boot.models.categorize.spi.GlobalRegistrations;
import org.hibernate.boot.models.categorize.spi.JavaTypeRegistration;
import org.hibernate.boot.models.categorize.spi.JdbcTypeRegistration;
import org.hibernate.boot.models.categorize.spi.JpaEventListener;
import org.hibernate.boot.models.categorize.spi.JpaEventListenerStyle;
import org.hibernate.boot.models.categorize.spi.NamedQueryRegistration;
import org.hibernate.boot.models.categorize.spi.SequenceGeneratorRegistration;
import org.hibernate.boot.models.categorize.spi.SqlResultSetMappingRegistration;
import org.hibernate.boot.models.categorize.spi.TableGeneratorRegistration;
import org.hibernate.boot.models.categorize.spi.UserTypeRegistration;
import org.hibernate.boot.models.xml.internal.QueryProcessing;
import org.hibernate.boot.models.xml.internal.XmlAnnotationHelper;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.jpa.AvailableHints;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.models.spi.AnnotationDescriptorRegistry;
import org.hibernate.models.spi.AnnotationTarget;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.ModelsContext;

import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.NamedStoredProcedureQuery;
import jakarta.persistence.QueryHint;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.TableGenerator;
import jakarta.persistence.AttributeConverter;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.hibernate.boot.models.JpaAnnotations.NAMED_STORED_PROCEDURE_QUERY;
import static org.hibernate.boot.models.xml.internal.QueryProcessing.collectResultClasses;
import static org.hibernate.internal.util.GenericsHelper.typeArguments;
import static org.hibernate.internal.util.StringHelper.unqualify;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;
import static org.hibernate.internal.util.collections.CollectionHelper.isEmpty;

/// Standard GlobalRegistrations impl
///
/// @since 9.0
/// @author Steve Ebersole
public class GlobalRegistrationsImpl implements GlobalRegistrations {
	private final ModelsContext modelsContext;
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
	private List<FetchProfileRegistration> fetchProfileRegistrations;
	private Map<String, String> importedRenameMap;

	private Map<String, SequenceGeneratorRegistration> sequenceGeneratorRegistrations;
	private Map<String, TableGeneratorRegistration> tableGeneratorRegistrations;
	private Map<String, GenericGeneratorRegistration> genericGeneratorRegistrations;
	private Map<String, SqlResultSetMappingRegistration> sqlResultSetMappingRegistrations;
	private Map<String, NamedQueryRegistration> namedQueryRegistrations;
	private Map<String, NamedQueryRegistration> namedNativeQueryRegistrations;
	private Map<String, NamedQueryRegistration> namedStoredProcedureQueryRegistrations;
	private Map<String, NamedEntityGraphDefinition> namedEntityGraphRegistrations;
	private List<DatabaseObjectRegistration> databaseObjectRegistrations;

	public GlobalRegistrationsImpl(ModelsContext modelsContext) {
		this( modelsContext, modelsContext.getClassDetailsRegistry(), modelsContext.getAnnotationDescriptorRegistry() );
	}

	public GlobalRegistrationsImpl(
			ModelsContext modelsContext,
			ClassDetailsRegistry classDetailsRegistry,
			AnnotationDescriptorRegistry descriptorRegistry) {
		this.modelsContext = modelsContext;
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
	public List<FetchProfileRegistration> getFetchProfileRegistrations() {
		return fetchProfileRegistrations == null ? emptyList() : fetchProfileRegistrations;
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
	public Map<String, SqlResultSetMappingRegistration> getSqlResultSetMappingRegistrations() {
		return sqlResultSetMappingRegistrations == null ? emptyMap() : sqlResultSetMappingRegistrations;
	}

	@Override
	public Map<String, NamedQueryRegistration> getNamedQueryRegistrations() {
		return namedQueryRegistrations == null ? emptyMap() : namedQueryRegistrations;
	}

	@Override
	public Map<String, NamedQueryRegistration> getNamedNativeQueryRegistrations() {
		return namedNativeQueryRegistrations == null ? emptyMap() : namedNativeQueryRegistrations;
	}

	@Override
	public Map<String, NamedQueryRegistration> getNamedStoredProcedureQueryRegistrations() {
		return namedStoredProcedureQueryRegistrations == null ? emptyMap() : namedStoredProcedureQueryRegistrations;
	}

	@Override
	public Map<String, NamedEntityGraphDefinition> getNamedEntityGraphRegistrations() {
		return namedEntityGraphRegistrations == null ? emptyMap() : namedEntityGraphRegistrations;
	}

	@Override
	public List<DatabaseObjectRegistration> getDatabaseObjectRegistrations() {
		return databaseObjectRegistrations == null ? emptyList() : databaseObjectRegistrations;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JavaTypeRegistration

	public void collectJavaTypeRegistrations(AnnotationTarget annotationTarget) {
		annotationTarget.forEachAnnotationUsage( org.hibernate.annotations.JavaTypeRegistration.class, modelsContext, (usage) -> collectJavaTypeRegistration(
				classDetailsRegistry.resolveClassDetails( usage.javaType().getName() ),
				classDetailsRegistry.resolveClassDetails( usage.descriptorClass().getName() )
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
		annotationTarget.forEachAnnotationUsage( org.hibernate.annotations.JdbcTypeRegistration.class, modelsContext, (usage) -> collectJdbcTypeRegistration(
				usage.registrationCode(),
				classDetailsRegistry.resolveClassDetails( usage.value().getName() )
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
		annotationTarget.forEachAnnotationUsage( org.hibernate.annotations.ConverterRegistration.class, modelsContext, (usage) -> {
			final ClassDetails domainType = usage.domainType() == void.class
					? null
					: classDetailsRegistry.resolveClassDetails( usage.domainType().getName() );
			final ClassDetails converterType = classDetailsRegistry.resolveClassDetails( usage.converter().getName() );
			collectConverterRegistration( new ConversionRegistration( domainType, converterType, usage.autoApply(), descriptorRegistry.getDescriptor( org.hibernate.annotations.ConverterRegistration.class ) ) );
		} );
	}

	public void collectConverter(AnnotationTarget annotationTarget) {
		final Converter converter = annotationTarget.getDirectAnnotationUsage( Converter.class );
		if ( converter == null || !( annotationTarget instanceof ClassDetails converterType ) ) {
			return;
		}
		final ClassDetails domainType = converterDomainType( converterType );
		collectConverterRegistration( new ConversionRegistration(
				domainType,
				converterType,
				converter.autoApply(),
				descriptorRegistry.getDescriptor( Converter.class )
		) );
	}

	private ClassDetails converterDomainType(ClassDetails converterType) {
		final Type[] typeArguments = typeArguments( AttributeConverter.class, converterType.toJavaClass() );
		if ( typeArguments.length == 0 || !( typeArguments[0] instanceof Class<?> domainType ) ) {
			return null;
		}
		return classDetailsRegistry.resolveClassDetails( domainType.getName() );
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
			collectConverterRegistration( new ConversionRegistration( explicitDomainType, converterType, autoApply, descriptorRegistry.getDescriptor( org.hibernate.annotations.ConverterRegistration.class ) ) );
		} );
	}

	public void collectConverterRegistration(ConversionRegistration conversion) {
		if ( converterRegistrations == null ) {
			converterRegistrations = new ArrayList<>();
		}
		converterRegistrations.add( conversion );
	}

	public void collectConverters(List<JaxbConverterImpl> converters) {
		if ( CollectionHelper.isEmpty( converters ) ) {
			return;
		}

		converters.forEach( (jaxbConverter) -> {
			final ClassDetails converterType = classDetailsRegistry.resolveClassDetails( jaxbConverter.getClazz() );
			collectConverterRegistration( new ConversionRegistration(
					null,
					converterType,
					jaxbConverter.isAutoApply(),
					descriptorRegistry.getDescriptor( Converter.class )
			) );
		} );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Named queries and graphs

	public void collectQueryReferences(JaxbEntityMappingsImpl jaxbRoot, XmlDocumentContext xmlDocumentContext) {
		collectNamedSqlResultSetMappings( jaxbRoot.getSqlResultSetMappings(), xmlDocumentContext );
		collectNamedQueries( jaxbRoot.getNamedQueries(), xmlDocumentContext );
		collectNamedNativeQueries( jaxbRoot.getNamedNativeQueries(), xmlDocumentContext );
		collectStoredProcedureQueries( jaxbRoot.getNamedProcedureQueries(), xmlDocumentContext );
	}

	private void collectNamedSqlResultSetMappings(
			List<JaxbSqlResultSetMappingImpl> jaxbSqlResultSetMappings,
			XmlDocumentContext xmlDocumentContext) {
		if ( CollectionHelper.isEmpty( jaxbSqlResultSetMappings ) ) {
			return;
		}

		if ( sqlResultSetMappingRegistrations == null ) {
			sqlResultSetMappingRegistrations = new HashMap<>();
		}

		jaxbSqlResultSetMappings.forEach( (jaxbMapping) -> {
			final SqlResultSetMappingJpaAnnotation annotation = JpaAnnotations.SQL_RESULT_SET_MAPPING.createUsage( modelsContext );
			final String name = jaxbMapping.getName();

			annotation.name( name );
			annotation.columns( QueryProcessing.extractColumnResults( jaxbMapping.getColumnResult(), xmlDocumentContext ) );
			annotation.classes( QueryProcessing.extractConstructorResults( jaxbMapping.getConstructorResult(), xmlDocumentContext ) );
			annotation.entities( QueryProcessing.extractEntityResults( jaxbMapping.getEntityResult(), xmlDocumentContext ) );

			sqlResultSetMappingRegistrations.put( name, new SqlResultSetMappingRegistration( name, annotation ) );
		} );
	}

	private void collectNamedQueries(List<JaxbNamedHqlQueryImpl> jaxbNamedQueries, XmlDocumentContext xmlDocumentContext) {
		if ( CollectionHelper.isEmpty( jaxbNamedQueries ) ) {
			return;
		}

		for ( JaxbNamedHqlQueryImpl jaxbNamedQuery : jaxbNamedQueries ) {
			final var queryAnnotation = JpaAnnotations.NAMED_QUERY.createUsage( modelsContext );
			final String name = jaxbNamedQuery.getName();

			queryAnnotation.name( name );
			queryAnnotation.query( jaxbNamedQuery.getQuery() );

			final var lockMode = jaxbNamedQuery.getLockMode();
			if ( lockMode != null ) {
				queryAnnotation.lockMode( lockMode );
			}

			queryAnnotation.hints( collectQueryHints( jaxbNamedQuery, xmlDocumentContext ) );
			collectNamedQueryRegistration( name, NamedQueryRegistration.Kind.HQL, true, queryAnnotation );
		}
	}

	private QueryHint[] collectQueryHints(JaxbNamedHqlQueryImpl jaxbNamedQuery, XmlDocumentContext xmlDocumentContext) {
		final List<QueryHint> hints = extractQueryHints( jaxbNamedQuery, xmlDocumentContext );

		if ( jaxbNamedQuery.isCacheable() == Boolean.TRUE ) {
			final var cacheableHint = JpaAnnotations.QUERY_HINT.createUsage( modelsContext );
			cacheableHint.name( AvailableHints.HINT_CACHEABLE );
			cacheableHint.value( Boolean.TRUE.toString() );
			hints.add( cacheableHint );

			if ( jaxbNamedQuery.getCacheMode() != null ) {
				final var cacheModeHint = JpaAnnotations.QUERY_HINT.createUsage( modelsContext );
				cacheModeHint.name( AvailableHints.HINT_CACHE_MODE );
				cacheModeHint.value( jaxbNamedQuery.getCacheMode().name() );
				hints.add( cacheModeHint );
			}

			if ( isNotEmpty( jaxbNamedQuery.getCacheRegion() ) ) {
				final var cacheRegionHint = JpaAnnotations.QUERY_HINT.createUsage( modelsContext );
				cacheRegionHint.name( AvailableHints.HINT_CACHE_REGION );
				cacheRegionHint.value( jaxbNamedQuery.getCacheRegion() );
				hints.add( cacheRegionHint );
			}
		}

		if ( isNotEmpty( jaxbNamedQuery.getComment() ) ) {
			final var hint = JpaAnnotations.QUERY_HINT.createUsage( modelsContext );
			hint.name( AvailableHints.HINT_COMMENT );
			hint.value( jaxbNamedQuery.getComment() );
			hints.add( hint );
		}

		if ( jaxbNamedQuery.getFetchSize() != null ) {
			final var hint = JpaAnnotations.QUERY_HINT.createUsage( modelsContext );
			hint.name( AvailableHints.HINT_FETCH_SIZE );
			hint.value( jaxbNamedQuery.getFetchSize().toString() );
			hints.add( hint );
		}

		if ( jaxbNamedQuery.isReadOnly() == Boolean.TRUE ) {
			final var hint = JpaAnnotations.QUERY_HINT.createUsage( modelsContext );
			hint.name( AvailableHints.HINT_READ_ONLY );
			hint.value( Boolean.TRUE.toString() );
			hints.add( hint );
		}

		if ( jaxbNamedQuery.getFlushMode() != null ) {
			final var hint = JpaAnnotations.QUERY_HINT.createUsage( modelsContext );
			hint.name( AvailableHints.HINT_FLUSH_MODE );
			hint.value( jaxbNamedQuery.getFlushMode().name() );
			hints.add( hint );
		}

		return hints.toArray( QueryHint[]::new );
	}

	private void collectNamedNativeQueries(
			List<JaxbNamedNativeQueryImpl> namedNativeQueries,
			XmlDocumentContext xmlDocumentContext) {
		if ( CollectionHelper.isEmpty( namedNativeQueries ) ) {
			return;
		}

		for ( JaxbNamedNativeQueryImpl jaxbNamedQuery : namedNativeQueries ) {
			final NamedNativeQueryJpaAnnotation queryAnnotation = JpaAnnotations.NAMED_NATIVE_QUERY.createUsage( modelsContext );
			final String name = jaxbNamedQuery.getName();

			queryAnnotation.name( name );
			queryAnnotation.query( jaxbNamedQuery.getQuery() );

			final String resultClass = jaxbNamedQuery.getResultClass();
			if ( isNotEmpty( resultClass ) ) {
				queryAnnotation.resultClass( classDetailsRegistry.resolveClassDetails( resultClass ).toJavaClass() );
			}

			applyResultSetMapping( jaxbNamedQuery, queryAnnotation, xmlDocumentContext );
			queryAnnotation.hints( collectNativeQueryHints( jaxbNamedQuery, xmlDocumentContext ) );

			collectNamedQueryRegistration( name, NamedQueryRegistration.Kind.NATIVE, true, queryAnnotation );
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

	private QueryHint[] collectNativeQueryHints(
			JaxbNamedNativeQueryImpl jaxbNamedQuery,
			XmlDocumentContext xmlDocumentContext) {
		final List<QueryHint> hints = extractQueryHints( jaxbNamedQuery, xmlDocumentContext );

		if ( jaxbNamedQuery.isCacheable() == Boolean.TRUE ) {
			final var cacheableHint = JpaAnnotations.QUERY_HINT.createUsage( modelsContext );
			cacheableHint.name( AvailableHints.HINT_CACHEABLE );
			cacheableHint.value( Boolean.TRUE.toString() );
			hints.add( cacheableHint );

			if ( jaxbNamedQuery.getCacheMode() != null ) {
				final var cacheModeHint = JpaAnnotations.QUERY_HINT.createUsage( modelsContext );
				cacheModeHint.name( AvailableHints.HINT_CACHE_MODE );
				cacheModeHint.value( jaxbNamedQuery.getCacheMode().name() );
				hints.add( cacheModeHint );
			}

			if ( isNotEmpty( jaxbNamedQuery.getCacheRegion() ) ) {
				final var cacheRegionHint = JpaAnnotations.QUERY_HINT.createUsage( modelsContext );
				cacheRegionHint.name( AvailableHints.HINT_CACHE_REGION );
				cacheRegionHint.value( jaxbNamedQuery.getCacheRegion() );
				hints.add( cacheRegionHint );
			}
		}

		if ( jaxbNamedQuery.isReadOnly() == Boolean.TRUE ) {
			final var hint = JpaAnnotations.QUERY_HINT.createUsage( modelsContext );
			hint.name( AvailableHints.HINT_READ_ONLY );
			hint.value( Boolean.TRUE.toString() );
			hints.add( hint );
		}

		if ( isNotEmpty( jaxbNamedQuery.getComment() ) ) {
			final var hint = JpaAnnotations.QUERY_HINT.createUsage( modelsContext );
			hint.name( AvailableHints.HINT_COMMENT );
			hint.value( jaxbNamedQuery.getComment() );
			hints.add( hint );
		}

		return hints.toArray( QueryHint[]::new );
	}

	private void collectStoredProcedureQueries(
			List<JaxbNamedStoredProcedureQueryImpl> namedProcedureQueries,
			XmlDocumentContext xmlDocumentContext) {
		if ( CollectionHelper.isEmpty( namedProcedureQueries ) ) {
			return;
		}

		for ( JaxbNamedStoredProcedureQueryImpl jaxbQuery : namedProcedureQueries ) {
			final var queryAnnotation = NAMED_STORED_PROCEDURE_QUERY.createUsage( modelsContext );
			final String name = jaxbQuery.getName();

			queryAnnotation.name( name );
			queryAnnotation.procedureName( jaxbQuery.getProcedureName() );
			queryAnnotation.resultClasses( collectResultClasses( jaxbQuery.getResultClasses(), xmlDocumentContext ) );

			final var resultSetMappings = jaxbQuery.getResultSetMappings();
			if ( CollectionHelper.isNotEmpty( resultSetMappings ) ) {
				queryAnnotation.resultSetMappings( resultSetMappings.toArray( String[]::new ) );
			}

			queryAnnotation.hints( extractQueryHints( jaxbQuery, xmlDocumentContext ).toArray( QueryHint[]::new ) );
			queryAnnotation.parameters( QueryProcessing.collectParameters(
					jaxbQuery.getProcedureParameters(),
					xmlDocumentContext
			) );

			collectNamedQueryRegistration( name, NamedQueryRegistration.Kind.CALLABLE, true, queryAnnotation );
		}
	}

	private List<QueryHint> extractQueryHints(JaxbQueryHintContainer jaxbQuery, XmlDocumentContext xmlDocumentContext) {
		final List<QueryHint> hints = new ArrayList<>();
		for ( var jaxbHint : jaxbQuery.getHints() ) {
			final var hint = JpaAnnotations.QUERY_HINT.createUsage( xmlDocumentContext.getModelBuildingContext() );
			hint.name( jaxbHint.getName() );
			hint.value( jaxbHint.getValue() );
			hints.add( hint );
		}
		return hints;
	}

	public void collectNamedQueryRegistrations(AnnotationTarget annotationTarget) {
		for ( NamedQuery usage : annotationTarget.getRepeatedAnnotationUsages( NamedQuery.class, modelsContext ) ) {
			collectNamedQueryRegistration( usage.name(), NamedQueryRegistration.Kind.HQL, true, usage );
		}
		for ( NamedNativeQuery usage : annotationTarget.getRepeatedAnnotationUsages( NamedNativeQuery.class, modelsContext ) ) {
			collectNamedQueryRegistration( usage.name(), NamedQueryRegistration.Kind.NATIVE, true, usage );
		}
		for ( NamedStoredProcedureQuery usage : annotationTarget.getRepeatedAnnotationUsages( NamedStoredProcedureQuery.class, modelsContext ) ) {
			collectNamedQueryRegistration( usage.name(), NamedQueryRegistration.Kind.CALLABLE, true, usage );
		}
		for ( org.hibernate.annotations.NamedQuery usage : annotationTarget.getRepeatedAnnotationUsages(
				org.hibernate.annotations.NamedQuery.class,
				modelsContext
		) ) {
			collectNamedQueryRegistration( usage.name(), NamedQueryRegistration.Kind.HQL, false, usage );
		}
		for ( org.hibernate.annotations.NamedNativeQuery usage : annotationTarget.getRepeatedAnnotationUsages(
				org.hibernate.annotations.NamedNativeQuery.class,
				modelsContext
		) ) {
			collectNamedQueryRegistration( usage.name(), NamedQueryRegistration.Kind.NATIVE, false, usage );
		}
	}

	public void collectSqlResultSetMappingRegistrations(AnnotationTarget annotationTarget) {
		for ( SqlResultSetMapping usage : annotationTarget.getRepeatedAnnotationUsages( SqlResultSetMapping.class, modelsContext ) ) {
			if ( sqlResultSetMappingRegistrations == null ) {
				sqlResultSetMappingRegistrations = new HashMap<>();
			}
			sqlResultSetMappingRegistrations.put(
					usage.name(),
					new SqlResultSetMappingRegistration( usage.name(), usage )
			);
		}
	}

	private void collectNamedQueryRegistration(
			String name,
			NamedQueryRegistration.Kind kind,
			boolean isJpa,
			Annotation configuration) {
		final Map<String, NamedQueryRegistration> registrations = switch ( kind ) {
			case HQL -> {
				if ( namedQueryRegistrations == null ) {
					namedQueryRegistrations = new HashMap<>();
				}
				yield namedQueryRegistrations;
			}
			case NATIVE -> {
				if ( namedNativeQueryRegistrations == null ) {
					namedNativeQueryRegistrations = new HashMap<>();
				}
				yield namedNativeQueryRegistrations;
			}
			case CALLABLE -> {
				if ( namedStoredProcedureQueryRegistrations == null ) {
					namedStoredProcedureQueryRegistrations = new HashMap<>();
				}
				yield namedStoredProcedureQueryRegistrations;
			}
		};
		registrations.put( name, new NamedQueryRegistration( name, kind, isJpa, configuration ) );
	}

	public void collectNamedEntityGraphRegistrations(ClassDetails classDetails) {
		for ( NamedEntityGraph usage : classDetails.getRepeatedAnnotationUsages( NamedEntityGraph.class, modelsContext ) ) {
			collectNamedEntityGraphRegistration( graphName( classDetails, usage ), jpaEntityName( classDetails ), usage );
		}
		for ( org.hibernate.annotations.NamedEntityGraph usage : classDetails.getRepeatedAnnotationUsages(
				org.hibernate.annotations.NamedEntityGraph.class,
				modelsContext
		) ) {
			collectNamedEntityGraphRegistration( usage.name(), null, usage );
		}
	}

	private void collectNamedEntityGraphRegistration(
			String name,
			String entityName,
			Annotation configuration) {
		if ( namedEntityGraphRegistrations == null ) {
			namedEntityGraphRegistrations = new HashMap<>();
		}
		namedEntityGraphRegistrations.put( name, new NamedEntityGraphDefinition(
				name,
				entityName,
				configuration instanceof NamedEntityGraph ? NamedEntityGraphDefinition.Source.JPA : NamedEntityGraphDefinition.Source.PARSED,
				(entityDomainClassResolver, entityDomainNameResolver, serviceRegistry) -> {
					throw new UnsupportedOperationException(
							"Named entity graph creation is deferred to ORM runtime integration - " + name
					);
				}
		) );
	}

	private static String graphName(ClassDetails classDetails, NamedEntityGraph graph) {
		return isNotEmpty( graph.name() ) ? graph.name() : jpaEntityName( classDetails );
	}

	private static String jpaEntityName(ClassDetails classDetails) {
		final Entity entity = classDetails.getDirectAnnotationUsage( Entity.class );
		if ( entity != null && isNotEmpty( entity.name() ) ) {
			return entity.name();
		}
		return StringHelper.unqualify( classDetails.getName() );
	}

	public void collectFetchProfiles(List<JaxbFetchProfileImpl> jaxbFetchProfiles) {
		if ( CollectionHelper.isEmpty( jaxbFetchProfiles ) ) {
			return;
		}

		if ( fetchProfileRegistrations == null ) {
			fetchProfileRegistrations = new ArrayList<>();
		}

		for ( JaxbFetchProfileImpl jaxbFetchProfile : jaxbFetchProfiles ) {
			final List<FetchProfileRegistration.FetchOverride> fetchOverrides = new ArrayList<>();
			for ( var jaxbFetch : jaxbFetchProfile.getFetch() ) {
				fetchOverrides.add( new FetchProfileRegistration.FetchOverride(
						jaxbFetch.getEntity(),
						jaxbFetch.getAssociation(),
						jaxbFetch.getStyle()
				) );
			}
			fetchProfileRegistrations.add( new FetchProfileRegistration( jaxbFetchProfile.getName(), fetchOverrides ) );
		}
	}

	public void collectFetchProfiles(AnnotationTarget annotationTarget) {
		for ( FetchProfile usage : annotationTarget.getRepeatedAnnotationUsages( FetchProfile.class, modelsContext ) ) {
			if ( fetchProfileRegistrations == null ) {
				fetchProfileRegistrations = new ArrayList<>();
			}

			final List<FetchProfileRegistration.FetchOverride> fetchOverrides = new ArrayList<>();
			for ( FetchProfile.FetchOverride fetchOverride : usage.fetchOverrides() ) {
				fetchOverrides.add( new FetchProfileRegistration.FetchOverride(
						fetchOverride.entity().getName(),
						fetchOverride.association(),
						fetchOverride.mode().name()
				) );
			}
			fetchProfileRegistrations.add( new FetchProfileRegistration( usage.name(), fetchOverrides ) );
		}
	}

	public void collectImportRenames(List<JaxbHqlImportImpl> imports) {
		if ( CollectionHelper.isEmpty( imports ) ) {
			return;
		}

		for ( JaxbHqlImportImpl importMapping : imports ) {
			final String rename = isNotEmpty( importMapping.getRename() )
					? importMapping.getRename()
					: unqualify( importMapping.getClazz() );
			collectImportRename( rename, importMapping.getClazz() );
		}
	}

	public void collectImportRename(ClassDetails classDetails) {
		final Imported importedUsage = classDetails.getDirectAnnotationUsage( Imported.class );
		if ( importedUsage != null ) {
			final String explicitRename = importedUsage.rename();
			final String rename = isNotEmpty( explicitRename ) ? explicitRename : unqualify( classDetails.getName() );
			collectImportRename( rename, classDetails.getName() );
		}
	}

	public void collectImportRename(String rename, String name) {
		if ( importedRenameMap == null ) {
			importedRenameMap = new HashMap<>();
		}

		importedRenameMap.put( rename, name );
	}

	public void collectDataBaseObject(List<JaxbDatabaseObjectImpl> databaseObjects) {
		if ( CollectionHelper.isEmpty( databaseObjects ) ) {
			return;
		}

		if ( databaseObjectRegistrations == null ) {
			databaseObjectRegistrations = new ArrayList<>();
		}

		for ( JaxbDatabaseObjectImpl jaxbDatabaseObject : databaseObjects ) {
			final var definition = jaxbDatabaseObject.getDefinition();
			final var dialectScopes = jaxbDatabaseObject.getDialectScopes();
			final List<DatabaseObjectRegistration.DialectScopeRegistration> scopeRegistrations = new ArrayList<>( dialectScopes.size() );
			for ( var dialectScope : dialectScopes ) {
				scopeRegistrations.add( new DatabaseObjectRegistration.DialectScopeRegistration(
						dialectScope.getName(),
						dialectScope.getContent(),
						dialectScope.getMinimumVersion(),
						dialectScope.getMaximumVersion()
				) );
			}
			databaseObjectRegistrations.add( new DatabaseObjectRegistration(
					jaxbDatabaseObject.getCreate(),
					jaxbDatabaseObject.getDrop(),
					definition != null ? definition.getClazz() : null,
					scopeRegistrations
			) );
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// UserTypeRegistration

	public void collectUserTypeRegistrations(AnnotationTarget annotationTarget) {
		annotationTarget.forEachAnnotationUsage( org.hibernate.annotations.TypeRegistration.class, modelsContext, (usage) -> collectUserTypeRegistration(
				classDetailsRegistry.resolveClassDetails( usage.basicClass().getName() ),
				classDetailsRegistry.resolveClassDetails( usage.userType().getName() )
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
		annotationTarget.forEachAnnotationUsage( org.hibernate.annotations.CompositeTypeRegistration.class, modelsContext, (usage) -> collectCompositeUserTypeRegistration(
				classDetailsRegistry.resolveClassDetails( usage.embeddableClass().getName() ),
				classDetailsRegistry.resolveClassDetails( usage.userType().getName() )
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
		annotationTarget.forEachAnnotationUsage( org.hibernate.annotations.CollectionTypeRegistration.class, modelsContext, (usage) -> collectCollectionTypeRegistration(
				usage.classification(),
				classDetailsRegistry.resolveClassDetails( usage.type().getName() ),
				extractParameterMap( usage )
		) );
	}

	private Map<String,String> extractParameterMap(org.hibernate.annotations.CollectionTypeRegistration source) {
		return extractParameterMap( source.parameters() );
	}

	private Map<String,String> extractParameterMap(Parameter[] parameters) {
		if ( parameters.length == 0 ) {
			return Collections.emptyMap();
		}
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
		annotationTarget.forEachAnnotationUsage( org.hibernate.annotations.EmbeddableInstantiatorRegistration.class, modelsContext, (usage) -> collectEmbeddableInstantiatorRegistration(
				classDetailsRegistry.resolveClassDetails( usage.embeddableClass().getName() ),
				classDetailsRegistry.resolveClassDetails( usage.instantiator().getName() )
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
		annotationTarget.forEachAnnotationUsage( FilterDef.class, modelsContext, (usage) -> collectFilterDefinition(
				usage.name(),
				usage.defaultCondition(),
				extractFilterParameters( usage )
		) );
	}

	private Map<String, ClassDetails> extractFilterParameters(FilterDef source) {
		final ParamDef[] parameters = source.parameters();
		if ( parameters.length == 0 ) {
			return null;
		}

		final Map<String, ClassDetails> result = new HashMap<>( parameters.length );
		for ( ParamDef parameter : parameters ) {
			result.put( parameter.name(), classDetailsRegistry.resolveClassDetails( parameter.type().getName() ) );
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

		if ( filterDefRegistrations.put( name, new FilterDefRegistration( name, defaultCondition, parameters ) ) != null ) {
			throw new AnnotationException( "Multiple '@FilterDef' annotations define a filter named '" + name + "'" );
		}
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
		classDetails.forEachAnnotationUsage( SequenceGenerator.class, modelsContext, this::collectSequenceGenerator );
		classDetails.forEachAnnotationUsage( TableGenerator.class, modelsContext, this::collectTableGenerator );
		classDetails.forEachAnnotationUsage( GenericGenerator.class, modelsContext, this::collectGenericGenerator );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Sequence generator

	public void collectSequenceGenerators(List<JaxbSequenceGeneratorImpl> sequenceGenerators) {
		if ( CollectionHelper.isEmpty( sequenceGenerators ) ) {
			return;
		}

		sequenceGenerators.forEach( (generator) -> {
			final SequenceGenerator annotationUsage = makeAnnotation(
					SequenceGenerator.class,
					Map.of(
							"name", generator.getName(),
							"sequenceName", generator.getSequenceName(),
							"catalog", generator.getCatalog(),
							"schema", generator.getSchema(),
							"initialValue", generator.getInitialValue(),
							"allocationSize", generator.getAllocationSize()
					)
			);

			collectSequenceGenerator( new SequenceGeneratorRegistration( generator.getName(), annotationUsage ) );
		} );
	}

	public void collectSequenceGenerator(SequenceGenerator usage) {
		collectSequenceGenerator( new SequenceGeneratorRegistration( usage.name(), usage ) );
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
			final TableGenerator annotationUsage = makeAnnotation(
					TableGenerator.class,
					Map.of(
							"name", generator.getName(),
							"table", generator.getTable(),
							"catalog", generator.getCatalog(),
							"schema", generator.getSchema(),
							"pkColumnName", generator.getPkColumnName(),
							"valueColumnName", generator.getValueColumnName(),
							"pkColumnValue", generator.getPkColumnValue(),
							"initialValue", generator.getInitialValue(),
							"allocationSize", generator.getAllocationSize()
					)
			);

			collectTableGenerator( new TableGeneratorRegistration( generator.getName(), annotationUsage ) );
		} );
	}

	public void collectTableGenerator(TableGenerator usage) {
		collectTableGenerator( new TableGeneratorRegistration( usage.name(), usage ) );
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

		genericGenerators.forEach( (generator) -> collectGenericGenerator(
				new GenericGeneratorRegistration(
						generator.getName(),
						generator.getClazz(),
						extractParameterMap( generator.getParameters() )
				)
		) );
	}

	public void collectGenericGenerator(GenericGenerator usage) {
		collectGenericGenerator(
				new GenericGeneratorRegistration(
						usage.type().getName(),
						usage.type().getName(),
						extractParameterMap( usage.parameters() )
				)
		);
	}

	public void collectGenericGenerator(GenericGeneratorRegistration generatorRegistration) {
		if ( genericGeneratorRegistrations == null ) {
			genericGeneratorRegistrations = new HashMap<>();
		}

		genericGeneratorRegistrations.put( generatorRegistration.name(), generatorRegistration );
	}

	@SuppressWarnings("unchecked")
	private static <A extends Annotation> A makeAnnotation(Class<A> annotationType, Map<String, ?> values) {
		final InvocationHandler handler = (proxy, method, args) -> {
			final String name = method.getName();
			if ( method.getParameterCount() == 0 ) {
				if ( name.equals( "annotationType" ) ) {
					return annotationType;
				}
				if ( values.containsKey( name ) && values.get( name ) != null ) {
					return values.get( name );
				}
				final Object defaultValue = method.getDefaultValue();
				if ( defaultValue != null ) {
					return defaultValue;
				}
			}
			if ( name.equals( "toString" ) ) {
				return annotationType.getName() + values;
			}
			if ( name.equals( "hashCode" ) ) {
				return values.hashCode();
			}
			if ( name.equals( "equals" ) ) {
				return proxy == args[0];
			}
			throw new UnsupportedOperationException( "No value available for " + annotationType.getName() + "." + name );
		};
		return (A) Proxy.newProxyInstance( annotationType.getClassLoader(), new Class<?>[] { annotationType }, handler );
	}
}
