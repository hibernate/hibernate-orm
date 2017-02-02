/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
	 * term embedded.  Resolved as a tuple of basic type values and mapped over multiple singular attributes.
	 * Specifically a composite identifier where there is no single attribute representing the composite value.
	 * Equivalent of:<ul>
	 *     <li>
	 *         a {@code <composite-id/>} mapping without a specified {@code name} XML-attribute (which would name
	 *         the single identifier attribute
	 *     </li>
	 *     <li>
	 *         multiple {@code @Id} annotations
	 *     </li>
	 * </ul>
	 *
	 * NOTE : May or may not have a related "lookup identifier class" as indicated by a {@code @IdClass} annotation.
	 *
	 * @see javax.persistence.IdClass
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
	 * @see javax.persistence.EmbeddedId
	 */
	AGGREGATED_COMPOSITE
}
