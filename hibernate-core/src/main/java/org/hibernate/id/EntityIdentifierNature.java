/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.id;

/**
 * Describes the possible natures of an entity-defined identifier.
 *
 * @author Steve Ebersole
 */
public enum EntityIdentifierNature {
	/**
	 * A simple identifier.  Resolved as a basic type and mapped to a singular, basic attribute.  Equivalent of:<ul>
	 *     <li>an {@code <id/>} mapping</li>
	 *     <li>a single {@code @Id} annotation</li>
	 * </ul>
	 */
	SIMPLE,

	/**
	 * What Hibernate used to term an "embedded composite identifier", which is not to be confused with the JPA
	 * term "embedded". Resolved as a tuple of basic type values and mapped over multiple singular attributes.
	 * More precisely, a composite identifier where there is no single attribute representing the composite value.
	 * <p>
	 * Equivalent to:
	 * <ul>
	 *     <li>
	 *         a {@code <composite-id/>} mapping <em>without</em> a specified {@code name} XML-attribute
	 *         (which would name the single identifier attribute, if it were specified), or
	 *     </li>
	 *     <li>
	 *         multiple {@code @Id} annotations.
	 *     </li>
	 * </ul>
	 * <p>
	 * May or may not have a related "lookup identifier class" as indicated by an {@code @IdClass} annotation.
	 *
	 * @see jakarta.persistence.IdClass
	 */
	NON_AGGREGATED_COMPOSITE,

	/**
	 * Composite identifier mapped to a single entity attribute by means of an actual component class used to
	 * aggregate the tuple values.
	 * Equivalent of:<ul>
	 *     <li>
	 *         a {@code <composite-id/>} mapping naming a single identifier attribute via the {@code name} XML-attribute
	 *     </li>
	 *     <li>
	 *         an {@code @EmbeddedId} annotation
	 *     </li>
	 * </ul>
	 *
	 * @see jakarta.persistence.EmbeddedId
	 */
	AGGREGATED_COMPOSITE
}
