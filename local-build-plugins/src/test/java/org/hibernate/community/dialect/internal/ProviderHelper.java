package org.hibernate.community.dialect.internal;

/// Provider-owned internal helper which must not be reported as an upstream
/// Hibernate dependency.
///
/// @author Steve Ebersole
public class ProviderHelper {
	public String apply(String value) {
		return value;
	}
}
