package org.hibernate.orm.test.interceptor;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.CallbackException;
import org.hibernate.Interceptor;
import org.hibernate.metamodel.RepresentationMode;

public class InstantiateInterceptor implements Interceptor {
	private String injectedString;

	public InstantiateInterceptor(String injectedString) {
		this.injectedString = injectedString;
	}

	@Override
	@Nullable
	public Object instantiate(
			@Nonnull String entityName,
			@Nonnull RepresentationMode entityMode,
			@Nullable Object id) throws CallbackException {
		if ( ! "org.hibernate.orm.test.interceptor.User".equals( entityName ) ) {
			return null;
		}
		// Simply inject a sample string into new instances
		User instance = new User();
		instance.setName( ( String ) id );
		instance.setInjectedString( injectedString );
		return instance;
	}
}
