/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
@Named(TheAlternativeNamedApplicationScopedBeanImpl.NAME)
public class TheAlternativeNamedApplicationScopedBeanImpl implements TheNamedApplicationScopedBean {
	public static final String NAME = "TheAlternativeNamedApplicationScopedBeanImpl_name";

	@jakarta.inject.Inject
	private TheNestedDependentBean nestedDependentBean;

	public TheAlternativeNamedApplicationScopedBeanImpl() {
		Monitor.theAlternativeNamedApplicationScopedBean().instantiated();
	}

	@Override
	public void ensureInitialized() {
		nestedDependentBean.ensureInitialized();
	}

	@PostConstruct
	public void postConstruct() {
		Monitor.theAlternativeNamedApplicationScopedBean().postConstructCalled();
	}

	@PreDestroy
	public void preDestroy() {
		Monitor.theAlternativeNamedApplicationScopedBean().preDestroyCalled();
	}
}
