/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cdi.general.hibernatesearch;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

/**
 * @author Yoann Rodiere
 */
@ApplicationScoped
@Named(TheMainNamedApplicationScopedBeanImpl.NAME)
public class TheMainNamedApplicationScopedBeanImpl implements TheNamedApplicationScopedBean {
	public static final String NAME = "TheMainNamedApplicationScopedBeanImpl_name";

	@jakarta.inject.Inject
	private TheNestedDependentBean nestedDependentBean;

	public TheMainNamedApplicationScopedBeanImpl() {
		Monitor.theMainNamedApplicationScopedBean().instantiated();
	}

	@Override
	public void ensureInitialized() {
		nestedDependentBean.ensureInitialized();
	}

	@PostConstruct
	public void postConstruct() {
		Monitor.theMainNamedApplicationScopedBean().postConstructCalled();
	}

	@PreDestroy
	public void preDestroy() {
		Monitor.theMainNamedApplicationScopedBean().preDestroyCalled();
	}
}
