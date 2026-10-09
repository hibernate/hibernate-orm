package org.hibernate.callback.spi;

import org.hibernate.Incubating;

import jakarta.annotation.Nullable;

/**
 * Describes how an {@link org.hibernate.Interceptor} was configured,
 * without resolving any CDI or managed beans.
 *
 * @author Sean Okafor
 *
 * @since 8.0
 */
@Incubating(since = "8.0")
public interface ConfiguredInterceptor {

	/**
	 * The type of interceptor configuration.
	 */
	InterceptorType getType();

	/**
	 * The raw configuration reference: an {@link org.hibernate.Interceptor}
	 * instance, a {@link Class}, or {@code null}.
	 */
	@Nullable Object getReference();
}
