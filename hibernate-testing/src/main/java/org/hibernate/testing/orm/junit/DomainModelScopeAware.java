package org.hibernate.testing.orm.junit;

/// Contract for injection of DomainModelScope.
///
/// @implNote Prefer use of JUnit injection via [DomainModelParameterResolver].
///
/// @author Steve Ebersole
public interface DomainModelScopeAware {
	void injectTestModelScope(DomainModelScope modelScope);
}
