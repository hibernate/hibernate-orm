/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.fetchprofiles.twophaseload.collection;

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
public class CollectionLoadedInTwoPhaseLoadTest extends BaseCoreFunctionalTestCase {

	// NOTE
	// there are two fetch profiles because when I use only one the relation OrgUnit.people
	// is missing in the fetch profile.
	// It is missing because of logic in FetchProfile.addFetch(). Do not understand the implementation
	// of the method now, so the workaround is to use two fetch profiles.
	static final String FETCH_PROFILE_NAME = "fp1";
	static final String FETCH_PROFILE_NAME_2 = "fp2";

	private final String OU_1 = "ou_1";
	private final String OU_2 = "ou_2";
	private final String P_1 = "p_1";
	private final String P_2 = "p_2";

	public void configure(Configuration cfg) {
		cfg.setProperty( Environment.GENERATE_STATISTICS, "true" );
	}

	@Test
	public void testIfEverythingIsLoaded() {
		createSampleData();
		sessionFactory().getStatistics().clear();
		try {
			OrgUnit ou1 = this.loadOrgUnitWithFetchProfile( OU_1 );
			Person p1 = ou1.findPerson( P_1 );
			OrgUnit ou2 = p1.findOrgUnit( OU_2 );
			Person p2 = ou2.findPerson( P_2 );
			@SuppressWarnings( "unused" )
			String email = p2.getEmail();
			assertEquals( 4, sessionFactory().getStatistics().getEntityLoadCount() );
		}
		catch (LazyInitializationException e) {
			Assert.fail( "Everything should be initialized" );
		}
	}

	public OrgUnit loadOrgUnitWithFetchProfile(String groupId) {
		Session s = this.openSession();
		s.enableFetchProfile( FETCH_PROFILE_NAME );
		s.enableFetchProfile( FETCH_PROFILE_NAME_2 );
		OrgUnit orgUnit = s.get( OrgUnit.class, groupId );
		s.close();
		return orgUnit;
	}

	private void createSampleData() {
		Session s = this.openSession();
		s.getTransaction().begin();

		OrgUnit ou1 = new OrgUnit( OU_1, "org unit one" );
		OrgUnit ou2 = new OrgUnit( OU_2, "org unit two" );
		Person p1 = new Person( P_1, "p1@coompany.com" );
		Person p2 = new Person( P_2, "p2@company.com" );

		ou1.addPerson( p1 );
		ou2.addPerson( p1 );
		ou2.addPerson( p2 );

		s.persist( ou1 );
		s.flush();
		s.getTransaction().commit();
		s.close();
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Person.class,
				OrgUnit.class
		};
	}

}
