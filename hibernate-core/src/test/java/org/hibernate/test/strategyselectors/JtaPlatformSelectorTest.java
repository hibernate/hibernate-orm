/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.strategyselectors;

import org.hibernate.boot.registry.selector.internal.DefaultJtaPlatformSelector;
import org.hibernate.engine.transaction.jta.platform.internal.AtomikosJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.BitronixJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.BorlandEnterpriseServerJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.JBossAppServerJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.JBossStandAloneJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.JOTMJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.JOnASJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.JRun4JtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.OC4JJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.OrionJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.ResinJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.SapNetWeaverJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.SunOneJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.WebSphereExtendedJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.WebSphereJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.WebSphereLibertyJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.WeblogicJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;

import org.junit.Assert;
import org.junit.Test;

public class JtaPlatformSelectorTest {

	private final DefaultJtaPlatformSelector strategySelector = new DefaultJtaPlatformSelector();

	@Test
	public void verifyAllJtaPlatformResolve() {

		// N.B. it might seem that there is some reduncancy, but it's not the case: the FQNs listed
		// here are legacy full classnames which are being re-mapped to the right class for backwards
		// compatibility reasons.

		testJtaPlatformResolves(
				strategySelector,
				AtomikosJtaPlatform.class,
				"Atomikos",
				"org.hibernate.service.jta.platform.internal.AtomikosJtaPlatform"
		);

		testJtaPlatformResolves(
				strategySelector,
				BorlandEnterpriseServerJtaPlatform.class,
				"Borland",
				"org.hibernate.service.jta.platform.internal.BorlandEnterpriseServerJtaPlatform"
		);

		testJtaPlatformResolves(
				strategySelector,
				BitronixJtaPlatform.class,
				"Bitronix",
				"org.hibernate.service.jta.platform.internal.BitronixJtaPlatform"
		);

		testJtaPlatformResolves(
				strategySelector,
				JBossAppServerJtaPlatform.class,
				"JBossAS",
				"org.hibernate.service.jta.platform.internal.JBossAppServerJtaPlatform"
		);

		testJtaPlatformResolves(
				strategySelector,
				JBossStandAloneJtaPlatform.class,
				"JBossTS",
				"org.hibernate.service.jta.platform.internal.JBossStandAloneJtaPlatform"
		);

		testJtaPlatformResolves(
				strategySelector,
				JOnASJtaPlatform.class,
				"JOnAS",
				"org.hibernate.service.jta.platform.internal.JOnASJtaPlatform"
		);

		testJtaPlatformResolves(
				strategySelector,
				JOTMJtaPlatform.class,
				"JOTM",
				"org.hibernate.service.jta.platform.internal.JOTMJtaPlatform"
		);

		testJtaPlatformResolves(
				strategySelector,
				JRun4JtaPlatform.class,
				"JRun4",
				"org.hibernate.service.jta.platform.internal.JRun4JtaPlatform"
		);

		testJtaPlatformResolves(
				strategySelector,
				OC4JJtaPlatform.class,
				"OC4J",
				"org.hibernate.service.jta.platform.internal.OC4JJtaPlatform"
		);

		testJtaPlatformResolves(
				strategySelector,
				OrionJtaPlatform.class,
				"Orion",
				"org.hibernate.service.jta.platform.internal.OrionJtaPlatform"
		);

		testJtaPlatformResolves(
				strategySelector,
				ResinJtaPlatform.class,
				"Resin",
				"org.hibernate.service.jta.platform.internal.ResinJtaPlatform"
		);

		testJtaPlatformResolves(
				strategySelector,
				SapNetWeaverJtaPlatform.class,
				"SapNetWeaver",
				"org.hibernate.service.jta.platform.internal.SapNetWeaverJtaPlatform"
		);

		testJtaPlatformResolves(
				strategySelector,
				SunOneJtaPlatform.class,
				"SunOne",
				"org.hibernate.service.jta.platform.internal.SunOneJtaPlatform"
		);

		testJtaPlatformResolves(
				strategySelector,
				WeblogicJtaPlatform.class,
				"Weblogic",
				"org.hibernate.service.jta.platform.internal.WeblogicJtaPlatform"
		);

		testJtaPlatformResolves(
				strategySelector,
				WebSphereLibertyJtaPlatform.class,
				"WebSphereLiberty",
				"org.hibernate.engine.transaction.jta.platform.internal.WebSphereLibertyJtaPlatform"
		);

		testJtaPlatformResolves(
				strategySelector,
				WebSphereJtaPlatform.class,
				"WebSphere",
				"org.hibernate.service.jta.platform.internal.WebSphereJtaPlatform"
		);

		testJtaPlatformResolves(
				strategySelector,
				WebSphereExtendedJtaPlatform.class,
				"WebSphereExtended",
				"org.hibernate.service.jta.platform.internal.WebSphereExtendedJtaPlatform"
		);
	}

	private static void testJtaPlatformResolves(final DefaultJtaPlatformSelector strategySelector, final Class expectedType, final String shortname, final String longname) {
		expectResolution(strategySelector, expectedType, shortname);
		expectResolution(strategySelector, expectedType, longname);
	}

	private static void expectResolution(final DefaultJtaPlatformSelector strategySelector, final Class expectedType, final String name) {
		Class<? extends JtaPlatform> aClass = strategySelector.resolve( name );
		Assert.assertNotNull( aClass );
		Assert.assertEquals( expectedType, aClass );
	}

}
