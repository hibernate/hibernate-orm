package org.hibernate.dialect.lock.internal;

/// Conservative fallback for dialects without a concurrency resolver.
/// JDBC isolation establishes basic read guarantees, but no explicit locking
/// strategy or reader/writer compatibility is inferred from SQL syntax.
///
/// @since 8.0
/// @author Steve Ebersole
public final class StandardTransactionConcurrencyResolver extends AbstractTransactionConcurrencyResolver {
	public static final StandardTransactionConcurrencyResolver INSTANCE = new StandardTransactionConcurrencyResolver();

	private StandardTransactionConcurrencyResolver() {
	}
}
