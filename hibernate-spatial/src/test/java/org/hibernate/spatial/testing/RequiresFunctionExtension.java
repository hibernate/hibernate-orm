/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.testing;

import java.util.Locale;
import java.util.Optional;

import org.hibernate.query.sqm.function.SqmFunctionRegistry;

import org.hibernate.testing.orm.junit.JUnitHelper;
import org.hibernate.testing.orm.junit.SessionFactoryExtension;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.TestingUtil;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import org.jboss.logging.Logger;

public class RequiresFunctionExtension implements ExecutionCondition {

	private static final ConditionEvaluationResult ENABLED = ConditionEvaluationResult.enabled(
			"No applicable @RequireFunction annotation" );

	private static final Logger log = Logger.getLogger( RequiresFunctionExtension.class );

	@Override
	public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
		Optional<Object> testInstance = context.getTestInstance();

		if ( !testInstance.isPresent() ) {
			return ENABLED;
		}

		ExtensionContext.Store store = JUnitHelper.locateExtensionStore(
				SessionFactoryExtension.class,
				context,
				testInstance.get()
		);

		final SessionFactoryScope existing = (SessionFactoryScope) store.get( SessionFactoryScope.class.getName() );

		if ( existing == null ) {
			return ConditionEvaluationResult.enabled( "" );
		}

		final Optional<RequiresFunction> requiresFunctions = TestingUtil.findEffectiveAnnotation(
				context,
				RequiresFunction.class
		);

		if ( requiresFunctions.isPresent() ) {
			String functionKey = requiresFunctions.get().key();
			SqmFunctionRegistry functionRegistry = existing.getSessionFactory()
					.getQueryEngine()
					.getSqmFunctionRegistry();
			if ( functionRegistry.findFunctionDescriptor( functionKey ) == null ) {
				return ConditionEvaluationResult.disabled( String.format(
						Locale.ROOT,
						"Failed: Required function %s is not available", functionKey
				) );
			}
			return ConditionEvaluationResult.enabled( "Required function is supported" );
		}

		return ENABLED;

	}
}
