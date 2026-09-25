package org.hibernate.testing.orm.junit;

import java.util.Objects;
import java.util.function.Supplier;

import org.hibernate.dialect.lock.spi.TransactionConcurrency;
import org.hibernate.testing.orm.ConcurrencyCheckResult;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;
import org.opentest4j.TestAbortedException;

/// Coordinates deferred checks with the factory extensions without depending
/// on annotation registration order or caching a descriptor across factories.
///
/// @since 8.0
/// @author Steve Ebersole
final class TransactionConcurrencyFeatureChecks {
	private TransactionConcurrencyFeatureChecks() {
	}

	static boolean isDeferred(RequiresDialectFeature requirement) {
		return TransactionConcurrencyFeatureCheck.class.isAssignableFrom( requirement.feature() );
	}

	static void validateScope(ExtensionContext context) {
		// A class may use method-level factories, so validate at the method only.
		if ( context.getTestMethod().isEmpty() ) {
			return;
		}
		final var testClass = context.getRequiredTestClass();
		final var testMethod = context.getRequiredTestMethod();
		if ( !AnnotationSupport.isAnnotated( testClass, SessionFactory.class )
				&& !AnnotationSupport.isAnnotated( testMethod, SessionFactory.class )
				&& !SessionFactoryProducer.class.isAssignableFrom( testClass )
				&& !AnnotationSupport.isAnnotated( testClass, Jpa.class )
				&& !AnnotationSupport.isAnnotated( testMethod, Jpa.class ) ) {
			throw new IllegalStateException( "TransactionConcurrencyFeatureCheck requires a managed factory scope: "
					+ context.getDisplayName() );
		}
	}

	static void evaluate(ExtensionContext context, Supplier<TransactionConcurrency> supplier) {
		TransactionConcurrency concurrency = null;
		for ( var requirement : TestingUtil.collectAnnotations(
				context, RequiresDialectFeature.class, RequiresDialectFeatureGroup.class ) ) {
			if ( !isDeferred( requirement ) ) {
				continue;
			}
			if ( concurrency == null ) {
				concurrency = supplier.get();
			}
			final TransactionConcurrencyFeatureCheck check;
			try {
				check = (TransactionConcurrencyFeatureCheck) requirement.feature().getConstructor().newInstance();
			}
			catch (ReflectiveOperationException e) {
				throw new IllegalStateException( "Unable to instantiate " + requirement.feature().getName(), e );
			}
			final var result = Objects.requireNonNull( check.evaluate( concurrency ), "Concurrency check returned null" );
			final var effective = requirement.reverse() ? result.reversed() : result;
			if ( effective != ConcurrencyCheckResult.MATCH ) {
				throw new TestAbortedException( "Unmet @RequiresDialectFeature [" + requirement.feature().getSimpleName()
						+ "]: descriptor=" + concurrency.getName() + ", result=" + result
						+ ", reverse=" + requirement.reverse() + "; " + requirement.comment() );
			}
		}
	}
}
