/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.annotation;

import jakarta.annotation.Nullable;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.util.List;

import static org.hibernate.processor.annotation.QueryMethod.PARAM_INDENT;
import static org.hibernate.processor.util.Constants.HIB_ENABLED_FETCH_PROFILE;
import static org.hibernate.processor.util.Constants.QUERY_OPTIONS;
import static org.hibernate.processor.util.Constants.TIMEOUT;
import static org.hibernate.processor.util.TypeUtils.getAnnotationMirror;
import static org.hibernate.processor.util.TypeUtils.getAnnotationValue;

/**
 * @author Gavin King
 */
final class QueryOptionsSupport {
	private QueryOptionsSupport() {
	}

	static void setQueryOptions(
			AbstractQueryMethod method,
			StringBuilder declaration,
			boolean update,
			boolean nativeQuery) {
		setQueryOptions( method, declaration,
				getAnnotationMirror( method.method, QUERY_OPTIONS ),
				update, nativeQuery );
	}

	static boolean appendEntityGraphArgument(
			AbstractQueryMethod method,
			StringBuilder declaration,
			String entity) {
		final var queryOptions = getAnnotationMirror( method.method, QUERY_OPTIONS );
		if ( queryOptions != null && !method.isReactive() ) {
			final var entityGraph = getAnnotationValue( queryOptions, "entityGraph" );
			if ( entityGraph != null ) {
				final var graphName = entityGraph.getValue().toString();
				if ( !graphName.isEmpty() ) {
					method.localSession( declaration );
					declaration
							.append(".getEntityGraph(")
							.append(method.annotationMetaEntity.importType(entity))
							.append(".class, ")
							.append(stringLiteral(graphName))
							.append(')');
					return true;
				}
			}
		}
		return false;
	}

	static void appendFindOptions(
			AbstractQueryMethod method,
			StringBuilder declaration,
			List<String> fetchProfiles,
			boolean naturalId) {
		if ( method.isReactive() ) {
			return;
		}
		if ( naturalId ) {
			appendFindOption( method, declaration, "org.hibernate.KeyType", "NATURAL" );
		}
		for ( var profile : fetchProfiles ) {
			declaration
					.append( ", new " )
					.append( method.annotationMetaEntity.importType( HIB_ENABLED_FETCH_PROFILE ) )
					.append( "(" )
					.append( profile )
					.append( ")" );
		}
		final var queryOptions = getAnnotationMirror( method.method, QUERY_OPTIONS );
		if ( queryOptions != null ) {
			appendTimeoutOption( method, declaration, queryOptions );
			appendEnumFindOption( method, declaration, queryOptions, "cacheStoreMode" );
			appendEnumFindOption( method, declaration, queryOptions, "cacheRetrieveMode" );
			appendEnumFindOption( method, declaration, queryOptions, "lockMode" );
			appendEnumFindOption( method, declaration, queryOptions, "lockScope" );
		}
	}

	private static void setQueryOptions(
			AbstractQueryMethod method,
			StringBuilder declaration,
			@Nullable AnnotationMirror queryOptions,
			boolean update,
			boolean nativeQuery) {
		if ( queryOptions != null && !method.isReactive() ) {
			setQueryHints( declaration, queryOptions );
			setEntityGraph( method, declaration, queryOptions, update, nativeQuery );
			setTimeout( method, declaration, queryOptions );
			setEnumQueryOption( method, declaration, queryOptions, "flush", "setQueryFlushMode" );
			if ( !update ) {
				setEnumQueryOption( method, declaration, queryOptions, "cacheStoreMode", "setCacheStoreMode" );
				setEnumQueryOption( method, declaration, queryOptions, "cacheRetrieveMode", "setCacheRetrieveMode" );
				setEnumQueryOption( method, declaration, queryOptions, "lockMode", "setLockMode" );
				setEnumQueryOption( method, declaration, queryOptions, "lockScope", "setLockScope" );
			}
		}
	}

	private static void setQueryHints(StringBuilder declaration, AnnotationMirror queryOptions) {
		final var hints = getAnnotationValue( queryOptions, "hints" );
		if ( hints != null ) {
			@SuppressWarnings("unchecked")
			final var values =
					(List<? extends AnnotationValue>)
							hints.getValue();
			for ( var value : values ) {
				final var hint = (AnnotationMirror) value.getValue();
				setQueryHint( declaration,
						annotationString( hint, "name" ),
						annotationString( hint, "value" ) );
			}
		}
	}

	private static void setQueryHint(StringBuilder declaration, String name, String value) {
		declaration
				.append(PARAM_INDENT)
				.append(".setHint(")
				.append(stringLiteral(name))
				.append(", ")
				.append(stringLiteral(value))
				.append(")");
	}

	private static void setEntityGraph(
			AbstractQueryMethod method,
			StringBuilder declaration,
			AnnotationMirror queryOptions,
			boolean update,
			boolean nativeQuery) {
		if ( !update && !nativeQuery ) {
			final var entityGraph = getAnnotationValue( queryOptions, "entityGraph" );
			if ( entityGraph != null ) {
				final var graphName = entityGraph.getValue().toString();
				if ( !graphName.isEmpty() ) {
					declaration
							.append(PARAM_INDENT)
							.append(".setHint(\"jakarta.persistence.loadgraph\", ");
					method.localSession( declaration );
					declaration
							.append(".getEntityGraph(")
							.append(stringLiteral(graphName))
							.append("))");
				}
			}
		}
	}

	private static void setTimeout(
			AbstractQueryMethod method,
			StringBuilder declaration,
			AnnotationMirror queryOptions) {
		final var timeout = getAnnotationValue( queryOptions, "timeout" );
		if ( timeout != null ) {
			declaration
					.append(PARAM_INDENT)
					.append(".setTimeout(")
					.append(method.annotationMetaEntity.importType(TIMEOUT))
					.append(".milliseconds(")
					.append(timeout.getValue())
					.append("))");
		}
	}

	private static void appendTimeoutOption(
			AbstractQueryMethod method,
			StringBuilder declaration,
			AnnotationMirror queryOptions) {
		final var timeout = getAnnotationValue( queryOptions, "timeout" );
		if ( timeout != null ) {
			declaration
					.append(", ")
					.append(method.annotationMetaEntity.importType(TIMEOUT))
					.append(".milliseconds(")
					.append(timeout.getValue())
					.append(")");
		}
	}

	private static void setEnumQueryOption(
			AbstractQueryMethod method,
			StringBuilder declaration,
			AnnotationMirror queryOptions,
			String member,
			String methodName) {
		final var option = getAnnotationValue( queryOptions, member );
		if ( option != null && option.getValue() instanceof VariableElement variable ) {
			final var type = (TypeElement) variable.getEnclosingElement();
			declaration
					.append(PARAM_INDENT)
					.append('.')
					.append(methodName)
					.append('(')
					.append(method.annotationMetaEntity.importType(type.getQualifiedName().toString()))
					.append('.')
					.append(variable.getSimpleName())
					.append(')');
		}
	}

	private static void appendEnumFindOption(
			AbstractQueryMethod method,
			StringBuilder declaration,
			AnnotationMirror queryOptions,
			String member) {
		final var option = getAnnotationValue( queryOptions, member );
		if ( option != null && option.getValue() instanceof VariableElement variable ) {
			final var type = (TypeElement) variable.getEnclosingElement();
			appendFindOption( method, declaration,
					type.getQualifiedName().toString(),
					variable.getSimpleName().toString() );
		}
	}

	private static void appendFindOption(
			AbstractQueryMethod method,
			StringBuilder declaration,
			String type,
			String member) {
		declaration
				.append(", ")
				.append(method.annotationMetaEntity.importType(type))
				.append('.')
				.append(member);
	}

	private static String annotationString(AnnotationMirror annotation, String member) {
		final var value = getAnnotationValue( annotation, member );
		return value == null ? "" : value.getValue().toString();
	}

	static String stringLiteral(String string) {
		final var literal =
				new StringBuilder( string.length() + 2 )
						.append('"');
		for ( int i = 0; i < string.length(); i++ ) {
			final var character = string.charAt( i );
			literal.append(switch ( character ) {
				case '\b' -> "\\b";
				case '\t' -> "\\t";
				case '\n' -> "\\n";
				case '\f' -> "\\f";
				case '\r' -> "\\r";
				case '"' -> "\\\"";
				case '\\' -> "\\\\";
				default -> character;
			});
		}
		return literal.append('"').toString();
	}
}
