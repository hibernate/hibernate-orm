/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cdi.general.hibernatesearch;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Named;

/**
 * @author Yoann Rodiere
 */
@Dependent
@Named(TheMainNamedDependentBeanImpl.NAME)
public class TheMainNamedDependentBeanImpl implements TheNamedDependentBean {
	public static final String NAME = "TheMainNamedDependentBeanImpl_name";

	@jakarta.inject.Inject
	private TheNestedDependentBean nestedDependentBean;

	public TheMainNamedDependentBeanImpl() {
		Monitor.theMainNamedDependentBean().instantiated();
	}

	@Override
	public void ensureInitialized() {
		nestedDependentBean.ensureInitialized();
	}

	@PostConstruct
	public void postConstruct() {
		Monitor.theMainNamedDependentBean().postConstructCalled();
	}

	@PreDestroy
	public void preDestroy() {
		Monitor.theMainNamedDependentBean().preDestroyCalled();
	}
}
