/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.annotation;

import org.hibernate.processor.model.MetaAttribute;
import org.hibernate.processor.model.Metamodel;
import org.hibernate.processor.util.TypeUtils;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.hibernate.metamodel.mapping.EntityIdentifierMapping.ID_ROLE_NAME;
import static org.hibernate.processor.util.Constants.ENTITY_AGENT;
import static org.hibernate.processor.util.Constants.ENTITY_MANAGER;
import static org.hibernate.processor.util.Constants.COMPLETABLE_FUTURE;
import static org.hibernate.processor.util.Constants.COMPLETION_STAGE;
import static org.hibernate.processor.util.Constants.JD_ASYNCHRONOUS;
import static org.hibernate.processor.util.Constants.SPRING_ENTITY_AGENT_PROVIDER;
import static org.hibernate.processor.util.Constants.SPRING_ENTITY_MANAGER_PROVIDER;
import static org.hibernate.processor.util.Constants.OBJECTS;
import static org.hibernate.processor.util.Constants.VOID;
import static org.hibernate.processor.util.TypeUtils.hasAnnotation;

/**
 * @author Gavin King
 */
public abstract class AbstractAnnotatedMethod implements MetaAttribute {

	final AnnotationMetaEntity annotationMetaEntity;
	final ExecutableElement method;
	final String sessionType;
	final String sessionName;

	public AbstractAnnotatedMethod(
			AnnotationMetaEntity annotationMetaEntity,
			ExecutableElement method,
			String sessionName, String sessionType) {
		this.annotationMetaEntity = annotationMetaEntity;
		this.method = method;
		this.sessionName = sessionName;
		this.sessionType = sessionType;
	}

	@Override
	public Metamodel getHostingEntity() {
		return annotationMetaEntity;
	}

	boolean isUsingEntityManager() {
		return ENTITY_MANAGER.equals(sessionType)
			|| SPRING_ENTITY_MANAGER_PROVIDER.equals(sessionType);
	}

	boolean isUsingEntityAgent() {
		return ENTITY_AGENT.equals(sessionType)
			|| SPRING_ENTITY_AGENT_PROVIDER.equals(sessionType);
	}

	boolean isUsingEntityHandler() {
		return isUsingEntityManager()
			|| isUsingEntityAgent();
	}

	boolean isUsingStatelessSession() {
		return annotationMetaEntity.isStatelessSession();
	}

	boolean isReactive() {
		return annotationMetaEntity.isReactive();
	}

	boolean isReactiveSessionAccess() {
		return annotationMetaEntity.isReactiveSessionAccess();
	}

	String localSessionName() {
		return isReactiveSessionAccess() ? "_session" : sessionName;
	}

	String getObjectCall() {
		return annotationMetaEntity.isProvidedSessionAccess()
			&& !sessionName.endsWith("()")
				? ".getObject()"
				: "";
	}

	void localSession(StringBuilder declaration) {
		declaration.append(localSessionName());
		if ( !isReactive() ) {
			declaration.append(getObjectCall());
		}
	}

	@Override
	public List<AnnotationMirror> inheritedAnnotations() {
		if ( annotationMetaEntity.isJakartaDataRepository() ) {
			return method.getAnnotationMirrors().stream()
					.filter(TypeUtils::isInheritedAnnotation)
					.collect(toList());
		}
		else {
			return emptyList();
		}
	}

	void nullCheck(StringBuilder declaration, String paramName) {
		declaration
				.append('\t')
				.append(annotationMetaEntity.staticImport(OBJECTS, "requireNonNull"))
				.append('(')
				.append(parameterName(paramName))
				.append(", \"Null ")
				.append(ID_ROLE_NAME.equals(paramName) ? "id" : paramName)
				.append("\");\n");
	}

	boolean isAsynchronousCompletionStage() {
		return hasAnnotation( method, JD_ASYNCHRONOUS )
			&& isCompletionStage( method.getReturnType() );
	}

	boolean isAsynchronousCompletionStageWithVoidResult() {
		final var resultType = completionStageResultType();
		return resultType != null && isVoid( resultType );
	}

	TypeMirror completionStageResultType() {
		return completionStageResultType( method.getReturnType() );
	}

	void returnResult(StringBuilder declaration) {
		declaration.append( "return " );
		if ( isAsynchronousCompletionStage() ) {
			declaration
					.append( annotationMetaEntity.staticImport( COMPLETABLE_FUTURE, "completedStage" ) )
					.append( '(' );
		}
	}

	void endReturnResult(StringBuilder declaration) {
		if ( isAsynchronousCompletionStage() ) {
			declaration.append( ')' );
		}
	}

	void returnNullResult(StringBuilder declaration) {
		returnResult( declaration );
		declaration.append( "null" );
		endReturnResult( declaration );
	}

	private static boolean isCompletionStage(TypeMirror returnType) {
		return completionStageResultType( returnType ) != null;
	}

	private static TypeMirror completionStageResultType(TypeMirror returnType) {
		if ( returnType.getKind() == TypeKind.DECLARED ) {
			final var declaredType = (DeclaredType) returnType;
			final var typeElement = (TypeElement) declaredType.asElement();
			return typeElement.getQualifiedName().contentEquals( COMPLETION_STAGE )
				&& declaredType.getTypeArguments().size() == 1
					? declaredType.getTypeArguments().get( 0 )
					: null;
		}
		return null;
	}

	private static boolean isVoid(TypeMirror returnType) {
		final var returnTypeKind = returnType.getKind();
		if ( returnTypeKind == TypeKind.VOID ) {
			return true;
		}
		else if ( returnTypeKind == TypeKind.DECLARED ) {
			final var declaredType = (DeclaredType) returnType;
			final var typeElement = (TypeElement) declaredType.asElement();
			return typeElement.getQualifiedName().contentEquals( VOID );
		}
		else {
			return false;
		}
	}

	static String parameterName(String name) {
		return name.equals(ID_ROLE_NAME)
				? "id"
				: name.replace('.', '$');
	}

	protected void handle(StringBuilder declaration, String handled, String rethrown) {
		if ( isReactive() ) {
			declaration
					.append( "\n\t\t\t.onFailure(" )
					.append( annotationMetaEntity.importType( handled ) )
					.append( ".class)\n" )
					.append( "\t\t\t\t\t.transform(_ex -> new " )
					.append( annotationMetaEntity.importType( rethrown ) )
					.append( "(_ex.getMessage(), _ex))" );

		}
		else {
			declaration
					.append( "\tcatch (" )
					.append( annotationMetaEntity.importType( handled ) )
					.append( " _ex) {\n" )
					.append( "\t\tthrow new " )
					.append( annotationMetaEntity.importType( rethrown ) )
					.append( "(_ex.getMessage(), _ex);\n" )
					.append( "\t}\n" );
		}
	}
}
