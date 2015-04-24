/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
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
package org.hibernate.spatial.dialect.oracle.criterion;

/**
 * Enumerates the types of spatial relationship masks supported by Oracle Spatial.
 */
public enum RelationshipMask {
	/**
	 * The "touch" relationship
	 */
	TOUCH,

	/**
	 * The "overlapbydisjoint" relationship
	 */
	OVERLAPBYDISJOINT,

	/**
	 * The "overlapbyintersect" relationship
	 */
	OVERLAPBYINTERSECT,

	/**
	 * The "equal" relationship
	 */
	EQUAL,

	/**
	 * The "inside" relationship
	 */
	INSIDE,

	/**
	 * The "coveredby" relationship
	 */
	COVEREDBY,

	/**
	 * The "contains" relationship
	 */
	CONTAINS,

	/**
	 * The "covers" relationship
	 */
	COVERS,

	/**
	 * The "anyinteract" relationship
	 */
	ANYINTERACT,

	/**
	 * The "on" relationship
	 */
	ON;

	/**
	 * Combines the passed "{@code RelationshipMask}s
	 *
	 * @param masks The array of masks to combine
	 * @return A {@code String} representing the combined relationship mask
	 */
	public static String booleanCombination(RelationshipMask[] masks) {
		String strMask = null;
		for ( RelationshipMask relationshipMask : masks ) {
			if ( strMask == null ) {
				strMask = relationshipMask.toString();
			}
			else {
				strMask += "+" + relationshipMask.toString();
			}
		}
		return strMask;
	}
}
