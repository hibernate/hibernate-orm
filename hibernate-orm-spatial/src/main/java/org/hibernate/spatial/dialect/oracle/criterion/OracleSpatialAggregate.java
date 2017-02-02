/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
