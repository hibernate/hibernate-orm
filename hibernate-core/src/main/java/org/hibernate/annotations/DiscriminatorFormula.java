/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Used to apply a Hibernate formula (derived value) as the inheritance discriminator "column".  Used in place of
 * the JPA {@link javax.persistence.DiscriminatorColumn} when a formula is wanted.
 *
 * To be placed on the root entity.
 *
 * @see Formula
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
@Target({TYPE})
@Retention(RUNTIME)
public @interface DiscriminatorFormula {
	/**
	 * The formula string.
	 */
	String value();
}
