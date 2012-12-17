/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.testing;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.dialect.Dialect;

/**
 * Annotation used to indicate that a test should be run only when run against the
 * indicated dialects.
 *
 * @see RequiresDialects
 *
 * @author Hardy Ferentschik
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresDialect {
	/**
	 * The dialects against which to run the test
	 *
	 * @return The dialects
	 */
	Class<? extends Dialect>[] value();

	/**
	 * Used to indicate if the dialects should be matched strictly (classes equal) or
	 * non-strictly (instanceof).
	 *
	 * @return Should strict matching be used?
	 */
	boolean strictMatching() default false;

	/**
	 * Comment describing the reason why the dialect is required.
	 *
	 * @return The comment
	 */
	String comment() default "";

	/**
	 * The key of a JIRA issue which relates this this restriction
	 *
	 * @return The jira issue key
	 */
	String jiraKey() default "";
}
