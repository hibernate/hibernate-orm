/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.junit5;

import java.util.Locale;
import java.util.function.Supplier;

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



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// ExecutionCondition - used to disable tests that are an `@ExpectedFailure`

	@Override
	public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
		log.tracef( "Evaluating @FailureExpected inclusion/exclusion [%s]", context.getDisplayName() );

		if ( context.getTestMethod().isPresent() ) {
			assert context.getParent().isPresent()
					&& context.getParent().get().getTestClass().isPresent()
					&& context.getRequiredTestMethod().getDeclaringClass().equals( context.getParent().get().getRequiredTestClass() );
			return evaluate( () -> context.getRequiredTestMethod().getAnnotation( FailureExpected.class ) );
		}
		else if ( context.getTestClass().isPresent() ) {
			return evaluate( () -> context.getRequiredTestClass().getAnnotation( FailureExpected.class ) );
		}
		else {
			throw new RuntimeException( "Unable to determine how to handle given ExtensionContext : " + context.getDisplayName() );
		}
	}

	private ConditionEvaluationResult evaluate(Supplier<FailureExpected> expectedFailureSupplier) {
		final FailureExpected expectedFailure = expectedFailureSupplier.get();
		if ( expectedFailure != null ) {
			// as opposed to the older approach of `hibernate.test.validatefailureexpected` as a system setting,
			//		we instead rely on JUnit5's built-in ability to deactivate
			//		ExecutionCondition(s).  So either:
			// 			1) the `@ExpectedFailure` is deactivated ("in effect") in which
			// 				case this method will get called.
			return ConditionEvaluationResult.disabled( "Disabled : @ExpectedFailure" );
		}

		return ConditionEvaluationResult.enabled( "No @ExpectedFailure" );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// BeforeEachCallback

	@Override
	public void beforeEach(ExtensionContext context) {
		final boolean markedExpectedFailure = isMarkedExpectedFailure( context );
		final ExtensionContext.Namespace namespace = generateNamespace( context );

		context.getStore( namespace ).put( IS_MARKED_STORE_KEY, markedExpectedFailure );
	}

	private boolean isMarkedExpectedFailure(ExtensionContext context) {
		// pre-requisite == test method available in context
		// 		- generally speaking there should also be a parent representing
		//			the Class; is there ever a case where this second part is not true?

		assert context.getTestMethod().isPresent();
		if ( context.getRequiredTestMethod().getAnnotation( FailureExpected.class ) != null ) {
			return true;
		}

		assert context.getParent().isPresent() && context.getParent().get().getTestClass().isPresent();
		if ( context.getParent().get().getRequiredTestClass().getAnnotation( FailureExpected.class ) != null ) {
			return true;
		}

		return false;
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
			// see if we had an expected failure...
			final Throwable expectedFailure = (Throwable) store.remove( EXPECTED_FAILURE_STORE_KEY );
			if ( expectedFailure == null ) {
				// there was no expected-failure, even though we are expecting one
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
