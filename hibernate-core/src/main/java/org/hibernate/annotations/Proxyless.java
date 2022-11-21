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
 * Specifies that the program should not be exposed to a
 * synthetic proxy object when the annotated lazily-fetched
 * {@linkplain jakarta.persistence.OneToOne one to one} or
 * {@linkplain jakarta.persistence.ManyToOne many to one}
 * association is unfetched. This annotation has no effect
 * on eager associations.
 * <p>
 * By default, that is, when an association is <em>not</em>
 * annotated {@code Proxyless}, an unfetched lazy association
 * is represented by a <em>proxy object</em> which holds the
 * identifier (foreign key) of the associated entity instance.
 * <ul>
 * <li>The identifier property of the proxy object is set
 *     when the proxy is instantiated.
 *     The program may obtain the entity identifier value
 *     of an unfetched proxy, without triggering lazy
 *     fetching, by calling the corresponding getter method.
 *     (It's even possible to set an association to reference
 *     an unfetched proxy.)
 * <li>A delegate entity instance is lazily fetched when any
 *     other method of the proxy is called.
 * <li>The proxy does not have the same concrete type as the
 *     proxied delegate, and so
 *     {@link org.hibernate.Hibernate#getClass(Object)}
 *     must be used in place of {@link Object#getClass()},
 *     and this method fetches the entity by side-effect.
 * <li>For a polymorphic association, the concrete type of
 *     the proxied entity instance is not known until the
 *     delegate is fetched from the database, and so
 *     {@link org.hibernate.Hibernate#unproxy(Object, Class)}}
 *     must be used to perform typecasts, and
 *     {@link org.hibernate.Hibernate#getClass(Object)}
 *     must be used instead of the Java {@code instanceof}
 *     operator.
 * </ul>
 * When an association is annotated {@code Proxyless}, there
 * is no such indirection, but the associated entity instance
 * is initially in an unloaded state, with only its identifier
 * field set.
 * <ul>
 * <li>The identifier field of an unloaded entity instance is
 *     set when the unloaded instance is instantiated.
 *     The program may obtain the identifier of an unloaded
 *     entity, without triggering lazy loading, by accessing
 *     the field containing the identifier.
 * <li>The remaining non-lazy state of the entity instance is
 *     loaded lazily when any other field is accessed.
 * <li>Typecasts, the Java {@code instanceof} operator, and
 *     {@link Object#getClass()} may be used as normal.
 * <li>Bytecode enhancement is required.
 * </ul>
 * <strong>Currently, Hibernate does not support {@code
 * Proxyless} for polymorphic associations </strong>
 *
 */
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Proxyless {
}
