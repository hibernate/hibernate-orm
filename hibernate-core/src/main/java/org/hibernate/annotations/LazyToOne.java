/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specify the laziness of a {@link jakarta.persistence.OneToOne}
 * or {@link jakarta.persistence.ManyToOne}) association.
 * <p>
 * This is an alternative to specifying the JPA
 * {@link jakarta.persistence.FetchType}.
 *
 * @author Emmanuel Bernard
 */
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface LazyToOne {
	/**
	 * The laziness of the association.
	 */
	LazyToOneOption value() default LazyToOneOption.PROXY;
}
