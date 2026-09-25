package org.hibernate.testing.orm;

/// Whether available concurrency facts establish a scenario prerequisite.
/// An undetermined answer must never become a match through negation.
///
/// @since 8.0
/// @author Steve Ebersole
public enum ConcurrencyCheckResult {
	/// The prerequisite is established.
	MATCH,
	/// The prerequisite is known not to hold.
	NON_MATCH,
	/// Available facts establish neither the prerequisite nor its opposite.
	UNDETERMINED;

	/// Reverse an established answer, preserving uncertainty.
	public ConcurrencyCheckResult reversed() {
		return switch ( this ) {
			case MATCH -> NON_MATCH;
			case NON_MATCH -> MATCH;
			case UNDETERMINED -> UNDETERMINED;
		};
	}
}
