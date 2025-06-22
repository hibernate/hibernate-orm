/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.persistence.FetchType;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Maps a to-one cardinality association taking values over several entity types which
 * are <em>not</em> related by the usual entity inheritance, using a discriminator
 * value stored on the <em>referring</em> side of the relationship.
 * <p>
 * This is quite different to
 * {@linkplain jakarta.persistence.InheritanceType#SINGLE_TABLE discriminated inheritance}
 * where the discriminator is stored along with the referenced entity hierarchy.
 * <p>
 * For example, consider an {@code Order} entity containing {@code Payment} information,
 * where a {@code Payment} might be a {@code CashPayment} or a {@code CreditCardPayment}.
 * An {@code @Any} mapping would store the discriminator value identifying the concrete
 * type of {@code Payment} along with the state of the associated {@code Order}, instead
 * of storing it with the {@code Payment} entity itself.
 * <pre>
 * interface Payment { ... }
 *
 * &#64;Entity
 * class CashPayment { ... }
 *
 * &#64;Entity
 * class CreditCardPayment { ... }
 *
 * &#64;Entity
 * class Order {
 *     ...
 *     &#64;Any
 *     &#64;AnyKeyJavaClass(UUID.class)   //the foreign key type
 *     &#64;JoinColumn(name="payment_id") //the foreign key column
 *     &#64;Column(name="payment_type")   //the discriminator column
 *     &#64;AnyDiscriminatorValue(discriminator="CASH", entity=CashPayment.class)
 *     &#64;AnyDiscriminatorValue(discriminator="CREDIT", entity=CreditCardPayment.class)
 *     Payment payment;
 *     ...
 * }
 * </pre>
 * <p>
 * In this example, {@code Payment} is <em>not</em> be declared as an entity type, and
 * is not annotated {@link jakarta.persistence.Entity @Entity}. It might even be an
 * interface, or at most just a {@linkplain jakarta.persistence.MappedSuperclass mapped
 * superclass}, of {@code CashPayment} and {@code CreditCardPayment}. So in terms of the
 * object/relational mappings, {@code CashPayment} and {@code CreditCardPayment} would
 * <em>not</em> be considered to participate in the same entity inheritance hierarchy.
 * On the other hand, {@code CashPayment} and {@code CreditCardPayment} must have the
 * same identifier type.
 * <p>
 * It's reasonable to think of the "foreign key" in an {@code Any} mapping is really a
 * composite value made up of the foreign key and discriminator taken together. Note,
 * however, that this composite foreign key is only conceptual and cannot be declared
 * as a physical constraint on the relational database table.
 * <ul>
 *     <li>{@link AnyDiscriminator}, {@link JdbcType}, or {@link JdbcTypeCode} specifies
 *         the type of the discriminator.
 *     <li>{@link AnyDiscriminatorValue} specifies how discriminator values map to entity
 *         types.
 *     <li>{@link jakarta.persistence.Column} or {@link Formula} specifies the column or
 *         formula in which the discriminator value is stored.
 *     <li>{@link AnyKeyJavaType}, {@link AnyKeyJavaClass}, {@link AnyKeyJdbcType}, or
 *         {@link AnyKeyJdbcTypeCode} specifies the type of the foreign key.
 *     <li>{@link jakarta.persistence.JoinColumn} specifies the foreign key column.
 * </ul>
 * <p>
 * Of course, {@code Any} mappings are disfavored, except in extremely special cases,
 * since it's much more difficult to enforce referential integrity at the database
 * level.
 *
 * @see ManyToAny
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface Any {
	/**
	 * Specifies whether the value of the field or property may be lazily loaded or must
	 * be eagerly fetched:
	 * <ul>
	 * <li>{@link FetchType#EAGER EAGER} specifies that the association must be eagerly
	 *     fetched.
	 * <li>{@link FetchType#LAZY LAZY} allows the association to be fetched lazily, but
	 *     this is possible only when bytecode enhancement is used.
	 * </ul>
	 * <p>
	 * If not explicitly specified, the default is {@code EAGER}.
	 */
	FetchType fetch() default FetchType.EAGER;

	/**
	 * Whether the association is optional.
	 * <p>
	 * If the discriminator {@link jakarta.persistence.Column Column} or the
	 * {@link jakarta.persistence.JoinColumn JoinColumn} are not nullable the
	 * association is always considered non-optional, regardless of this value.
	 *
	 * @return {@code false} if the association cannot be null.
	 */
	boolean optional() default true;
}
