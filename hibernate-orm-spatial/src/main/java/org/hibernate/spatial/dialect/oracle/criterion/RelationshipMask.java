/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
