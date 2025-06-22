/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.annotation;

import org.checkerframework.checker.nullness.qual.Nullable;

import javax.lang.model.element.ExecutableElement;
import java.util.List;

import static org.hibernate.metamodel.mapping.EntityIdentifierMapping.ID_ROLE_NAME;
import static org.hibernate.processor.util.Constants.HIB_SESSION;

/**
 * @author Gavin King
 */
public abstract class AbstractFinderMethod extends AbstractQueryMethod  {
	final @Nullable String containerType;
	final String entity;
	final List<String> fetchProfiles;

	AbstractFinderMethod(
			AnnotationMetaEntity annotationMetaEntity,
			ExecutableElement method,
			String methodName,
			String entity,
			@Nullable String containerType,
			boolean belongsToDao,
			String sessionType,
			String sessionName,
			List<String> fetchProfiles,
			List<String> paramNames,
			List<String> paramTypes,
			List<OrderBy> orderBys,
			boolean addNonnullAnnotation,
			boolean convertToDataExceptions,
			String fullReturnType,
			boolean nullable) {
		super( annotationMetaEntity, method,
				methodName,
				paramNames, paramTypes, entity,
				sessionType, sessionName,
				belongsToDao, orderBys,
				addNonnullAnnotation,
				convertToDataExceptions,
				fullReturnType,
				nullable );
		this.entity = entity;
		this.containerType = containerType;
		this.fetchProfiles = fetchProfiles;
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
	public String getTypeDeclaration() {
		return entity;
	}

	@Override
	public String getAttributeNameDeclarationString() {
		throw new UnsupportedOperationException("operation not supported");
	}

	void comment(StringBuilder declaration) {
		declaration
				.append("\n/**")
				.append("\n * ")
				.append(this instanceof CriteriaDeleteMethod ? "Delete" : "Find")
				.append(" {@link ")
				.append(annotationMetaEntity.importType(entity))
				.append("}");
		long paramCount = paramTypes.stream()
				.filter(type -> !isSpecialParam(type))
				.count();
		if ( paramCount> 0 ) {
			declaration
					.append(" by ");
			int count = 0;
			for (int i = 0; i < paramTypes.size(); i++) {
				final String type = paramTypes.get(i);
				if ( !isSpecialParam(type) ) {
					if ( count>0 ) {
						if ( count + 1 == paramCount) {
							declaration
									.append(paramCount>2 ? ", and " : " and "); //Oxford comma
						}
						else {
							declaration
									.append(", ");
						}
					}
					count++;
					final String path = paramNames.get(i);
					if ( ID_ROLE_NAME.equals(path) ) {
						declaration
								.append("identifier");
					}
					else {
						declaration
								.append("{@link ")
								.append(annotationMetaEntity.importType(entity))
								.append('#')
								.append(qualifier(path))
								.append(' ')
								.append(path)
								.append("}");
					}
				}
			}
		}
		declaration
				.append('.')
				.append("\n *");
		see( declaration );
//		declaration
//				.append("\n *");
//		for (String param : paramNames) {
//			declaration
//					.append("\n * @see ")
//					.append(annotationMetaEntity.importType(entity))
//					.append('#')
//					.append(param);
//		}
		declaration
				.append("\n **/\n");
	}

	String qualifier(String name) {
		final int index = name.indexOf('.');
		return index > 0 ? name.substring(0, index) : name;
	}

	void unwrapSession(StringBuilder declaration) {
		if ( isUsingEntityManager() ) {
			declaration
					.append(".unwrap(")
					.append(annotationMetaEntity.importType(HIB_SESSION))
					.append(".class)\n\t\t\t");
		}
	}

	boolean enableFetchProfile(StringBuilder declaration, boolean unwrapped) {
		if ( !fetchProfiles.isEmpty() ) {
			unwrapQuery( declaration, unwrapped );
			unwrapped = true;
		}
		for ( String profile : fetchProfiles ) {
			declaration
					.append("\t\t\t.enableFetchProfile(")
					.append(profile)
					.append(")\n");
		}
		return unwrapped;
	}

	void tryReturn(StringBuilder declaration) {
		if (dataRepository) {
			declaration
					.append("\ttry {\n\t");
		}
		declaration
				.append("\treturn ")
				.append(sessionName);
	}

	void modifiers(StringBuilder declaration) {
		declaration
				.append(belongsToDao ? "@Override\npublic " : "public static ");
	}
}
