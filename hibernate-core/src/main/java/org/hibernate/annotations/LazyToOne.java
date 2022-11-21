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
 * {@link jakarta.persistence.ManyToOne} association. This
 * is an alternative to specifying only the JPA
 * {@link jakarta.persistence.FetchType}. This annotation
 * is occasionally useful, since there are observable
 * differences in semantics between:
 * <ul>
 * <li>{@linkplain LazyToOneOption#FALSE eager fetching},
 * <li>lazy fetching via interception of calls on a
 *     {@linkplain LazyToOneOption#PROXY proxy object},
 *     and
 * <li>lazy fetching via interception of field access on
 *     the {@linkplain LazyToOneOption#NO_PROXY owning side}
 *     of the association.
 * </ul>
 * By default, an unfetched lazy association is represented
 * by a <em>proxy object</em>. This approach comes with the
 * major advantage that the program may obtain the identifier
 * (foreign key) of the associated entity instance without
 * fetching it from the database. Since a proxy carries a
 * foreign key, it's even possible to set an association
 * using an unfetched proxy. On the other hand, for a
 * polymorphic association, the concrete type of the entity
 * instance represented by a proxy is unknown, and so
 * {@link org.hibernate.Hibernate#getClass(Object)} must
 * be used to obtain the concrete type, fetching the entity
 * by side effect.
 * <p>
 * With {@code LazyToOne(NO_PROXY)}, lazy fetching occurs
 * when the field holding the reference to the associated
 * entity is accessed. With this approach, it's impossible
 * to obtain the identifier of the associated entity without
 * fetching the entity from the database. On the other hand,
 * {@code instanceof} and {@link Object#getClass()} work as
 * normal, since the concrete type of the entity instance is
 * known immediately. This is usually bad tradeoff.
 *
 * @author Emmanuel Bernard
 */
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface LazyToOne {
	/**
	 * A {@link LazyToOneOption} which determines how lazy
	 * fetching should be handled.
	 */
	LazyToOneOption value() default LazyToOneOption.PROXY;
}
