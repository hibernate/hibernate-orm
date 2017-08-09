package org.hibernate.test.bytecode.enhancement.detached;

import org.hibernate.SessionFactory;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.transaction.TransactionUtil;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import static org.junit.Assert.assertNotNull;

/**
 * @author Luis Barreiro
 */
@TestForIssue( jiraKey = "HHH-11426" )
@RunWith( BytecodeEnhancerRunner.class )
public class DetachedGetIdentifierTest extends BaseCoreFunctionalTestCase {

    @Override
    public Class<?>[] getAnnotatedClasses() {
        return new Class<?>[]{SimpleEntity.class};
    }

    @Test
    public void test() {
        SimpleEntity[] entities = new SimpleEntity[2];
        entities[0] = new SimpleEntity();
        entities[0].name = "test";

        TransactionUtil.doInJPA( this::sessionFactory, em -> {
            entities[1] = em.merge( entities[0] );
            assertNotNull( em.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier( entities[1] ) );
        } );

        // Call as detached entity
        try ( SessionFactory sessionFactory = sessionFactory() ) {
            assertNotNull( sessionFactory.getPersistenceUnitUtil().getIdentifier( entities[1] ) );
        }
    }

    // --- //

    @Entity
    @Table( name = "SIMPLE_ENTITY" )
    private static class SimpleEntity {

        @Id
        @GeneratedValue
        Long id;

        String name;
    }
}
