package org.hibernate.test.mapping.hhh99999;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.hql.spi.id.inline.InlineIdsOrClauseBulkIdStrategy;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.test.mapping.hhh99999.entity.PlayerStat;
import org.hibernate.test.mapping.hhh99999.entity.Score;
import org.hibernate.testing.TestForIssue;
import org.junit.Before;
import org.junit.Test;

/**
 * This template demonstrates how to develop a test case for Hibernate ORM, using the Java Persistence API.
 */
@TestForIssue( jiraKey = "HHH-99999" )
public class MapsIdDerivedIdentifiersTest extends BaseEntityManagerFunctionalTestCase
{
    @Override
    protected Class<?>[] getAnnotatedClasses()
    {
        return new Class<?>[] { PlayerStat.class,
                                Score.class };
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
    
    @Test
    public void testMapsIdDerivedIdentifiers() throws Exception
    {
        doInJPA( this::entityManagerFactory, em ->
        {
            // do nothing
        });
    }
}