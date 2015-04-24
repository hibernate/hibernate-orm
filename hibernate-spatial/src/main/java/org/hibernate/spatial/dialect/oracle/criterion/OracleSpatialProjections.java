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

/**
 * Factory class for SpationProjection functions *
 *
 * @author Tom Acree
 */
public final class OracleSpatialProjections {

	private OracleSpatialProjections() {
	}

	/**
	 * Applies a "CONCAT_LRS" projection to the named property.
	 *
	 * @param propertyName The name of the geometry property
	 *
	 * @return OracleSpatialProjection
	 *
	 * @see OracleSpatialProjection
	 */
	public static OracleSpatialProjection concatLrs(String propertyName) {
		return new OracleSpatialProjection(
				OracleSpatialAggregate.LRS_CONCAT,
				propertyName
		);
	}

	/**
	 * Applies a "CENTROID" projection to the named property.
	 *
	 * @param propertyName The name of the geometry property
	 *
	 * @return OracleSpatialProjection
	 *
	 * @see OracleSpatialProjection
	 */
	public static OracleSpatialProjection centroid(String propertyName) {
		return new OracleSpatialProjection(
				OracleSpatialAggregate.CENTROID,
				propertyName
		);
	}

	/**
	 * Applies a "CONCAT_LINES" projection to the named property.
	 *
	 * @param propertyName The name of the geometry property
	 *
	 * @return OracleSpatialProjection
	 *
	 * @see OracleSpatialProjection
	 */
	public static OracleSpatialProjection concatLines(String propertyName) {
		return new OracleSpatialProjection(
				OracleSpatialAggregate.CONCAT_LINES,
				propertyName
		);
	}

	/**
	 * Applies the specified {@code OracleSpatialProjection} to the named property.
	 *
	 * @param projection The projection function
	 * @param propertyName The name of the geometry property
	 *
	 * @return OracleSpatialProjection
	 *
	 * @see OracleSpatialProjection
	 */
	public static OracleSpatialProjection projection(int projection, String propertyName) {
		return new OracleSpatialProjection( projection, propertyName );
	}
}
