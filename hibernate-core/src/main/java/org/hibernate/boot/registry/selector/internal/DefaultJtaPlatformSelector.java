/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.registry.selector.internal;

import java.util.List;
import java.util.Objects;

import org.hibernate.cfg.TransactionSettings;
import org.hibernate.engine.transaction.jta.platform.internal.AtomikosJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.JBossAppServerJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.JBossStandAloneJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.NarayanaJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.ResinJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.GlassFishJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.WebSphereLibertyJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.WeblogicJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.WildFlyStandAloneJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;

import static org.hibernate.internal.log.DeprecationLogger.DEPRECATION_LOGGER;

public class DefaultJtaPlatformSelector implements LazyServiceResolver<JtaPlatform> {

	@Override
	public Class<? extends JtaPlatform> resolve(final String name) {
		Objects.requireNonNull( name);
		if ( name.isBlank() ) {
			return null;
		}
		else if ( name.startsWith( "org." ) ) {
			return legacy( name, this );
		}
		else {
			return switch ( name ) {
				case "JBossAS" -> JBossAppServerJtaPlatform.class;
				case "JBossTS" -> {
					DEPRECATION_LOGGER.deprecatedJtaPlatformSetting(
							TransactionSettings.JTA_PLATFORM, name,
							List.of( "Narayana", "WildFlyStandadlone" ) );
					yield JBossStandAloneJtaPlatform.class;
				}
				case "Weblogic" -> WeblogicJtaPlatform.class;
				case "WebSphere", "WebSphereLiberty" -> WebSphereLibertyJtaPlatform.class;
				case "Atomikos" -> AtomikosJtaPlatform.class;
				case "Resin" -> ResinJtaPlatform.class;
				case "GlassFish", "Payara", "SunOne" -> GlassFishJtaPlatform.class;
				case "Narayana" -> NarayanaJtaPlatform.class;
				case "WildFlyStandAlone" -> WildFlyStandAloneJtaPlatform.class;
				default -> null;
			};
		}
	}


	/**
	 * Special case: we have several old fully qualified classnames which need to
	 * be remapped to their new names for backwards compatibility reasons.
	 */
	private static Class<? extends JtaPlatform> legacy(
			final String name,
			final DefaultJtaPlatformSelector defaultJtaPlatformSelector) {

		//First, let's deal with the special cases which don't follow any recognizable pattern:
		switch ( name ) {
			case "org.hibernate.service.jta.platform.internal.JBossAppServerJtaPlatform" -> {
				return JBossAppServerJtaPlatform.class;
			}
			case "org.hibernate.service.jta.platform.internal.JBossStandAloneJtaPlatform" -> {
				DEPRECATION_LOGGER.deprecatedJtaPlatformSetting(
						TransactionSettings.JTA_PLATFORM, name,
						List.of( "Narayana", "WildFlyStandadlone" ) );
				return JBossStandAloneJtaPlatform.class;
			}
			case "org.hibernate.engine.transaction.jta.platform.internal.NarayanaJtaPlatform" -> {
				return NarayanaJtaPlatform.class;
			}
			case "org.hibernate.engine.transaction.jta.platform.internal.WebSphereLibertyJtaPlatform" -> {
				return WebSphereLibertyJtaPlatform.class;
			}
			case "org.hibernate.engine.transaction.jta.platform.internal.WildFlyStandAloneJtaPlatform" -> {
				return WildFlyStandAloneJtaPlatform.class;
			}
		}

		//All other ones follow a pattern, beginning with the same prefix and ending with the same postfix,
		//if your remove those the remaining section happens to match the short name.
		final String LEGACY_PREFIX = "org.hibernate.service.jta.platform.internal.";
		final String LEGACY_POSTFIX = "JtaPlatform";

		//All these follow the same pattern, allowing us to use recursion into the main method:
		if ( name.startsWith( LEGACY_PREFIX ) && name.endsWith( LEGACY_POSTFIX ) ) {
			final String shortName = name.substring( LEGACY_PREFIX.length(), name.length() - LEGACY_POSTFIX.length() );
			return defaultJtaPlatformSelector.resolve( shortName );
		}
		return null;
	}

}
