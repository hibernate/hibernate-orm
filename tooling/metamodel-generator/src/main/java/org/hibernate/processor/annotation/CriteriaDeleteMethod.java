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
			List<ParameterConstraint> parameterConstraints,
			boolean belongsToDao,
			String sessionType,
			String sessionName,
			boolean addNonnullAnnotation,
			boolean dataRepository,
			String fullReturnType) {
		super( annotationMetaEntity, method, methodName, entity, null, null, belongsToDao, sessionType,
				sessionName, emptyList(), paramNames, paramTypes, emptyList(), addNonnullAnnotation, dataRepository,
				parameterConstraints, fullReturnType, false );
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

	@Override
	void executeQuery(StringBuilder declaration, List<String> paramTypes) {
		createSpecification( declaration );
		handleRestrictionParameters( declaration, paramTypes );
		tryReturn(declaration);
		createQuery( declaration, false );
		QueryOptionsSupport.setQueryOptions( this, declaration, true, false );
		execute( declaration );
	}

	void tryReturn(StringBuilder declaration) {
		if ( !isReactive() ) {
			declaration
					.append("\n\ttry {\n\t\t");
		}
		if ( !returnsNoResult() ) {
			returnResult( declaration );
			if ( returnsLong() ) {
				declaration.append( "(long) " );
			}
		}
	}

	@Override
	String createQueryMethod() {
		if ( isUsingEntityManager() ) {
			return "createQuery";
		}
		else if ( isUsingEntityAgent() ) {
			return "createStatement";
		}
		else {
			return "createMutationQuery";
		}
	}

	private boolean returnsLong() {
		return "long".equals( fullReturnType )
			|| fullReturnType.endsWith( "<java.lang.Long>" );
	}

	private boolean returnsNoResult() {
		return "void".equals( fullReturnType )
			|| isAsynchronousCompletionStageWithVoidResult();
	}

	@Override
	String specificationType() {
		return "org.hibernate.query.specification.MutationSpecification";
	}

	private void execute(StringBuilder declaration) {
		declaration
				.append(".executeUpdate()");
		if ( isAsynchronousCompletionStageWithVoidResult() ) {
			declaration
					.append( ";\n\t\t" );
			returnNullResult( declaration );
		}
		else if ( isReactive()
				&& fullReturnType.endsWith("<java.lang.Void>") ) {
			declaration
					.append( ".replaceWithVoid()" );
		}
		else {
			endReturnResult( declaration );
		}
	}

	@Override
	String createCriteriaMethod() {
		return "createCriteriaDelete";
	}
}
