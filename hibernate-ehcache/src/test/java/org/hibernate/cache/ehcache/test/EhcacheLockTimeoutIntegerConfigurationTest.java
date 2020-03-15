/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.ehcache.test;

import java.util.Map;
import org.hibernate.cache.ehcache.ConfigSettings;

public class EhcacheLockTimeoutIntegerConfigurationTest
		extends EhcacheLockTimeoutStringConfigurationTest {

	@Override
	protected void addSettings(Map settings) {
		super.addSettings( settings );
		settings.put( ConfigSettings.EHCACHE_CONFIGURATION_CACHE_LOCK_TIMEOUT, 1000 );
	}

}
