/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.xml.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.FlushMode;
import org.hibernate.annotations.FlushModeType;
import org.hibernate.annotations.NamedNativeQuery;
import org.hibernate.annotations.NamedQuery;
import org.hibernate.boot.jaxb.mapping.spi.JaxbColumnResultImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbConstructorResultImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityResultImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbFieldResultImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbNamedNativeQueryImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbNamedHqlQueryImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbNamedStoredProcedureQueryImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbQueryHint;
import org.hibernate.boot.jaxb.mapping.spi.JaxbQueryHintImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbStoredProcedureParameterImpl;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.internal.ColumnResultJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.ConstructorResultJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.EntityResultJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.FieldResultJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.NamedNativeQueriesAnnotation;
import org.hibernate.boot.models.annotations.internal.NamedNativeQueriesJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.NamedNativeQueryAnnotation;
import org.hibernate.boot.models.annotations.internal.NamedNativeQueryJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.NamedQueriesAnnotation;
import org.hibernate.boot.models.annotations.internal.NamedQueriesJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.NamedQueryAnnotation;
import org.hibernate.boot.models.annotations.internal.NamedQueryJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.NamedStoredProcedureQueriesJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.NamedStoredProcedureQueryJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.QueryHintJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.StoredProcedureParameterJpaAnnotation;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.models.spi.MutableClassDetails;
import org.hibernate.models.spi.ModelsContext;

import jakarta.persistence.ColumnResult;
import jakarta.persistence.ConstructorResult;
import jakarta.persistence.EntityResult;
import jakarta.persistence.FieldResult;
import jakarta.persistence.NamedStoredProcedureQuery;
import jakarta.persistence.QueryHint;
import jakarta.persistence.StoredProcedureParameter;

import static org.hibernate.boot.models.JpaAnnotations.NAMED_STORED_PROCEDURE_QUERY;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;

/**
 * Processing of queries from XML
 *
 * @author Steve Ebersole
 */
public class QueryProcessing {
	public static void applyNamedQueries(
			JaxbEntityImpl jaxbEntity,
			MutableClassDetails classDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( CollectionHelper.isEmpty( jaxbEntity.getNamedQueries() ) ) {
			return;
		}

		final ModelsContext modelBuildingContext = xmlDocumentContext.getModelBuildingContext();
		List<NamedQueryAnnotation> namedHqlQueryList = null;
		List<NamedQueryJpaAnnotation> namedJpqlQueryList = null;

		for ( int i = 0; i < jaxbEntity.getNamedQueries().size(); i++ ) {
			final JaxbNamedHqlQueryImpl jaxbNamedQuery = jaxbEntity.getNamedQueries().get( i );

			if ( CollectionHelper.isNotEmpty( jaxbNamedQuery.getHints() ) ) {
				// treat this as a Jakarta Persistence named-query
				if ( namedJpqlQueryList == null ) {
					namedJpqlQueryList = new ArrayList<>();
				}
				final NamedQueryJpaAnnotation namedJpqlQuery = JpaAnnotations.NAMED_QUERY.createUsage( modelBuildingContext );
				namedJpqlQueryList.add( namedJpqlQuery );
				namedJpqlQuery.apply( jaxbNamedQuery, xmlDocumentContext );
			}
			else {
				// treat this as a named HQL query
				if ( namedHqlQueryList == null ) {
					namedHqlQueryList = new ArrayList<>();
				}
				final NamedQueryAnnotation namedQuery = HibernateAnnotations.NAMED_QUERY.createUsage( xmlDocumentContext.getModelBuildingContext() );
				namedHqlQueryList.add( namedQuery );
				namedQuery.apply( jaxbNamedQuery, xmlDocumentContext );
			}
		}

		if ( namedJpqlQueryList != null ) {
			final NamedQueriesJpaAnnotation namedJpqlQueries = (NamedQueriesJpaAnnotation) classDetails.replaceAnnotationUsage(
					JpaAnnotations.NAMED_QUERY,
					JpaAnnotations.NAMED_QUERIES,
					modelBuildingContext
			);
			namedJpqlQueries.value( namedJpqlQueryList.toArray( jakarta.persistence.NamedQuery[]::new ) );
		}
		if ( namedHqlQueryList != null ) {
			final NamedQueriesAnnotation namedQueries = (NamedQueriesAnnotation) classDetails.replaceAnnotationUsage(
					HibernateAnnotations.NAMED_QUERY,
					HibernateAnnotations.NAMED_QUERIES,
					modelBuildingContext
			);
			namedQueries.value( namedHqlQueryList.toArray( NamedQuery[]::new ) );
		}
	}

	public static FlushModeType interpretFlushMode(FlushMode flushMode) {
		return switch ( flushMode ) {
			case AUTO -> FlushModeType.AUTO;
			case ALWAYS -> FlushModeType.ALWAYS;
			case COMMIT -> FlushModeType.COMMIT;
			case MANUAL -> FlushModeType.MANUAL;
		};
	}

	public static final QueryHint[] NO_HINTS = new QueryHint[0];
	public static QueryHint[] collectQueryHints(List<JaxbQueryHintImpl> jaxbHints, XmlDocumentContext xmlDocumentContext) {
		if ( CollectionHelper.isEmpty( jaxbHints ) ) {
			return NO_HINTS;
		}

		final QueryHint[] hints = new QueryHint[jaxbHints.size()];
		final ModelsContext modelBuildingContext = xmlDocumentContext.getModelBuildingContext();
		for ( int i = 0; i < jaxbHints.size(); i++ ) {
			final QueryHintJpaAnnotation queryHintUsage = JpaAnnotations.QUERY_HINT.createUsage( modelBuildingContext );
			hints[i] = queryHintUsage;
			final JaxbQueryHint jaxbHint = jaxbHints.get(i);
			queryHintUsage.name( jaxbHint.getName() );
			queryHintUsage.value( jaxbHint.getValue() );
		}
		return hints;
	}

	public static void applyNamedNativeQueries(
			JaxbEntityImpl jaxbEntity,
			MutableClassDetails classDetails,
			JaxbEntityMappingsImpl jaxbRoot,
			XmlDocumentContext xmlDocumentContext) {
		if ( CollectionHelper.isEmpty( jaxbEntity.getNamedNativeQueries() ) ) {
			return;
		}

		final ModelsContext modelBuildingContext = xmlDocumentContext.getModelBuildingContext();
		List<NamedNativeQueryAnnotation> namedQueryList = null;
		List<NamedNativeQueryJpaAnnotation> namedJpaQueryList = null;

		for ( int i = 0; i < jaxbEntity.getNamedNativeQueries().size(); i++ ) {
			final JaxbNamedNativeQueryImpl jaxbNamedQuery = jaxbEntity.getNamedNativeQueries().get( i );

			if ( needsJpaNativeQuery( jaxbNamedQuery ) ) {
				// @jakarta.persistence.NamedNativeQuery
				if ( namedJpaQueryList == null ) {
					namedJpaQueryList = new ArrayList<>();
				}
				final NamedNativeQueryJpaAnnotation namedQuery = JpaAnnotations.NAMED_NATIVE_QUERY.createUsage( modelBuildingContext );
				namedJpaQueryList.add( namedQuery );
				namedQuery.apply( jaxbNamedQuery, xmlDocumentContext );
			}
			else {
				// @org.hibernate.annotations.NamedNativeQuery
				if ( namedQueryList == null ) {
					namedQueryList = new ArrayList<>();
				}
				final NamedNativeQueryAnnotation namedQuery = HibernateAnnotations.NAMED_NATIVE_QUERY.createUsage( modelBuildingContext );
				namedQueryList.add( namedQuery );
				namedQuery.apply( jaxbNamedQuery, xmlDocumentContext );
			}
		}

		if ( namedJpaQueryList != null ) {
			final NamedNativeQueriesJpaAnnotation namedQueriesUsage = (NamedNativeQueriesJpaAnnotation) classDetails.replaceAnnotationUsage(
					JpaAnnotations.NAMED_NATIVE_QUERY,
					JpaAnnotations.NAMED_NATIVE_QUERIES,
					modelBuildingContext
			);
			namedQueriesUsage.value( namedJpaQueryList.toArray( jakarta.persistence.NamedNativeQuery[]::new ) );
		}
		if ( namedQueryList != null ) {
			final NamedNativeQueriesAnnotation namedQueriesUsage = (NamedNativeQueriesAnnotation) classDetails.replaceAnnotationUsage(
					HibernateAnnotations.NAMED_NATIVE_QUERY,
					HibernateAnnotations.NAMED_NATIVE_QUERIES,
					modelBuildingContext
			);
			namedQueriesUsage.value( namedQueryList.toArray(NamedNativeQuery[]::new ) );
		}
	}

	private static boolean needsJpaNativeQuery(JaxbNamedNativeQueryImpl jaxbNamedQuery) {
		return CollectionHelper.isNotEmpty( jaxbNamedQuery.getHints() )
				|| CollectionHelper.isNotEmpty( jaxbNamedQuery.getColumnResult() )
				|| CollectionHelper.isNotEmpty( jaxbNamedQuery.getConstructorResult() )
				|| CollectionHelper.isNotEmpty( jaxbNamedQuery.getEntityResult() );
	}


	private static final ColumnResult[] NO_COLUMN_RESULTS = new ColumnResult[0];
	public static ColumnResult[] extractColumnResults(
			List<JaxbColumnResultImpl> jaxbColumnResultList,
			XmlDocumentContext xmlDocumentContext) {
		if ( CollectionHelper.isEmpty( jaxbColumnResultList ) ) {
			return NO_COLUMN_RESULTS;
		}

		final ModelsContext modelBuildingContext = xmlDocumentContext.getModelBuildingContext();
		final ColumnResult[] columnResults = new ColumnResult[jaxbColumnResultList.size()];
		for ( int i = 0; i < jaxbColumnResultList.size(); i++ ) {
			final ColumnResultJpaAnnotation columnResult = JpaAnnotations.COLUMN_RESULT.createUsage( modelBuildingContext );
			columnResults[i] = columnResult;

			final JaxbColumnResultImpl jaxbColumnResult = jaxbColumnResultList.get( i );
			columnResult.name( jaxbColumnResult.getName() );
			if ( isNotEmpty( jaxbColumnResult.getClazz() ) ) {
				columnResult.type( xmlDocumentContext.resolveJavaType( jaxbColumnResult.getClazz() ).toJavaClass() );
			}
		}
		return columnResults;
	}

	private final static ConstructorResult[] NO_CONSTRUCTOR_RESULTS = new ConstructorResult[0];
	public static ConstructorResult[] extractConstructorResults(
			List<JaxbConstructorResultImpl> jaxbConstructorResultList,
			XmlDocumentContext xmlDocumentContext) {
		if ( CollectionHelper.isEmpty( jaxbConstructorResultList ) ) {
			return NO_CONSTRUCTOR_RESULTS;
		}

		final ModelsContext modelBuildingContext = xmlDocumentContext.getModelBuildingContext();
		final ConstructorResult[] constructorResults = new ConstructorResult[jaxbConstructorResultList.size()];
		for ( int i = 0; i < jaxbConstructorResultList.size(); i++ ) {
			final ConstructorResultJpaAnnotation constructorResult = JpaAnnotations.CONSTRUCTOR_RESULT.createUsage( modelBuildingContext );
			constructorResults[i] = constructorResult;

			final JaxbConstructorResultImpl jaxbConstructorResult = jaxbConstructorResultList.get( i );
			constructorResult.targetClass( xmlDocumentContext.resolveJavaType( jaxbConstructorResult.getTargetClass() ).toJavaClass() );
			if ( CollectionHelper.isNotEmpty( jaxbConstructorResult.getColumns() ) ) {
				final ColumnResult[] columnResults = extractColumnResults(
						jaxbConstructorResult.getColumns(),
						xmlDocumentContext
				);
				if ( columnResults != null ) {
					constructorResult.columns( columnResults );
				}
			}
		}
		return constructorResults;
	}

	private static final EntityResult[] NO_ENTITY_RESULTS = new EntityResult[0];
	public static EntityResult[] extractEntityResults(
			List<JaxbEntityResultImpl> jaxbEntityResults,
			XmlDocumentContext xmlDocumentContext) {
		if ( CollectionHelper.isEmpty( jaxbEntityResults ) ) {
			return NO_ENTITY_RESULTS;
		}

		final ModelsContext modelBuildingContext = xmlDocumentContext.getModelBuildingContext();
		final EntityResult[] entityResults = new EntityResult[jaxbEntityResults.size()];
		for ( int i = 0; i < jaxbEntityResults.size(); i++ ) {
			final EntityResultJpaAnnotation entityResult = JpaAnnotations.ENTITY_RESULT.createUsage( modelBuildingContext );
			entityResults[i] = entityResult;

			final JaxbEntityResultImpl jaxbEntityResult = jaxbEntityResults.get( i );
			entityResult.entityClass( xmlDocumentContext.resolveJavaType( jaxbEntityResult.getEntityClass() ).toJavaClass() );
			if ( StringHelper.isNotEmpty( jaxbEntityResult.getDiscriminatorColumn() ) ) {
				entityResult.discriminatorColumn( jaxbEntityResult.getDiscriminatorColumn() );
			}
			if ( jaxbEntityResult.getLockMode() != null ) {
				entityResult.lockMode( jaxbEntityResult.getLockMode() );
			}

			final FieldResult[] fieldResults = extractFieldResults(
					jaxbEntityResult.getFieldResult(),
					xmlDocumentContext
			);
			if ( fieldResults != null ) {
				entityResult.fields( fieldResults );
			}
		}
		return entityResults;
	}

	private static FieldResult[] extractFieldResults(
			List<JaxbFieldResultImpl> jaxbFieldResults,
			XmlDocumentContext xmlDocumentContext) {
		if ( CollectionHelper.isEmpty( jaxbFieldResults ) ) {
			return null;
		}

		final ModelsContext modelBuildingContext = xmlDocumentContext.getModelBuildingContext();
		final FieldResult[] fieldResults = new FieldResult[jaxbFieldResults.size()];
		for ( int i = 0; i < jaxbFieldResults.size(); i++ ) {
			final FieldResultJpaAnnotation fieldResult = JpaAnnotations.FIELD_RESULT.createUsage( modelBuildingContext );
			fieldResults[i] = fieldResult;

			final JaxbFieldResultImpl jaxbFieldResult = jaxbFieldResults.get( i );
			fieldResult.name( jaxbFieldResult.getName() );
			fieldResult.column( jaxbFieldResult.getColumn() );
		}
		return fieldResults;
	}

	public static void applyNamedProcedureQueries(
			JaxbEntityImpl jaxbEntity,
			MutableClassDetails classDetails,
			XmlDocumentContext xmlDocumentContext) {
		final List<JaxbNamedStoredProcedureQueryImpl> jaxbQueries = jaxbEntity.getNamedStoredProcedureQueries();
		if ( CollectionHelper.isEmpty( jaxbQueries ) ) {
			return;
		}

		final ModelsContext modelBuildingContext = xmlDocumentContext.getModelBuildingContext();
		final NamedStoredProcedureQueriesJpaAnnotation namedQueriesUsage = (NamedStoredProcedureQueriesJpaAnnotation) classDetails.replaceAnnotationUsage(
				NAMED_STORED_PROCEDURE_QUERY,
				JpaAnnotations.NAMED_STORED_PROCEDURE_QUERIES,
				modelBuildingContext
		);

		final NamedStoredProcedureQuery[] namedQueries = new NamedStoredProcedureQuery[jaxbQueries.size()];
		namedQueriesUsage.value( namedQueries );

		for ( int i = 0; i < jaxbQueries.size(); i++ ) {
			final NamedStoredProcedureQueryJpaAnnotation namedQuery = NAMED_STORED_PROCEDURE_QUERY.createUsage( modelBuildingContext );
			namedQueries[i] = namedQuery;

			final JaxbNamedStoredProcedureQueryImpl jaxbQuery = jaxbQueries.get( i );
			namedQuery.apply( jaxbQuery, xmlDocumentContext );
		}
	}

	private static final StoredProcedureParameter[] NO_PARAMS = new StoredProcedureParameter[0];
	public static StoredProcedureParameter[] collectParameters(
			List<JaxbStoredProcedureParameterImpl> jaxbParameters,
			XmlDocumentContext xmlDocumentContext) {
		if ( CollectionHelper.isEmpty( jaxbParameters ) ) {
			return NO_PARAMS;
		}

		final ModelsContext ModelsContext = xmlDocumentContext.getModelBuildingContext();
		final StoredProcedureParameter[] result = new StoredProcedureParameter[jaxbParameters.size()];
		for ( int i = 0; i < jaxbParameters.size(); i++ ) {
			final StoredProcedureParameterJpaAnnotation param = JpaAnnotations.STORED_PROCEDURE_PARAMETER.createUsage( ModelsContext );
			result[i] = param;

			final JaxbStoredProcedureParameterImpl jaxbParam = jaxbParameters.get( i );
			param.apply( jaxbParam, xmlDocumentContext );
		}
		return result;
	}

	private static final Class<?>[] NO_CLASSES = new Class[0];
	public static Class<?>[] collectResultClasses(List<String> resultClasses, XmlDocumentContext xmlDocumentContext) {
		if ( CollectionHelper.isEmpty( resultClasses ) ) {
			return NO_CLASSES;
		}
		final Class<?>[] result = new Class<?>[resultClasses.size()];
		for ( int i = 0; i < resultClasses.size(); i++ ) {
			result[i] = xmlDocumentContext.resolveJavaType( resultClasses.get( i ) ).toJavaClass();
		}
		return result;
	}

	public static String[] collectResultMappings(List<String> resultClasses, XmlDocumentContext xmlDocumentContext) {
		if ( CollectionHelper.isEmpty( resultClasses ) ) {
			return ArrayHelper.EMPTY_STRING_ARRAY;
		}
		return resultClasses.toArray( String[]::new );
	}
}
