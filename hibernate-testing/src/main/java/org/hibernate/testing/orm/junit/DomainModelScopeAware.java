/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.junit;

/**
 * @author Steve Ebersole
 */
public interface DomainModelScopeAware {
	void injectTestModelScope(DomainModelScope modelScope);
}
