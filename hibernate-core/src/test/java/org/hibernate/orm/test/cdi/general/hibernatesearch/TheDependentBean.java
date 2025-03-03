/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cdi.general.hibernatesearch;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.Dependent;

/**
 * @author Yoann Rodiere
 */
@Dependent
public class TheDependentBean {
	@jakarta.inject.Inject
	private TheNestedDependentBean dependentBean;

	public TheDependentBean() {
		Monitor.theDependentBean().instantiated();
	}

	public void ensureInitialized() {
		dependentBean.ensureInitialized();
	}

	@PostConstruct
	public void postConstruct() {
		Monitor.theDependentBean().postConstructCalled();
	}

	@PreDestroy
	public void preDestroy() {
		Monitor.theDependentBean().preDestroyCalled();
	}
}
