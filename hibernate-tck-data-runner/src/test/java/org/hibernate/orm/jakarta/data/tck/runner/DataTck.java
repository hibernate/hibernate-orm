/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.jakarta.data.tck.runner;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Configures a Jakarta Data TCK test with ORM SessionFactory and Weld CDI.
 * <p>
 * The {@link DataTckExtension} builds a SessionFactory from the declared domain classes,
 * then bootstraps a Weld CDI container with the repository classes and injects the test instance.
 */
@Inherited
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(DataTckExtension.class)
public @interface DataTck {
	/**
	 * Entity classes for the domain model.
	 */
	Class<?>[] domainClasses();

	/**
	 * Generated repository implementation classes to register as CDI beans.
	 */
	Class<?>[] repositoryClasses();

	/**
	 * Whether the test method gets a single entity manager and it is shared
	 * with all the repositories.
	 */
	boolean sharedEntityManager() default false;
}
