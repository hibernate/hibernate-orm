/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.internal;

import java.util.Map;

import org.hibernate.Internal;
import org.hibernate.cfg.JdbcSettings;

/// Common connection-setting normalization for native and JPA bootstrap.
/// Entry-point adapters select effective values according to their own source precedence.
/// Registry normalization supplies Hibernate aliases using explicit Hibernate settings first,
/// then Jakarta settings, then legacy javax settings, without adding JPA defaults.
///
/// @since 8.0
/// @author Steve Ebersole
@Internal
public final class BootstrapSettingsNormalizer {
	private BootstrapSettingsNormalizer() {
	}

	/// Normalizes the registry-owned settings map in place after entry-point assembly.
	/// Within each namespace, non-JTA datasource settings take precedence over JTA settings.
	public static Map<String, Object> normalize(Map<String, Object> settings) {
		for ( var setting : ConnectionSetting.values() ) {
			setting.applyDefault( settings, setting.jakartaName );
		}
		for ( var setting : ConnectionSetting.values() ) {
			setting.applyDefault( settings, setting.legacyName );
		}
		return settings;
	}

	/// Alias families shared by registry normalization and JPA settings assembly.
	/// Mirroring a resolved value is separate from selecting it: only the JPA adapter
	/// requests legacy output aliases, after resolving its source-specific precedence.
	@SuppressWarnings("deprecation")
	public enum ConnectionSetting {
		URL( JdbcSettings.URL, JdbcSettings.JAKARTA_JDBC_URL, JdbcSettings.JPA_JDBC_URL ),
		DRIVER( JdbcSettings.DRIVER, JdbcSettings.JAKARTA_JDBC_DRIVER, JdbcSettings.JPA_JDBC_DRIVER ),
		USER( JdbcSettings.USER, JdbcSettings.JAKARTA_JDBC_USER, JdbcSettings.JPA_JDBC_USER ),
		PASSWORD( JdbcSettings.PASS, JdbcSettings.JAKARTA_JDBC_PASSWORD, JdbcSettings.JPA_JDBC_PASSWORD ),
		NON_JTA_DATASOURCE( JdbcSettings.DATASOURCE, JdbcSettings.JAKARTA_NON_JTA_DATASOURCE, JdbcSettings.JPA_NON_JTA_DATASOURCE ),
		JTA_DATASOURCE( JdbcSettings.DATASOURCE, JdbcSettings.JAKARTA_JTA_DATASOURCE, JdbcSettings.JPA_JTA_DATASOURCE );

		private final String hibernateName;
		private final String jakartaName;
		private final String legacyName;

		ConnectionSetting(String hibernateName, String jakartaName, String legacyName) {
			this.hibernateName = hibernateName;
			this.jakartaName = jakartaName;
			this.legacyName = legacyName;
		}

		private void applyDefault(Map<String, Object> settings, String alias) {
			final var value = settings.get( alias );
			if ( value != null ) {
				settings.putIfAbsent( hibernateName, value );
			}
		}

		/// Publishes an effective JPA value under all supported aliases.
		public void applyResolvedValue(Map<String, Object> settings, Object value) {
			settings.put( hibernateName, value );
			applyPersistenceAliases( settings, value );
		}

		/// Publishes the persistence aliases while a JPA adapter removes conflicting settings.
		public void applyPersistenceAliases(Map<String, Object> settings, Object value) {
			settings.put( jakartaName, value );
			settings.put( legacyName, value );
		}

		/// Removes all aliases when the adapter determines that this setting is absent.
		public void remove(Map<String, Object> settings) {
			settings.remove( hibernateName );
			settings.remove( jakartaName );
			settings.remove( legacyName );
		}
	}
}
