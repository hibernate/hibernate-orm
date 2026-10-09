package org.hibernate.callback.spi;

import org.hibernate.Incubating;
import org.hibernate.Interceptor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.resource.beans.spi.ManagedBean;

import jakarta.annotation.Nullable;

/**
 * Strategy for resolving and managing {@link Interceptor} instances
 * across session lifecycle boundaries.
 *
 * @author Sean Okafor
 *
 * @since 8.0
 */
@Incubating(since = "8.0")
public interface InterceptorStrategy {

	/**
	 * Obtain an interceptor for a newly created session.
	 */
	Interceptor getInterceptorForSession(SessionFactoryImplementor sessionFactory);

	/**
	 * Release an interceptor when its session closes.
	 */
	default void releaseInterceptor(Interceptor interceptor) {
	}

	/**
	 * Does this strategy produce session-scoped interceptors, i.e. a
	 * distinct interceptor instance per session?
	 * <p>
	 * Consulted by {@code noSessionInterceptorCreation()} to determine
	 * whether a session-level interceptor should be suppressed: scoped
	 * interceptors are suppressed, while global and explicitly-provided
	 * instance interceptors are not.
	 */
	default boolean isScoped() {
		return false;
	}

	/**
	 * Register an additional holder of the given interceptor, which was
	 * already obtained from {@link #getInterceptorForSession}, so that
	 * the strategy can track how many sessions are sharing it.
	 * <p>
	 * Called when a child session is created sharing its parent's
	 * interceptor. Strategies that track per-instance state (such as
	 * {@code ScopedInterceptorStrategy}) use this to ref-count the
	 * underlying bean so it isn't destroyed while any holder is still
	 * using it.
	 */
	default void registerSharedUse(Interceptor interceptor) {
	}

	/**
	 * Return a factory-level interceptor, or {@code null} if this
	 * strategy does not provide one.
	 *
	 * @deprecated Use {@link #getFactoryInterceptorBean()} instead
	 */
	@Deprecated(since = "8.0")
	default @Nullable Interceptor getFactoryInterceptor() {
		final var bean = getFactoryInterceptorBean();
		return bean == null ? null : bean.getBeanInstance();
	}

	/**
	 * Return a managed bean wrapping the factory-level interceptor,
	 * or {@code null} if this strategy does not provide one.
	 * The bean is not resolved until {@link ManagedBean#getBeanInstance()}
	 * is called.
	 */
	default @Nullable ManagedBean<? extends Interceptor> getFactoryInterceptorBean() {
		return null;
	}
}
