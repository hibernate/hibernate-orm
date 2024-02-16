/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.util;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;

import org.hibernate.testing.jdbc.SharedDriverManagerConnectionProviderImpl;

public class ServiceRegistryUtil {

	public static StandardServiceRegistryBuilder serviceRegistryBuilder() {
		return applySettings( new StandardServiceRegistryBuilder() );
	}

	public static StandardServiceRegistryBuilder serviceRegistryBuilder(BootstrapServiceRegistry bsr) {
		return applySettings( new StandardServiceRegistryBuilder( bsr ) );
	}

	public static StandardServiceRegistryImpl serviceRegistry() {
		return (StandardServiceRegistryImpl) serviceRegistryBuilder().build();
	}

	public static StandardServiceRegistryBuilder applySettings(StandardServiceRegistryBuilder builder) {
		if ( !Environment.getProperties().containsKey( AvailableSettings.CONNECTION_PROVIDER )
				&& !builder.getSettings().containsKey( AvailableSettings.CONNECTION_PROVIDER ) ) {
			builder.applySetting(
					AvailableSettings.CONNECTION_PROVIDER,
					SharedDriverManagerConnectionProviderImpl.getInstance()
			);
			builder.applySetting(
					AvailableSettings.CONNECTION_PROVIDER_DISABLES_AUTOCOMMIT,
					Boolean.TRUE
			);
		}
		return builder;
	}

	public static void applySettings(Map<?, ?> properties) {
		if ( !properties.containsKey( AvailableSettings.CONNECTION_PROVIDER ) ) {
			//noinspection unchecked
			( (Map<Object, Object>) properties ).put(
					AvailableSettings.CONNECTION_PROVIDER,
					SharedDriverManagerConnectionProviderImpl.getInstance()
			);
			( (Map<Object, Object>) properties ).put(
					AvailableSettings.CONNECTION_PROVIDER_DISABLES_AUTOCOMMIT,
					Boolean.TRUE
			);
		}
	}

	public static Map<String, Object> createBaseSettings() {
		final Map<String, Object> settings = new HashMap<>();
		ServiceRegistryUtil.applySettings( settings );
		return settings;
	}
}
