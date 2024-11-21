/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.annotation;


import javax.lang.model.element.ExecutableElement;

import java.util.Set;

import static java.lang.Character.toUpperCase;
import static org.hibernate.processor.util.Constants.UNI;

public class LifecycleMethod extends AbstractAnnotatedMethod {
	private final String entity;
	private final String methodName;
	private final String parameterName;
	private final String operationName;
	private final boolean addNonnullAnnotation;
	private final boolean iterateParameter;
	private final boolean returnArgument;
	private final boolean hasGeneratedId;

	public LifecycleMethod(
			AnnotationMetaEntity annotationMetaEntity,
			ExecutableElement method,
			String entity,
			String methodName,
			String parameterName,
			String sessionName,
			String sessionType,
			String operationName,
			boolean addNonnullAnnotation,
			boolean iterateParameter,
			boolean returnArgument,
			boolean hasGeneratedId) {
		super(annotationMetaEntity, method, sessionName, sessionType);
		this.entity = entity;
		this.methodName = methodName;
		this.parameterName = parameterName;
		this.operationName = operationName;
		this.addNonnullAnnotation = addNonnullAnnotation;
		this.iterateParameter = iterateParameter;
		this.returnArgument = returnArgument;
		this.hasGeneratedId = hasGeneratedId;
	}

	@Override
	public boolean hasTypedAttribute() {
		return true;
	}

	@Override
	public boolean hasStringAttribute() {
		return false;
	}

	private String capitalize(String string) {
		return toUpperCase(string.charAt(0)) + string.substring(1);
	}

	static final Set<String> eventTypes = Set.of("insert", "update", "delete");

	@Override
	public String getAttributeDeclarationString() {
		StringBuilder declaration = new StringBuilder();
		preamble(declaration);
		nullCheck(declaration);
		preEvent( declaration );
		declaration.append("\ttry {\n");
		delegateCall(declaration);
		returnArgumentReactively(declaration);
		declaration.append("\t}\n");
		if ( operationName.equals("insert") ) {
			convertException(declaration,
					"org.hibernate.exception.ConstraintViolationException",
					"jakarta.data.exceptions.EntityExistsException");
		}
		else {
			convertException(declaration,
					"org.hibernate.StaleStateException",
					"jakarta.data.exceptions.OptimisticLockingFailureException");
		}
		convertException(declaration,
				"jakarta.persistence.PersistenceException",
				"jakarta.data.exceptions.DataException");
		postEvent( declaration );
		returnArgument(declaration);
		declaration.append("}");
		return declaration.toString();
	}

	private void postEvent(StringBuilder declaration) {
		if ( annotationMetaEntity.getContext().addDependentAnnotation()
				&& eventTypes.contains( operationName )
				&& !isReactive() ) {
			final String postEventType = "Post" + capitalize( operationName ) + "Event";
			annotationMetaEntity.importType( "jakarta.data.event." + postEventType );
			declaration
					.append( "\tevent.select(new TypeLiteral<" )
					.append( postEventType )
					.append( "<" )
					.append( annotationMetaEntity.importType( entity ) )
					.append( ">>(){}).fire(new " )
					.append( postEventType )
					.append( "<>(" )
					.append( parameterName )
					.append( "));\n" );
		}
	}

	private void preEvent(StringBuilder declaration) {
		if ( annotationMetaEntity.getContext().addDependentAnnotation()
				&& eventTypes.contains( operationName )
				&& !isReactive()) {
			final String preEventType = "Pre" + capitalize( operationName ) + "Event";
			annotationMetaEntity.importType( "jakarta.data.event." + preEventType );
			annotationMetaEntity.importType( "jakarta.data.event.LifecycleEvent" );
			annotationMetaEntity.importType( "jakarta.enterprise.util.TypeLiteral" );
			annotationMetaEntity.importType( "jakarta.enterprise.event.Event" );
			annotationMetaEntity.importType( "jakarta.inject.Inject" );
			declaration
					.append( "\tevent.select(new TypeLiteral<" )
					.append( preEventType )
					.append( "<" )
					.append( annotationMetaEntity.importType( entity ) )
					.append( ">>(){}).fire(new " )
					.append( preEventType )
					.append( "<>(" )
					.append( parameterName )
					.append( "));\n" );
		}
	}

	private void returnArgumentReactively(StringBuilder declaration) {
		if ( isReactive() ) {
			if ( returnArgument ) {
				declaration
						.append(".replaceWith(")
						.append(parameterName)
						.append(")")
						.append(";\n");
			}
			else {
				declaration
						.append(";\n");
			}
		}
	}

	private void returnArgument(StringBuilder declaration) {
		if ( returnArgument && !isReactive() ) {
			declaration
					.append( "\treturn " )
					.append( parameterName )
					.append( ";\n" );
		}
	}

	private void delegateCall(StringBuilder declaration) {
		if ( isReactive() ) {
			declaration
					.append("\t\treturn ")
					.append(sessionName);
			if ( isReactiveSessionAccess() ) {
				declaration
						.append(".chain(")
						.append(localSessionName())
						.append(" -> ")
						.append(localSessionName());
			}
			declaration
					.append('.')
					.append(operationName)
					.append('(')
					.append(parameterName)
					.append(')');
			if ( isReactiveSessionAccess() ) {
				declaration
						.append(')');
			}
		}
		else {
			if ( iterateParameter ) {
				declaration
						.append("\t\tfor (var _entity : ")
						.append(parameterName)
						.append(") {\n\t");
			}
			if ( "upsert".equals(operationName) && hasGeneratedId ) {
				declaration
						.append("\t\tif (")
						.append(sessionName)
						.append(".getIdentifier(")
						.append(iterateParameter ? "_entity" : parameterName)
						.append(") == null)\n")
						.append("\t\t\t")
						.append(sessionName)
						.append('.')
						.append("insert")
						.append('(')
						.append(iterateParameter ? "_entity" : parameterName)
						.append(')')
						.append(";\n")
						.append("\t\telse\n\t");
			}
			declaration
					.append("\t\t")
					.append(sessionName)
					.append('.')
					.append(operationName)
					.append('(')
					.append(iterateParameter ? "_entity" : parameterName)
					.append(')')
					.append(";\n");
			if ( iterateParameter ) {
				declaration
						.append("\t\t}\n");
			}
		}
	}

	private void preamble(StringBuilder declaration) {
		declaration
				.append("\n@Override\npublic ")
				.append(returnType())
				.append(' ')
				.append(methodName)
				.append('(');
		notNull(declaration);
		declaration
				.append(annotationMetaEntity.importType(entity))
				.append(' ')
				.append(parameterName)
				.append(')')
				.append(" {\n");
	}

	private String returnType() {
		final String entityType = annotationMetaEntity.importType(entity);
		if ( isReactive() ) {
			return annotationMetaEntity.importType(UNI)
					+ '<' + (returnArgument ? entityType : "Void") + '>';
		}
		else {
			return returnArgument
					? entityType
					: "void";
		}
	}

	private void nullCheck(StringBuilder declaration) {
		declaration
				.append("\tif (")
				.append(parameterName)
				.append(" == null) throw new IllegalArgumentException(\"Null ")
				.append(parameterName)
				.append("\");\n");
	}

	private void convertException(StringBuilder declaration, String exception, String convertedException) {
		declaration
				.append("\tcatch (")
				.append(annotationMetaEntity.importType(exception))
				.append(" exception) {\n")
				.append("\t\tthrow new ")
				.append(annotationMetaEntity.importType(convertedException))
				.append("(exception.getMessage(), exception);\n")
				.append("\t}\n");
	}

	private void notNull(StringBuilder declaration) {
		if ( addNonnullAnnotation ) {
			declaration
					.append('@')
					.append(annotationMetaEntity.importType("jakarta.annotation.Nonnull"))
					.append(' ');
		}
	}

	@Override
	public String getAttributeNameDeclarationString() {
		throw new UnsupportedOperationException("operation not supported");
	}

	@Override
	public String getMetaType() {
		throw new UnsupportedOperationException("operation not supported");
	}

	@Override
	public String getPropertyName() {
		return methodName;
	}

	@Override
	public String getTypeDeclaration() {
		return entity;
	}
}
