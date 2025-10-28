/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cdi.testsupport;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.Retention;

/**
 * @see jakarta.enterprise.inject.se.SeContainerInitializer
 *
 * @author Steve Ebersole
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention( RetentionPolicy.RUNTIME )
@ExtendWith( CdiContainerExtension.class )
@ExtendWith( CdiContainerParameterResolver.class )
public @interface CdiContainer {
	/**
	 * @see jakarta.enterprise.inject.se.SeContainerInitializer#addBeanClasses(Class...)
	 */
	Class<?>[] beanClasses() default {};

	/**
	 * @see jakarta.enterprise.inject.se.SeContainerInitializer#disableDiscovery()
	 */
	boolean enableDiscovery() default false;
}
