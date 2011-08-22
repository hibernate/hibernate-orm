/**
 * $Id: OracleSpatialProjections.java 67 2007-12-16 16:41:55Z maesenka $
 *
 * This file is part of Hibernate Spatial, an extension to the 
 * hibernate ORM solution for geographic data. 
 *  
 * Copyright © 2007 Geovise BVBA
 * Copyright © 2007 K.U. Leuven LRD, Spatial Applications Division, Belgium
 *
 * This work was partially supported by the European Commission, 
 * under the 6th Framework Programme, contract IST-2-004688-STP.
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
 *
 * For more information, visit: http://www.hibernatespatial.org/
 */
package org.hibernate.spatial.dialect.oracle.criterion;

/**
 * Factory class for SpationProjection functions *
 * 
 * @author Tom Acree
 */
public final class OracleSpatialProjections {

	private OracleSpatialProjections() {
	}

	public static OracleSpatialProjection concatLrs(String propertyName) {
		return new OracleSpatialProjection(OracleSpatialAggregate.LRS_CONCAT,
				propertyName);
	}

	public static OracleSpatialProjection centroid(String propertyName) {
		return new OracleSpatialProjection(OracleSpatialAggregate.CENTROID,
				propertyName);
	}

	public static OracleSpatialProjection concatLines(String propertyName) {
		return new OracleSpatialProjection(OracleSpatialAggregate.CONCAT_LINES,
				propertyName);
	}

	public static OracleSpatialProjection projection(int projection,
			String propertyName) {
		return new OracleSpatialProjection(projection, propertyName);
	}
}
