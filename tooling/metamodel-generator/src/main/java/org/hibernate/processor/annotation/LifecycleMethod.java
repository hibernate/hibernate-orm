/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.annotation;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;
import java.util.Collection;
import java.util.Set;

import static java.lang.Character.toUpperCase;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import static javax.lang.model.type.TypeKind.VOID;
import static org.hibernate.processor.util.Constants.EVENT;
import static org.hibernate.processor.util.Constants.INJECT;
import static org.hibernate.processor.util.Constants.JD_LIFECYCLE_EVENT;
import static org.hibernate.processor.util.Constants.LIST;
import static org.hibernate.processor.util.Constants.NONNULL;
import static org.hibernate.processor.util.Constants.TYPE_LITERAL;
import static org.hibernate.processor.util.Constants.UNI;
import static org.hibernate.processor.util.TypeUtils.resolveTypeName;

public class LifecycleMethod extends AbstractAnnotatedMethod {
	private final String entity;
	private final String actualEntity;
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
			String actualEntity,
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
		this.actualEntity = actualEntity;
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

	private String capitalize(String string) {
		return toUpperCase(string.charAt(0)) + string.substring(1);
	}

	static final Set<String> eventTypes = Set.of("insert", "update", "delete");

	@Override
	public String getAttributeDeclarationString() {
		StringBuilder declaration = new StringBuilder();
		preamble(declaration);
		nullCheck(declaration, parameterName);
		fireEvents(declaration, "Pre");
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
		fireEvents(declaration, "Post");
		returnArgument(declaration);
		declaration.append("}");
		return declaration.toString();
	}

	private void fireEvents(StringBuilder declaration, String prefix) {
		if ( annotationMetaEntity.getContext().isDataEventPackageAvailable()
				&& annotationMetaEntity.getContext().addDependentAnnotation()
				&& eventTypes.contains( operationName )
				&& !isReactive()) {
			final String entityName = iterateEvents( declaration );
			fireEvent( declaration, entityName, prefix + capitalize( operationName ) + "Event" );
			endIterateEvents( declaration );
		}
	}

	private void fireEvent(StringBuilder declaration, String entityName, String eventType) {
		annotationMetaEntity.importType( JD_LIFECYCLE_EVENT );
		annotationMetaEntity.importType( TYPE_LITERAL );
		annotationMetaEntity.importType( EVENT );
		annotationMetaEntity.importType( INJECT );
		annotationMetaEntity.importType( "jakarta.data.event." + eventType );
		if (parameterKind != ParameterKind.NORMAL) {
			declaration.append( "\t" );
		}
		declaration
				.append( "\tif (event != null) {\n" );
		if (parameterKind != ParameterKind.NORMAL) {
			declaration.append( "\t" );
		}
		declaration
				.append( "\t\tevent.select(new TypeLiteral<" )
				.append( eventType )
				.append( "<" )
				.append( annotationMetaEntity.importType( actualEntity ) )
				.append( ">>(){})\n\t\t\t\t.fire(new " )
				.append( eventType )
				.append( "<>(" )
				.append( entityName )
				.append( "));\n");
		if (parameterKind != ParameterKind.NORMAL) {
			declaration.append( "\t" );
		}
		declaration
				.append("\t}\n" );
	}

	private void endIterateEvents(StringBuilder declaration) {
		if (parameterKind != ParameterKind.NORMAL) {
			declaration
					.append( "\t}\n");
		}
	}

	private String iterateEvents(StringBuilder declaration) {
		if (parameterKind != ParameterKind.NORMAL) {
			declaration
					.append( "\tfor (var _entity : ")
					.append( parameterName )
					.append(") {\n" );
			return "_entity";
		}
		else {
			return parameterName;
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

	private void returnArgumentReactively(StringBuilder declaration) {
		if ( isReactive() ) {
			if ( returnArgument ) {
				declaration
						.append( "\n\t\t\t.replaceWith(")
						.append(parameterName)
						.append(")");
			}
		}
	}


	private void convertExceptions(StringBuilder declaration) {
		if ( operationName.equals("insert") ) {
			handle( declaration,
					"org.hibernate.exception.ConstraintViolationException",
					"jakarta.data.exceptions.EntityExistsException");
		}
		else {
			handle( declaration,
					"org.hibernate.StaleStateException",
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
		if ( isGeneratedIdUpsert() ) {
			declaration
					.append("\t\tif (")
					.append(sessionName)
					.append(getObjectCall())
					.append(".getIdentifier(")
					.append(parameterName)
					.append(") == null)\n")
					.append("\t\t\t")
					.append(sessionName)
					.append(getObjectCall())
					.append('.')
					.append("insert");
			argument( declaration );
			declaration
					.append(";\n")
					.append("\t\telse\n\t");
		}
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
			declaration
					.append("(")
					.append(localSessionName())
					.append(".getIdentifier(")
					.append(parameterName)
					.append(") == null ? ")
					.append(localSessionName())
					.append('.')
					.append("insert")
					.append('(')
					.append(parameterName)
					.append(')')
					.append(" : ");
		}
		declaration
				.append(localSessionName())
				.append( '.' )
				.append( operationName );
		// note that there is no upsertAll() method
		argument( declaration );
		if ( isGeneratedIdUpsert() ) {
			declaration
					.append(')');
		}
		if ( isReactiveSessionAccess() ) {
			declaration
					.append(')');
		}
	}

	private boolean isGeneratedIdUpsert() {
		return "upsert".equals( operationName ) && hasGeneratedId;
	}

	private void preamble(StringBuilder declaration) {
		declaration
				.append("\n@Override\npublic ")
				.append(parameterTypeBopunds())
				.append(returnType())
				.append(' ')
				.append(methodName)
				.append('(');
		notNull(declaration);
		final var parameters = method.getParameters();
		assert parameters.size() == 1;
		final VariableElement element = parameters.get(0);
		declaration
				.append(resolveAsString(element.asType()))
				.append(' ')
				.append(element.getSimpleName())
				.append(')')
				.append(" {\n");
	}

	private String parameterTypeBopunds() {
		if ( method.getTypeParameters().isEmpty() ) {
			return "";
		}
		return method.getTypeParameters().stream()
				.map( this::resolveTypeParameter )
				.collect( joining( ", ", " <", "> " ) );
	}

	private String resolveAsString(TypeMirror type) {
		if ( type.getKind().isPrimitive() || type.getKind() == VOID ) {
			return type.toString();
		}
		else if ( type instanceof DeclaredType declaredType ) {
			final var element = annotationMetaEntity.importType(
					((TypeElement) declaredType.asElement()).getQualifiedName().toString()
			);
			if ( declaredType.getTypeArguments().isEmpty() ) {
				return element;
			}
			return element + declaredType.getTypeArguments().stream().map( this::resolveAsString )
					.collect( joining( ",", "<", ">" ) );
		}
		else if ( type instanceof TypeVariable typeVariable ) {
			final var value = typeVariable.toString();
			if ( methodTypeParameters.contains( value ) ) {
				return value;
			}
			return annotationMetaEntity.importType( resolveTypeName( element, requireNonNull( method.getEnclosingElement() ), value ) );
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
			return intersectionType.getBounds().stream().map( this::resolveAsString ).collect( joining( "&" ) );
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
		if ( returnArgument ) {
			return resolveAsString(method.getReturnType());
		}
		else {
			return isReactive() ? annotationMetaEntity.importType(UNI) + "<Void>" : "void";
		}
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
