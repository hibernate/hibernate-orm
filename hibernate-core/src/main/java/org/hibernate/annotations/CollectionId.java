/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.hibernate.id.IdentifierGenerator;

import jakarta.persistence.Column;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Describe an identifier column for a bag.
 *
 * @author Emmanuel Bernard
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface CollectionId {
	/**
	 * The column containing the collection id.
	 */
	Column column() default @Column;

	/**
	 * Implementation for generating values.
	 *
	 * @apiNote Mutually exclusive with {@link #generator()}
	 */
	Class<? extends IdentifierGenerator> generatorImplementation() default IdentifierGenerator.class;

	/**
	 * The generator name.
	 * <p>
	 * Can specify either a built-in strategy ({@code "sequence"}, for example)
	 * or a named JPA id generator.
	 *
	 * @apiNote Mutually exclusive with {@link #generatorImplementation()}
	 */
	String generator() default "";
}
