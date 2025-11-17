/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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

	public static final String ORACLE_OGC_STRICT = "hibernate.spatial.oracle_ogc_strict";

	/**
	 * SRID to use for the DB2 Spatial Dialects.
	 */
	public static final String DB2_DEFAULT_SRID = "hibernate.spatial.db2.srid";

	private HibernateSpatialConfigurationSettings() {
		//prevent this object from being instantiated
	}

}
