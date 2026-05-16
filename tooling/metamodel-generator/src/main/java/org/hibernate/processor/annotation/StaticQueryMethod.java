/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.annotation;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.processor.model.MetaAttribute;
import org.hibernate.processor.model.Metamodel;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.util.ArrayList;
import java.util.List;

import static org.hibernate.processor.annotation.AbstractAnnotatedMethod.parameterName;
import static org.hibernate.processor.annotation.QueryOptionsSupport.stringLiteral;
import static org.hibernate.processor.util.Constants.LIST;
import static org.hibernate.processor.util.Constants.MAP;
import static org.hibernate.processor.util.Constants.STATEMENT_REFERENCE;
import static org.hibernate.processor.util.Constants.STATIC_STATEMENT_REFERENCE;
import static org.hibernate.processor.util.Constants.STATIC_TYPED_QUERY_REFERENCE;
import static org.hibernate.processor.util.Constants.TIMEOUT;
import static org.hibernate.processor.util.Constants.TYPED_QUERY_REFERENCE;
import static org.hibernate.processor.util.NullnessUtil.castNonNull;
import static org.hibernate.processor.util.TypeUtils.getAnnotationValue;

/**
 * @author Gavin King
 */
class StaticQueryMethod implements MetaAttribute {
	private final AnnotationMetaEntity annotationMetaEntity;
	private final ExecutableElement method;
	private final String methodName;
	private final String queryMethodName;
	private final boolean statement;
	private final boolean nativeQuery;
	private final @Nullable String resultTypeName;
	private final @Nullable String resultTypeClass;
	private final List<String> paramNames;
	private final List<String> paramTypes;
	private final @Nullable AnnotationMirror queryOptions;

	StaticQueryMethod(
			AnnotationMetaEntity annotationMetaEntity,
			ExecutableElement method,
			String methodName,
			String queryMethodName,
			boolean statement,
			boolean nativeQuery,
			@Nullable String resultTypeName,
			@Nullable String resultTypeClass,
			List<String> paramNames,
			List<String> paramTypes,
			@Nullable AnnotationMirror queryOptions) {
		this.annotationMetaEntity = annotationMetaEntity;
		this.method = method;
		this.methodName = methodName;
		this.queryMethodName = queryMethodName;
		this.statement = statement;
		this.nativeQuery = nativeQuery;
		this.resultTypeName = resultTypeName;
		this.resultTypeClass = resultTypeClass;
		this.paramNames = paramNames;
		this.paramTypes = paramTypes;
		this.queryOptions = queryOptions;
	}

	@Override
	public boolean hasTypedAttribute() {
		return true;
	}

	@Override
	public boolean hasStringAttribute() {
		return false;
	}

	@Override
	public String getAttributeDeclarationString() {
		final StringBuilder declaration = new StringBuilder();
		comment( declaration );
		modifiers( declaration );
		returnType( declaration );
		parameters( declaration );
		declaration
				.append(" {\n\treturn new ")
				.append(annotationMetaEntity.importType(
						statement ? STATIC_STATEMENT_REFERENCE : STATIC_TYPED_QUERY_REFERENCE ))
				.append(statement ? "(" : "<>(");
		constructorArguments( declaration );
		declaration
				.append("\n\t);\n}");
		return declaration.toString();
	}

	private void comment(StringBuilder declaration) {
		declaration
				.append("\n/**")
				.append("\n * Return a reference to the query named {@code ")
				.append(queryName())
				.append("}.")
				.append("\n *")
				.append("\n * @see ")
				.append(annotationMetaEntity.getQualifiedName())
				.append("#")
				.append(queryMethodName)
				.append("(")
				.append(parameterList())
				.append(")")
				.append("\n **/\n");
	}

	private String queryName() {
		return annotationMetaEntity.getSimpleName() + "." + queryMethodName;
	}

	private void modifiers(StringBuilder declaration) {
		declaration.append("public static ");
	}

	private void returnType(StringBuilder declaration) {
		if ( statement ) {
			declaration
					.append(annotationMetaEntity.importType(STATEMENT_REFERENCE))
					.append(' ');
		}
		else {
			declaration
					.append(annotationMetaEntity.importType(TYPED_QUERY_REFERENCE))
					.append('<')
					.append(annotationMetaEntity.importType(castNonNull(resultTypeName)))
					.append("> ");
		}
		declaration.append(methodName);
	}

	private String parameterList() {
		return paramTypes.stream()
				.map(StaticQueryMethod::erasedType)
				.map(annotationMetaEntity::importType)
				.reduce((x, y) -> x + ',' + y)
				.orElse("");
	}

	private void parameters(StringBuilder declaration) {
		declaration.append('(');
		for ( int i = 0; i < paramNames.size(); i++ ) {
			if ( i > 0 ) {
				declaration.append(", ");
			}
			declaration
					.append(annotationMetaEntity.importType(parameterType( i )))
					.append(' ')
					.append(parameterName(paramNames.get( i )));
		}
		declaration.append(')');
	}

	private String parameterType(int index) {
		final String paramType = paramTypes.get( index );
		if ( method.isVarArgs() && index == paramTypes.size() - 1 && paramType.endsWith("[]") ) {
			return paramType.substring( 0, paramType.length() - 2 ) + "...";
		}
		else {
			return paramType;
		}
	}

	private void constructorArguments(StringBuilder declaration) {
		declaration
				.append("\n\t\t\t")
				.append(stringLiteral(queryName()))
				.append(",\n\t\t\t")
				.append(annotationMetaEntity.importType(annotationMetaEntity.getQualifiedName()))
				.append(".class,\n\t\t\t")
				.append(stringLiteral(methodName));
		if ( !statement ) {
			declaration
					.append(",\n\t\t\t")
					.append(annotationMetaEntity.importType(erasedType(castNonNull(resultTypeClass))))
					.append(".class");
		}
		declaration
				.append(",\n\t\t\t")
				.append(classList())
				.append(",\n\t\t\t")
				.append(nameList())
				.append(",\n\t\t\t")
				.append(argumentList());
		queryOptions( declaration );
	}

	private String classList() {
		final StringBuilder list = new StringBuilder()
				.append(annotationMetaEntity.importType(LIST))
				.append(".of(");
		for ( int i = 0; i < paramTypes.size(); i++ ) {
			if ( i > 0 ) {
				list.append(", ");
			}
			list
					.append(annotationMetaEntity.importType(erasedType(paramTypes.get( i ))))
					.append(".class");
		}
		return list.append(')').toString();
	}

	private String nameList() {
		final StringBuilder list = new StringBuilder()
				.append(annotationMetaEntity.importType(LIST))
				.append(".of(");
		for ( int i = 0; i < paramNames.size(); i++ ) {
			if ( i > 0 ) {
				list.append(", ");
			}
			list.append(stringLiteral(paramNames.get( i )));
		}
		return list.append(')').toString();
	}

	private String argumentList() {
		final StringBuilder list = new StringBuilder()
				.append(annotationMetaEntity.importType(LIST))
				.append(".of(");
		for ( int i = 0; i < paramNames.size(); i++ ) {
			if ( i > 0 ) {
				list.append(", ");
			}
			list
					.append("(Object) ")
					.append(parameterName(paramNames.get( i )));
		}
		return list.append(')').toString();
	}

	private void queryOptions(StringBuilder declaration) {
		final AnnotationMirror optionsAnnotation = queryOptions;
		if ( optionsAnnotation != null ) {
			final String entityGraph = statement || nativeQuery ? null : entityGraph( optionsAnnotation );
			final String hints = hints( optionsAnnotation );
			final List<String> options = options( optionsAnnotation );
			if ( entityGraph != null || hints != null || !options.isEmpty() ) {
				if ( statement ) {
					declaration
							.append(",\n\t\t\t")
							.append(hints == null ? emptyMap() : hints);
				}
				else {
					declaration
							.append(",\n\t\t\t")
							.append(entityGraph == null ? "null" : entityGraph)
							.append(",\n\t\t\t")
							.append(hints == null ? emptyMap() : hints);
				}
				for ( String option : options ) {
					declaration
							.append(",\n\t\t\t")
							.append(option);
				}
			}
		}
	}

	private @Nullable String entityGraph(AnnotationMirror queryOptions) {
		final AnnotationValue entityGraph = getAnnotationValue( queryOptions, "entityGraph" );
		if ( entityGraph != null ) {
			final String value = entityGraph.getValue().toString();
			if ( !value.isEmpty() ) {
				return stringLiteral( value );
			}
		}
		return null;
	}

	private String emptyMap() {
		return annotationMetaEntity.importType(MAP) + ".of()";
	}

	private @Nullable String hints(AnnotationMirror queryOptions) {
		final AnnotationValue hints = getAnnotationValue( queryOptions, "hints" );
		if ( hints != null ) {
			@SuppressWarnings("unchecked")
			final List<? extends AnnotationValue> values =
					(List<? extends AnnotationValue>) hints.getValue();
			if ( !values.isEmpty() ) {
				final StringBuilder builder = new StringBuilder()
						.append(annotationMetaEntity.importType(MAP))
						.append(".ofEntries(");
				for ( int i = 0; i < values.size(); i++ ) {
					if ( i > 0 ) {
						builder.append(", ");
					}
					final AnnotationMirror hint = (AnnotationMirror) values.get( i ).getValue();
					builder
							.append(annotationMetaEntity.importType(MAP))
							.append(".entry(")
							.append(stringLiteral(annotationString( hint, "name" )))
							.append(", ")
							.append(stringLiteral(annotationString( hint, "value" )))
							.append(')');
				}
				return builder.append(')').toString();
			}
		}
		return null;
	}

	private String annotationString(AnnotationMirror annotation, String member) {
		final AnnotationValue value = getAnnotationValue( annotation, member );
		return value == null ? "" : value.getValue().toString();
	}

	private List<String> options(AnnotationMirror queryOptions) {
		final List<String> options = new ArrayList<>();
		addEnumOption( options, queryOptions, "flush" );
		addTimeoutOption( options, queryOptions );
		if ( !statement ) {
			addEnumOption( options, queryOptions, "cacheStoreMode" );
			addEnumOption( options, queryOptions, "cacheRetrieveMode" );
			addEnumOption( options, queryOptions, "lockMode" );
			addEnumOption( options, queryOptions, "lockScope" );
		}
		return options;
	}

	private void addTimeoutOption(List<String> options, AnnotationMirror queryOptions) {
		final AnnotationValue timeout = getAnnotationValue( queryOptions, "timeout" );
		if ( timeout != null ) {
			options.add(annotationMetaEntity.importType(TIMEOUT)
					+ ".milliseconds(" + timeout.getValue() + ")");
		}
	}

	private void addEnumOption(List<String> options, AnnotationMirror queryOptions, String member) {
		final AnnotationValue option = getAnnotationValue( queryOptions, member );
		if ( option != null && option.getValue() instanceof VariableElement variable ) {
			final TypeElement type = (TypeElement) variable.getEnclosingElement();
			options.add(annotationMetaEntity.importType(type.getQualifiedName().toString())
					+ "." + variable.getSimpleName());
		}
	}

	private static String erasedType(String type) {
		String result = type;
		while ( result.startsWith( "@" ) ) {
			final int index = result.lastIndexOf( ' ' );
			if ( index > 0 ) {
				result = result.substring( index + 1 );
			}
			else {
				break;
			}
		}
		final int typeArgumentIndex = result.indexOf( '<' );
		if ( typeArgumentIndex >= 0 ) {
			result = result.substring( 0, typeArgumentIndex );
		}
		if ( result.endsWith("...") ) {
			result = result.substring( 0, result.length() - 3 ) + "[]";
		}
		return result;
	}

	@Override
	public String getAttributeNameDeclarationString() {
		throw new UnsupportedOperationException("operation not supported");
	}

	@Override
	public String getMetaType() {
		return statement ? STATEMENT_REFERENCE : TYPED_QUERY_REFERENCE;
	}

	@Override
	public String getPropertyName() {
		return methodName;
	}

	@Override
	public String getTypeDeclaration() {
		return statement ? STATEMENT_REFERENCE : TYPED_QUERY_REFERENCE;
	}

	@Override
	public Metamodel getHostingEntity() {
		return annotationMetaEntity;
	}
}
