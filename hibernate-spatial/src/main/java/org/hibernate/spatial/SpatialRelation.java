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
package org.hibernate.spatial;

/**
 * These spatial relations are all defined in "OpenGIS Simple Feature
 * Specification for SQL, Rev. 1.1" of the Open Geospatial Consortium (OGC).
 *
 * @author Karel Maesen
 */
public interface SpatialRelation {

	public static int EQUALS = 0;

	public static int DISJOINT = 1;

	public static int TOUCHES = 2;

	public static int CROSSES = 3;

	public static int WITHIN = 4;

	public static int OVERLAPS = 5;

	public static int CONTAINS = 6;

	public static int INTERSECTS = 7;

	@Deprecated
	public static int FILTER = 8;

}
