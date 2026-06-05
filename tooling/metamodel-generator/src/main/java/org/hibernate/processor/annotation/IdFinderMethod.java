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
import static org.hibernate.processor.util.TypeUtils.isPrimitive;

/**
 * @author Gavin King
 */
public class IdFinderMethod extends AbstractFinderMethod {

	private final String paramName;
	private final String paramType;

	public IdFinderMethod(
			AnnotationMetaEntity annotationMetaEntity,
			ExecutableElement method,
			String methodName, String entity,
			@Nullable String containerType, //must be null or Optional
			List<String> paramNames, List<String> paramTypes,
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
		var idParameter = idParameter(paramNames, paramTypes);
		this.paramName = paramNames.get(idParameter);
		this.paramType = paramTypes.get(idParameter);
	}

	private static int idParameter(List<String> paramNames, List<String> paramTypes) {
		for (int i = 0; i < paramNames.size(); i ++ ) {
			if ( !isSessionParameter( paramTypes.get(i) ) ) {
				return i;
			}
		}
		return -1; // should never occur!
	}

	@Override
	boolean isNullable(int index) {
		return false;
	}

	@Override
	boolean singleResult() {
		return false; // we don't need to convert Query exceptions
	}

	@Override
	public String getAttributeDeclarationString() {
		final var declaration = new StringBuilder();
		comment( declaration );
		modifiers( declaration );
		preamble( declaration, paramTypes );
		if ( paramName != null && !isPrimitive(paramType) ) {
			nullCheck( declaration, paramName );
		}
		varOrReturn( declaration );
		find( declaration );
		throwIfNull( declaration );
		convertExceptions( declaration );
		closingBrace( declaration );
		return declaration.toString();
	}

	private void throwIfNull(StringBuilder declaration) {
		if (containerType != null) {
			if ( isReactive() ) {
				declaration
						.append("\n\t\t\t.map(")
						.append(annotationMetaEntity.importType(containerType))
						.append("::")
						.append("ofNullable");
			}
			declaration
					.append(')');
			endReturnResult( declaration );
		}
		else if (!nullable) {
			if ( isReactive() ) {
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
				declaration
						.append( ";\n" );
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
				.append( "' for given id [\" + " )
				.append( parameterName(paramName) )
				.append( " + \"]\",\n\t\t\t\t\tnew " )
				.append( annotationMetaEntity.importType( "org.hibernate.ObjectNotFoundException" ) )
				.append( "((Object) " )
				.append( parameterName(paramName) )
				.append( ", \"" )
				.append( entity )
				.append( "\"))");
	}

	private void throwObjectNotFound(StringBuilder declaration) {
		declaration
				.append( "throw new " )
				.append( annotationMetaEntity.importType( "org.hibernate.ObjectNotFoundException" ) )
				.append( "((Object) " )
				.append( paramName )
				.append( ", \"" )
				.append( entity )
				.append( "\")" );
	}

	private void varOrReturn(StringBuilder declaration) {
		if (dataRepository && !isReactive()) {
			declaration
					.append("\ttry {\n\t");
		}
		if (containerType != null && !isReactive()) {
			declaration
					.append("\t");
			returnResult( declaration );
			declaration
					.append(annotationMetaEntity.staticImport(containerType, "ofNullable"))
					.append('(');
		}
		else if (!nullable && !isReactive()) {
			declaration
					.append("\tvar _result = ");
		}
		else {
			declaration
					.append("\t");
			returnResult( declaration );
		}
		declaration
				.append(sessionName)
				.append(getObjectCall());
	}

	private void find(StringBuilder declaration) {
		if ( isReactiveSessionAccess() ) {
			declaration
					.append(".chain(")
					.append(localSessionName())
					.append(" -> ")
					.append(localSessionName());
		}
		declaration
				.append(isReactive() && isUsingStatelessSession() ? ".get(" : ".find(");
		if ( !appendEntityGraphArgument( this, declaration, entity ) ) {
			declaration
					.append(annotationMetaEntity.importType(entity))
					.append(".class");
		}
		declaration
				.append(", ")
				.append(parameterName(paramName));
		appendFindOptions( this, declaration, fetchProfiles, false );
		if ( isReactiveSessionAccess() ) {
			declaration
					.append(')');
		}
		declaration
				.append(")");
	}
}
