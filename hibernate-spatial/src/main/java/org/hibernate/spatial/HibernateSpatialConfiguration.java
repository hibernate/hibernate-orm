package org.hibernate.spatial;

import java.io.Serializable;

import org.hibernate.spatial.dialect.oracle.ConnectionFinder;

/**
 * A global configuration object that is is used by
 * some Dialects during construction.
 *
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 8/16/13
 */
public class HibernateSpatialConfiguration implements Serializable {

	private static final Log LOG = LogFactory.make();
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
			LOG.info( String.format( "Setting OGC_STRICT mode for Oracle Spatial dialect to %s.", ogcStrict ) );
		}
		if ( connectionFinder != null ) {
			this.connectionFinder = connectionFinder;
			LOG.info(
					String.format(
							"Using ConnectionFinder implementation:  %s (only relevant for Oracle Spatial dialect).",
							connectionFinder.getClass().getCanonicalName()
					)
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
