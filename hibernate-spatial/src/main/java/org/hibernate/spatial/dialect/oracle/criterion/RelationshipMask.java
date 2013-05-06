/*
 * This file is part of Hibernate Spatial, an extension to the
 *  hibernate ORM solution for spatial (geographic) data.
 *
 *  Copyright © 2007-2012 Geovise BVBA
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
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
