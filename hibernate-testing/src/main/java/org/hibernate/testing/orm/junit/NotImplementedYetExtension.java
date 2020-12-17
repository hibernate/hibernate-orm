/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.junit;

import java.util.Locale;

import org.hibernate.NotImplementedYetException;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;

import org.jboss.logging.Logger;

/**
 * JUnit 5 extension used to support {@link NotImplementedYet} handling
 *
 * @author Jan Schatteman
 */
public class NotImplementedYetExtension
		implements ExecutionCondition, AfterEachCallback, TestExecutionExceptionHandler {

	private static final Logger log = Logger.getLogger( NotImplementedYetExtension.class );

	private static final String NOTIMPLEMENTED_STORE_KEY = "NOT_IMPLEMENTED";

	@Override
	public void afterEach(ExtensionContext context) throws Exception {
		log.debugf( "#afterEach(%s)", context.getDisplayName() );

		class NotImplementedYetExceptionExpected extends RuntimeException {
			private NotImplementedYetExceptionExpected() {
				super(
						String.format(
								Locale.ROOT,
								"`%s#%s` is marked as '@NotImplementedYet' but did not throw a NotImplementedYetException.\n" +
										" Either it should or, the tested functionality has been implemented, the Test passes," +
										" and @NotImplementedYet should be removed",
								context.getRequiredTestClass().getName(),
								context.getRequiredTestMethod().getName()
						)
				);
			}
		}

		Throwable throwable = context.getStore( getNamespace( context ) ).remove(
				NOTIMPLEMENTED_STORE_KEY,
				Throwable.class
		);
		if ( throwable == null ) {
			throw new NotImplementedYetExceptionExpected();
		}
	}

	@Override
	public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
		log.debugf( "#handleTestExecutionException(%s)", context.getDisplayName() );

		// If an exception is thrown, then it needs to be of type NotImplementedYetException
		context.getStore( getNamespace( context ) ).put( NOTIMPLEMENTED_STORE_KEY, throwable );
		if ( throwable instanceof NotImplementedYetException ) {
			log.debugf( "#Captured exception %s - ignoring it", throwable );
			return;
		}
		// If not, rethrow
		throw throwable;
	}

	@Override
	public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
		log.debugf( "#evaluateExecutionCondition(%s)", context.getDisplayName() );

		if ( !context.getElement().isPresent() ) {
			throw new RuntimeException( "Unable to determine how to handle given ExtensionContext : " + context.getDisplayName() );
		}

		// Test this in case some other annotation were extended with NotImplementedYetExtension
		if ( !TestingUtil.hasEffectiveAnnotation( context, NotImplementedYet.class ) ) {
			return ConditionEvaluationResult.disabled( context.getDisplayName() + " is not marked as `@NotImplementedYet`" );
		}
		return ConditionEvaluationResult.enabled( "Always enabled" );
	}

	private ExtensionContext.Namespace getNamespace(ExtensionContext context) {
		return ExtensionContext.Namespace.create(
				getClass().getName(),
				context.getRequiredTestMethod().getClass(),
				context.getRequiredTestMethod().getName()
		);
	}
}
