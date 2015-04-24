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
