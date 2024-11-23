/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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
@Target( ElementType.TYPE )
@Retention( RetentionPolicy.RUNTIME )

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
