/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
package org.hibernate.test.unionsubclass.alias;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.junit.functional.FunctionalTestCase;

/**
 * http://opensource.atlassian.com/projects/hibernate/browse/HHH-4825
 * @author Strong Liu <stliu@redhat.com>
 */
public class SellCarTest extends FunctionalTestCase {

    public SellCarTest( String string ) {
        super( string );
    }

    public String[] getMappings() {
        return new String[] { "unionsubclass/alias/mapping.hbm.xml" };
    }

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
        id.setNum( new Long( 100 ) );
        return id;
    }
}
