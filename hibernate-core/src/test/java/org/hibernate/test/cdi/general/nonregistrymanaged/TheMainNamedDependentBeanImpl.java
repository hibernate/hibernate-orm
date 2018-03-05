/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.cdi.general.nonregistrymanaged;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.Dependent;
import javax.inject.Named;

/**
 * @author Yoann Rodiere
 */
@Dependent
@Named(TheMainNamedDependentBeanImpl.NAME)
public class TheMainNamedDependentBeanImpl implements TheNamedDependentBean {
	public static final String NAME = "TheMainNamedDependentBeanImpl_name";

	@javax.inject.Inject
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
