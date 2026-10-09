package org.hibernate.callback.internal;

import java.util.function.Supplier;

import org.hibernate.Interceptor;
import org.hibernate.callback.spi.ConfiguredInterceptor;
import org.hibernate.callback.spi.InterceptorType;

import jakarta.annotation.Nullable;

/**
 * Default implementation of {@link ConfiguredInterceptor}.
 *
 * @author Sean Okafor
 */
public record ConfiguredInterceptorImpl(
		InterceptorType type,
		@Nullable Object reference
) implements ConfiguredInterceptor {

	private static final ConfiguredInterceptorImpl NONE =
			new ConfiguredInterceptorImpl( InterceptorType.NONE, null );

	public static ConfiguredInterceptorImpl none() {
		return NONE;
	}

	public static ConfiguredInterceptorImpl ofInstance(Interceptor interceptor) {
		return new ConfiguredInterceptorImpl( InterceptorType.INSTANCE, interceptor );
	}

	public static ConfiguredInterceptorImpl ofGlobal(Class<? extends Interceptor> interceptorClass) {
		return new ConfiguredInterceptorImpl( InterceptorType.GLOBAL, interceptorClass );
	}

	public static ConfiguredInterceptorImpl ofScoped(Class<? extends Interceptor> interceptorClass) {
		return new ConfiguredInterceptorImpl( InterceptorType.SCOPED, interceptorClass );
	}

	public static ConfiguredInterceptorImpl ofSupplied(Supplier<? extends Interceptor> supplier) {
		return new ConfiguredInterceptorImpl( InterceptorType.SUPPLIED, supplier );
	}

	@Override
	public InterceptorType getType() {
		return type;
	}

	@Override
	public @Nullable Object getReference() {
		return reference;
	}
}
