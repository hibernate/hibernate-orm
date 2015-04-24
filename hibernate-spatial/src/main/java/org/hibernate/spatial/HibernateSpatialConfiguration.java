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
