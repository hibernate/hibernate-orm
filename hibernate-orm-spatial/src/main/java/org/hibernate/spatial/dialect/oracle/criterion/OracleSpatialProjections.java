/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
