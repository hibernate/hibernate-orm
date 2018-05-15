/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.readonly;

import org.hibernate.CacheMode;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;

/**
 * @author Gail Badner
 */
public abstract class AbstractReadOnlyTest extends BaseCoreFunctionalTestCase {
	@Override
	public void configure(Configuration cfg) {
		cfg.setProperty( Environment.GENERATE_STATISTICS, "true");
		cfg.setProperty( Environment.STATEMENT_BATCH_SIZE, "0" );
	}

	public Session openSession() {
		Session s = super.openSession();
		s.setCacheMode( getSessionCacheMode() );
		return s;
	}

	protected CacheMode getSessionCacheMode() {
		return CacheMode.IGNORE;
	}

	protected void clearCounts() {
		sessionFactory().getStatistics().clear();
	}

	protected void assertInsertCount(int expected) {
		int inserts = ( int ) sessionFactory().getStatistics().getEntityInsertCount();
		assertEquals( "unexpected insert count", expected, inserts );
	}

	protected void assertUpdateCount(int expected) {
		int updates = ( int ) sessionFactory().getStatistics().getEntityUpdateCount();
		assertEquals( "unexpected update counts", expected, updates );
	}

	protected void assertDeleteCount(int expected) {
		int deletes = ( int ) sessionFactory().getStatistics().getEntityDeleteCount();
		assertEquals( "unexpected delete counts", expected, deletes );
	}
}
