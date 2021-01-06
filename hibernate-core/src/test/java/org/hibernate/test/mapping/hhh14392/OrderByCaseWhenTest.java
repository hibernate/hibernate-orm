/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.mapping.hhh14392;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.hql.spi.id.inline.InlineIdsOrClauseBulkIdStrategy;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.test.mapping.hhh14392.entity.Club;
import org.hibernate.test.mapping.hhh14392.entity.Team;
import org.hibernate.testing.TestForIssue;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

@TestForIssue(jiraKey = "HHH-14392")
public class OrderByCaseWhenTest extends BaseEntityManagerFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Club.class,
				Team.class
		};
	}

	@Override
	protected void addConfigOptions(Map options) {
		options.put( AvailableSettings.HQL_BULK_ID_STRATEGY, InlineIdsOrClauseBulkIdStrategy.class.getName() );
	}

	@Before
	public void setUp() {
		doInJPA( this::entityManagerFactory, em ->
		{
			Club club = new Club(Integer.valueOf( 999 ), "BC Hibernate", "BCHI");

			em.persist( club );

			Integer clubId = club.getId();

			List<Team> teams = club.getTeams();

			// the insertion order is the order in which the teams shall be ordered upon retrieval (after refresh below)
			// sort logic: if "over" then by age group ascending, if under by age group descending, then by gender DESC, then by ordinal nbr

			teams.add( new Team( clubId, "O20M", Integer.valueOf( 1 ) ) );  // over 20 male, 1st team
			teams.add( new Team( clubId, "O20M", Integer.valueOf( 2 ) ) );  // over 20 male, 2nd team
			teams.add( new Team( clubId, "O20F", Integer.valueOf( 1 ) ) );  // over 20 female, 1st team
			teams.add( new Team( clubId, "O35M", Integer.valueOf( 1 ) ) );  // over 35 male, 1st team
			teams.add( new Team( clubId, "U20M", Integer.valueOf( 1 ) ) );  // under 20 male, 1st team
			teams.add( new Team( clubId, "U20F", Integer.valueOf( 1 ) ) );  // under 20 female, 1st team
			teams.add( new Team( clubId, "U18M", Integer.valueOf( 1 ) ) );  // under 18 male, 1st team
			teams.add( new Team( clubId, "U18F", Integer.valueOf( 1 ) ) );  // under 18 female, 1st team
			teams.add( new Team( clubId, "U16M", Integer.valueOf( 1 ) ) );  // under 16 male, 1st team
			teams.add( new Team( clubId, "U16F", Integer.valueOf( 1 ) ) );  // under 16 female, 1st team
			teams.add( new Team( clubId, "U14M", Integer.valueOf( 1 ) ) );  // under 14 male, 1st team
			teams.add( new Team( clubId, "U14F", Integer.valueOf( 1 ) ) );  // under 14 female, 1st team

			for ( Team team : teams )
			{
				System.out.println( "-------- Inserting team: " + team );

				em.merge( team );
			}

			System.out.println( "----------- EM flush!" );
			em.flush();

			// CALL REFRESH, otherwise the list above will be taken, which is still in order of insert/add
			System.out.println( "----------- EM refresh club!" );
			em.refresh( club );

			System.out.println( "Number of teams: " + club.getTeams().size() );
		} );
	}

	@Test
	public void testOrderByCaseWhen() {
		doInJPA( this::entityManagerFactory, em ->
		{
			System.out.println( "----------- EM find!" );

			// reload club via em.find
			Club club = em.find( Club.class, Integer.valueOf( 999 ) );

			List<Team> teams = club.getTeams();

			System.out.println( "Number of teams for club " + club + ": " + teams.size() );

			// fetch teams and print: they should be sorted "O20M" first and "U14F" last
			teams.forEach( t -> System.out.println( "Sorted team: " + t ) );

			// now do the tests
//			Team first = teams.get( 0 );
//			Team last = teams.get( teams.size() - 1 );
//
//			assertEquals( "O20M", first.getTeamTypeCode() );
//			// ...
//			// all others are somewhere inbetween, especially "O35M"
//			// ...
//			assertEquals( "U14F", last.getTeamTypeCode() );
		} );
	}
}
