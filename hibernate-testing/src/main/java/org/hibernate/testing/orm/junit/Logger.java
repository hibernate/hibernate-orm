/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.junit;

/// Must specify one of [#loggerNameClass] or [#loggerName()]
///
/// @author Steve Ebersole
public @interface Logger {
	// I think we can actually look up the "bare" Logger and still get the same
	// capability in terms of register listeners
	//Class<? extends BasicLogger> messageLoggerClass() default CoreMessageLogger.class;

	/**
	 * The `Class` used as the base for the logger name.
	 *
	 * @see org.jboss.logging.Logger#getLogger(Class)
	 */
	Class<?> loggerNameClass() default void.class;

	/**
	 * The `Class` used as the base for the logger name.
	 *
	 * @see org.jboss.logging.Logger#getLogger(String)
	 */
	String loggerName() default "";
}
