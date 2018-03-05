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

/**
 * @author Yoann Rodiere
 */
@Dependent
public class TheDependentBean {
	@javax.inject.Inject
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
