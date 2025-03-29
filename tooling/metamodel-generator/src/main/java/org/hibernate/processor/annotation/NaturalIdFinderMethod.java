/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.annotation;

import org.checkerframework.checker.nullness.qual.Nullable;

import javax.lang.model.element.ExecutableElement;
import java.util.List;

import static java.util.Collections.emptyList;

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
			String fullReturnType) {
		super( annotationMetaEntity, method, methodName, entity, containerType, belongsToDao, sessionType, sessionName,
				fetchProfiles, paramNames, paramTypes, emptyList(), addNonnullAnnotation, dataRepository, fullReturnType,
				true );
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
		final StringBuilder declaration = new StringBuilder();
		comment( declaration );
		modifiers( declaration );
		preamble( declaration, paramTypes );
		tryReturn( declaration );
		unwrapSession( declaration );
		if ( isReactive() ) {
			findReactively( declaration );
		}
		else {
			findBlockingly( declaration );
		}
		convertExceptions( declaration );
		closingBrace( declaration );
		return declaration.toString();
	}

	private void findBlockingly(StringBuilder declaration) {
		declaration
				.append(".byNaturalId(")
				.append(annotationMetaEntity.importType(entity))
				.append(".class)\n");
		enableFetchProfile( declaration, true );
		for ( int i = 0; i < paramNames.size(); i ++ ) {
			if ( !isSessionParameter( paramTypes.get(i) ) ) {
				final String paramName = paramNames.get(i);
				declaration
						.append("\t\t\t.using(")
						.append(annotationMetaEntity.importType(entity + '_'))
						.append('.')
						.append(paramName)
						.append(", ")
						.append(paramName)
						.append(")\n");
			}
		}
		if ( containerType == null ) {
			//TODO we should probably throw if this returns null
			declaration
					.append("\t\t\t.load()");
		}
		else {
			declaration
					.append("\t\t\t.loadOptional()");
		}
	}

	private void findReactively(StringBuilder declaration) {
		boolean composite = isComposite();
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
		boolean first = true;
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
				final String paramName = paramNames.get(i);
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
		declaration.append(')');
	}

	private boolean isComposite() {
		return paramTypes.stream()
				.filter(type -> !isSessionParameter(type)).count() > 1;
	}
}
