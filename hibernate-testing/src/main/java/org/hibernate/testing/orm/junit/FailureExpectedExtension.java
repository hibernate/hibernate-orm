/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.junit;

import java.util.Locale;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;

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


	public static final boolean failureExpectedValidation;

	static {
		failureExpectedValidation = Boolean.getBoolean( FailureExpected.VALIDATE_FAILURE_EXPECTED );
		log.debugf( "FailureExpectedExtension#failureExpectedValidation = %s", failureExpectedValidation );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// ExecutionCondition
	// 		- used to disable tests that are an `@ExpectedFailure` when
	// 			failureExpectedValidation == false which is the default.
	//
	// 			When failureExpectedValidation == true, the test is allowed to
	// 			run and we validate that the test does in fact fail.

	@Override
	public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
		log.tracef( "#evaluateExecutionCondition(%s)", context.getDisplayName() );

		if ( !context.getElement().isPresent() ) {
			throw new RuntimeException( "Unable to determine how to handle given ExtensionContext : " + context.getDisplayName() );
		}

		log.debugf( "Evaluating context - %s [failureExpectedValidation = %s]", context.getDisplayName(), failureExpectedValidation );

		if ( TestingUtil.hasEffectiveAnnotation( context, FailureExpected.class )
				|| TestingUtil.hasEffectiveAnnotation( context, FailureExpectedGroup.class ) ) {
			// The test is marked as `FailureExpected`...
			if ( failureExpectedValidation ) {
				log.debugf( "Executing test marked with `@FailureExpected` for validation" );
				return ConditionEvaluationResult.enabled( "@ExpectedFailure validation" );
			}
			else {
				return ConditionEvaluationResult.disabled( "Disabled : @ExpectedFailure" );
			}
		}

		return ConditionEvaluationResult.enabled( "No @ExpectedFailure" );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// BeforeEachCallback
	//		- used to determine whether a test is considered as an expected
	//			failure.  If so,

	@Override
	public void beforeEach(ExtensionContext context) {
		log.tracef( "#beforeEach(%s)", context.getDisplayName() );

		final boolean markedExpectedFailure = TestingUtil.hasEffectiveAnnotation( context, FailureExpected.class )
				|| TestingUtil.hasEffectiveAnnotation( context, FailureExpectedGroup.class );

		log.debugf( "Checking for @FailureExpected [%s] - %s", context.getDisplayName(), markedExpectedFailure );

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
		log.tracef( "#afterEach(%s)", context.getDisplayName() );

		final ExtensionContext.Store store = context.getStore( generateNamespace( context ) );

		final Boolean isMarked = (Boolean) store.remove( IS_MARKED_STORE_KEY );
		log.debugf( "Post-handling for @FailureExpected [%s] - %s", context.getDisplayName(), isMarked );

		if ( isMarked == Boolean.TRUE ) {
			final Throwable expectedFailure = (Throwable) store.remove( EXPECTED_FAILURE_STORE_KEY );
			log.debugf( "  >> Captured exception - %s", expectedFailure );

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
		log.tracef( "#handleTestExecutionException(%s, %s)", context.getDisplayName(), throwable.getClass().getName() );

		final ExtensionContext.Store store = context.getStore( generateNamespace( context ) );

		final Boolean isMarked = (Boolean) store.get( IS_MARKED_STORE_KEY );
		log.debugf( "Handling test exception [%s]; marked @FailureExcepted = %s", context.getDisplayName(), isMarked );

		if ( isMarked == Boolean.TRUE ) {
			// test is marked as an `@ExpectedFailure`:

			//		1) add the exception to the store
			store.put( EXPECTED_FAILURE_STORE_KEY, throwable );
			log.debugf( "  >> Stored expected failure - %s", throwable );

			// 		2) eat the failure
			return;
		}

		// otherwise, re-throw
		throw throwable;
	}
}
