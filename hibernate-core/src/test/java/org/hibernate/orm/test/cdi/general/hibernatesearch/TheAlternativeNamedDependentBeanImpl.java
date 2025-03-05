/*
 * SPDX-License-Identifier: Apache-2.0
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
@Named(TheAlternativeNamedDependentBeanImpl.NAME)
public class TheAlternativeNamedDependentBeanImpl implements TheNamedDependentBean {
	public static final String NAME = "TheAlternativeNamedDependentBeanImpl_name";

	@jakarta.inject.Inject
	private TheNestedDependentBean nestedDependentBean;

	public TheAlternativeNamedDependentBeanImpl() {
		Monitor.theAlternativeNamedDependentBean().instantiated();
	}

	@Override
	public void ensureInitialized() {
		nestedDependentBean.ensureInitialized();
	}

	@PostConstruct
	public void postConstruct() {
		Monitor.theAlternativeNamedDependentBean().postConstructCalled();
	}

	@PreDestroy
	public void preDestroy() {
		Monitor.theAlternativeNamedDependentBean().preDestroyCalled();
	}
}
