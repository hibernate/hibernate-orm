/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cdi.general.hibernatesearch;

import jakarta.enterprise.inject.Vetoed;

/**
 * @author Yoann Rodiere
 */
@Vetoed
public class TheReflectionInstantiatedBean {

	public void ensureInitialized() {
		// No-op
	}

}
