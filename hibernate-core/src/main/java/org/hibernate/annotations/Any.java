/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
 * <p>
 * In this example, {@code Payment} would <em>not</em> be declared as an entity type, and
 * would not be annotated {@link jakarta.persistence.Entity @Entity}. It might even be an
 * interface, or at most just a {@linkplain jakarta.persistence.MappedSuperclass mapped
 * superclass}, of {@code CashPayment} and {@code CreditCardPayment}. So in terms of the
 * object/relational mappings, {@code CashPayment} and {@code CreditCardPayment} would
 * <em>not</em> be considered to participate in the same entity inheritance hierarchy.
 * <p>
 * It's reasonable to think of the "foreign key" in an {@code Any} mapping is really a
 * composite value made up of the foreign key and discriminator taken together. Note,
 * however, that this composite foreign key is only conceptual and cannot be declared
 * as a physical constraint on the relational database table.
 * <ul>
 *     <li>{@link AnyDiscriminator}, {@link JdbcType}, or {@link JdbcTypeCode} specifies
 *         the type of the discriminator.
 *     <li>{@link AnyDiscriminatorValues} specifies how discriminator values map to entity
 *         types.
 *     <li>{@link jakarta.persistence.Column} or {@link Formula} specifies the column or
 *         formula in which the discriminator value is stored.
 *     <li>{@link AnyKeyJavaType}, {@link AnyKeyJavaClass}, {@link AnyKeyJdbcType}, or
 *         or {@link AnyKeyJdbcTypeCode} specifies the type of the foreign key.
 *     <li>{@link jakarta.persistence.JoinColumn} specifies the foreign key column.
 * </ul>
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
