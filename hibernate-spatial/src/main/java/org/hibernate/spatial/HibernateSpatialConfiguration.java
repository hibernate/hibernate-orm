package org.hibernate.spatial;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A global configuration object that is is used by
 * some Dialects during construction.
 *
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 8/16/13
 */
public class HibernateSpatialConfiguration {


	final static protected Map<String, Object> config = new ConcurrentHashMap<String, Object>( 2 );
	static private Log LOG = LogFactory.make();

	static public void addToConfigMap(Map<String, Object> map) {
		config.putAll( map );
	}

	static public boolean isOgcStrictMode() {
		Boolean mode = getAs( AvailableSettings.OGC_STRICT, Boolean.class );
		return mode == null ? true : mode;
	}

	static public <T> T getAs(String key, Class<T> returnClass) {
		Object val = config.get( key );
		if ( val == null ) {
			return null;
		}
		if ( returnClass.isInstance( val ) ) {
			return (T) val;
		}
		throw new RuntimeException(
				String.format(
						"Configuration error: unexpected type for feature %s (expected %s)",
						key.toString(), returnClass.getName()
				)
		);
	}

	public static class AvailableSettings {

		public static final String OGC_STRICT = "hibernate.spatial.ogc_strict";

	}


}
