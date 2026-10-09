package org.hibernate.internal;

import org.hibernate.EntityNameResolver;
import org.hibernate.Interceptor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.resource.beans.spi.ManagedBean;

import jakarta.annotation.Nullable;

/**
 * @author Steve Ebersole
 */
class CoordinatingEntityNameResolver implements EntityNameResolver {
	private final SessionFactoryImplementor sessionFactory;
	@Nullable
	private final ManagedBean<? extends Interceptor> interceptorBean;
	private final Interceptor interceptor;
	private final SharedSessionContractImplementor session;

	CoordinatingEntityNameResolver(
			SessionFactoryImplementor sessionFactory,
			@Nullable ManagedBean<? extends Interceptor> interceptorBean) {
		this.sessionFactory = sessionFactory;
		this.interceptorBean = interceptorBean;
		this.interceptor = null;
		this.session = null;
	}

	CoordinatingEntityNameResolver(
			SessionFactoryImplementor sessionFactory,
			Interceptor interceptor,
			SharedSessionContractImplementor session) {
		this.sessionFactory = sessionFactory;
		this.interceptorBean = null;
		this.interceptor = interceptor;
		this.session = session;
	}

	private Interceptor interceptor() {
		if ( interceptor != null ) {
			return interceptor;
		}
		return interceptorBean != null ? interceptorBean.getBeanInstance() : EmptyInterceptor.INSTANCE;
	}

	@Override
	public String resolveEntityName(Object entity) {
		final Interceptor resolved = interceptor();
		final String interceptorEntityName = session == null
				? resolved.getEntityName( entity )
				: session.callInterceptorCallback( () -> resolved.getEntityName( entity ) );
		if ( interceptorEntityName != null ) {
			return interceptorEntityName;
		}

		for ( var resolver : sessionFactory.getSessionFactoryOptions().getEntityNameResolvers() ) {
			final String resolverEntityName = resolver.resolveEntityName( entity );
			if ( resolverEntityName != null ) {
				return resolverEntityName;
			}
		}

		for ( var resolver : sessionFactory.getMappingMetamodel().getEntityNameResolvers() ) {
			final String resolverEntityName = resolver.resolveEntityName( entity );
			if ( resolverEntityName != null ) {
				return resolverEntityName;
			}
		}

		// the old-time stand-by...
		return entity.getClass().getName();
	}
}
