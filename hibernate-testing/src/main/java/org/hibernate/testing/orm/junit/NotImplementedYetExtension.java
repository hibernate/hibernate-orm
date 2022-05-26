/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.junit;

import java.util.Locale;
import java.util.Optional;

import org.hibernate.NotImplementedYetException;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;

import org.jboss.logging.Logger;

import static org.hibernate.testing.orm.junit.FailureExpectedExtension.failureExpectedValidation;

/**
 * JUnit 5 extension used to support {@link NotImplementedYet} handling
 *
 * @author Jan Schatteman
 */
public class NotImplementedYetExtension
		implements TestInstancePostProcessor, ExecutionCondition, BeforeEachCallback,
		AfterEachCallback, AfterAllCallback, TestExecutionExceptionHandler {

	private static final Logger log = Logger.getLogger( NotImplementedYetExtension.class );

	private static final String IS_MARKED_STORE_KEY = "IS_MARKED";
	private static final String IS_STRICT_STORE_KEY = "IS_STRICT";
	private static final String EXCEPTION_STORE_KEY = "NOT_IMPLEMENTED";

	@Override
	public void postProcessTestInstance(Object testInstance, ExtensionContext context) throws Exception {
		JUnitHelper.discoverCallbacks( context, getClass(), NotImplementedYetCallback.class );
	}

	@Override
	public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
		log.debugf( "#evaluateExecutionCondition(%s)", context.getDisplayName() );

		if ( context.getElement().isEmpty() ) {
			throw new RuntimeException( "Unable to determine how to handle given ExtensionContext : " + context.getDisplayName() );
		}

		log.debugf( "Evaluating context - %s [failureExpectedValidation = %s]", context.getDisplayName(), failureExpectedValidation );

		// Test this in case some other annotation were extended with NotImplementedYetExtension
		final Optional<NotImplementedYet> annotation = TestingUtil.findEffectiveAnnotation( context, NotImplementedYet.class );
		if ( annotation.isPresent() ) {
			// The test is marked as `NotImplementedYet`...
			if ( failureExpectedValidation ) {
				log.debugf( "Executing test marked with `@NotImplementedYet` for validation" );
				return ConditionEvaluationResult.enabled( "@NotImplementedYet validation" );
			}
			else {

				return ConditionEvaluationResult.disabled( "Disabled : @NotImplementedYet - " + annotation.get().reason() );
			}
		}

		return ConditionEvaluationResult.enabled( "No @NotImplementedYet" );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// BeforeEachCallback
	//		- used to determine whether a test is considered as an expected
	//			failure.  If so,

	@Override
	public void beforeEach(ExtensionContext context) {
		log.tracef( "#beforeEach(%s)", context.getDisplayName() );

		final Optional<NotImplementedYet> annRef = TestingUtil.findEffectiveAnnotation( context, NotImplementedYet.class );

		final boolean isMarked = annRef.isPresent();
		final boolean isStrict;
		if ( isMarked ) {
			final NotImplementedYet ann = annRef.get();
			isStrict = ann.strict();
		}
		else {
			isStrict = false;
		}

		log.debugf(
				"Checking `%s` for @NotImplementedYet - isMarked = %s, isStrict = %s",
				context.getDisplayName(),
				isMarked,
				isStrict
		);

		final ExtensionContext.Namespace namespace = generateNamespace( context );
		context.getStore( namespace ).put( IS_MARKED_STORE_KEY, isMarked );
		context.getStore( namespace ).put( IS_STRICT_STORE_KEY, isStrict );

	}

	@Override
	public void afterEach(ExtensionContext context) {
		log.debugf( "#afterEach(%s)", context.getDisplayName() );

		final ExtensionContext.Namespace namespace = generateNamespace( context );
		final ExtensionContext.Store store = context.getStore( namespace );

		final Boolean isMarked = (Boolean) store.remove( IS_MARKED_STORE_KEY );
		log.debugf( "Post-handling for @FailureExpected [%s] - %s", context.getDisplayName(), isMarked );

		if ( isMarked == Boolean.TRUE ) {
			final Throwable expectedFailure = (Throwable) store.remove( EXCEPTION_STORE_KEY );
			final Boolean isStrict = (Boolean) store.remove( IS_STRICT_STORE_KEY );
			log.debugf( "  >> Captured exception - %s", expectedFailure );

			if ( expectedFailure == null ) {
				// even though we expected a failure, the test did not fail
				throw new NotImplementedYetExceptionExpected(
						context.getRequiredTestClass().getName(),
						context.getRequiredTestMethod().getName(),
						isStrict
				);
			}
		}
	}

	@Override
	public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
		log.debugf( "#handleTestExecutionException(%s)", context.getDisplayName() );

		final ExtensionContext.Namespace namespace = generateNamespace( context );
		final ExtensionContext.Store store = context.getStore( namespace );

		final Boolean isMarked = (Boolean) store.get( IS_MARKED_STORE_KEY );
		final Boolean isStrict = (Boolean) store.get( IS_STRICT_STORE_KEY );

		if ( isMarked ) {
			JUnitHelper.invokeCallbacks( context, getClass() );

			Throwable t = throwable;
			do {
				if ( t instanceof NotImplementedYetException || ! isStrict ) {
					store.put( EXCEPTION_STORE_KEY, t );

					log.debugf( "#Captured exception %s - ignoring it as expected", t );
					return;
				}
				t = t.getCause();
			} while ( t != null );
		}

		// Otherwise, rethrow
		throw throwable;
	}

	private ExtensionContext.Namespace generateNamespace(ExtensionContext context) {
		return ExtensionContext.Namespace.create(
				getClass().getName(),
				context.getRequiredTestMethod().getClass(),
				context.getRequiredTestMethod().getName()
		);
	}

	@Override
	public void afterAll(ExtensionContext context) {
		JUnitHelper.cleanupCallbacks( context, getClass() );
	}

	public static class NotImplementedYetExceptionExpected extends RuntimeException {

		private NotImplementedYetExceptionExpected(String testClassName, String testMethodName, boolean strict) {
			super(
					String.format(
							Locale.ROOT,
							"`%s#%s` is marked with `@NotImplementedYet`, however the test did not " +
									"fail (%s).  If the functionality has been implemented the `@NotImplementedYet` " +
									"annotation should be removed",
							testClassName,
							testMethodName,
							strict
					)
			);
		}
	}
}
