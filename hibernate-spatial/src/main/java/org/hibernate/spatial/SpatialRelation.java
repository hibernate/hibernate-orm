/*
 * This file is part of Hibernate Spatial, an extension to the
 * hibernate ORM solution for spatial (geographic) data.
 *
 * Copyright © 2007-2013 Geovise BVBA
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
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
