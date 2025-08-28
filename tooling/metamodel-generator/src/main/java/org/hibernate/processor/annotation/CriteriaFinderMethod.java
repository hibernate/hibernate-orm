/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.annotation;

import org.jspecify.annotations.Nullable;

import javax.lang.model.element.ExecutableElement;
import java.util.List;

/**
 * @author Gavin King
 */
public class CriteriaFinderMethod extends AbstractCriteriaMethod {

	private final List<Boolean> paramNullability;

	CriteriaFinderMethod(
			AnnotationMetaEntity annotationMetaEntity,
			ExecutableElement method,
			String methodName, String entity,
			@Nullable String containerType,
			List<String> paramNames, List<String> paramTypes,
			List<Boolean> paramNullability,
			List<Boolean> multivalued,
			List<Boolean> paramPatterns,
			boolean belongsToDao,
			String sessionType,
			String sessionName,
			List<String> fetchProfiles,
			List<OrderBy> orderBys,
			boolean addNonnullAnnotation,
			boolean dataRepository,
			String fullReturnType,
			boolean nullable) {
		super( annotationMetaEntity, method, methodName, entity, containerType, belongsToDao, sessionType, sessionName,
				fetchProfiles, paramNames, paramTypes, orderBys, addNonnullAnnotation, dataRepository, multivalued,
				paramPatterns, fullReturnType, nullable );
		this.paramNullability = paramNullability;
	}

	@Override
	public boolean isNullable(int index) {
		return paramNullability.get(index);
	}

	@Override
	boolean singleResult() {
		return containerType == null;
	}

	@Override
	void executeQuery(StringBuilder declaration, List<String> paramTypes) {
		declaration
				.append('\n');
		createSpecification( declaration );
		handleRestrictionParameters( declaration, paramTypes );
		collectOrdering( declaration, paramTypes, containerType );
		tryReturn( declaration, paramTypes, containerType );
		castResult( declaration );
		createQuery( declaration );
		handlePageParameters( declaration, paramTypes, containerType );
		boolean unwrapped = initiallyUnwrapped();
		unwrapped = enableFetchProfile( declaration, unwrapped );
		execute( declaration, paramTypes, unwrapped );
	}

	private void castResult(StringBuilder declaration) {
		if ( containerType == null && !fetchProfiles.isEmpty()
				&& isUsingEntityManager() ) {
			declaration
					.append("(")
					.append(annotationMetaEntity.importType(entity))
					.append(") ");
		}
	}

	private void execute(StringBuilder declaration, List<String> paramTypes, boolean unwrapped) {
		executeSelect( declaration, paramTypes, containerType, unwrapped, isHibernateQueryType(containerType) );
	}

	@Override
	String createQueryMethod() {
		return isUsingEntityManager()
			|| isReactive()
			|| isUnspecializedQueryType(containerType)
				? "createQuery"
				: "createSelectionQuery";
	}

	@Override
	String createCriteriaMethod() {
		return "createQuery";
	}

//	@Override
//	String returnType() {
//		final StringBuilder type = new StringBuilder();
//		if ( "[]".equals(containerType) ) {
//			if ( returnTypeName == null ) {
//				throw new AssertionFailure("array return type, but no type name");
//			}
//			type.append(annotationMetaEntity.importType(returnTypeName)).append("[]");
//		}
//		else {
//			final boolean returnsUni = isReactive() && isUnifiableReturnType(containerType);
//			if ( returnsUni ) {
//				type.append(annotationMetaEntity.importType(Constants.UNI)).append('<');
//			}
//			if ( containerType != null ) {
//				type.append(annotationMetaEntity.importType(containerType)).append('<');
//			}
//			type.append(annotationMetaEntity.importType(entity));
//			if ( containerType != null ) {
//				type.append('>');
//			}
//			if ( returnsUni ) {
//				type.append('>');
//			}
//		}
//		return type.toString();
//	}
}
