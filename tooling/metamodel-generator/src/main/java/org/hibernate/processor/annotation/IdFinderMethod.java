/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.annotation;

import org.checkerframework.checker.nullness.qual.Nullable;

import javax.lang.model.element.ExecutableElement;
import java.util.List;

import static java.util.Collections.emptyList;
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
		super( annotationMetaEntity, method, methodName, entity, containerType, belongsToDao, sessionType, sessionName,
				fetchProfiles, paramNames, paramTypes, emptyList(), addNonnullAnnotation, dataRepository, fullReturnType,
				nullable );
		int idParameter = idParameter(paramNames, paramTypes);
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
		return true;
	}

	@Override
	public String getAttributeDeclarationString() {
		final StringBuilder declaration = new StringBuilder();
		comment( declaration );
		modifiers( declaration );
		preamble( declaration, paramTypes );
		if ( paramName != null && !isPrimitive(paramType) ) {
			nullCheck( declaration, paramName );
		}
		varOrReturn( declaration );
		if ( fetchProfiles.isEmpty() ) {
			findWithNoFetchProfiles( declaration );
		}
		else {
			findWithFetchProfiles( declaration );
		}
		throwIfNull( declaration );
		convertExceptions( declaration );
		closingBrace( declaration );
		return declaration.toString();
	}

	private void throwIfNull(StringBuilder declaration) {
		if (containerType != null) {
			declaration
					.append(')');
		}
		else if (!nullable) {
			declaration
					.append(", \"")
					.append(entity)
					.append("\", ")
					.append(paramName)
					.append(')');
		}
		declaration
				.append(";\n");
	}

	private void varOrReturn(StringBuilder declaration) {
		if (dataRepository) {
			declaration
					.append("\ttry {\n\t");
		}
		if (containerType != null) {
			declaration
					.append("\treturn ")
					.append(annotationMetaEntity.staticImport(containerType, "ofNullable"))
					.append('(');
		}
		else if (!nullable) {
			declaration
					.append("\treturn ")
					.append(annotationMetaEntity.staticImport("org.hibernate.exception.spi.Exceptions", "require"))
					.append('(');
		}
		else {
			declaration
					.append("\treturn ");
		}
		declaration
				.append(sessionName);
	}

	@Override
	void convertExceptions(StringBuilder declaration) {
		if (dataRepository) {
			if ( !nullable && containerType==null ) {
				declaration
						.append("\t}\n")
						.append("\tcatch (")
						.append(annotationMetaEntity.importType("org.hibernate.ObjectNotFoundException"))
						.append(" exception) {\n")
						.append("\t\tthrow new ")
						.append(annotationMetaEntity.importType("jakarta.data.exceptions.EmptyResultException"))
						.append("(exception.getMessage(), exception);\n");
			}
			declaration
					.append("\t}\n")
					.append("\tcatch (")
					.append(annotationMetaEntity.importType("jakarta.persistence.PersistenceException"))
					.append(" exception) {\n")
					.append("\t\tthrow new ")
					.append(annotationMetaEntity.importType("jakarta.data.exceptions.DataException"))
					.append("(exception.getMessage(), exception);\n")
					.append("\t}\n");
		}
	}

	private void findWithFetchProfiles(StringBuilder declaration) {
		unwrapSession( declaration );
		declaration
				.append(".byId(")
				.append(annotationMetaEntity.importType(entity))
				.append(".class)\n");
		enableFetchProfile( declaration, true );
		declaration
				.append("\t\t\t.load(")
				.append(paramName)
				.append(")");
	}

	private void findWithNoFetchProfiles(StringBuilder declaration) {
		if ( isReactiveSessionAccess() ) {
			declaration
					.append(".chain(")
					.append(localSessionName())
					.append(" -> ")
					.append(localSessionName());
		}
		declaration
				.append(isUsingStatelessSession() ? ".get(" : ".find(")
				.append(annotationMetaEntity.importType(entity))
				.append(".class, ")
				.append(paramName);
		if ( isReactiveSessionAccess() ) {
			declaration
					.append(')');
		}
		declaration
				.append(")");
	}
}
