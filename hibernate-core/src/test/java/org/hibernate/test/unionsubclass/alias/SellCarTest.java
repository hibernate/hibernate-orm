/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.unionsubclass.alias;

import org.junit.Test;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author <a href="mailto:stliu@redhat.com">Strong Liu</a>
 */
@TestForIssue( jiraKey = "HHH-4825" )
public class SellCarTest extends BaseCoreFunctionalTestCase {
    public String[] getMappings() {
        return new String[] { "unionsubclass/alias/mapping.hbm.xml" };
    }

	@Test
    public void testSellCar() throws Exception {
        prepareData();
        Session session = openSession();
        Transaction tx = session.beginTransaction();
        Query query = session.createQuery( "from Seller" );
        Seller seller = (Seller) query.uniqueResult();
        assertNotNull( seller );
        assertEquals( 1, seller.getBuyers().size() );
        tx.commit();
        session.close();
    }

    private void prepareData() {
        Session session = openSession();
        Transaction tx = session.beginTransaction();
        session.save( createData() );
        tx.commit();
        session.close();
    }

    @SuppressWarnings( {"unchecked"})
	private Object createData() {
        Seller stliu = new Seller();
        stliu.setId( createID( "stliu" ) );
        CarBuyer zd = new CarBuyer();
        zd.setId( createID( "zd" ) );
        zd.setSeller( stliu );
        zd.setSellerName( stliu.getId().getName() );
        stliu.getBuyers().add( zd );
        return stliu;
    }

	private PersonID createID( String name ) {
        PersonID id = new PersonID();
        id.setName( name );
        id.setNum( Long.valueOf( 100 ) );
        return id;
    }
}
