package org.hibernate.callback.internal;

import java.util.function.Supplier;

import org.hibernate.Interceptor;
import org.hibernate.callback.spi.InterceptorStrategy;
import org.hibernate.engine.spi.SessionFactoryImplementor;

/**
 * Strategy that delegates interceptor creation to a user-supplied
 * {@link Supplier}. A new interceptor is obtained per session.
 *
 * @author Sean Okafor
 */
public class SupplierInterceptorStrategy implements InterceptorStrategy {

	private final Supplier<? extends Interceptor> supplier;

	public SupplierInterceptorStrategy(Supplier<? extends Interceptor> supplier) {
		this.supplier = supplier;
	}

	@Override
	public Interceptor getInterceptorForSession(SessionFactoryImplementor sessionFactory) {
		return supplier.get();
	}

	@Override
	public boolean isScoped() {
		return true;
	}
}
