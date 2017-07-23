/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.junit4;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/**
 * @author Steve Ebersole
 */
public class DatabaseOverrideSettings {
	/**
	 * Singleton access
	 */
	public static final DatabaseOverrideSettings INSTANCE = new DatabaseOverrideSettings();

	private Map<String,Properties> overridesMap;

	private DatabaseOverrideSettings() {
	}

	public Properties getDatabaseOverrides(String db) {
		if ( overridesMap == null ) {
			overridesMap = new HashMap<>();
		}

		return overridesMap.computeIfAbsent(
				db,
				this::loadOverridesFile
		);
	}

	private Properties loadOverridesFile(String db) {
		final Properties properties = new Properties();

		final String resourceName = String.format( Locale.ROOT, "/org/hibernate/orm/test/databases/%s.properties", db );
		System.out.format( "Attempting to locate database override resource : %s", resourceName );

		final InputStream stream = getClass().getResourceAsStream( resourceName );
		try {
			if ( stream == null ) {
				throw new RuntimeException( "Could not locate database-specific setting overrides (null stream) : " + db  );
			}

			properties.load( stream );
			return properties;
		}
		catch (IOException e) {
			throw new RuntimeException( "Could not locate database-specific setting overrides : " + db, e );
		}
		finally {
			if ( stream != null ) {
				try {
					stream.close();
				}
				catch (IOException ignore) {
				}
			}
		}
	}
}
