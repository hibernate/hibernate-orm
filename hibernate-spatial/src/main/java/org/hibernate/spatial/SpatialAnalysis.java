/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial;

/**
 * The spatial analysis functions defined in the OGC SFS specification.
 *
 * @author Karel Maesen
 */
public interface SpatialAnalysis {

	/**
	 * The distance function
	 */
	public static int DISTANCE = 1;

	/**
	 * The buffer function
	 */
	public static int BUFFER = 2;

	/**
	 * The convexhull function
	 */
	public static int CONVEXHULL = 3;

	/**
	 * The intersection function
	 */
	public static int INTERSECTION = 4;

	/**
	 * The union function
	 */
	public static int UNION = 5;

	/**
	 * The difference function
	 */
	public static int DIFFERENCE = 6;

	/**
	 * The symmetric difference function
	 */
	public static int SYMDIFFERENCE = 7;

}
