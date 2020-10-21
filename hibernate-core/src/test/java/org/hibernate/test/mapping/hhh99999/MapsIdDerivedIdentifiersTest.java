package org.hibernate.test.mapping.hhh99999;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

import java.util.List;
import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.hql.spi.id.inline.InlineIdsOrClauseBulkIdStrategy;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.test.mapping.hhh99999.entity.Assignment;
import org.hibernate.test.mapping.hhh99999.entity.Club;
import org.hibernate.test.mapping.hhh99999.entity.Game;
import org.hibernate.test.mapping.hhh99999.entity.Player;
import org.hibernate.test.mapping.hhh99999.entity.PlayerStat;
import org.hibernate.test.mapping.hhh99999.entity.RefpoolMember;
import org.hibernate.test.mapping.hhh99999.entity.Roster;
import org.hibernate.test.mapping.hhh99999.entity.Score;
import org.hibernate.test.mapping.hhh99999.entity.Stat;
import org.hibernate.test.mapping.hhh99999.entity.Team;
import org.hibernate.test.mapping.hhh99999.entity.TeamMember;
import org.hibernate.testing.TestForIssue;
import org.junit.Before;
import org.junit.Test;

/**
 * This template demonstrates how to develop a test case for Hibernate ORM, using the Java Persistence API.
 */
@TestForIssue( jiraKey = "HHH-13812" )
public class MapsIdDerivedIdentifiersTest extends BaseEntityManagerFunctionalTestCase
{
    private static final Integer CLUB_ID = Integer.valueOf( 999 );
            
    @Override
    protected Class<?>[] getAnnotatedClasses()
    {
        return new Class<?>[] { Assignment.class,
                                Club.class,
                                Game.class,
                                Player.class,
                                PlayerStat.class,
                                RefpoolMember.class,
                                Roster.class,
                                Score.class,
                                Stat.class,
                                Team.class,
                                TeamMember.class,
                                TeamMember.class };
    }
    
    @Override
    protected void addConfigOptions( Map options )
    {
        options.put( AvailableSettings.GLOBALLY_QUOTED_IDENTIFIERS, Boolean.TRUE );
        options.put( AvailableSettings.HQL_BULK_ID_STRATEGY, InlineIdsOrClauseBulkIdStrategy.class.getName() );
    }
    
    @Before
    public void setUp()
    {
        doInJPA( this::entityManagerFactory, em ->
        {
            Club club = new Club( 1, "Boston Celtics" );
            club.setId( CLUB_ID );
            
            em.persist( club );
            
            Integer clubId = club.getId();
            
            List<Team> teams = club.getTeams();
            
            // the below order is the order in which the teams shall be ordered upon retrieval (after refresh below)
            // sort logic: if "over" then by age group ascending, if under by age group descending, then by gender DESC,
            // then by ordinal nbr
            
            teams.add( new Team( clubId, "O20M", Integer.valueOf( 1 ) ) ); // over 20 male, 1st team
            teams.add( new Team( clubId, "O20M", Integer.valueOf( 2 ) ) ); // over 20 male, 2nd team
            teams.add( new Team( clubId, "O20F", Integer.valueOf( 1 ) ) ); // over 20 female, 1st team
            teams.add( new Team( clubId, "O35M", Integer.valueOf( 1 ) ) ); // over 35 male, 1st team
            teams.add( new Team( clubId, "U20M", Integer.valueOf( 1 ) ) ); // under 20 male, 1st team
            teams.add( new Team( clubId, "U20F", Integer.valueOf( 1 ) ) ); // under 20 female, 1st team
            teams.add( new Team( clubId, "U18M", Integer.valueOf( 1 ) ) ); // under 18 male, 1st team
            teams.add( new Team( clubId, "U18F", Integer.valueOf( 1 ) ) ); // under 18 female, 1st team
            teams.add( new Team( clubId, "U16M", Integer.valueOf( 1 ) ) ); // under 16 male, 1st team
            teams.add( new Team( clubId, "U16F", Integer.valueOf( 1 ) ) ); // under 16 female, 1st team
            teams.add( new Team( clubId, "U14M", Integer.valueOf( 1 ) ) ); // under 14 male, 1st team
            teams.add( new Team( clubId, "U14F", Integer.valueOf( 1 ) ) ); // under 14 female, 1st team
            
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
            
            System.out.println( "----------- TA commit!" );
        } );
    }
    
    /**
     * See
     * https://stackoverflow.com/questions/59587201/jpa-hibernate-case-in-orderby-annotation-throws-exception-unexpected-token-ca
     * 
     * @throws Exception
     */
    @Test
    public void testCaseWhenInOrderBy() throws Exception
    {
        doInJPA( this::entityManagerFactory, em ->
        {
            System.out.println( "Starting test for HHH-99999..." );
        });
    }
}