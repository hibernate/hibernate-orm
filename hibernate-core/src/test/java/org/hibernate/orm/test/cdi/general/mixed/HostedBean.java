package org.hibernate.orm.test.cdi.general.mixed;

/**
 * @author Steve Ebersole
 */
public class HostedBean {
	private final InjectedHostedBean injectedHostedBean;

	@jakarta.inject.Inject
	public HostedBean(InjectedHostedBean injectedHostedBean) {
		this.injectedHostedBean = injectedHostedBean;
	}

	public InjectedHostedBean getInjectedHostedBean() {
		return injectedHostedBean;
	}
}
