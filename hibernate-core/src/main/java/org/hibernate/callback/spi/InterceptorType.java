package org.hibernate.callback.spi;

import org.hibernate.Incubating;

/**
 * Describes how an {@link org.hibernate.Interceptor} was configured.
 *
 * @author Sean Okafor
 *
 * @since 8.0
 */
@Incubating(since = "8.0")
public enum InterceptorType {
	NONE,
	INSTANCE,
	GLOBAL,
	SCOPED,
	SUPPLIED
}
