/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.registry.selector.internal;

import java.util.Objects;

import org.hibernate.engine.transaction.jta.platform.internal.*;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;

public class DefaultJtaPlatformSelector implements LazyServiceResolver<JtaPlatform> {

	@Override
	public Class<? extends JtaPlatform> resolve(final String name) {
		Objects.requireNonNull( name);
		if ( name.isEmpty() ) {
			return null;
		}
		//Let's organize all string matches in groups by first letter:
		final char n = name.charAt( 0 );
		switch ( n ) {
			case 'B': return caseB( name );
			case 'J': return caseJ( name );
			case 'W': return caseW( name );
			case 'o': return caseLegacy( name, this );
			default: return caseOthers( name );
		}
	}

	private static Class<? extends JtaPlatform> caseB(final String name) {
		if ( "Bitronix".equals( name ) ) {
			return BitronixJtaPlatform.class;
		}
		if ( "Borland".equals( name ) ) {
			return BorlandEnterpriseServerJtaPlatform.class;
		}
		return null;
	}

	private static Class<? extends JtaPlatform> caseJ(final String name) {
		if ( "JBossAS".equals( name ) ) {
			return JBossAppServerJtaPlatform.class;
		}
		if ( "JBossTS".equals( name ) ) {
			return JBossStandAloneJtaPlatform.class;
		}
		if ( "JOnAS".equals( name ) ) {
			return JOnASJtaPlatform.class;
		}
		if ( "JOTM".equals( name ) ) {
			return JOTMJtaPlatform.class;
		}
		if ( "JRun4".equals( name ) ) {
			return JRun4JtaPlatform.class;
		}
		return null;
	}

	private static Class<? extends JtaPlatform> caseW(final String name) {
		if ( "Weblogic".equals( name ) ) {
			return WeblogicJtaPlatform.class;
		}
		if ( "WebSphereLiberty".equals( name ) ) {
			return WebSphereLibertyJtaPlatform.class;
		}
		if ( "WebSphere".equals( name ) ) {
			return WebSphereJtaPlatform.class;
		}
		if ( "WebSphereExtended".equals( name ) ) {
			return WebSphereExtendedJtaPlatform.class;
		}
		return null;
	}

	private static Class<? extends JtaPlatform> caseOthers(final String name) {
		if ( "Atomikos".equals( name ) ) {
			return AtomikosJtaPlatform.class;
		}
		if ( "OC4J".equals( name ) ) {
			return OC4JJtaPlatform.class;
		}
		if ( "Orion".equals( name ) ) {
			return OrionJtaPlatform.class;
		}
		if ( "Resin".equals( name ) ) {
			return ResinJtaPlatform.class;
		}
		if ( "SapNetWeaver".equals( name ) ) {
			return SapNetWeaverJtaPlatform.class;
		}
		if ( "SunOne".equals( name ) ) {
			return SunOneJtaPlatform.class;
		}
		if ( "TongWeb".equals( name ) ) {
			return TongWebJtaPlatform.class;
		}
		return null;
	}

	/**
	 * Special case: we have several old fully qualified classnames which need to
	 * be remapped to their new names for backwards compatibility reasons.
	 * @param name
	 * @param defaultJtaPlatformSelector
	 * @return
	 */
	private static Class<? extends JtaPlatform> caseLegacy(
			final String name,
			final DefaultJtaPlatformSelector defaultJtaPlatformSelector) {

		//First, let's deal with the special cases which don't follow any recognizable pattern:
		if ( name.equals( "org.hibernate.service.jta.platform.internal.BorlandEnterpriseServerJtaPlatform" ) ) {
			return BorlandEnterpriseServerJtaPlatform.class;
		}
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
		if ( name.equals( "org.hibernate.engine.transaction.jta.platform.internal.TongWebJtaPlatform" ) ) {
			return TongWebJtaPlatform.class;
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
