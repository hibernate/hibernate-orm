package org.hibernate.query.hhh13812;

import static org.junit.Assert.assertEquals;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.hibernate.testing.TestForIssue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * This template demonstrates how to develop a test case for Hibernate ORM, using the Java Persistence API.
 */
public class CaseWhenInOrderByTestCase
{
    private EntityManagerFactory emf;
    
    @Before
//    @BeforeEach
    public void init()
    {
        emf = Persistence.createEntityManagerFactory( "???" );
    }
    
    @After
//    @AfterEach
    public void destroy()
    {
        emf.close();
    }

    /**
     * See https://stackoverflow.com/questions/59587201/jpa-hibernate-case-in-orderby-annotation-throws-exception-unexpected-token-ca
     * 
     * @throws Exception
     */
    @Test
    @TestForIssue( jiraKey = "HHH-13812" )
    public void testCaseWhenInOrderBy() throws Exception
    {
        EntityManager em = emf.createEntityManager();
        
        // TA begin
        em.getTransaction().begin();
        
        SimpleClub club = new SimpleClub( "BC Testing", "TEST" );
        club.setId( Integer.valueOf( 999 ) );
        
        em.persist( club );
        
        Integer clubId = club.getId();
        
        List<SimpleTeam> teams = club.getTeams();
        
        // the insertion order is the order in which the teams shall be ordered upon retrieval (after refresh below)
        // sort logic: if "over" then by age group ascending, if under by age group descending, then by gender DESC, then by ordinal nbr
        
        teams.add( new SimpleTeam( clubId, "O20M", Integer.valueOf( 1 ) ) );  // over 20 male, 1st team
        teams.add( new SimpleTeam( clubId, "O20M", Integer.valueOf( 2 ) ) );  // over 20 male, 2nd team
        teams.add( new SimpleTeam( clubId, "O20F", Integer.valueOf( 1 ) ) );  // over 20 female, 1st team
        teams.add( new SimpleTeam( clubId, "O35M", Integer.valueOf( 1 ) ) );  // over 35 male, 1st team
        teams.add( new SimpleTeam( clubId, "U20M", Integer.valueOf( 1 ) ) );  // under 20 male, 1st team
        teams.add( new SimpleTeam( clubId, "U20F", Integer.valueOf( 1 ) ) );  // under 20 female, 1st team
        teams.add( new SimpleTeam( clubId, "U18M", Integer.valueOf( 1 ) ) );  // under 18 male, 1st team
        teams.add( new SimpleTeam( clubId, "U18F", Integer.valueOf( 1 ) ) );  // under 18 female, 1st team
        teams.add( new SimpleTeam( clubId, "U16M", Integer.valueOf( 1 ) ) );  // under 16 male, 1st team
        teams.add( new SimpleTeam( clubId, "U16F", Integer.valueOf( 1 ) ) );  // under 16 female, 1st team
        teams.add( new SimpleTeam( clubId, "U14M", Integer.valueOf( 1 ) ) );  // under 14 male, 1st team
        teams.add( new SimpleTeam( clubId, "U14F", Integer.valueOf( 1 ) ) );  // under 14 female, 1st team
        
        for ( SimpleTeam team : teams )
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
        em.getTransaction().commit();
        // TA end
        
        System.out.println( "----------- EM find!" );

        // reload club via em.find
        club = em.find( SimpleClub.class, Integer.valueOf( 999 ) );
        
        teams = club.getTeams();
        
        System.out.println( "Number of teams for club " + club + ": " + teams.size() );
        
        // fetch teams and print: they should be sorted "O20M" first and "U14F" last
        teams.forEach( t -> System.out.println( "Sorted team: " + t ) );
        
        // now do the tests
        SimpleTeam first = teams.get( 0 );
        SimpleTeam last = teams.get( teams.size() - 1 );
        
        assertEquals( "O20M", first.getTeamTypeCode() );
        // ...
        // all others are somewhere inbetween, especially "O35M"
        // ...
        assertEquals( "U14F", last.getTeamTypeCode() );
        
        em.close();
    }
}