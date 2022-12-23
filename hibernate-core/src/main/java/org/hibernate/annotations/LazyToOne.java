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
 * Specifies the machinery used to handle lazy fetching of
 * the annotated {@link jakarta.persistence.OneToOne} or
 * {@link jakarta.persistence.ManyToOne} association.
 * This is an alternative to specifying only the JPA
 * {@link jakarta.persistence.FetchType}.
 *
 * @author Emmanuel Bernard
 *
 * @deprecated use JPA annotations to specify the
 *             {@link jakarta.persistence.FetchType}
 */
@Deprecated(since="6.2")
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface LazyToOne {
	/**
	 * A {@link LazyToOneOption} which determines how lazy
	 * fetching should be handled.
	 */
	LazyToOneOption value() default LazyToOneOption.PROXY;
}
