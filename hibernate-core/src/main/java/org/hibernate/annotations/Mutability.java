/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
 * Used to specify a {@link MutabilityPlan} for a basic value mapping.
 * <ul>
 *     <li>
 *         When applied to a Map-valued attribute, describes the
 *         {@code Map} value. Use {@link MapKeyMutability} to
 *         describe the key instead.
 *     </li>
 *     <li>
 *         When applied to a {@code List} of array-valued attribute,
 *         describes the element.
 *     </li>
 *     <li>
 *         When mapping an id-bag, describes the collection element.
 *         Use {@link CollectionIdMutability} to describe the
 *         {@link CollectionId}.
 *     </li>
 *     <li>
 *         For other collection mappings, describes the elements.
 *     </li>
 *     <li>
 *         For discriminated association mappings ({@link Any} and
 *         {@link ManyToAny}), describes the discriminator value.
 *     </li>
 * </ul>
 *
 * Resolved as a {@link org.hibernate.resource.beans.spi.ManagedBean}.
 *
 * See <a href="package-summary.html#basic-value-mapping"/> for a
 * high-level discussion of basic value mapping.
 *
 * @apiNote Valid on {@link ElementType#TYPE} in very limited cases.
 * At the moment it is only supported on implementations of
 * {@link jakarta.persistence.AttributeConverter}.
 *
 * @see Immutable
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
