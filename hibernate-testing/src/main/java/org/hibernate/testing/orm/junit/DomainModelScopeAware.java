/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.junit;

/// Contract for injection of DomainModelScope.
///
/// @implNote Prefer use of JUnit injection via [DomainModelParameterResolver].
///
/// @author Steve Ebersole
public interface DomainModelScopeAware {
	void injectTestModelScope(DomainModelScope modelScope);
}
