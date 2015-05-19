/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial;

/**
 * Enumeration of types of Spatial Aggregation
 *
 * @author Karel Maesen
 */
public interface SpatialAggregate {

	/**
	 * Enum value for extent aggregation.
	 */
	public static final int EXTENT = 1;

}
