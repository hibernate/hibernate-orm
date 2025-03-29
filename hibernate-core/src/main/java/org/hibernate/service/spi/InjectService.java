/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.service.spi;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotation to allow services to request injection of other services.
 *
 * @author Steve Ebersole
 */
@Target(METHOD)
@Retention(RUNTIME)
public @interface InjectService {
	/**
	 * The service role to inject, the default is to use the type of the parameter to which this method is
	 * attached.
	 *
	 * @return The service role.
	 */
	Class serviceRole() default Void.class;

	/**
	 * Is the service to be injected required (not optional)?
	 *
	 * @return True if the service is required.
	 */
	boolean required() default true;
}
