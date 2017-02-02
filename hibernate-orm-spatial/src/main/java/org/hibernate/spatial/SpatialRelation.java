/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial;

/**
 * Enumerates the supported spatial relations.
 *
 * <p>Most of these relations are defined in "OpenGIS Simple Features Specification for SQL, rev. 1.1  (OGC 99-049),
 * section 2.1.13.3. "</p>
 *
 * @author Karel Maesen
 */
public interface SpatialRelation {

	/**
	 * The geometries are spatially equal to each other.
	 */
	public static int EQUALS = 0;

	/**
	 * The geometries are spatially dijoint
	 */
	public static int DISJOINT = 1;

	/**
	 * The geometries touch
	 */
	public static int TOUCHES = 2;

	/**
	 * The geometries cross
	 */
	public static int CROSSES = 3;

	/**
	 * The first geometry is spatially within the second
	 */
	public static int WITHIN = 4;

	/**
	 * The geometries spatially overlap
	 */
	public static int OVERLAPS = 5;

	/**
	 * The first geometry spatially contains the second
	 */
	public static int CONTAINS = 6;

	/**
	 * The first geometry intersects the second
	 */
	public static int INTERSECTS = 7;

	/**
	 * The bounding box of the first geometry intersects the bounding box of the second
	 *
	 * <p>This relation is not defined in OGC 99-049, it corresponds to the Postgis '&&' operator.</p>
	 */
	public static int FILTER = 8;

}
