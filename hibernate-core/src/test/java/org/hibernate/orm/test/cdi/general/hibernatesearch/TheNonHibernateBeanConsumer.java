/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cdi.general.hibernatesearch;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;

/**
 * Simulates CDI bean consumers outside of Hibernate ORM
 * that rely on the same beans used in Hibernate ORM.
 *
 * Allows us to assert that consumed beans can be shared between Hibernate ORM consumers
 * and non-Hibernate consumers.
 *
 * @author Yoann Rodiere
 */
@Singleton
public class TheNonHibernateBeanConsumer {
	public static final String NAME = "TheAlternativeNamedApplicationScopedBeanImpl_name";

	@jakarta.inject.Inject
	private TheSharedApplicationScopedBean sharedApplicationScopedBean;

	@PostConstruct
	public void ensureInstancesInitialized() {
		sharedApplicationScopedBean.ensureInitialized();
	}
}
