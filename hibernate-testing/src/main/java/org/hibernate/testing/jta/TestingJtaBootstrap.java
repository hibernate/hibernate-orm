/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.jta;

import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;

/**
 * @author Steve Ebersole
 */
public final class TestingJtaBootstrap {
	public static final TestingJtaBootstrap INSTANCE = new TestingJtaBootstrap();

	@SuppressWarnings("unchecked")
	public static void prepare(Map configValues) {
		configValues.put( AvailableSettings.JTA_PLATFORM, TestingJtaPlatformImpl.INSTANCE );
		configValues.put( Environment.CONNECTION_PROVIDER, JtaAwareConnectionProviderImpl.class.getName() );
		configValues.put( "javax.persistence.transactionType", "JTA" );
	}

	private TestingJtaBootstrap() {
	}
}
