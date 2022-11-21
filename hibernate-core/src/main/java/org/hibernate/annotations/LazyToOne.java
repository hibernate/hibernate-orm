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
 * by a <em>proxy object</em> which holds the identifier
 * (foreign key) of the associated entity instance.
 * It's possible to obtain the identifier from an unfetched
 * proxy, without fetching the entity from the database, by
 * calling the corresponding getter method. (It's even
 * possible to set an association to reference an unfetched
 * proxy.) Lazy fetching occurs when any other method of the
 * proxy is called. Once fetched, the proxy delegates all
 * method invocations to the fetched entity instance.
 * For a polymorphic association, the concrete type of the
 * entity instance represented by a proxy is unknown, and
 * so {@link org.hibernate.Hibernate#getClass(Object)} must
 * be used to obtain the concrete type, fetching the entity
 * by side effect. Similarly, typecasts must be performed
 * using {@link org.hibernate.Hibernate#unproxy(Object, Class)}.
 * <p>
 * With {@code LazyToOne(NO_PROXY)}, an associated entity
 * instance begins in an unloaded state, with only its
 * identifier field set. Thus, it's possible to obtain the
 * identifier if an unloaded entity, without triggering
 * lazy loading. Typecasts, {@code instanceof}, and
 * {@link Object#getClass()} work as normal. But this
 * option is only available when bytecode enhancement is
 * used.
 * <p>
 * <strong>Currently, Hibernate does not support
 * {@code LazyToOne(NO_PROXY)} for polymorphic associations,
 * and instead falls back to using a proxy!</strong>
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
