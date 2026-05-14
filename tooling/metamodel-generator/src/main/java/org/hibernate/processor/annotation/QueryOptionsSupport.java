/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.annotation;

import org.checkerframework.checker.nullness.qual.Nullable;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.util.List;

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
		setQueryOptions( method, declaration, getAnnotationMirror( method.method, QUERY_OPTIONS ), update, nativeQuery );
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
		final AnnotationValue hints = getAnnotationValue( queryOptions, "hints" );
		if ( hints != null ) {
			@SuppressWarnings("unchecked")
			final List<? extends AnnotationValue> values =
					(List<? extends AnnotationValue>) hints.getValue();
			for ( AnnotationValue value : values ) {
				final AnnotationMirror hint = (AnnotationMirror) value.getValue();
				setQueryHint( declaration, annotationString( hint, "name" ), annotationString( hint, "value" ) );
			}
		}
	}

	private static void setQueryHint(StringBuilder declaration, String name, String value) {
		declaration
				.append("\n\t\t\t.setHint(")
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
			final AnnotationValue entityGraph = getAnnotationValue( queryOptions, "entityGraph" );
			if ( entityGraph != null ) {
				final String graphName = entityGraph.getValue().toString();
				if ( !graphName.isEmpty() ) {
					declaration
							.append("\n\t\t\t.setHint(\"jakarta.persistence.loadgraph\", ");
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
		final AnnotationValue timeout = getAnnotationValue( queryOptions, "timeout" );
		if ( timeout != null ) {
			declaration
					.append("\n\t\t\t.setTimeout(")
					.append(method.annotationMetaEntity.importType(TIMEOUT))
					.append(".milliseconds(")
					.append(timeout.getValue())
					.append("))");
		}
	}

	private static void setEnumQueryOption(
			AbstractQueryMethod method,
			StringBuilder declaration,
			AnnotationMirror queryOptions,
			String member,
			String methodName) {
		final AnnotationValue option = getAnnotationValue( queryOptions, member );
		if ( option != null && option.getValue() instanceof VariableElement variable ) {
			final TypeElement type = (TypeElement) variable.getEnclosingElement();
			declaration
					.append("\n\t\t\t.")
					.append(methodName)
					.append("(")
					.append(method.annotationMetaEntity.importType(type.getQualifiedName().toString()))
					.append(".")
					.append(variable.getSimpleName())
					.append(")");
		}
	}

	private static String annotationString(AnnotationMirror annotation, String member) {
		final AnnotationValue value = getAnnotationValue( annotation, member );
		return value == null ? "" : value.getValue().toString();
	}

	static String stringLiteral(String string) {
		final StringBuilder literal =
				new StringBuilder( string.length() + 2 )
						.append('"');
		for ( int i = 0; i < string.length(); i++ ) {
			final char character = string.charAt( i );
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
