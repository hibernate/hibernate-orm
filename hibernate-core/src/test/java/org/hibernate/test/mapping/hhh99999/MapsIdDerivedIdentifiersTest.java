package org.hibernate.test.mapping.hhh99999;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.hql.spi.id.inline.InlineIdsOrClauseBulkIdStrategy;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.test.mapping.hhh99999.entity.Game;
import org.hibernate.test.mapping.hhh99999.entity.PlayerStat;
import org.hibernate.test.mapping.hhh99999.entity.Score;
import org.hibernate.test.mapping.hhh99999.entity.Stat;
import org.hibernate.testing.TestForIssue;
import org.junit.Before;
import org.junit.Test;

/**
 * This template demonstrates how to develop a test case for Hibernate ORM, using the Java Persistence API.
 */
@TestForIssue( jiraKey = "HHH-13812" )
public class MapsIdDerivedIdentifiersTest extends BaseEntityManagerFunctionalTestCase
{
    @Override
    protected Class<?>[] getAnnotatedClasses()
    {
        return new Class<?>[] { Game.class,
                                PlayerStat.class,
                                Score.class,
                                Stat.class };
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
            // do nothing
        } );
    }
    
    /**
     * See
     * https://stackoverflow.com/questions/59587201/jpa-hibernate-case-in-orderby-annotation-throws-exception-unexpected-token-ca
     * 
     * @throws Exception
     */
    @Test
    public void testMapsIdDerivedIdentifiers() throws Exception
    {
        doInJPA( this::entityManagerFactory, em ->
        {
            // do nothing
        });
    }
}