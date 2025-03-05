/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.junit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.integrator.spi.Integrator;

/**
 * Used to define the bootstrap ServiceRegistry to be used for testing.
 */
@Inherited
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)

@ServiceRegistryFunctionalTesting
public @interface BootstrapServiceRegistry {

	Class<? extends Integrator>[] integrators() default {};

	JavaService[] javaServices() default {};

	@interface JavaService {
		/**
		 * Logically `?` is `T`
		 */
		Class<?> role();
		/**
		 * Logically `?` is `S extends T`
		 */
		Class<?> impl();
	}

}
