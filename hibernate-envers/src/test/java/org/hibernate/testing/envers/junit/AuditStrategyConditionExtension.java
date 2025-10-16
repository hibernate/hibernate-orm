/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.envers.junit;

import org.hibernate.envers.strategy.internal.DefaultAuditStrategy;
import org.hibernate.envers.strategy.spi.AuditStrategy;
import org.hibernate.testing.envers.RequiresAuditStrategy;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.util.AnnotationUtils;

public record AuditStrategyConditionExtension(String auditStrategyInUse) implements ExecutionCondition {
	@Override
	public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
		// get the annotation from the test method or class
		var annotation = AnnotationUtils.findAnnotation( context.getElement(), RequiresAuditStrategy.class );
		if ( annotation.isEmpty() ) {
			annotation = AnnotationUtils.findAnnotation( context.getTestClass(), RequiresAuditStrategy.class );
		}

		if ( annotation.isEmpty() ) {
			return ConditionEvaluationResult.enabled( "No audit strategy requirement" );
		}

		String strategyNameInUse = getStrategySimpleName( auditStrategyInUse );

		for ( Class<? extends AuditStrategy> strategy : annotation.get().value() ) {
			if ( strategy.getSimpleName().equals( strategyNameInUse ) ) {
				return ConditionEvaluationResult.enabled(
						"Audit strategy " + strategyNameInUse + " is allowed"
				);
			}
		}

		return ConditionEvaluationResult.disabled(
				"Required audit strategy not available: " + strategyNameInUse
		);
	}

	private String getStrategySimpleName(String fullName) {
		if ( fullName == null ) {
			return DefaultAuditStrategy.class.getSimpleName();
		}
		return fullName.substring( fullName.lastIndexOf( '.' ) + 1 );
	}
}
