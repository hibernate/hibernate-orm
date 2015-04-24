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
