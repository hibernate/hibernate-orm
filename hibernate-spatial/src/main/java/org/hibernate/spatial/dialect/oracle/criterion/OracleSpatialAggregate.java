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
