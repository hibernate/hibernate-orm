/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.annotation;

import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.processor.Context;
import org.hibernate.processor.model.MetaAttribute;
import org.hibernate.processor.model.Metamodel;
import org.hibernate.processor.util.Constants;
import org.hibernate.processor.validation.ProcessorSessionFactory;
import org.hibernate.processor.validation.Validation;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import java.util.List;

import static java.lang.Character.isJavaIdentifierStart;
import static org.hibernate.processor.util.Constants.ENTITY_GRAPH;
import static org.hibernate.processor.util.Constants.HIB_ENABLED_FETCH_PROFILE;
import static org.hibernate.processor.util.Constants.JAVA_OBJECT;
import static org.hibernate.processor.util.Constants.NAMED_QUERY;
import static org.hibernate.processor.util.Constants.TYPED_QUERY_REFERENCE;
import static org.hibernate.processor.util.TypeUtils.containsAnnotation;
import static org.hibernate.processor.util.TypeUtils.getAnnotationMirror;
import static org.hibernate.processor.util.TypeUtils.getAnnotationValue;
import static org.hibernate.processor.util.SqmTypeUtils.resultType;

public abstract class AnnotationMeta implements Metamodel {

	void addAuxiliaryMembers() {
		addAuxiliaryMembersForAnnotation( NAMED_QUERY, "QUERY_" );
		addAuxiliaryMembersForRepeatableAnnotation( Constants.NAMED_QUERIES, "QUERY_" );
		addAuxiliaryMembersForAnnotation( Constants.NAMED_NATIVE_QUERY, "QUERY_" );
		addAuxiliaryMembersForRepeatableAnnotation( Constants.NAMED_NATIVE_QUERIES, "QUERY_" );
		addAuxiliaryMembersForAnnotation( Constants.SQL_RESULT_SET_MAPPING, "MAPPING_" );
		addAuxiliaryMembersForRepeatableAnnotation( Constants.SQL_RESULT_SET_MAPPINGS, "MAPPING_" );
		addAuxiliaryMembersForAnnotation( Constants.NAMED_ENTITY_GRAPH, "GRAPH_" );
		addAuxiliaryMembersForRepeatableAnnotation( Constants.NAMED_ENTITY_GRAPHS, "GRAPH_" );

		addAuxiliaryMembersForAnnotation( Constants.HIB_NAMED_QUERY, "QUERY_" );
		addAuxiliaryMembersForRepeatableAnnotation( Constants.HIB_NAMED_QUERIES, "QUERY_" );
		addAuxiliaryMembersForAnnotation( Constants.HIB_NAMED_NATIVE_QUERY, "QUERY_" );
		addAuxiliaryMembersForRepeatableAnnotation( Constants.HIB_NAMED_NATIVE_QUERIES, "QUERY_" );
		addAuxiliaryMembersForAnnotation( Constants.HIB_FETCH_PROFILE, "PROFILE_" );
		addAuxiliaryMembersForRepeatableAnnotation( Constants.HIB_FETCH_PROFILES, "PROFILE_" );
		addAuxiliaryMembersForAnnotation( Constants.HIB_FILTER_DEF, "FILTER_" );
		addAuxiliaryMembersForRepeatableAnnotation( Constants.HIB_FILTER_DEFS, "FILTER_" );
	}

	void checkNamedQueries() {
		boolean checkHql = containsAnnotation( getElement(), Constants.CHECK_HQL )
						|| containsAnnotation( getElement().getEnclosingElement(), Constants.CHECK_HQL );
		handleNamedQueryAnnotation( NAMED_QUERY, checkHql );
		handleNamedQueryRepeatableAnnotation( Constants.NAMED_QUERIES, checkHql );
		handleNamedQueryAnnotation( Constants.HIB_NAMED_QUERY, checkHql );
		handleNamedQueryRepeatableAnnotation( Constants.HIB_NAMED_QUERIES, checkHql );
	}

	private void handleNamedQueryAnnotation(String annotationName, boolean checkHql) {
		final AnnotationMirror mirror = getAnnotationMirror( getElement(), annotationName );
		if ( mirror != null ) {
			handleNamedQuery( mirror, checkHql );
		}
	}

	private void handleNamedQueryRepeatableAnnotation(String annotationName, boolean checkHql) {
		final AnnotationMirror mirror = getAnnotationMirror( getElement(), annotationName );
		if ( mirror != null ) {
			final AnnotationValue value = getAnnotationValue( mirror );
			if ( value != null ) {
				@SuppressWarnings("unchecked")
				final List<? extends AnnotationValue> annotationValues =
						(List<? extends AnnotationValue>) value.getValue();
				for ( AnnotationValue annotationValue : annotationValues ) {
					handleNamedQuery( (AnnotationMirror) annotationValue.getValue(), checkHql );
				}
			}
		}
	}

	private void handleNamedQuery(AnnotationMirror mirror, boolean checkHql) {
		final AnnotationValue nameValue = getAnnotationValue( mirror, "name" );
		if ( nameValue != null ) {
			final String name = nameValue.getValue().toString();
			final Context context = getContext();
			final boolean reportErrors = context.checkNamedQuery( name );
			final AnnotationValue value = getAnnotationValue( mirror, "query" );
			if ( value != null && value.getValue() instanceof String hql ) {
				final SqmStatement<?> statement =
						Validation.validate(
								hql,
								null,
								true,
								// If we are in the scope of @CheckHQL, semantic errors in the
								// query result in compilation errors. Otherwise, they only
								// result in warnings, so we don't break working code.
								new WarningErrorHandler( context, getElement(), mirror, value, hql,
										reportErrors, checkHql ),
								ProcessorSessionFactory.create( context.getProcessingEnvironment(),
										context.getEntityNameMappings(), context.getEnumTypesByValue(),
										context.isIndexing() )
						);
				if ( !isJakartaDataStyle()
					&& statement instanceof SqmSelectStatement<?> selectStatement ) {
					if ( isQueryMethodName( name ) ) {
						final AnnotationValue annotationValue = getAnnotationValue( mirror, "resultClass" );
						final String resultType = annotationValue != null
								? annotationValue.getValue().toString()
								: resultType( selectStatement );
						putMember( name,
								new NamedQueryMethod(
										this,
										selectStatement,
										name.substring( 1 ),
										isRepository(),
										getSessionType(),
										getSessionVariableName(),
										context.addNonnullAnnotation(),
										resultType
								)
						);
					}
					if ( getAnnotationValue( mirror, "resultClass" ) == null ) {
						final String resultType = resultType( selectStatement );
						if ( resultType != null ) {
							putMember( "QUERY_" + name,
									new TypedMetaAttribute( this, name, "QUERY_", resultType,
											TYPED_QUERY_REFERENCE, hql ) );
						}
					}
				}
			}
		}
	}

	private static boolean isQueryMethodName(String name) {
		return name.length() >= 2
			&& name.charAt(0) == '#'
			&& isJavaIdentifierStart( name.charAt(1) )
			&& name.substring(2).chars().allMatch(Character::isJavaIdentifierPart);
	}

	private void addAuxiliaryMembersForRepeatableAnnotation(String annotationName, String prefix) {
		final AnnotationMirror mirror = getAnnotationMirror( getElement(), annotationName );
		if ( mirror != null ) {
			final AnnotationValue value = getAnnotationValue( mirror );
			if ( value != null ) {
				@SuppressWarnings("unchecked")
				final List<? extends AnnotationValue> annotationValues =
						(List<? extends AnnotationValue>) value.getValue();
				for ( AnnotationValue annotationValue : annotationValues ) {
					addAuxiliaryMembersForMirror( (AnnotationMirror) annotationValue.getValue(), prefix );
				}
			}
		}
	}

	private void addAuxiliaryMembersForAnnotation(String annotationName, String prefix) {
		final AnnotationMirror mirror = getAnnotationMirror( getElement(), annotationName );
		if ( mirror != null ) {
			addAuxiliaryMembersForMirror( mirror, prefix );
		}
	}

	private void addAuxiliaryMembersForMirror(AnnotationMirror mirror, String prefix) {
		if ( !isJakartaDataStyle() ) {
			mirror.getElementValues().forEach((key, value) -> {
				if ( key.getSimpleName().contentEquals("name") ) {
					final String name = value.getValue().toString();
					if ( !name.isEmpty() ) {
						putMember( prefix + name, auxiliaryMember( mirror, prefix, name ) );
					}
				}
			});
		}
	}

	private NameMetaAttribute auxiliaryMember(AnnotationMirror mirror, String prefix, String name) {
		return switch (prefix) {
			case "QUERY_" -> {
				final AnnotationValue resultClass = getAnnotationValue( mirror, "resultClass" );
				// if there is no explicit result class, we will infer it later by
				// type checking the query (this is allowed but not required by JPA)
				// and then we will replace this TypedMetaAttribute
				final String resultTypeName =
						resultClass == null ? JAVA_OBJECT : resultClass.getValue().toString();
				yield new TypedMetaAttribute( this, name, prefix, resultTypeName,
						TYPED_QUERY_REFERENCE, null );
			}
			case "GRAPH_" ->
					new TypedMetaAttribute( this, name, prefix, getQualifiedName(),
							ENTITY_GRAPH, null );
			case "PROFILE_" ->
					new EnabledFetchProfileMetaAttribute( this, name, prefix,
							HIB_ENABLED_FETCH_PROFILE );
			default ->
					new NameMetaAttribute( this, name, prefix );
		};
	}

	protected String getSessionVariableName() {
		return "entityManager";
	}

	abstract boolean isRepository();

	abstract @Nullable String getSessionType();

	abstract void putMember(String name, MetaAttribute nameMetaAttribute);

	/**
	 * Adjusts the severity of validation errors that occur type-checking
	 * a named HQL query, depending on whether we are within the scope
	 * of a {@link org.hibernate.annotations.processing.CheckHQL} annotation.
	 * <p>
	 * We always type-check; but when there's no {@code CheckHQL} in scope,
	 * we report problems as warnings only. This is necessary, since we don't
	 * yet accept absolutely everything that is legal: we don't know about
	 * XML, we don't handle converters, and we don't handle {@code enum}
	 * types very well.
	 */
	private static class WarningErrorHandler extends ErrorHandler {
		private final boolean reportErrors;
		private final boolean checkHql;

		private WarningErrorHandler(
				Context context,
				Element element,
				AnnotationMirror mirror,
				AnnotationValue value, String hql,
				boolean reportErrors,
				boolean checkHql) {
			super(context, element, mirror, value, hql);
			this.reportErrors = reportErrors;
			this.checkHql = checkHql;
		}

		@Override
		public void error(int start, int end, String message) {
			if (reportErrors) {
				if (checkHql) {
					super.error( start, end, message );
				}
				else {
					super.warn( start, end, message );
				}
			}
		}

		@Override
		public void warn(int start, int end, String message) {
			if (reportErrors) {
				super.warn( start, end, message );
			}
		}

		@Override
		public void syntaxError(
				Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine,
				String message, RecognitionException e) {
			if (reportErrors) {
				super.syntaxError( recognizer, offendingSymbol, line, charPositionInLine, message, e );
			}
		}
	}
}
