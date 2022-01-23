/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.persistence.Column;
import jakarta.persistence.FetchType;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Maps a discriminated to-one style association pointing to one of several entity types
 * depending on a local discriminator, as opposed to discriminated inheritance where the
 * discriminator is kept as part of the entity hierarchy (see {@link jakarta.persistence.Inheritance}
 * and {@link jakarta.persistence.InheritanceType#SINGLE_TABLE} for details about discriminated
 * inheritance mappings).
 * <p/>
 * For example, if you consider an {@code Order} entity containing {@code Payment} information
 * where {@code Payment} might be of type {@code CashPayment} or {@code CreditCardPayment},
 * the {@code @Any} approach would be to keep that discriminator and matching value on the
 * {@code Order} itself instead as part of the {@code Payment} class. Thought of another way,
 * the "foreign key" is really made up of the value and discriminator.  Note however that this
 * composite foreign-key is a conceptual and cannot be phsical.
 * <ul>
 *     <li>Use {@link Column} or {@link Formula} to define the "column" to which the
 *         discriminator is mapped.
 *     <li>Use {@link jakarta.persistence.JoinColumn} to describe the key column
 *     <li>Use {@link AnyDiscriminator}, {@link JdbcType} or {@link JdbcTypeCode} to
 *         describe the mapping for the discriminator
 *     <li>Use {@link AnyKeyJavaType}, {@link AnyKeyJavaClass}, {@link AnyKeyJdbcType}
 *         or {@link AnyKeyJdbcTypeCode} to describe the mapping for the key
 *     <li>Use {@link AnyDiscriminatorValues} to specify how discriminator values map
 *         to entity types
 * </ul>
 * @see ManyToAny
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface Any {
	/**
	 * Defines whether the value of the field or property should be lazily loaded or must be
	 * eagerly fetched. The EAGER strategy is a requirement on the persistence provider runtime
	 * that the value must be eagerly fetched. The LAZY strategy is applied when bytecode
	 * enhancement is used. If not specified, defaults to EAGER.
	 */
	FetchType fetch() default FetchType.EAGER;

	/**
	 * Whether the association is optional.
	 *
	 * If set to false then a non-null relationship must always exist.
	 */
	boolean optional() default true;
}
