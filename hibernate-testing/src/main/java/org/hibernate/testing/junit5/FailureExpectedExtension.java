/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.junit5;

import java.util.Locale;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.junit.platform.commons.support.AnnotationSupport;

import org.jboss.logging.Logger;

/**
 * JUnit 5 extension used to support {@link FailureExpected} handling
 *
 * @author Steve Ebersole
 */
public class FailureExpectedExtension
		implements ExecutionCondition, BeforeEachCallback, AfterEachCallback, TestExecutionExceptionHandler {

	private static final Logger log = Logger.getLogger( FailureExpectedExtension.class );

	private static final String IS_MARKED_STORE_KEY = "IS_MARKED";
	private static final String EXPECTED_FAILURE_STORE_KEY = "EXPECTED_FAILURE";


	private static final boolean failureExpectedValidation;

	static {
		failureExpectedValidation = Boolean.getBoolean( FailureExpected.VALIDATE_FAILURE_EXPECTED );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// ExecutionCondition - used to disable tests that are an `@ExpectedFailure`

	@Override
	public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
		if ( !context.getElement().isPresent() ) {
			throw new RuntimeException( "Unable to determine how to handle given ExtensionContext : " + context.getDisplayName() );
		}

		if ( ! failureExpectedValidation ) {
			// we are not validating the expected failures occur, so it does not matter
			// if a `@FailureExpected` is present because we simply skip the test
			if ( AnnotationSupport.findAnnotation( context.getElement().get(), FailureExpected.class ).isPresent() ) {
				return ConditionEvaluationResult.disabled( "Disabled : @ExpectedFailure" );
			}
		}

		return ConditionEvaluationResult.enabled( "No @ExpectedFailure" );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// BeforeEachCallback

	@Override
	public void beforeEach(ExtensionContext context) {
		final boolean markedExpectedFailure = AnnotationUtil.hasEffectiveAnnotation( context, FailureExpected.class );
		final ExtensionContext.Namespace namespace = generateNamespace( context );

		context.getStore( namespace ).put( IS_MARKED_STORE_KEY, markedExpectedFailure );
	}

	private ExtensionContext.Namespace generateNamespace(ExtensionContext context) {
		return ExtensionContext.Namespace.create(
				getClass().getName(),
				context.getRequiredTestMethod().getClass(),
				context.getRequiredTestMethod().getName()
		);
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// AfterEachCallback - used to interpret the outcome of the test depending
	//		on whether it was marked as an `@ExpectedFailure`



	@Override
	public void afterEach(ExtensionContext context) {
		final ExtensionContext.Store store = context.getStore( generateNamespace( context ) );

		if ( store.get( IS_MARKED_STORE_KEY ) == Boolean.TRUE ) {
			final Throwable expectedFailure = (Throwable) store.remove( EXPECTED_FAILURE_STORE_KEY );
			if ( expectedFailure == null ) {
				// even though we expected a failure, the test did not fail
				throw new ExpectedFailureDidNotFail( context );
			}
		}
	}

	private static class ExpectedFailureDidNotFail extends RuntimeException {
		ExpectedFailureDidNotFail(ExtensionContext context) {
			super(
					String.format(
							Locale.ROOT,
							"`%s#%s` was marked as `@ExpectedFailure`, but did not fail",
							context.getRequiredTestClass().getName(),
							context.getRequiredTestMethod().getName()
					)
			);
		}
	}

	@Override
	public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {

		final ExtensionContext.Store store = context.getStore( generateNamespace( context ) );
		if ( store.get( IS_MARKED_STORE_KEY ) == Boolean.TRUE ) {
			// test is marked as an `@ExpectedFailure`:

			//		1) add an entry to the store
			store.put( EXPECTED_FAILURE_STORE_KEY, throwable );

			// 		2) eat the failure
			return;
		}

		// otherwise, re-throw
		throw throwable;
	}
}
