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


import org.hibernate.spatial.SpatialAggregate;

/**
 * Defines types of Oracle Spatial aggregate functions
 *
 * @author Karel Maesen, Geovise BVBA
 */
public interface OracleSpatialAggregate extends SpatialAggregate {

	/**
	 * LRS_CONCAT aggregate function
	 */
	public static int LRS_CONCAT = 100;

	/**
	 * CENTROID aggregate function
	 */
	public static int CENTROID = 101;

	/**
	 * CONCAT_LINES aggregate function
	 */
	public static int CONCAT_LINES = 102;

	/**
	 * UNION aggregate function
	 */
	public static int UNION = 103;

	/**
	 * CONVEXHULL aggregate function
	 */
	public static int CONVEXHULL = 104;
}
