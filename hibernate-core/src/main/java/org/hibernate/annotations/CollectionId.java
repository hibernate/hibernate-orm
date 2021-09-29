/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.persistence.Column;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Describe an identifier column for a bag (ie an idbag).
 *
 * EXPERIMENTAL: the structure of this annotation might slightly change (generator() mix strategy and generator
 * 
 * @author Emmanuel Bernard
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface CollectionId {
	/**
	 * Collection id column(s).
	 *
	 * @deprecated Only basic (single column) collection-ids are supported.
	 * Use {@link #column} instead
	 */
	@Deprecated
	Column[] columns() default {};

	Column column() default @Column;

	/**
	 * id type, type.type() must be set.
	 */
	Type type();

	/**
	 * The generator name.  For example 'identity' or a defined generator name
	 */
	String generator();
}
