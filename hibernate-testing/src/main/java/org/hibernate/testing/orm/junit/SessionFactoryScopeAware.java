package org.hibernate.testing.orm.junit;

/**
 * @author Steve Ebersole
 */
public interface SessionFactoryScopeAware {
	/**
	 * Callback to inject the SessionFactoryScope into the container
	 */
	void injectSessionFactoryScope(SessionFactoryScope scope);
}
