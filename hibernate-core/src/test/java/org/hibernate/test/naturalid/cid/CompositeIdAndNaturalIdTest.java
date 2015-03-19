/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.naturalid.cid;

import static org.junit.Assert.assertNotNull;

import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.criterion.Restrictions;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

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
