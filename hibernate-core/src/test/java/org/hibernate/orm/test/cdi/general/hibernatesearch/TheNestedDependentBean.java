/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.cdi.general.hibernatesearch;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.Dependent;

/**
 * A dependent bean required by other beans, but never requested directly
 * to the {@link org.hibernate.resource.beans.spi.ManagedBeanRegistry}
 * or {@link org.hibernate.resource.beans.container.spi.BeanContainer}.
 *
 * @author Yoann Rodiere
 */
@Dependent
public class TheNestedDependentBean {

	public TheNestedDependentBean() {
		Monitor.theNestedDependentBean().instantiated();
	}

	public void ensureInitialized() {
		// Nothing to do: if this method is called, all surrounding proxies have been initialized
	}

	@PostConstruct
	public void postConstruct() {
		Monitor.theNestedDependentBean().postConstructCalled();
	}

	@PreDestroy
	public void preDestroy() {
		Monitor.theNestedDependentBean().preDestroyCalled();
	}
}
