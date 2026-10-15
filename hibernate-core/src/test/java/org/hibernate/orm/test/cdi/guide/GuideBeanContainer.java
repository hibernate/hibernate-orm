package org.hibernate.orm.test.cdi.guide;

import org.hibernate.resource.beans.container.spi.BeanContainer;
import org.hibernate.resource.beans.container.spi.ContainedBean;
import org.hibernate.resource.beans.internal.ManagedBeanRegistryImpl;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;
import org.hibernate.resource.beans.spi.ManagedBean;

/// Test adapter for the User Guide's registration examples. Its delegate exercises
/// Hibernate's fallback, caching, bootstrap-safe acquisition, and release contracts.
/// This fixture is not included as an application adapter implementation in the guide.
///
/// @author Steve Ebersole
public class GuideBeanContainer implements BeanContainer {

	private final BeanContainer delegate = new ManagedBeanRegistryImpl( null ).getBeanContainer();
	boolean stopped;

	@Override
	public <B> ContainedBean<B> getBean(Class<B> type, LifecycleOptions options, BeanInstanceProducer producer) {
		return delegate.getBean( type, options, producer );
	}

	@Override
	public <B> ContainedBean<B> getBean(String name, Class<B> type, LifecycleOptions options, BeanInstanceProducer producer) {
		return delegate.getBean( name, type, options, producer );
	}

	@Override
	public <B> ContainedBean<B> getBootstrapSafeBean(Class<B> type, LifecycleOptions options, BeanInstanceProducer producer) {
		return delegate.getBootstrapSafeBean( type, options, producer );
	}

	@Override
	public void releaseBean(ManagedBean<?> bean) {
		delegate.releaseBean( bean );
	}

	@Override
	public void stop() {
		stopped = true;
		delegate.stop();
	}
}
