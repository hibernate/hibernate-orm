/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.annotation;

import javax.lang.model.element.ExecutableElement;
import java.util.List;

import static java.util.Collections.emptyList;

/**
 * @author Gavin King
 */
public class CriteriaDeleteMethod extends AbstractCriteriaMethod {

	private final List<Boolean> paramNullability;

	CriteriaDeleteMethod(
			AnnotationMetaEntity annotationMetaEntity,
			ExecutableElement method,
			String methodName, String entity,
			List<String> paramNames,
			List<String> paramTypes,
			List<Boolean> paramNullability,
			List<Boolean> multivalued,
			List<Boolean> paramPatterns,
			boolean belongsToDao,
			String sessionType,
			String sessionName,
			boolean addNonnullAnnotation,
			boolean dataRepository,
			String fullReturnType) {
		super( annotationMetaEntity, method, methodName, entity, null, belongsToDao, sessionType,
				sessionName, emptyList(), paramNames, paramTypes, emptyList(), addNonnullAnnotation, dataRepository,
				multivalued, paramPatterns, fullReturnType, false );
		this.paramNullability = paramNullability;
	}

	@Override
	public boolean isNullable(int index) {
		return paramNullability.get(index);
	}

	@Override
	boolean singleResult() {
		return true;
	}

//	@Override
//	String returnType() {
//		return returnType;
//	}

	@Override
	void executeQuery(StringBuilder declaration, List<String> paramTypes) {
		createSpecification( declaration );
		handleRestrictionParameters( declaration, paramTypes );
		tryReturn(declaration);
		createQuery( declaration );
		execute( declaration );
	}

	void tryReturn(StringBuilder declaration) {
		declaration
				.append("\n\ttry {\n\t\t");
		if ( !"void".equals(fullReturnType) ) {
			declaration
					.append("return ");
		}
	}

	@Override
	String createQueryMethod() {
		return isUsingEntityManager() || isReactive()
				? "createQuery"
				: "createMutationQuery";
	}

	@Override
	String specificationType() {
		return "org.hibernate.query.specification.MutationSpecification";
	}

	private void execute(StringBuilder declaration) {
		declaration
				.append("\t\t\t.executeUpdate()");
	}

	@Override
	String createCriteriaMethod() {
		return "createCriteriaDelete";
	}
}
