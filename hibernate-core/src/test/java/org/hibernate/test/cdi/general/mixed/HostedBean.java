/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.cdi.general.mixed;

/**
 * @author Steve Ebersole
 */
public class HostedBean {
	private final InjectedHostedBean injectedHostedBean;

	@javax.inject.Inject
	public HostedBean(InjectedHostedBean injectedHostedBean) {
		this.injectedHostedBean = injectedHostedBean;
	}

	public InjectedHostedBean getInjectedHostedBean() {
		return injectedHostedBean;
	}
}
