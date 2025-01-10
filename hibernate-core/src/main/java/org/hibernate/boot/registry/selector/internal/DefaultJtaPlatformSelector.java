/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.registry.selector.internal;

import java.util.Objects;

import org.hibernate.engine.transaction.jta.platform.internal.AtomikosJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.JBossAppServerJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.JBossStandAloneJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.JOTMJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.JOnASJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.JRun4JtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.OC4JJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.OrionJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.ResinJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.SunOneJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.WebSphereExtendedJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.WebSphereJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.WebSphereLibertyJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.WeblogicJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;

public class DefaultJtaPlatformSelector implements LazyServiceResolver<JtaPlatform> {

	@Override
	public Class<? extends JtaPlatform> resolve(final String name) {
		Objects.requireNonNull( name);
		if ( name.isBlank() ) {
			return null;
		}
		//Let's organize all string matches in groups by first letter:
		return switch ( name.charAt( 0 ) ) {
			case 'J'-> caseJ( name );
			case 'W'-> caseW( name );
			case 'o'-> caseLegacy( name, this );
			default -> caseOthers( name );
		};
	}

	private static Class<? extends JtaPlatform> caseJ(final String name) {
		return switch ( name ) {
			case "JBossAS" -> JBossAppServerJtaPlatform.class;
			case "JBossTS" -> JBossStandAloneJtaPlatform.class;
			case "JOnAS" -> JOnASJtaPlatform.class;
			case "JOTM" -> JOTMJtaPlatform.class;
			case "JRun4" -> JRun4JtaPlatform.class;
			case null, default -> null;
		};
	}

	private static Class<? extends JtaPlatform> caseW(final String name) {
		return switch ( name ) {
			case "Weblogic" -> WeblogicJtaPlatform.class;
			case "WebSphereLiberty" -> WebSphereLibertyJtaPlatform.class;
			case "WebSphere" -> WebSphereJtaPlatform.class;
			case "WebSphereExtended" -> WebSphereExtendedJtaPlatform.class;
			case null, default -> null;
		};
	}

	private static Class<? extends JtaPlatform> caseOthers(final String name) {
		return switch ( name ) {
			case "Atomikos" -> AtomikosJtaPlatform.class;
			case "OC4J" -> OC4JJtaPlatform.class;
			case "Orion" -> OrionJtaPlatform.class;
			case "Resin" -> ResinJtaPlatform.class;
			case "SunOne" -> SunOneJtaPlatform.class;
			case null, default -> null;
		};
	}

	/**
	 * Special case: we have several old fully qualified classnames which need to
	 * be remapped to their new names for backwards compatibility reasons.
	 */
	private static Class<? extends JtaPlatform> caseLegacy(
			final String name,
			final DefaultJtaPlatformSelector defaultJtaPlatformSelector) {

		//First, let's deal with the special cases which don't follow any recognizable pattern:
		if ( name.equals( "org.hibernate.service.jta.platform.internal.JBossAppServerJtaPlatform" ) ) {
			return JBossAppServerJtaPlatform.class;
		}
		if ( name.equals( "org.hibernate.service.jta.platform.internal.JBossStandAloneJtaPlatform" ) ) {
			return JBossStandAloneJtaPlatform.class;
		}
		//This one shouldn't be necessary as it matches the implementation FQN, but let's translate the existing
		//code faithfully.
		if ( name.equals( "org.hibernate.engine.transaction.jta.platform.internal.WebSphereLibertyJtaPlatform" ) ) {
			return WebSphereLibertyJtaPlatform.class;
		}

		//All other ones follow a pattern, beginning with the same prefix and ending with the same postfix,
		//if your remove those the remaining section happens to match the short name.
		final String LEGACY_PREFIX = "org.hibernate.service.jta.platform.internal.";
		final String LEGACY_POSTFIX = "JtaPlatform";

		//All these follow the same pattern, allowing us to use recursion into the main method:
		if ( name.startsWith( LEGACY_PREFIX ) && name.endsWith( LEGACY_POSTFIX ) ) {
			final String cleanName = name.substring( LEGACY_PREFIX.length(), name.length() - LEGACY_POSTFIX.length() );
			return defaultJtaPlatformSelector.resolve( cleanName );
		}
		return null;
	}

}
