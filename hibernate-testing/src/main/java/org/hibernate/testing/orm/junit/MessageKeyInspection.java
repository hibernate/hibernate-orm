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

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Injects the ability to watch for a log messages being triggered.
 *
 * For watching a multiple message-keys, see {@link LoggingInspections}
 */
@Inherited
@Target({ ElementType.TYPE, ElementType.ANNOTATION_TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)

@ExtendWith( MessageKeyInspectionExtension.class )
@ExtendWith( MessageKeyWatcherResolver.class )
public @interface MessageKeyInspection {
	/**
	 * The message key to look for.
	 *
	 * @apiNote This is effectively a starts-with check.  We simply check
	 * that the logged message starts with the value from here
	 */
	String messageKey();

	/**
	 * The logger to watch on
	 */
	Logger logger();
}
