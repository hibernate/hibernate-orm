/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.strategyselectors;

import org.hibernate.boot.registry.selector.internal.DefaultJtaPlatformSelector;
import org.hibernate.engine.transaction.jta.platform.internal.AtomikosJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.JBossAppServerJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.JBossStandAloneJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.ResinJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.GlassFishJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.WebSphereLibertyJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.WeblogicJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
				ResinJtaPlatform.class,
				"Resin",
				"org.hibernate.service.jta.platform.internal.ResinJtaPlatform"
		);

		testJtaPlatformResolves(
				strategySelector,
				GlassFishJtaPlatform.class,
				"SunOne",
				"org.hibernate.service.jta.platform.internal.SunOneJtaPlatform"
		);

		testJtaPlatformResolves(
				strategySelector,
				GlassFishJtaPlatform.class,
				"GlassFish",
				"org.hibernate.service.jta.platform.internal.SunOneJtaPlatform"
		);

		testJtaPlatformResolves(
				strategySelector,
				GlassFishJtaPlatform.class,
				"Payara",
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

	}

	private static void testJtaPlatformResolves(final DefaultJtaPlatformSelector strategySelector, final Class expectedType, final String shortname, final String longname) {
		expectResolution(strategySelector, expectedType, shortname);
		expectResolution(strategySelector, expectedType, longname);
	}

	private static void expectResolution(final DefaultJtaPlatformSelector strategySelector, final Class expectedType, final String name) {
		Class<? extends JtaPlatform> aClass = strategySelector.resolve( name );
		assertNotNull( aClass );
		assertEquals( expectedType, aClass );
	}

}
