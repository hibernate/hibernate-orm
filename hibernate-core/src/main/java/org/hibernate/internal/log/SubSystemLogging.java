/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.log;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Used to annotate classes which define sub-system style logging where
 * loggers are hierarchically defined around functionalities rather than
 * class and package names
 * <p>
 * This is helpful to find such classes and is used to generate report
 * (as a release artifact) describing logger names for logging configuration
 * by the application.
 * <p>
 * At the moment Hibernate uses a mix sub-system logging and the more traditional
 * package and class name based logging.  This annotation focuses on the classes
 * defining the sub-system approach
 *
 * @author Steve Ebersole
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface SubSystemLogging {
	/**
	 * Base category name for sub-system style logging
	 */
	String BASE = "org.hibernate.orm";

	/**
	 * The sub-system name, which is used as the "logger name"
	 */
	String name();

	/**
	 * Description of the information logged
	 */
	String description();

	/**
	 * Aside from test usage, is the associated logger always used
	 * through the sub-system category name?
	 */
	boolean mixed() default false;
}
