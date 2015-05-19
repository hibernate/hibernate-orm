/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.naturalid.cid;

import static org.junit.Assert.assertNotNull;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Donnchadh O Donnabhain
 */
@SuppressWarnings("unchecked")
public class EmbeddedAndNaturalIdTest extends BaseCoreFunctionalTestCase {
    @TestForIssue(jiraKey = "HHH-9333")
    @Test
    public void testSave() {
        // prepare some test data...
        Session session = openSession();
        session.beginTransaction();
        A account = new A(new AId(1), "testCode");
        session.save( account );
        session.getTransaction().commit();
        session.close();

        // clean up
        session = openSession();
        session.beginTransaction();
        session.delete( account );
        session.getTransaction().commit();
        session.close();
    }

    @TestForIssue(jiraKey = "HHH-9333")
    @Test
    public void testNaturalIdCriteria() {
        Session s = openSession();
        s.beginTransaction();
        A u = new A(new AId(1), "testCode" );
        s.persist( u );
        s.getTransaction().commit();
        s.close();

        s = openSession();
        s.beginTransaction();
        u = ( A ) s.createCriteria( A.class )
                .add( Restrictions.naturalId().set( "shortCode", "testCode" ) )
                .uniqueResult();
        assertNotNull( u );
        s.getTransaction().commit();
        s.close();

        s = openSession();
        s.beginTransaction();
        s.createQuery( "delete A" ).executeUpdate();
        s.getTransaction().commit();
        s.close();
    }

    @TestForIssue(jiraKey = "HHH-9333")
    @Test
    public void testByNaturalId() {
        Session s = openSession();
        s.beginTransaction();
        A u = new A(new AId(1), "testCode" );
        s.persist( u );
        s.getTransaction().commit();
        s.close();

        s = openSession();
        s.beginTransaction();
        u = ( A ) s.byNaturalId(A.class).using("shortCode", "testCode").load();
        assertNotNull( u );
        s.getTransaction().commit();
        s.close();

        s = openSession();
        s.beginTransaction();
        s.createQuery( "delete A" ).executeUpdate();
        s.getTransaction().commit();
        s.close();
    }
    
    @Override
    protected Class[] getAnnotatedClasses() {
        return new Class[] {
                A.class,
                AId.class
        };
    }

}
