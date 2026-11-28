package org.hibernate.testing.orm.junit;

import org.hibernate.MappingException;
import org.hibernate.TemporalPreservationException;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.internal.util.config.ConfigurationException;
import org.hibernate.query.SemanticException;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.LifecycleMethodExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.opentest4j.TestAbortedException;

/// Supports running the whole test suite with temporal preservation enabled
/// for every test, which the build does when given `-PtemporalPreservation`.
///
/// Temporal preservation rejects every mapping and value it does not support,
/// and much of the test suite maps such things, for purposes which have
/// nothing to do with Java Time. Some tests also explicitly disable direct
/// JDBC access, which contradicts the setting, and some use query functions
/// which are not yet supported for a value it stores in two columns. A test
/// which is rejected for any of these reasons
/// says nothing about temporal preservation, so it is reported as aborted
/// instead of failed, as are the remaining tests of its class. Every other
/// outcome is left alone: a test which runs must pass.
///
/// The tests of temporal preservation itself are exempt. For them a
/// rejection is always a real result.
///
/// Registered through the service loader, and so active only when JUnit's
/// automatic extension detection is enabled, and then only when the setting
/// is enabled through a system property.
///
/// @author Steve Ebersole
public class TemporalPreservationProfileExtension
		implements ExecutionCondition, TestExecutionExceptionHandler, LifecycleMethodExecutionExceptionHandler {
	private static final String EXEMPT_PACKAGE = "org.hibernate.orm.test.type.temporal.preservation";
	private static final Namespace NAMESPACE = Namespace.create( TemporalPreservationProfileExtension.class );

	/// Once a test of a class has been rejected, the rest of the class is not
	/// run. The test harness releases the factory of a test which throws, and
	/// with it any data the class set up for all of its tests, so what follows
	/// would fail for want of that data.
	@Override
	public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
		final var testClass = context.getTestClass();
		if ( context.getTestMethod().isPresent()
				&& testClass.isPresent()
				&& context.getRoot().getStore( NAMESPACE ).get( testClass.get().getName() ) != null ) {
			return ConditionEvaluationResult.disabled(
					"An earlier test of this class used a mapping or value which temporal preservation does not support"
			);
		}
		return ConditionEvaluationResult.enabled( null );
	}

	@Override
	public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
		throw translate( context, throwable );
	}

	@Override
	public void handleBeforeAllMethodExecutionException(ExtensionContext context, Throwable throwable)
			throws Throwable {
		throw translate( context, throwable );
	}

	@Override
	public void handleBeforeEachMethodExecutionException(ExtensionContext context, Throwable throwable)
			throws Throwable {
		throw translate( context, throwable );
	}

	@Override
	public void handleAfterEachMethodExecutionException(ExtensionContext context, Throwable throwable)
			throws Throwable {
		throw translate( context, throwable );
	}

	@Override
	public void handleAfterAllMethodExecutionException(ExtensionContext context, Throwable throwable)
			throws Throwable {
		throw translate( context, throwable );
	}

	private static Throwable translate(ExtensionContext context, Throwable throwable) {
		if ( Boolean.getBoolean( MappingSettings.TEMPORAL_PRESERVATION )
				&& !isExempt( context )
				&& isRejection( throwable ) ) {
			context.getTestClass().ifPresent(
					testClass -> context.getRoot().getStore( NAMESPACE ).put( testClass.getName(), Boolean.TRUE )
			);
			return new TestAbortedException(
					"Uses a mapping or value which temporal preservation does not support",
					throwable
			);
		}
		return throwable;
	}

	private static boolean isExempt(ExtensionContext context) {
		return context.getTestClass()
				.map( testClass -> testClass.getName().startsWith( EXEMPT_PACKAGE ) )
				.orElse( false );
	}

	private static boolean isRejection(Throwable throwable) {
		for ( Throwable cause = throwable; cause != null; cause = cause.getCause() ) {
			if ( cause instanceof TemporalPreservationException
				|| ( cause instanceof MappingException
						|| cause instanceof ConfigurationException
						|| cause instanceof SemanticException )
						&& String.valueOf( cause.getMessage() ).contains( MappingSettings.TEMPORAL_PRESERVATION ) ) {
				return true;
			}
			if ( cause.getCause() == cause ) {
				break;
			}
		}
		return false;
	}
}
