/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.fetchprofiles.twophaseload.entity;

import org.hibernate.LazyInitializationException;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

@TestForIssue( jiraKey = "HHH-12297")
public class EntityLoadedInTwoPhaseLoadTest extends BaseCoreFunctionalTestCase {

	static final String FETCH_PROFILE_NAME = "fp1";

	public void configure(Configuration cfg) {
		cfg.setProperty( Environment.GENERATE_STATISTICS, "true" );
	}

	@Test
	public void testIfAllRelationsAreInitialized() {
		long startId = this.createSampleData();
		sessionFactory().getStatistics().clear();
		try {
			Start start = this.loadStartWithFetchProfile( startId );
			@SuppressWarnings( "unused" )
			String value = start.getVia2().getMid().getFinish().getValue();
			assertEquals( 4, sessionFactory().getStatistics().getEntityLoadCount() );
		}
		catch (LazyInitializationException e) {
			Assert.fail( "Everything should be initialized" );
		}
	}

	public Start loadStartWithFetchProfile(long startId) {
		Session s = this.openSession();
		s.enableFetchProfile( FETCH_PROFILE_NAME );
		Start start = s.get( Start.class, startId );
		s.close();
		return start;
	}

	private long createSampleData() {
		Session s = this.openSession();
		s.getTransaction().begin();

		Finish finish = new Finish( "foo" );
		Mid mid = new Mid( finish );
		Via2 via2 = new Via2( mid );
		Start start = new Start( null, via2 );

		s.persist( start );
		s.flush();
		s.getTransaction().commit();
		s.close();
		return start.getId();
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Start.class,
				Mid.class,
				Finish.class,
				Via1.class,
				Via2.class
		};
	}
}
