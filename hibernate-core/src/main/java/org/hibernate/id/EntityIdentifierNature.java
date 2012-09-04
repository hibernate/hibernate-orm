/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.id;

/**
 * Describes the nature of the entity-defined identifier.
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
