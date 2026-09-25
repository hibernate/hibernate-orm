package org.hibernate.testing.orm.junit;

/**
 * @author Steve Ebersole
 */
public interface ServiceRegistryScopeAware {
	void injectServiceRegistryScope(ServiceRegistryScope registryScope);
}
