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
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbNamedNativeQueryImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbNamedQueryBase;
import org.hibernate.boot.jaxb.mapping.spi.JaxbNamedQueryImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbQueryHint;
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
import jakarta.persistence.QueryHint;

import static org.hibernate.internal.util.StringHelper.isEmpty;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;

/**
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
					final MutableAnnotationUsage<jakarta.persistence.NamedQueries> namedJpqlQueriesUsage = AnnotationUsageHelper.getOrCreateUsage(
							JpaAnnotations.NAMED_QUERIES,
							classDetails,
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
					final MutableAnnotationUsage<NamedQueries> namedHqlQueriesUsage = AnnotationUsageHelper.getOrCreateUsage(
							HibernateAnnotations.NAMED_QUERIES,
							classDetails,
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
		final MutableAnnotationUsage<NamedQuery> namedQueryUsage = HibernateAnnotations.NAMED_QUERY.createUsage(
				classDetails,
				modelBuildingContext
		);
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
				switch ( cacheMode ) {
					case GET -> {
						AnnotationUsageHelper.applyAttributeIfSpecified( "cacheRegion", cacheMode, namedQueryUsage );
						AnnotationUsageHelper.applyAttributeIfSpecified( "cacheRegion", cacheMode, namedQueryUsage );
					}
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
		final MutableAnnotationUsage<jakarta.persistence.NamedQuery> namedQueryUsage = JpaAnnotations.NAMED_QUERY.createUsage(
				classDetails,
				modelBuildingContext
		);
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
				final MutableAnnotationUsage<QueryHint> queryHintUsage = JpaAnnotations.QUERY_HINT.createUsage(
						classDetails,
						modelBuildingContext
				);
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
					final MutableAnnotationUsage<jakarta.persistence.NamedNativeQueries> namedQueriesUsage = AnnotationUsageHelper.getOrCreateUsage(
							JpaAnnotations.NAMED_NATIVE_QUERIES,
							classDetails,
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
					final MutableAnnotationUsage<NamedNativeQueries> namedQueriesUsage = AnnotationUsageHelper.getOrCreateUsage(
							HibernateAnnotations.NAMED_NATIVE_QUERIES,
							classDetails,
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
		final MutableAnnotationUsage<jakarta.persistence.NamedNativeQuery> namedQueryUsage = JpaAnnotations.NAMED_NATIVE_QUERY.createUsage(
				classDetails,
				modelBuildingContext
		);
		namedQueryUsageList.add( namedQueryUsage );

		namedQueryUsage.setAttributeValue( "name", jaxbNamedQuery.getName() );
		namedQueryUsage.setAttributeValue( "query", jaxbNamedQuery.getQuery() );
		applyQueryHints( jaxbNamedQuery, classDetails, namedQueryUsage, modelBuildingContext );

		applyResultClassAndSynchronizations( jaxbNamedQuery, namedQueryUsage, modelBuildingContext, xmlDocumentContext );
		applyResultSetMappings( jaxbNamedQuery, namedQueryUsage, modelBuildingContext, xmlDocumentContext );
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
			SourceModelBuildingContext modelBuildingContext,
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
			for ( JaxbColumnResultImpl jaxbColumnResult : jaxbNamedQuery.getColumnResult() ) {
				final MutableAnnotationUsage<ColumnResult> columnResultUsage = JpaAnnotations.COLUMN_RESULT.createUsage(
						annotationTarget,
						modelBuildingContext
				);
				columnResultUsage.setAttributeValue( "name", jaxbColumnResult.getName() );
				if ( isNotEmpty( jaxbColumnResult.getClazz() ) ) {
					final String className = xmlDocumentContext.resolveClassName( jaxbColumnResult.getClazz() );
					columnResultUsage.setAttributeValue( "type", modelBuildingContext.getClassDetailsRegistry().getClassDetails( className ) );
				}
			}
		}

		// todo (7.0) : finish the rest
	}

	private static void applyHibernateNativeQuery(
			JaxbNamedNativeQueryImpl jaxbNamedQuery,
			MutableClassDetails classDetails,
			List<MutableAnnotationUsage<NamedNativeQuery>> namedQueryUsageList,
			SourceModelBuildingContext modelBuildingContext,
			XmlDocumentContext xmlDocumentContext) {
		final MutableAnnotationUsage<NamedNativeQuery> namedQueryUsage = HibernateAnnotations.NAMED_NATIVE_QUERY.createUsage(
				classDetails,
				modelBuildingContext
		);
		namedQueryUsageList.add( namedQueryUsage );

		namedQueryUsage.setAttributeValue( "name", jaxbNamedQuery.getName() );
		namedQueryUsage.setAttributeValue( "query", jaxbNamedQuery.getQuery() );

		applyResultClassAndSynchronizations( jaxbNamedQuery, namedQueryUsage, modelBuildingContext, xmlDocumentContext );
		applyResultSetMappings( jaxbNamedQuery, namedQueryUsage, modelBuildingContext, xmlDocumentContext );
	}

	public static void applyNamedProcedureQueries(
			JaxbEntityImpl jaxbEntity,
			MutableClassDetails classDetails,
			XmlDocumentContext xmlDocumentContext) {
		// todo (7.0) : implement
	}

	private static void applyNamedQueryBaseDetails(
			JaxbNamedQueryBase baseDetails,
			MutableAnnotationUsage<?> namedQueryUsage,
			MutableClassDetails classDetails,
			SourceModelBuildingContext modelBuildingContext) {
		assert isNotEmpty( baseDetails.getName() );
		namedQueryUsage.setAttributeValue( "name", baseDetails.getName() );

		if ( CollectionHelper.isNotEmpty( baseDetails.getHints() ) ) {
			final ArrayList<AnnotationUsage<QueryHint>> hints = CollectionHelper.arrayList( baseDetails.getHints().size() );
			namedQueryUsage.setAttributeValue( "hints", hints );

			for ( JaxbQueryHint jaxbHint : baseDetails.getHints() ) {
				final MutableAnnotationUsage<QueryHint> queryHintUsage = JpaAnnotations.QUERY_HINT.createUsage( classDetails, modelBuildingContext );
				queryHintUsage.setAttributeValue( "name", jaxbHint.getName() );
				queryHintUsage.setAttributeValue( "value", jaxbHint.getValue() );
			}
		}
	}
}
