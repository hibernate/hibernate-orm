package org.hibernate.testing.orm.junit;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.lock.spi.TransactionConcurrency;
import org.hibernate.testing.orm.ConcurrencyCheckResult;

/// A [RequiresDialectFeature] check evaluated against the effective factory
/// after bootstrap, before user before-each methods. Unlike dialect-only checks,
/// unmet requirements abort the invocation instead of disabling it before bootstrap.
/// Requires a SessionFactoryScope or EntityManagerFactoryScope managed by the
/// testing extensions. Tests creating their own factories should use
/// [org.hibernate.testing.orm.TransactionConcurrencyChecks] directly.
///
/// An undetermined result remains unmet even when the annotation is reversed.
/// A false read guarantee must not be interpreted as proof of its opposite.
///
/// @since 8.0
/// @author Steve Ebersole
public interface TransactionConcurrencyFeatureCheck extends DialectFeatureCheck {
	/// Evaluate the prerequisite using the factory's resolved configuration.
	ConcurrencyCheckResult evaluate(TransactionConcurrency concurrency);

	/// Reject evaluation without the effective factory configuration.
	@Override
	default boolean apply(Dialect dialect) {
		throw new IllegalStateException( "This check requires resolved TransactionConcurrency" );
	}
}
