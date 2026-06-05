/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.annotation;

import jakarta.annotation.Nullable;

import javax.lang.model.element.ExecutableElement;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.hibernate.processor.annotation.QueryOptionsSupport.appendEntityGraphArgument;
import static org.hibernate.processor.annotation.QueryOptionsSupport.appendFindOptions;

/**
 * @author Gavin King
 */
public class NaturalIdFinderMethod extends AbstractFinderMethod {

	public static final String IDENTIFIER = "org.hibernate.reactive.common.Identifier";

	private final List<Boolean> paramNullability;

	public NaturalIdFinderMethod(
			AnnotationMetaEntity annotationMetaEntity,
			ExecutableElement method,
			String methodName, String entity,
			@Nullable String containerType, //must be null or Optional
			List<String> paramNames, List<String> paramTypes,
			List<Boolean> paramNullability,
			boolean belongsToDao,
			String sessionType,
			String sessionName,
			List<String> fetchProfiles,
			boolean addNonnullAnnotation,
			boolean dataRepository,
			String fullReturnType,
			boolean nullable) {
		super( annotationMetaEntity, method, methodName, entity, entity, containerType, belongsToDao, sessionType, sessionName,
				fetchProfiles, paramNames, paramTypes, emptyList(), addNonnullAnnotation, dataRepository, fullReturnType,
				nullable );
		this.paramNullability = paramNullability;
	}

	@Override
	boolean isNullable(int index) {
		// natural ids can be null
		return paramNullability.get(index);
	}

	@Override
	boolean singleResult() {
		return true;
	}

	@Override
	public String getAttributeDeclarationString() {
		final var declaration = new StringBuilder();
		comment( declaration );
		modifiers( declaration );
		preamble( declaration, paramTypes );
		if ( isReactive() ) {
			returnReactively( declaration );
			findReactively( declaration );
			throwIfNullReactively( declaration );
		}
		else {
			createNaturalIdKey( declaration );
			findBlockingly( declaration );
		}
		convertExceptions( declaration );
		closingBrace( declaration );
		return declaration.toString();
	}

	private void returnReactively(StringBuilder declaration) {
		declaration
				.append("\t");
		returnResult( declaration );
		declaration
				.append(sessionName)
				.append(getObjectCall());
	}

	private void findBlockingly(StringBuilder declaration) {
		if ( dataRepository ) {
			declaration
					.append("\ttry {\n\t");
		}
		if ( containerType != null ) {
			declaration
					.append("\t");
			returnResult( declaration );
			declaration
					.append(annotationMetaEntity.staticImport(containerType, "ofNullable"))
					.append('(');
			findInvocation( declaration );
			declaration.append(')');
			endReturnResult( declaration );
		}
		else if ( !nullable ) {
			declaration
					.append("\tvar _result = ");
			findInvocation( declaration );
			declaration.append(";\n");
			throwIfNullBlockingly( declaration );
		}
		else {
			declaration
					.append("\t");
			returnResult( declaration );
			findInvocation( declaration );
			endReturnResult( declaration );
		}
	}

	private void findInvocation(StringBuilder declaration) {
		declaration
				.append(sessionName)
				.append(getObjectCall())
				.append(".find(");
		if ( !appendEntityGraphArgument( this, declaration, entity ) ) {
			declaration
					.append(annotationMetaEntity.importType(entity))
					.append(".class");
		}
		declaration
				.append(", ")
				.append(naturalIdKey());
		appendFindOptions( this, declaration, fetchProfiles, true );
		declaration.append(")");
	}

	private void throwIfNullBlockingly(StringBuilder declaration) {
		if ( dataRepository ) {
			declaration
					.append( "\t\tif (_result == null) " );
			throwEmptyResult( declaration );
			declaration
					.append( ";\n" )
					.append( "\t\t" );
			returnResult( declaration );
			declaration.append( "_result" );
			endReturnResult( declaration );
		}
		else {
			declaration
					.append( "\tif (_result == null) " );
			throwObjectNotFound( declaration );
			declaration
					.append( ";\n" )
					.append( "\t" );
			returnResult( declaration );
			declaration.append( "_result" );
			endReturnResult( declaration );
		}
	}

	private void throwIfNullReactively(StringBuilder declaration) {
		if ( containerType != null ) {
			declaration
					.append("\n\t\t\t.map(")
					.append(annotationMetaEntity.importType(containerType))
					.append("::")
					.append("ofNullable)");
			endReturnResult( declaration );
		}
		else if ( !nullable ) {
			declaration
					.append( "\n\t\t\t.replaceIfNullWith(() -> { " );
			if ( dataRepository ) {
				throwEmptyResult( declaration );
			}
			else {
				throwObjectNotFound( declaration );
			}
			declaration
					.append( "; })" );
		}
		else {
			endReturnResult( declaration );
		}
	}

	private void throwEmptyResult(StringBuilder declaration) {
		declaration
				.append( "throw new " )
				.append( annotationMetaEntity.importType( "jakarta.data.exceptions.EmptyResultException" ) )
				.append( "(\"No '" )
				.append( annotationMetaEntity.importType( entity ) )
				.append( "' for given natural id [\" + " );
		appendNaturalIdKey( declaration );
		declaration
				.append( " + \"]\",\n\t\t\t\t\tnew " )
				.append( annotationMetaEntity.importType( "org.hibernate.ObjectNotFoundException" ) )
				.append( "((Object) " );
		appendNaturalIdKey( declaration );
		declaration
				.append( ", \"" )
				.append( entity )
				.append( "\"))");
	}

	private void throwObjectNotFound(StringBuilder declaration) {
		declaration
				.append( "throw new " )
				.append( annotationMetaEntity.importType( "org.hibernate.ObjectNotFoundException" ) )
				.append( "((Object) " );
		appendNaturalIdKey( declaration );
		declaration
				.append( ", \"" )
				.append( entity )
				.append( "\")" );
	}

	private void appendNaturalIdKey(StringBuilder declaration) {
		if ( isReactive() && hasCompositeNaturalId() ) {
			declaration
					.append( annotationMetaEntity.importType( "java.util.Arrays" ) )
					.append( ".asList(" );
			var first = true;
			for ( int i = 0; i < paramNames.size(); i++ ) {
				if ( !isSessionParameter( paramTypes.get( i ) ) ) {
					if ( first ) {
						first = false;
					}
					else {
						declaration.append( ", " );
					}
					declaration.append( parameterName( paramNames.get( i ) ) );
				}
			}
			declaration.append( ')' );
		}
		else {
			declaration.append( naturalIdKey() );
		}
	}

	private void createNaturalIdKey(StringBuilder declaration) {
		if ( hasCompositeNaturalId() ) {
			declaration
					.append("\tvar _key = new ")
					.append(annotationMetaEntity.importType( "java.util.HashMap" ))
					.append("<String, Object>();\n");
			for ( int i = 0; i < paramNames.size(); i ++ ) {
				if ( !isSessionParameter( paramTypes.get(i) ) ) {
					final var paramName = paramNames.get(i);
					declaration
							.append("\t_key.put(")
							.append(QueryOptionsSupport.stringLiteral( paramName ))
							.append(", ")
							.append(parameterName( paramName ))
							.append(");\n");
				}
			}
		}
	}

	private String naturalIdKey() {
		return hasCompositeNaturalId()
				? "_key"
				: parameterName( naturalIdParameterName() );
	}

	private String naturalIdParameterName() {
		for ( int i = 0; i < paramNames.size(); i ++ ) {
			if ( !isSessionParameter( paramTypes.get(i) ) ) {
				return paramNames.get(i);
			}
		}
		throw new IllegalStateException( "no natural id parameter" );
	}

	private boolean hasCompositeNaturalId() {
		var count = 0;
		for ( String paramType : paramTypes ) {
			if ( !isSessionParameter( paramType ) ) {
				count++;
			}
		}
		return count > 1;
	}

	private void findReactively(StringBuilder declaration) {
		final var composite = hasCompositeNaturalId();
		if ( isReactiveSessionAccess() ) {
			declaration
					.append(".chain(")
					.append(localSessionName())
					.append(" -> ")
					.append(localSessionName());
		}
		declaration
				.append(".find(");
		if (composite) {
			declaration.append("\n\t\t\t");
		}
		declaration
				.append(annotationMetaEntity.importType(entity))
				.append(".class, ");
		if (composite) {
			declaration
					.append("\n\t\t\t")
					.append(annotationMetaEntity.importType(IDENTIFIER))
					.append(".composite(");
		}
		var first = true;
		for ( int i = 0; i < paramNames.size(); i ++ ) {
			if ( !isSessionParameter( paramTypes.get(i) ) ) {
				if ( first ) {
					first = false;
				}
				else {
					declaration
							.append(", ");
				}
				if (composite) {
					declaration
							.append("\n\t\t\t\t");
				}
				final var paramName = paramNames.get(i);
				declaration
						.append(annotationMetaEntity.importType(IDENTIFIER))
						.append(".id(")
						.append(annotationMetaEntity.importType(entity + '_'))
						.append('.')
						.append(paramName)
						.append(", ")
						.append(paramName)
						.append(")");
			}
		}
		if (composite) {
			declaration.append("\n\t\t\t)\n\t");
		}
		if ( isReactiveSessionAccess() ) {
			declaration.append(')');
		}
		declaration.append(')');
	}
}
