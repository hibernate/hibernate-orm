/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cdi.general.hibernatesearch;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * @author Yoann Rodiere
 */
@ApplicationScoped
public class TheApplicationScopedBean {

	@jakarta.inject.Inject
	private TheNestedDependentBean nestedDependentBean;

	public TheApplicationScopedBean() {
		Monitor.theApplicationScopedBean().instantiated();
	}

	public void ensureInitialized() {
		nestedDependentBean.ensureInitialized();
	}

	@PostConstruct
	public void postConstruct() {
		Monitor.theApplicationScopedBean().postConstructCalled();
	}

	@PreDestroy
	public void preDestroy() {
		Monitor.theApplicationScopedBean().preDestroyCalled();
	}

}
