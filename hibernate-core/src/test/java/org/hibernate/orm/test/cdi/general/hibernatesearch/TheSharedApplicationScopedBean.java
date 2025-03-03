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
public class TheSharedApplicationScopedBean {

	@jakarta.inject.Inject
	private TheNestedDependentBean nestedDependentBean;

	public TheSharedApplicationScopedBean() {
		Monitor.theSharedApplicationScopedBean().instantiated();
	}

	public void ensureInitialized() {
		nestedDependentBean.ensureInitialized();
	}

	@PostConstruct
	public void postConstruct() {
		Monitor.theSharedApplicationScopedBean().postConstructCalled();
	}

	@PreDestroy
	public void preDestroy() {
		Monitor.theSharedApplicationScopedBean().preDestroyCalled();
	}

}
