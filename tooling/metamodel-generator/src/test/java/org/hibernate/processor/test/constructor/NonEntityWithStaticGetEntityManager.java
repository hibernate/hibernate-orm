/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.constructor;

import jakarta.persistence.EntityManager;

public abstract class NonEntityWithStaticGetEntityManager {

	public static EntityManager getEntityManager() {
		// In a real-world scenario, this would contain some framework-specific code
		throw new IllegalStateException( "This method shouldn't be called in tests" );
	}

}
