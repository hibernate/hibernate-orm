/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.junit4;

import javax.transaction.SystemException;

import org.hibernate.engine.transaction.internal.jta.JtaStatusHelper;

import org.hibernate.testing.AfterClassOnce;
import org.hibernate.testing.jdbc.leak.ConnectionLeakUtil;
import org.hibernate.testing.jta.TestingJtaPlatformImpl;
import org.junit.After;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;

import org.jboss.logging.Logger;

/**
 * The base unit test adapter.
 *
 * @author Steve Ebersole
 */
@RunWith( CustomRunner.class )
public abstract class BaseUnitTestCase {
	private static final Logger log = Logger.getLogger( BaseUnitTestCase.class );

	private static boolean enableConnectionLeakDetection = Boolean.TRUE.toString()
			.equals( System.getenv( "HIBERNATE_CONNECTION_LEAK_DETECTION" ) );

	private ConnectionLeakUtil connectionLeakUtil;

	@Rule
	public TestRule globalTimeout = new Timeout( 30 * 60 * 1000 ); // no test should run longer than 30 minutes

	public BaseUnitTestCase() {
		if ( enableConnectionLeakDetection ) {
			connectionLeakUtil = new ConnectionLeakUtil();
		}
	}

	@AfterClassOnce
	public void assertNoLeaks() {
		if ( enableConnectionLeakDetection ) {
			connectionLeakUtil.assertNoLeaks();
		}
	}

	@After
	public void releaseTransactions() {
		if ( JtaStatusHelper.isActive( TestingJtaPlatformImpl.INSTANCE.getTransactionManager() ) ) {
			log.warn( "Cleaning up unfinished transaction" );
			try {
				TestingJtaPlatformImpl.INSTANCE.getTransactionManager().rollback();
			}
			catch (SystemException ignored) {
			}
		}
	}

	protected void sleep(long millis) {
		try {
			Thread.sleep( millis );
		}
		catch ( InterruptedException e ) {
			Thread.interrupted();
		}
	}
}
