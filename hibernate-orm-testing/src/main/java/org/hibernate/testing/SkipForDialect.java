/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.dialect.Dialect;

/**
 * Annotation used to indicate that a test should be skipped when run against the
 * indicated dialects.
 *
 * @see SkipForDialects
 *
 * @author Hardy Ferentschik
 * @author Steve Ebersole
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
@Repeatable(SkipForDialects.class)
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
