/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.hibernate.type.descriptor.java.MutabilityPlan;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies a {@link MutabilityPlan} for a basic value mapping.
 *
 * Mutability refers to whether the internal state of a value can change.
 * For example, {@linkplain java.util.Date Date} is considered mutable because its
 * internal state can be changed using {@link java.util.Date#setTime} whereas
 * {@linkplain java.lang.String String} is considered immutable because its internal
 * state cannot be changed.  Hibernate uses this distinction when it can for internal
 * optimizations.
 *
 * Hibernate understands the inherent mutability of a large number of Java types -
 * {@linkplain java.util.Date Date}, {@linkplain java.lang.String String}, etc.
 * {@linkplain Mutability} and friends allow plugging in specific strategies.
 *
 *
 *
 * <h3>Mutability for basic-typed attributes</h3>
 * <p>
 * For basic-valued attributes, {@code @Mutability} specifies the mutability
 * of the basic value type.
 * <p>
 * This is <em>not</em> the same as saying that the attribute itself is mutable
 * or immutable. A mutable attribute may have a type whose values are immutable.
 *
 * <h3>Mutability for values belonging to collections</h3>
 * <p>
 * Even collection elements, indexes, keys, and values have mutability
 * plans, and so this annotation may be applied to a collection-valued
 * attribute:
 * <ul>
 *     <li>
 *         When applied to a {@code Map}-valued attribute, it describes
 *         the values of the map.
 *         Use {@link MapKeyMutability} to describe the keys of the map.
 *     </li>
 *     <li>
 *         When mapping an id-bag, it describes the elements of the bag.
 *         Use {@link CollectionIdMutability} to describe the
 *         {@link CollectionId}.
 *     </li>
 *     <li>
 *         For {@code List}-valued attributes, or for any other collection
 *         mapping, it describes the elements of the collection.
 *     </li>
 *     <li>
 *         When applied to an array-valued attribute, it describes the
 *         array element.
 *     </li>
 * </ul>
 * <p>
 * Again, this is not the same as saying that the collection itself is
 * mutable or immutable. One may add or remove immutable values to or
 * from a mutable collection.
 *
 * <h3>Discriminated association mappings</h3>
 * <p>
 * For discriminated association mappings ({@link Any} or {@link ManyToAny}),
 * this annotation describes the mutability of the discriminator value.
 * <p>
 * This is not likely to be useful.
 *
 * <h3>Mutability for converters</h3>
 * <p>
 * {@code @Mutability} may also be used to specify the mutability of a
 * Java type handled by a JPA {@link jakarta.persistence.AttributeConverter},
 * circumventing the need to treat it as mutable.
 * <p>
 * Either:
 * <ul>
 * <li>annotate the Java type itself, or
 * <li>annotate the {@code AttributeConverter} class.
 * </ul>
 *
 * @apiNote Except for the case of converters, this annotation is not
 *          usually applied to a {@linkplain ElementType#TYPE type}.
 *
 * @see Immutable
 * @see MutabilityPlan
 * @see <a href="package-summary.html#basic-value-mapping">Basic value type mappings</a>
 * @see org.hibernate.type
 *
 * @since 6.0
 *
 * @author Steve Ebersole
 */
@Target({METHOD, FIELD, ANNOTATION_TYPE, TYPE})
@Inherited
@Retention(RUNTIME)
public @interface Mutability {
	/**
	 * A class implementing {@link MutabilityPlan}.
	 */
	Class<? extends MutabilityPlan<?>> value();
}
