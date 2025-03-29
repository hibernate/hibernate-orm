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

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Injects the ability to watch multiple for log messages being triggered.
 *
 * Only available at the class-level
 *
 * For watching a single message-key, {@link MessageKeyInspection} is a
 * better option.
 */
@Inherited
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)

@ExtendWith( LoggingInspectionsExtension.class )
@ExtendWith( LoggingInspectionsScopeResolver.class )
public @interface LoggingInspections {
	Message[] messages() default {};

	@interface Message {
		/**
		 * The message-key to watch for.  The message-key is the combination of
		 * {@link org.jboss.logging.annotations.MessageLogger#projectCode()}
		 * and {@link org.jboss.logging.annotations.Message#id()} used by
		 * JBoss Logging to prefix each messaged log event
		 */
		String messageKey();

		/**
		 * Descriptor of the log messages to watch for
		 */
		Logger[] loggers() default {};
	}
}
