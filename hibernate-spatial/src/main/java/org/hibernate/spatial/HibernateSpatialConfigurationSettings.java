/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial;

import java.io.Serializable;

/**
 * Hibernate Spatial specific configuration settings.
 *
 * @author Karel Maesen, Geovise BVBA
 * creation-date: 8/16/13
 */
public class HibernateSpatialConfigurationSettings implements Serializable {

	/**
	 * The canonical class name of the Oracle ConnectionFinder implementation that will be used by the
	 * Oracle spatial dialects
	 */

	public static final String CONNECTION_FINDER = "hibernate.spatial.connection_finder";
	/**
	 * SRID to use for the DB2 Spatial Dialects.
	 */
	public static final String DB2_DEFAULT_SRID = "hibernate.spatial.db2.srid";

	private HibernateSpatialConfigurationSettings() {
		//prevent this object from being instantiated
	}


}
