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
package org.hibernate.junit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.dialect.Dialect;

/**
 * Annotation used to indicate that a test should be skipped when run against the
 * indicated dialects.
 *
 * @author Hardy Ferentschik
 * @author Steve Ebersole
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface SkipForDialect {
	/**
	 * The dialects against which to skip the test
	 * @return The dialects
	 */
	Class<? extends Dialect>[] value();

	/**
	 * Used to indicate if the dialects should be matched strictly (classes equal) or
	 * non-strictly (instanceof).
	 * @return Should strict matching be used?
	 */
	boolean strictMatching() default false;

	/**
	 * Comment describing the reason for the skip.
	 * @return The comment
	 */
	String comment() default "";

	/**
	 * The key of a JIRA issue which covers the reason for this skip.  Eventually we should make this
	 * a requirement.
	 * @return The jira issue key
	 */
	String jiraKey() default "";
}