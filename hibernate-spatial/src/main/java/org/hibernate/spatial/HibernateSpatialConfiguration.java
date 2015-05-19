/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial;

import org.geolatte.geom.codec.db.oracle.ConnectionFinder;

import java.io.Serializable;

import org.jboss.logging.Logger;

/**
 * A global configuration object that is is used by
 * some Dialects during construction.
 *
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 8/16/13
 */
public class HibernateSpatialConfiguration implements Serializable {

	private static final HSMessageLogger LOG = Logger.getMessageLogger(
			HSMessageLogger.class,
			HibernateSpatialConfiguration.class.getName()
	);
	private Boolean isOgcStrict = Boolean.TRUE;
	private ConnectionFinder connectionFinder;

	/**
	 * Holds the configuration for Hibernate Spatial dialects.
	 */
	public HibernateSpatialConfiguration() {
	}

	/**
	 * Creates a Configuration for Hibernate spatial
	 *
	 * @param ogcStrict true for OGC Strict mode
	 * @param connectionFinder the fully-qualified Class name for the {@code ConnectionFinder}
	 */
	public HibernateSpatialConfiguration(Boolean ogcStrict, ConnectionFinder connectionFinder) {
		if ( ogcStrict != null ) {
			this.isOgcStrict = ogcStrict;
			LOG.debugf( "Setting OGC_STRICT mode for Oracle Spatial dialect to %s.", ogcStrict );
		}
		if ( connectionFinder != null ) {
			this.connectionFinder = connectionFinder;
			LOG.debugf(
					"Using ConnectionFinder implementation:  %s (only relevant for Oracle Spatial dialect).",
					connectionFinder.getClass().getCanonicalName()
			);
		}
	}

	public Boolean isOgcStrictMode() {
		return isOgcStrict;
	}

	public ConnectionFinder getConnectionFinder() {
		return connectionFinder;
	}

	/**
	 * Collects the property names for Hibernate Spatial configuration properties.
	 */
	public static class AvailableSettings {
		/**
		 * Determines whether or nog to use the OracleSpatial10gDialect in OGC_STRICT mode or not (values: true or false)
		 */
		public static final String OGC_STRICT = "hibernate.spatial.ogc_strict";
		/**
		 * The canonical class name to use as Oracle ConnectionFinder implementation.
		 */
		public static final String CONNECTION_FINDER = "hibernate.spatial.connection_finder";
	}


}
