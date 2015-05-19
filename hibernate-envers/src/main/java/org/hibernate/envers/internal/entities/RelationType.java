/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.entities;


/**
 * Type of a relation between two entities.
 *
 * @author Adam Warski (adam at warski dot org)
 */
public enum RelationType {
	/**
	 * A single-reference-valued relation. The entity owns the relation.
	 */
	TO_ONE,
	/**
	 * A single-reference-valued relation. The entity doesn't own the relation. It is directly mapped in the related
	 * entity.
	 */
	TO_ONE_NOT_OWNING,
	/**
	 * A collection-of-references-valued relation. The entity doesn't own the relation. It is directly mapped in the
	 * related entity.
	 */
	TO_MANY_NOT_OWNING,
	/**
	 * A collection-of-references-valued relation. The entity owns the relation. It is mapped using a middle table.
	 */
	TO_MANY_MIDDLE,
	/**
	 * A collection-of-references-valued relation. The entity doesn't own the relation. It is mapped using a middle
	 * table.
	 */
	TO_MANY_MIDDLE_NOT_OWNING
}
