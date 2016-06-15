/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.naturalid.cid;

import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.criterion.Restrictions;

import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * @author Donnchadh O Donnabhain
 */
public class CompositeIdAndNaturalIdTest extends BaseCoreFunctionalTestCase {

    public String[] getMappings() {
        return new String[] { "naturalid/cid/Account.hbm.xml" };
    }

    public void configure(Configuration cfg) {
        cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "false" );
        cfg.setProperty( Environment.USE_QUERY_CACHE, "false" );
        cfg.setProperty( Environment.GENERATE_STATISTICS, "false" );
    }

    @Test
    @TestForIssue( jiraKey = "HHH-10360")
    public void testNaturalIdNullability() {
        final EntityPersister persister = sessionFactory().getEntityPersister( Account.class.getName() );
        final int propertyIndex = persister.getEntityMetamodel().getPropertyIndex( "shortCode" );
        // the natural ID mapped as non-nullable
        assertFalse( persister.getPropertyNullability()[propertyIndex] );
    }

    @Test
    public void testSave() {
        // prepare some test data...
        Session session = openSession();
        session.beginTransaction();
        Account account = new Account(new AccountId(1), "testAcct");
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

    @Test
    public void testNaturalIdCriteria() {
        Session s = openSession();
        s.beginTransaction();
        Account u = new Account(new AccountId(1), "testAcct" );
        s.persist( u );
        s.getTransaction().commit();
        s.close();

        s = openSession();
        s.beginTransaction();
        u = ( Account ) s.createCriteria( Account.class )
                .add( Restrictions.naturalId().set( "shortCode", "testAcct" ) )
                .setCacheable( true )
                .uniqueResult();
        assertNotNull( u );
        s.getTransaction().commit();
        s.close();

        s = openSession();
        s.beginTransaction();
        s.createQuery( "delete Account" ).executeUpdate();
        s.getTransaction().commit();
        s.close();
    }

    @Test
    public void testByNaturalId() {
        Session s = openSession();
        s.beginTransaction();
        Account u = new Account(new AccountId(1), "testAcct" );
        s.persist( u );
        s.getTransaction().commit();
        s.close();

        s = openSession();
        s.beginTransaction();
        u = ( Account ) s.byNaturalId(Account.class).using("shortCode", "testAcct").load();
        assertNotNull( u );
        s.getTransaction().commit();
        s.close();

        s = openSession();
        s.beginTransaction();
        s.createQuery( "delete Account" ).executeUpdate();
        s.getTransaction().commit();
        s.close();
    }

}
