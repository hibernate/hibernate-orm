/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.junit;

/**
 * @author Steve Ebersole
 */
public interface SessionFactoryScopeAware {
	/**
	 * Callback to inject the SessionFactoryScope into the container
	 */
	void injectSessionFactoryScope(SessionFactoryScope scope);
}
