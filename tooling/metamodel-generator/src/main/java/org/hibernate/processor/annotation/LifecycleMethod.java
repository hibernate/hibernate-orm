/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.annotation;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;
import java.util.Collection;
import java.util.StringJoiner;

import static java.util.stream.Collectors.toSet;
import static javax.lang.model.type.TypeKind.VOID;
import static org.hibernate.internal.util.NullnessUtil.castNonNull;
import static org.hibernate.processor.util.Constants.COMPLETION_STAGE;
import static org.hibernate.processor.util.Constants.HIB_STATELESS_SESSION;
import static org.hibernate.processor.util.Constants.LIST;
import static org.hibernate.processor.util.Constants.NONNULL;
import static org.hibernate.processor.util.Constants.UNI;
import static org.hibernate.processor.util.TypeUtils.resolveTypeName;

public class LifecycleMethod extends AbstractAnnotatedMethod {
	private final String entity;
	private final String methodName;
	private final String parameterName;
	private final String operationName;
	private final boolean addNonnullAnnotation;
	private final ParameterKind parameterKind;
	private final boolean returnArgument;
	private final boolean hasGeneratedId;
	private final Collection<String> methodTypeParameters;
	private final TypeElement element;

	public enum ParameterKind {
		NORMAL,
		ARRAY,
		LIST
	}

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
			ParameterKind parameterKind,
			boolean returnArgument,
			boolean hasGeneratedId,
			TypeElement element) {
		super(annotationMetaEntity, method, sessionName, sessionType);
		this.entity = entity;
		this.methodName = methodName;
		this.parameterName = parameterName;
		this.operationName = operationName;
		this.addNonnullAnnotation = addNonnullAnnotation;
		this.parameterKind = parameterKind;
		this.returnArgument = returnArgument;
		this.hasGeneratedId = hasGeneratedId;
		this.methodTypeParameters = method.getTypeParameters().stream()
				.map( TypeParameterElement::asType )
				.map( TypeMirror::toString )
				.collect( toSet() );
		this.element = element;
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
		var declaration = new StringBuilder();
		preamble(declaration);
		nullCheck(declaration, parameterName);
		declareReturnArgument( declaration );
		if ( !isReactive() ) {
			declaration.append( "\ttry {\n" );
		}
		delegateCall(declaration);
		returnArgumentReactively(declaration);
		if ( !isReactive() ) {
			declaration.append( "\t}\n" );
		}
		convertExceptions( declaration );
		if ( isReactive() ) {
			declaration
					.append( ";\n" );
		}
		returnArgument(declaration);
		returnNullCompletionStage( declaration );
		declaration.append("}");
		return declaration.toString();
	}

	private void declareReturnArgument(StringBuilder declaration) {
		if ( isMerge() && parameterKind != ParameterKind.NORMAL && !isReactive() ) {
			declaration
					.append("\t")
					.append(resolveAsString( returnArgumentType() ))
					.append(" _result;\n");
		}
	}

	private void returnArgument(StringBuilder declaration) {
		if ( returnArgument && !isReactive() ) {
			declaration
					.append( "\t" );
			returnResult( declaration );
			declaration.append( returnedArgumentName() );
			endReturnResult( declaration );
			declaration.append( ";\n" );
		}
	}

	private void returnNullCompletionStage(StringBuilder declaration) {
		if ( !returnArgument
				&& !isReactive()
				&& isAsynchronousCompletionStageWithVoidResult() ) {
			declaration.append( "\t" );
			returnNullResult( declaration );
			declaration.append( ";\n" );
		}
	}

	private String returnedArgumentName() {
		return isMerge()
			&& parameterKind != ParameterKind.NORMAL
				? "_result"
				: parameterName;
	}

	private void returnArgumentReactively(StringBuilder declaration) {
		if ( returnArgument && isReactive() ) {
			declaration
					.append( "\n\t\t\t.replaceWith(" )
					.append( parameterName )
					.append( ")" );
		}
	}


	private void convertExceptions(StringBuilder declaration) {
		if ( operationName.equals("insert") ) {
			handle( declaration,
					"jakarta.persistence.EntityExistsException",
					"jakarta.data.exceptions.EntityExistsException");
		}
		else {
			handle( declaration,
					"jakarta.persistence.OptimisticLockException",
					"jakarta.data.exceptions.OptimisticLockingFailureException");
		}
		handle( declaration,
				"jakarta.persistence.PersistenceException",
				"jakarta.data.exceptions.DataException");
	}

	private void delegateCall(StringBuilder declaration) {
		if ( isReactive() ) {
			// TODO: handle the case of an iterable parameter
			delegateReactively( declaration );
		}
		else {
			delegateBlockingly( declaration );
		}
	}

	private void delegateBlockingly(StringBuilder declaration) {
		if ( isMerge() ) {
			delegateMergeBlockingly( declaration );
		}
		else if ( isStatefulOperation() && parameterKind != ParameterKind.NORMAL ) {
			delegateStatefulManyBlockingly( declaration );
		}
		else if ( isGeneratedIdUpsert() && parameterKind != ParameterKind.NORMAL ) {
			declaration
					.append("\t\tfor (var _entity : ")
					.append(parameterName)
					.append(") {\n");
			upsertOrInsertByGeneratedId( declaration, "_entity", "\t\t\t" );
			declaration.append("\t\t}\n");
		}
		else if ( isGeneratedIdUpsert() ) {
			upsertOrInsertByGeneratedId( declaration, parameterName, "\t\t" );
		}
		else {
			declaration
					.append("\t\t")
					.append(sessionName)
					.append(getObjectCall())
					.append('.')
					.append(operationName);
			argument( declaration );
			declaration
					.append(";\n");
		}
	}

	private String sessionWithGetIdentifier() {
		final var session = sessionName + getObjectCall();
		return isUsingEntityAgent()
				? session + ".unwrap(" + annotationMetaEntity.importType( HIB_STATELESS_SESSION ) + ".class)"
				: session;
	}

	private void delegateStatefulManyBlockingly(StringBuilder declaration) {
		declaration
				.append("\t\tfor (var _entity : ")
				.append(parameterName)
				.append(") {\n")
				.append("\t\t\t")
				.append(sessionName)
				.append(getObjectCall())
				.append('.')
				.append(operationName)
				.append("(_entity);\n")
				.append("\t\t}\n");
	}

	private void upsertOrInsertByGeneratedId(StringBuilder declaration, String entityVar, String indent) {
		declaration
				.append(indent)
				.append("if (")
				.append(sessionWithGetIdentifier())
				.append(".getIdentifier(")
				.append(entityVar)
				.append(") == null)\n")
				.append(indent)
				.append('\t')
				.append(sessionName)
				.append(getObjectCall())
				.append(".insert(")
				.append(entityVar)
				.append(");\n")
				.append(indent)
				.append("else\n")
				.append(indent)
				.append('\t')
				.append(sessionName)
				.append(getObjectCall())
				.append(".upsert(")
				.append(entityVar)
				.append(");\n");
	}

	private void delegateMergeBlockingly(StringBuilder declaration) {
		switch ( parameterKind ) {
			case NORMAL:
				declaration
						.append("\t\t")
						.append(parameterName)
						.append(" = ")
						.append(sessionName)
						.append(getObjectCall())
						.append(".merge(")
						.append(parameterName)
						.append(");\n");
				break;
			case LIST:
				declaration
						.append("\t\t_result = new ")
						.append(annotationMetaEntity.importType( "java.util.ArrayList" ))
						.append("<>();\n")
						.append("\t\tfor (var _entity : ")
						.append(parameterName)
						.append(") {\n")
						.append("\t\t\t_result.add(")
						.append(sessionName)
						.append(getObjectCall())
						.append(".merge(_entity));\n")
						.append("\t\t}\n");
				break;
			case ARRAY:
				declaration
						.append("\t\t_result = ")
						.append(parameterName)
						.append(".clone();\n")
						.append("\t\tfor (var _index = 0; _index < ")
						.append(parameterName)
						.append(".length; _index++) {\n")
						.append("\t\t\t_result[_index] = ")
						.append(sessionName)
						.append(getObjectCall())
						.append(".merge(")
						.append(parameterName)
						.append("[_index]);\n")
						.append("\t\t}\n");
				break;
		}
	}

	private void argument(StringBuilder declaration) {
		switch ( parameterKind ) {
			case LIST:
				if ( isReactive() ) {
					declaration
							.append("All")
							.append("(")
							.append(parameterName)
							.append(".toArray()")
							.append( ")" );
				}
				else {
					declaration
							.append("Multiple")
							.append("(")
							.append(parameterName)
							.append(")");
				}
				break;
			case ARRAY:
				if ( isReactive() ) {
					declaration
							.append("All")
							.append("((Object[]) ")
							.append(parameterName)
							.append(")");
				}
				else {
					declaration
							.append("Multiple")
							.append("(")
							.append(annotationMetaEntity.importType(LIST))
							.append(".of(")
							.append(parameterName)
							.append("))");
				}
				break;
			default:
				declaration
						.append('(')
						.append(parameterName)
						.append(')');
		}
	}

	private void delegateReactively(StringBuilder declaration) {
		if ( isGeneratedIdUpsert() && parameterKind != ParameterKind.NORMAL ) {
			delegateGeneratedIdUpsertManyReactively( declaration );
			return;
		}
		declaration
				.append("\treturn ");
		if ( isReactiveSessionAccess() ) {
			declaration
					.append(sessionName)
					.append(".chain(")
					.append(localSessionName())
					.append(" -> ");
		}
		if ( isGeneratedIdUpsert() ) {
			declaration.append("(");
			upsertOrInsertByGeneratedIdReactively( declaration, parameterName );
			declaration.append(")");
		}
		else {
			declaration
					.append(localSessionName())
					.append( '.' )
					.append( operationName );
			// note that there is no upsertAll() method
			argument( declaration );
		}
		if ( isReactiveSessionAccess() ) {
			declaration
					.append(')');
		}
	}

	private void delegateGeneratedIdUpsertManyReactively(StringBuilder declaration) {
		final var uni = annotationMetaEntity.importType( UNI );
		final var session = localSessionName();
		if ( isReactiveSessionAccess() ) {
			declaration
					.append("\treturn ")
					.append(sessionName)
					.append(".chain(")
					.append(session)
					.append(" -> {\n");
		}
		final var indent = isReactiveSessionAccess() ? "\t\t" : "\t";
		declaration
				.append(indent)
				.append("var _chain = ")
				.append(uni)
				.append(".createFrom().voidItem();\n")
				.append(indent)
				.append("for (var _entity : ")
				.append(parameterName)
				.append(") {\n")
				.append(indent)
				.append("\t_chain = _chain.chain(() -> ");
		upsertOrInsertByGeneratedIdReactively( declaration, "_entity" );
		declaration
				.append(");\n")
				.append(indent)
				.append("}\n");
		if ( isReactiveSessionAccess() ) {
			declaration
					.append("\t\treturn _chain;\n")
					.append("\t})");
		}
		else {
			declaration
					.append("\treturn _chain");
		}
	}

	private void upsertOrInsertByGeneratedIdReactively(StringBuilder declaration, String entityVar) {
		final var session = localSessionName();
		declaration
				.append(session)
				.append(".getIdentifier(")
				.append(entityVar)
				.append(") == null ? ")
				.append(session)
				.append(".insert(")
				.append(entityVar)
				.append(") : ")
				.append(session)
				.append(".upsert(")
				.append(entityVar)
				.append(")");
	}

	private boolean isGeneratedIdUpsert() {
		return "upsert".equals( operationName ) && hasGeneratedId;
	}

	private boolean isMerge() {
		return "merge".equals( operationName );
	}

	private boolean isStatefulOperation() {
		return switch ( operationName ) {
			case "persist", "merge", "refresh", "remove", "detach" -> true;
			default -> false;
		};
	}

	private void preamble(StringBuilder declaration) {
		declaration
				.append("\n@Override\n");
		returnNonnull( declaration );
		declaration
				.append("public ")
				.append(parameterTypeBounds())
				.append(returnType())
				.append(' ')
				.append(methodName)
				.append('(');
		notNull(declaration);
		final var parameters = method.getParameters();
		assert parameters.size() == 1;
		final var element = parameters.get(0);
		declaration
				.append(resolveAsString(element.asType()))
				.append(' ')
				.append(element.getSimpleName())
				.append(')')
				.append(" {\n");
	}

	private void returnNonnull(StringBuilder declaration) {
		if ( addNonnullAnnotation && returnsUniOrCompletionStage() ) {
			declaration
					.append('@')
					.append(annotationMetaEntity.importType(NONNULL))
					.append('\n');
		}
	}

	private boolean returnsUniOrCompletionStage() {
		if ( method.getReturnType() instanceof DeclaredType declaredType
				&& declaredType.asElement() instanceof TypeElement typeElement ) {
			final var returnTypeName = typeElement.getQualifiedName();
			return returnTypeName.contentEquals( UNI )
				|| returnTypeName.contentEquals( COMPLETION_STAGE );
		}
		else {
			return false;
		}
	}

	private String parameterTypeBounds() {
		if ( method.getTypeParameters().isEmpty() ) {
			return "";
		}
		else {
			final var typeParams = new StringJoiner( ", ", " <", "> " );
			for ( var typeParameterElement : method.getTypeParameters() ) {
				typeParams.add( resolveTypeParameter( typeParameterElement ) );
			}
			return typeParams.toString();
		}
	}

	private String resolveAsString(TypeMirror type) {
		if ( type.getKind().isPrimitive() || type.getKind() == VOID ) {
			return type.toString();
		}
		else if ( type instanceof DeclaredType declaredType ) {
			final var typeElement = (TypeElement) declaredType.asElement();
			final var element =
					annotationMetaEntity.importType( typeElement.getQualifiedName().toString() );
			if ( declaredType.getTypeArguments().isEmpty() ) {
				return element;
			}
			else {
				final var typeArgs = new StringJoiner( ",", "<", ">" );
				for ( var typeMirror : declaredType.getTypeArguments() ) {
					typeArgs.add( resolveAsString( typeMirror ) );
				}
				return element + typeArgs;
			}
		}
		else if ( type instanceof TypeVariable typeVariable ) {
			final var value = typeVariable.toString();
			return methodTypeParameters.contains( value )
					? value
					: annotationMetaEntity.importType(
							// castNonNull shouldn't be necessary; see JavaxLangModelLibraryModels
							resolveTypeName( element, castNonNull( method.getEnclosingElement() ), value ) );
		}
		else if ( type instanceof WildcardType wildcardType ) {
			return "?"
				+ (wildcardType.getExtendsBound() == null ? ""
					: " extends " + resolveAsString( wildcardType.getExtendsBound() ))
				+ (wildcardType.getSuperBound() == null ? ""
					: " super " + resolveAsString( wildcardType.getExtendsBound() ));
		}
		else if ( type instanceof ArrayType arrayType ) {
			return resolveAsString( arrayType.getComponentType() ) + "[]";
		}
		else if ( type instanceof IntersectionType intersectionType ) {
			final var intersection = new StringJoiner( "&" );
			for ( var typeMirror : intersectionType.getBounds() ) {
				intersection.add( resolveAsString( typeMirror ) );
			}
			return intersection.toString();
		}
		else {
			return type.toString();
		}
	}

	private String resolveTypeParameter(TypeParameterElement p) {
		final var type = (TypeVariable) p.asType();
		return type.toString()
			+ (type.getUpperBound().getKind() == TypeKind.NULL ? ""
				: " extends " + resolveAsString( type.getUpperBound() ))
			+ (type.getLowerBound().getKind() == TypeKind.NULL ? ""
				: " super " + resolveAsString( type.getLowerBound() ));
	}

	private String returnType() {
		if ( returnArgument || isAsynchronousCompletionStage() ) {
			return resolveAsString(method.getReturnType());
		}
		else {
			return isReactive() ? annotationMetaEntity.importType(UNI) + "<Void>" : "void";
		}
	}

	private TypeMirror returnArgumentType() {
		final var typeArgument = completionStageResultType();
		return typeArgument == null ? method.getReturnType() : typeArgument;
	}

	private void notNull(StringBuilder declaration) {
		if ( addNonnullAnnotation ) {
			declaration
					.append('@')
					.append(annotationMetaEntity.importType(NONNULL))
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
