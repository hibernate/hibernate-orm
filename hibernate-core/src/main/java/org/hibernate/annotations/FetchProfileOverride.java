/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import jakarta.persistence.FetchType;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static jakarta.persistence.FetchType.EAGER;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.hibernate.annotations.FetchMode.JOIN;

/**
 * Overrides the fetching strategy for the annotated association
 * in a certain named {@linkplain FetchProfile fetch profile}. A
 * "strategy" is a fetching {@linkplain #mode method}, together
 * with the {@linkplain #fetch timing}. If {@link #mode} and
 * {@link #fetch} are both unspecified, the strategy defaults to
 * {@linkplain FetchType#EAGER eager} {@linkplain FetchMode#JOIN join}
 * fetching.
 * <p>
 * The specified {@linkplain #profile profile name} must match the
 * name of an existing fetch profile declared using the
 * {@link FetchProfile#name @FetchProfile} annotation.
 *
 * @author Gavin King
 *
 * @see FetchMode
 * @see FetchProfile
 * @see FetchProfile.FetchOverride
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)
@Repeatable(FetchProfileOverrides.class)
public @interface FetchProfileOverride {
	/**
	 * The method that should be used to fetch the association
	 * in the named fetch profile.
	 */
	FetchMode mode() default JOIN;

	/**
	 * The timing of association fetching in the named fetch
	 * profile.
	 */
	FetchType fetch() default EAGER;

	/**
	 * The name of the {@link FetchProfile fetch profile} in
	 * which this fetch mode should be applied.
	 */
	String profile();
}
