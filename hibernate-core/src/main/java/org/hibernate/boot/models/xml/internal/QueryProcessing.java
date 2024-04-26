/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.xml.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.CacheMode;
import org.hibernate.annotations.NamedNativeQueries;
import org.hibernate.annotations.NamedNativeQuery;
import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;
import org.hibernate.boot.jaxb.mapping.spi.JaxbColumnResultImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbConstructorResultImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityResultImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbFieldResultImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbNamedNativeQueryImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbNamedQueryBase;
import org.hibernate.boot.jaxb.mapping.spi.JaxbNamedQueryImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbNamedStoredProcedureQueryImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbQueryHint;
import org.hibernate.boot.jaxb.mapping.spi.JaxbStoredProcedureParameterImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbSynchronizedTableImpl;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.internal.AnnotationUsageHelper;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.models.spi.AnnotationTarget;
import org.hibernate.models.spi.AnnotationUsage;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MutableAnnotationUsage;
import org.hibernate.models.spi.MutableClassDetails;
import org.hibernate.models.spi.SourceModelBuildingContext;

import jakarta.persistence.ColumnResult;
import jakarta.persistence.ConstructorResult;
import jakarta.persistence.EntityResult;
import jakarta.persistence.FieldResult;
import jakarta.persistence.NamedStoredProcedureQueries;
import jakarta.persistence.NamedStoredProcedureQuery;
import jakarta.persistence.QueryHint;
import jakarta.persistence.StoredProcedureParameter;

import static org.hibernate.boot.models.internal.AnnotationUsageHelper.applyAttributeIfSpecified;
import static org.hibernate.boot.models.internal.AnnotationUsageHelper.applyStringAttributeIfSpecified;
import static org.hibernate.internal.util.StringHelper.isEmpty;
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

		final SourceModelBuildingContext modelBuildingContext = xmlDocumentContext.getModelBuildingContext();
		List<MutableAnnotationUsage<NamedQuery>> namedHqlQueryList = null;
		List<MutableAnnotationUsage<jakarta.persistence.NamedQuery>> namedJpqlQueryList = null;
		
		for ( int i = 0; i < jaxbEntity.getNamedQueries().size(); i++ ) {
			final JaxbNamedQueryImpl jaxbNamedQuery = jaxbEntity.getNamedQueries().get( i );

			if ( CollectionHelper.isNotEmpty( jaxbNamedQuery.getHints() ) ) {
				// treat this as a Jakarta Persistence named-query
				if ( namedJpqlQueryList == null ) {
					final MutableAnnotationUsage<jakarta.persistence.NamedQueries> namedJpqlQueriesUsage = classDetails.replaceAnnotationUsage(
							JpaAnnotations.NAMED_QUERY,
							JpaAnnotations.NAMED_QUERIES,
							modelBuildingContext
					);
					classDetails.addAnnotationUsage( namedJpqlQueriesUsage );

					namedJpqlQueryList = new ArrayList<>();
					namedJpqlQueriesUsage.setAttributeValue( "value", namedJpqlQueryList );
				}
				applyNamedJpqlQuery( jaxbNamedQuery, classDetails, namedJpqlQueryList, modelBuildingContext );
			}
			else {
				// treat this as a named HQL query
				if ( namedHqlQueryList == null ) {
					final MutableAnnotationUsage<NamedQueries> namedHqlQueriesUsage = classDetails.replaceAnnotationUsage(
							HibernateAnnotations.NAMED_QUERY,
							HibernateAnnotations.NAMED_QUERIES,
							modelBuildingContext
					);
					classDetails.addAnnotationUsage( namedHqlQueriesUsage );

					namedHqlQueryList = new ArrayList<>();
					namedHqlQueriesUsage.setAttributeValue( "value", namedHqlQueryList );
				}
				applyNamedHqlQuery( jaxbNamedQuery, classDetails, namedHqlQueryList, modelBuildingContext );
			}
		}
	}

	public static void applyNamedHqlQuery(
			JaxbNamedQueryImpl jaxbNamedQuery,
			MutableClassDetails classDetails,
			List<MutableAnnotationUsage<NamedQuery>> namedQueryList,
			SourceModelBuildingContext modelBuildingContext) {
		final MutableAnnotationUsage<NamedQuery> namedQueryUsage = HibernateAnnotations.NAMED_QUERY.createUsage( modelBuildingContext );
		namedQueryList.add( namedQueryUsage );

		namedQueryUsage.setAttributeValue( "name", jaxbNamedQuery.getName() );
		namedQueryUsage.setAttributeValue( "query", jaxbNamedQuery.getQuery() );

		AnnotationUsageHelper.applyAttributeIfSpecified( "comment", jaxbNamedQuery.getComment(), namedQueryUsage );

		AnnotationUsageHelper.applyAttributeIfSpecified( "readOnly", jaxbNamedQuery.isReadOnly(), namedQueryUsage );
		AnnotationUsageHelper.applyAttributeIfSpecified( "flushMode", jaxbNamedQuery.getFlushMode(), namedQueryUsage );
		if ( jaxbNamedQuery.isCacheable() == Boolean.TRUE ) {
			namedQueryUsage.setAttributeValue( "cacheable", true );
			AnnotationUsageHelper.applyAttributeIfSpecified( "cacheRegion", jaxbNamedQuery.getCacheRegion(), namedQueryUsage );
			AnnotationUsageHelper.applyAttributeIfSpecified( "cacheMode", jaxbNamedQuery.getCacheMode(), namedQueryUsage );

			final CacheMode cacheMode = jaxbNamedQuery.getCacheMode();
			if ( cacheMode != null && cacheMode != CacheMode.IGNORE ) {
				if ( cacheMode == CacheMode.GET ) {
					applyAttributeIfSpecified( "cacheRegion", cacheMode, namedQueryUsage );
					applyAttributeIfSpecified( "cacheRegion", cacheMode, namedQueryUsage );
				}
				AnnotationUsageHelper.applyAttributeIfSpecified( "cacheRegion", cacheMode, namedQueryUsage );
				AnnotationUsageHelper.applyAttributeIfSpecified( "cacheRegion", cacheMode, namedQueryUsage );
			}
		}
		AnnotationUsageHelper.applyAttributeIfSpecified( "fetchSize", jaxbNamedQuery.getFetchSize(), namedQueryUsage );
		AnnotationUsageHelper.applyAttributeIfSpecified( "timeout", jaxbNamedQuery.getTimeout(), namedQueryUsage );

		AnnotationUsageHelper.applyAttributeIfSpecified( "timeout", jaxbNamedQuery.getTimeout(), namedQueryUsage );
	}

	private static void applyNamedJpqlQuery(
			JaxbNamedQueryImpl jaxbNamedQuery,
			ClassDetails classDetails,
			List<MutableAnnotationUsage<jakarta.persistence.NamedQuery>> namedQueryList,
			SourceModelBuildingContext modelBuildingContext) {
		final MutableAnnotationUsage<jakarta.persistence.NamedQuery> namedQueryUsage = JpaAnnotations.NAMED_QUERY.createUsage( modelBuildingContext );
		namedQueryList.add( namedQueryUsage );

		namedQueryUsage.setAttributeValue( "name", jaxbNamedQuery.getName() );
		namedQueryUsage.setAttributeValue( "query", jaxbNamedQuery.getQuery() );

		applyQueryHints( jaxbNamedQuery, classDetails, namedQueryUsage, modelBuildingContext );
	}

	private static void applyQueryHints(
			JaxbNamedQueryBase jaxbNamedQuery,
			ClassDetails classDetails,
			MutableAnnotationUsage<?> namedQueryUsage, 
			SourceModelBuildingContext modelBuildingContext) {
		if ( CollectionHelper.isNotEmpty( jaxbNamedQuery.getHints() ) ) {
			final ArrayList<AnnotationUsage<QueryHint>> hints = CollectionHelper.arrayList( jaxbNamedQuery.getHints().size() );
			namedQueryUsage.setAttributeValue( "hints", hints );

			for ( JaxbQueryHint jaxbHint : jaxbNamedQuery.getHints() ) {
				final MutableAnnotationUsage<QueryHint> queryHintUsage = JpaAnnotations.QUERY_HINT.createUsage( modelBuildingContext );
				queryHintUsage.setAttributeValue( "name", jaxbHint.getName() );
				queryHintUsage.setAttributeValue( "value", jaxbHint.getValue() );
			}
		}
	}

	public static void applyNamedNativeQueries(
			JaxbEntityImpl jaxbEntity,
			MutableClassDetails classDetails,
			JaxbEntityMappingsImpl jaxbRoot,
			XmlDocumentContext xmlDocumentContext) {
		if ( CollectionHelper.isEmpty( jaxbEntity.getNamedNativeQueries() ) ) {
			return;
		}


		final SourceModelBuildingContext modelBuildingContext = xmlDocumentContext.getModelBuildingContext();
		List<MutableAnnotationUsage<NamedNativeQuery>> namedHibernateQueryList = null;
		List<MutableAnnotationUsage<jakarta.persistence.NamedNativeQuery>> namedJpaQueryList = null;
		
		for ( int i = 0; i < jaxbEntity.getNamedNativeQueries().size(); i++ ) {
			final JaxbNamedNativeQueryImpl jaxbNamedQuery = jaxbEntity.getNamedNativeQueries().get( i );

			if ( needsJpaNativeQuery( jaxbNamedQuery ) ) {
				// @jakarta.persistence.NamedNativeQuery
				if ( namedJpaQueryList == null ) {
					final MutableAnnotationUsage<jakarta.persistence.NamedNativeQueries> namedQueriesUsage = classDetails.replaceAnnotationUsage(
							JpaAnnotations.NAMED_NATIVE_QUERY,
							JpaAnnotations.NAMED_NATIVE_QUERIES,
							modelBuildingContext
					);
					classDetails.addAnnotationUsage( namedQueriesUsage );

					namedJpaQueryList = new ArrayList<>();
					namedQueriesUsage.setAttributeValue( "value", namedQueriesUsage );
				}

				applyJpaNativeQuery( jaxbNamedQuery, classDetails, namedJpaQueryList, modelBuildingContext, xmlDocumentContext );
			}
			else {
				// @org.hibernate.annotations.NamedNativeQuery
				if ( namedHibernateQueryList == null ) {
					final MutableAnnotationUsage<NamedNativeQueries> namedQueriesUsage = classDetails.replaceAnnotationUsage(
							HibernateAnnotations.NAMED_NATIVE_QUERY,
							HibernateAnnotations.NAMED_NATIVE_QUERIES,
							modelBuildingContext
					);
					classDetails.addAnnotationUsage( namedQueriesUsage );

					namedHibernateQueryList = new ArrayList<>();
					namedQueriesUsage.setAttributeValue( "value", namedHibernateQueryList );
				}

				applyHibernateNativeQuery( jaxbNamedQuery, classDetails, namedHibernateQueryList, modelBuildingContext, xmlDocumentContext );
			}
		}
	}

	private static boolean needsJpaNativeQuery(JaxbNamedNativeQueryImpl jaxbNamedQuery) {
		return CollectionHelper.isNotEmpty( jaxbNamedQuery.getHints() )
				|| CollectionHelper.isNotEmpty( jaxbNamedQuery.getColumnResult() )
				|| CollectionHelper.isNotEmpty( jaxbNamedQuery.getConstructorResult() )
				|| CollectionHelper.isNotEmpty( jaxbNamedQuery.getEntityResult() );
	}

	private static void applyJpaNativeQuery(
			JaxbNamedNativeQueryImpl jaxbNamedQuery,
			MutableClassDetails classDetails,
			List<MutableAnnotationUsage<jakarta.persistence.NamedNativeQuery>> namedQueryUsageList,
			SourceModelBuildingContext modelBuildingContext,
			XmlDocumentContext xmlDocumentContext) {
		final MutableAnnotationUsage<jakarta.persistence.NamedNativeQuery> namedQueryUsage = JpaAnnotations.NAMED_NATIVE_QUERY.createUsage( modelBuildingContext );
		namedQueryUsageList.add( namedQueryUsage );

		namedQueryUsage.setAttributeValue( "name", jaxbNamedQuery.getName() );
		namedQueryUsage.setAttributeValue( "query", jaxbNamedQuery.getQuery() );
		applyQueryHints( jaxbNamedQuery, classDetails, namedQueryUsage, modelBuildingContext );

		applyResultClassAndSynchronizations( jaxbNamedQuery, namedQueryUsage, modelBuildingContext, xmlDocumentContext );
		applyResultSetMappings( jaxbNamedQuery, namedQueryUsage, xmlDocumentContext );
		applyResults( jaxbNamedQuery, namedQueryUsage, classDetails, modelBuildingContext, xmlDocumentContext );
	}

	private static void applyResultClassAndSynchronizations(
			JaxbNamedNativeQueryImpl jaxbNamedQuery,
			MutableAnnotationUsage<?> namedQueryUsage,
			SourceModelBuildingContext modelBuildingContext,
			XmlDocumentContext xmlDocumentContext) {
		final List<String> syncSpaces = new ArrayList<>();

		if ( jaxbNamedQuery.getResultClass() != null ) {
			final String resultClassName = xmlDocumentContext.resolveClassName( jaxbNamedQuery.getResultClass() );
			syncSpaces.add( resultClassName );
			namedQueryUsage.setAttributeValue(
					"resultClass",
					modelBuildingContext.getClassDetailsRegistry().getClassDetails( resultClassName )
			);
		}

		for ( JaxbSynchronizedTableImpl synchronization : jaxbNamedQuery.getSynchronizations() ) {
			syncSpaces.add( synchronization.getTable() );
		}

		if ( CollectionHelper.isNotEmpty( syncSpaces ) ) {
			namedQueryUsage.setAttributeValue( "querySpaces", syncSpaces );
		}
	}

	private static void applyResultSetMappings(
			JaxbNamedNativeQueryImpl jaxbNamedQuery,
			MutableAnnotationUsage<?> namedQueryUsage,
			XmlDocumentContext xmlDocumentContext) {
		if ( isEmpty( jaxbNamedQuery.getResultSetMapping() ) ) {
			return;
		}

		namedQueryUsage.setAttributeValue( "resultSetMapping", jaxbNamedQuery.getResultSetMapping() );
	}

	private static void applyResults(
			JaxbNamedNativeQueryImpl jaxbNamedQuery,
			MutableAnnotationUsage<jakarta.persistence.NamedNativeQuery> namedQueryUsage,
			AnnotationTarget annotationTarget,
			SourceModelBuildingContext modelBuildingContext,
			XmlDocumentContext xmlDocumentContext) {
		if ( CollectionHelper.isNotEmpty( jaxbNamedQuery.getColumnResult() ) ) {
			final ArrayList<MutableAnnotationUsage<ColumnResult>> columnResultList = extractColumnResults(
					jaxbNamedQuery.getColumnResult(),
					annotationTarget,
					modelBuildingContext,
					xmlDocumentContext
			);
			namedQueryUsage.setAttributeValue( "columns", columnResultList );
		}

		if ( CollectionHelper.isNotEmpty( jaxbNamedQuery.getConstructorResult() ) ) {
			final ArrayList<MutableAnnotationUsage<ConstructorResult>> constructorResultList = extractConstructorResults(
					jaxbNamedQuery.getConstructorResult(),
					annotationTarget,
					modelBuildingContext,
					xmlDocumentContext
			);
			namedQueryUsage.setAttributeValue( "classes", constructorResultList );
		}

		if ( CollectionHelper.isNotEmpty( jaxbNamedQuery.getEntityResult() ) ) {
			final ArrayList<MutableAnnotationUsage<EntityResult>> entityResultList = extractEntityResults(
					jaxbNamedQuery.getEntityResult(),
					annotationTarget,
					modelBuildingContext,
					xmlDocumentContext
			);
			namedQueryUsage.setAttributeValue( "entities", entityResultList );
		}
	}

	private static ArrayList<MutableAnnotationUsage<ColumnResult>> extractColumnResults(
			List<JaxbColumnResultImpl> jaxbColumnResultList,
			AnnotationTarget annotationTarget,
			SourceModelBuildingContext modelBuildingContext,
			XmlDocumentContext xmlDocumentContext) {
		assert CollectionHelper.isNotEmpty( jaxbColumnResultList );

		final ArrayList<MutableAnnotationUsage<ColumnResult>> columnResultList = CollectionHelper.arrayList( jaxbColumnResultList.size() );
		for ( JaxbColumnResultImpl jaxbColumnResult : jaxbColumnResultList ) {
			final MutableAnnotationUsage<ColumnResult> columnResultUsage = JpaAnnotations.COLUMN_RESULT.createUsage( modelBuildingContext );
			columnResultList.add( columnResultUsage );
			columnResultUsage.setAttributeValue( "name", jaxbColumnResult.getName() );
			if ( isNotEmpty( jaxbColumnResult.getClazz() ) ) {
				columnResultUsage.setAttributeValue( "type", xmlDocumentContext.resolveJavaType( jaxbColumnResult.getClazz() ) );
			}
		}
		return columnResultList;
	}

	private static ArrayList<MutableAnnotationUsage<ConstructorResult>> extractConstructorResults(
			List<JaxbConstructorResultImpl> jaxbConstructorResultList,
			AnnotationTarget annotationTarget,
			SourceModelBuildingContext modelBuildingContext,
			XmlDocumentContext xmlDocumentContext) {
		assert CollectionHelper.isNotEmpty( jaxbConstructorResultList );

		final ArrayList<MutableAnnotationUsage<ConstructorResult>> constructorResultList = CollectionHelper.arrayList( jaxbConstructorResultList.size() );
		for ( JaxbConstructorResultImpl jaxbConstructorResult : jaxbConstructorResultList ) {
			final MutableAnnotationUsage<ConstructorResult> constructorResultUsage = JpaAnnotations.CONSTRUCTOR_RESULT.createUsage( modelBuildingContext );
			constructorResultList.add( constructorResultUsage );

			constructorResultUsage.setAttributeValue( "targetClass", xmlDocumentContext.resolveJavaType( jaxbConstructorResult.getTargetClass() ) );

			if ( CollectionHelper.isNotEmpty( jaxbConstructorResult.getColumns() ) ) {
				final ArrayList<MutableAnnotationUsage<ColumnResult>> columnResultList = extractColumnResults(
						jaxbConstructorResult.getColumns(),
						annotationTarget,
						modelBuildingContext,
						xmlDocumentContext
				);
				constructorResultUsage.setAttributeValue( "columns", columnResultList );
			}
		}
		return constructorResultList;
	}

	private static ArrayList<MutableAnnotationUsage<EntityResult>> extractEntityResults(
			List<JaxbEntityResultImpl> jaxbEntityResults,
			AnnotationTarget annotationTarget,
			SourceModelBuildingContext modelBuildingContext,
			XmlDocumentContext xmlDocumentContext) {
		assert CollectionHelper.isNotEmpty( jaxbEntityResults );

		final ArrayList<MutableAnnotationUsage<EntityResult>> entityResultList = CollectionHelper.arrayList( jaxbEntityResults.size() );
		for ( JaxbEntityResultImpl jaxbEntityResult : jaxbEntityResults ) {
			final MutableAnnotationUsage<EntityResult> entityResultUsage = JpaAnnotations.ENTITY_RESULT.createUsage( modelBuildingContext );
			entityResultList.add( entityResultUsage );

			entityResultUsage.setAttributeValue( "entityClass", xmlDocumentContext.resolveJavaType( jaxbEntityResult.getEntityClass() ) );
			applyAttributeIfSpecified( "lockMode", jaxbEntityResult.getLockMode(), entityResultUsage );
			applyStringAttributeIfSpecified( "discriminatorColumn", jaxbEntityResult.getDiscriminatorColumn(), entityResultUsage );

			if ( CollectionHelper.isNotEmpty( jaxbEntityResult.getFieldResult() ) ) {
				final ArrayList<MutableAnnotationUsage<FieldResult>> fieldResultList = extractFieldResults(
						jaxbEntityResult.getFieldResult(),
						annotationTarget,
						modelBuildingContext,
						xmlDocumentContext
				);
				entityResultUsage.setAttributeValue( "fields", fieldResultList );
			}
		}

		return entityResultList;
	}

	private static ArrayList<MutableAnnotationUsage<FieldResult>> extractFieldResults(
			List<JaxbFieldResultImpl> jaxbFieldResults,
			AnnotationTarget annotationTarget,
			SourceModelBuildingContext modelBuildingContext,
			XmlDocumentContext xmlDocumentContext) {
		assert CollectionHelper.isNotEmpty( jaxbFieldResults );

		final ArrayList<MutableAnnotationUsage<FieldResult>> fieldResultList = CollectionHelper.arrayList( jaxbFieldResults.size() );
		for ( JaxbFieldResultImpl jaxbFieldResult : jaxbFieldResults ) {
			final MutableAnnotationUsage<FieldResult> fieldResultUsage = JpaAnnotations.FIELD_RESULT.createUsage( modelBuildingContext );
			fieldResultList.add( fieldResultUsage );

			fieldResultUsage.setAttributeValue( "name", jaxbFieldResult.getName() );
			fieldResultUsage.setAttributeValue( "column", jaxbFieldResult.getColumn() );
		}

		return fieldResultList;
	}

	private static void applyHibernateNativeQuery(
			JaxbNamedNativeQueryImpl jaxbNamedQuery,
			MutableClassDetails classDetails,
			List<MutableAnnotationUsage<NamedNativeQuery>> namedQueryUsageList,
			SourceModelBuildingContext modelBuildingContext,
			XmlDocumentContext xmlDocumentContext) {
		final MutableAnnotationUsage<NamedNativeQuery> namedQueryUsage = HibernateAnnotations.NAMED_NATIVE_QUERY.createUsage( modelBuildingContext );
		namedQueryUsageList.add( namedQueryUsage );

		namedQueryUsage.setAttributeValue( "name", jaxbNamedQuery.getName() );
		namedQueryUsage.setAttributeValue( "query", jaxbNamedQuery.getQuery() );

		applyResultClassAndSynchronizations( jaxbNamedQuery, namedQueryUsage, modelBuildingContext, xmlDocumentContext );
		applyResultSetMappings( jaxbNamedQuery, namedQueryUsage, xmlDocumentContext );
	}

	public static void applyNamedProcedureQueries(
			JaxbEntityImpl jaxbEntity,
			MutableClassDetails classDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( CollectionHelper.isEmpty( jaxbEntity.getNamedStoredProcedureQueries() ) ) {
			return;
		}

		final SourceModelBuildingContext modelBuildingContext = xmlDocumentContext.getModelBuildingContext();
		final MutableAnnotationUsage<NamedStoredProcedureQueries> namedQueriesUsage = classDetails.replaceAnnotationUsage(
				JpaAnnotations.NAMED_STORED_PROCEDURE_QUERY,
				JpaAnnotations.NAMED_STORED_PROCEDURE_QUERIES,
				modelBuildingContext
		);
		final List<AnnotationUsage<NamedStoredProcedureQuery>> namedQueryList = CollectionHelper.arrayList( jaxbEntity.getNamedStoredProcedureQueries().size() );
		namedQueriesUsage.setAttributeValue( "value", namedQueryList );

		for ( JaxbNamedStoredProcedureQueryImpl jaxbQuery : jaxbEntity.getNamedStoredProcedureQueries() ) {
			final MutableAnnotationUsage<NamedStoredProcedureQuery> namedQueryUsage = JpaAnnotations.NAMED_STORED_PROCEDURE_QUERY.createUsage( modelBuildingContext );
			namedQueryList.add( namedQueryUsage );

			namedQueryUsage.setAttributeValue( "name", jaxbQuery.getName() );
			namedQueryUsage.setAttributeValue( "procedureName", jaxbQuery.getProcedureName() );

			applyQueryHints( jaxbQuery, classDetails, namedQueryUsage, modelBuildingContext );
			applyQueryParameters( jaxbQuery, classDetails, namedQueryUsage, xmlDocumentContext );
			applyResultSetMappings( jaxbQuery, namedQueryUsage, xmlDocumentContext );
			applyResultClasses( jaxbQuery, namedQueryUsage, xmlDocumentContext );
		}
	}

	private static void applyQueryParameters(
			JaxbNamedStoredProcedureQueryImpl jaxbQuery,
			MutableClassDetails classDetails,
			MutableAnnotationUsage<NamedStoredProcedureQuery> queryUsage,
			XmlDocumentContext xmlDocumentContext) {
		if ( CollectionHelper.isEmpty( jaxbQuery.getProcedureParameters() ) ) {
			return;
		}

		final SourceModelBuildingContext sourceModelBuildingContext = xmlDocumentContext.getModelBuildingContext();

		final ArrayList<AnnotationUsage<StoredProcedureParameter>> parameterList = CollectionHelper.arrayList( jaxbQuery.getProcedureParameters().size() );
		queryUsage.setAttributeValue( "parameters", parameterList );

		for ( JaxbStoredProcedureParameterImpl jaxbParameter : jaxbQuery.getProcedureParameters() ) {
			final MutableAnnotationUsage<StoredProcedureParameter> parameterUsage = JpaAnnotations.STORED_PROCEDURE_PARAMETER.createUsage( sourceModelBuildingContext );
			parameterList.add( parameterUsage );

			applyStringAttributeIfSpecified( "name", jaxbParameter.getName(), parameterUsage );
			applyAttributeIfSpecified( "mode", jaxbParameter.getMode(), parameterUsage );

			parameterUsage.setAttributeValue( "type", xmlDocumentContext.resolveJavaType( jaxbParameter.getClazz() ) );
		}
	}

	private static void applyResultSetMappings(
			JaxbNamedStoredProcedureQueryImpl jaxbQuery,
			MutableAnnotationUsage<NamedStoredProcedureQuery> namedQueryUsage,
			XmlDocumentContext xmlDocumentContext) {
		if ( CollectionHelper.isEmpty( jaxbQuery.getResultSetMappings() ) ) {
			return;
		}

		namedQueryUsage.setAttributeValue( "resultSetMappings", jaxbQuery.getResultSetMappings() );
	}

	private static void applyResultClasses(
			JaxbNamedStoredProcedureQueryImpl jaxbQuery,
			MutableAnnotationUsage<NamedStoredProcedureQuery> namedQueryUsage,
			XmlDocumentContext xmlDocumentContext) {
		if ( CollectionHelper.isEmpty( jaxbQuery.getResultClasses() ) ) {
			return;
		}

		final ArrayList<ClassDetails> resultClasses = CollectionHelper.arrayList( jaxbQuery.getResultClasses().size() );
		namedQueryUsage.setAttributeValue( "resultClasses", resultClasses );

		for ( String resultClass : jaxbQuery.getResultClasses() ) {
			final MutableClassDetails resultClassDetails = xmlDocumentContext.resolveJavaType( resultClass );
			resultClasses.add( resultClassDetails );
		}
	}

}
