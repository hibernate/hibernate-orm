package org.hibernate.callback.internal;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

import org.hibernate.Interceptor;
import org.hibernate.callback.spi.InterceptorStrategy;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.resource.beans.spi.BeanInstanceCaching;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.service.ServiceRegistry;

/**
 * Strategy that creates a distinct {@link Interceptor} for each session via a
 * bootstrap-safe uncached bean. Each bean is tracked and released when
 * the session closes.
 *
 * @author Sean Okafor
 */
public class ScopedInterceptorStrategy implements InterceptorStrategy {

	private final Class<? extends Interceptor> interceptorClass;
	private final ServiceRegistry serviceRegistry;
	private final Map<Interceptor, TrackedBean> activeBeans =
			Collections.synchronizedMap( new IdentityHashMap<>() );

	public ScopedInterceptorStrategy(Class<? extends Interceptor> interceptorClass, ServiceRegistry serviceRegistry) {
		this.interceptorClass = interceptorClass;
		this.serviceRegistry = serviceRegistry;
	}

	@Override
	public Interceptor getInterceptorForSession(SessionFactoryImplementor sessionFactory) {
		final var registry = serviceRegistry.getService( ManagedBeanRegistry.class );
		final ManagedBean<? extends Interceptor> bean =
				registry.getBootstrapSafeBean( interceptorClass, BeanInstanceCaching.DISALLOW );
		final Interceptor interceptor = bean.getBeanInstance();
		activeBeans.put( interceptor, new TrackedBean( bean ) );
		return interceptor;
	}

	@Override
	public boolean isScoped() {
		return true;
	}

	@Override
	public void registerSharedUse(Interceptor interceptor) {
		synchronized ( activeBeans ) {
			final var tracked = activeBeans.get( interceptor );
			if ( tracked != null ) {
				tracked.refCount++;
			}
		}
	}

	@Override
	public void releaseInterceptor(Interceptor interceptor) {
		final ManagedBean<? extends Interceptor> beanToRelease;
		synchronized ( activeBeans ) {
			final var tracked = activeBeans.get( interceptor );
			if ( tracked == null ) {
				return;
			}
			if ( --tracked.refCount > 0 ) {
				return;
			}
			activeBeans.remove( interceptor );
			beanToRelease = tracked.bean;
		}
		serviceRegistry.getService( ManagedBeanRegistry.class ).releaseBean( beanToRelease );
	}

	private static final class TrackedBean {
		private final ManagedBean<? extends Interceptor> bean;
		private int refCount = 1;

		private TrackedBean(ManagedBean<? extends Interceptor> bean) {
			this.bean = bean;
		}
	}
}
