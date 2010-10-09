/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
package org.hibernate.envers.entities;

/**
 * Type of a relation between two entities.
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
