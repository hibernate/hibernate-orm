/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.stateless;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import org.hibernate.FetchMode;
import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;

/**
 * @author stliu
 */
public class StatelessSessionQueryTest extends BaseCoreFunctionalTestCase {
	@Override
	public void configure( Configuration cfg ) {
		super.configure( cfg );
		cfg.setProperty( Environment.MAX_FETCH_DEPTH, "1" );
	}

	@Override
	public String[] getMappings() {
		return new String[] { "stateless/Contact.hbm.xml" };
	}

	@Test
	public void testCriteria() {
		TestData testData=new TestData();
		testData.createData();
		StatelessSession s = sessionFactory().openStatelessSession();
		assertEquals( 1, s.createCriteria( Contact.class ).list().size() );
		s.close();
		testData.cleanData();
	}

	@Test
	public void testCriteriaWithSelectFetchMode() {
		TestData testData=new TestData();
		testData.createData();
		StatelessSession s = sessionFactory().openStatelessSession();
		assertEquals( 1, s.createCriteria( Contact.class ).setFetchMode( "org", FetchMode.SELECT )
				.list().size() );
		s.close();
		testData.cleanData();
	}

	@Test
	public void testHQL() {
		TestData testData=new TestData();
		testData.createData();
		StatelessSession s = sessionFactory().openStatelessSession();
		assertEquals( 1, s.createQuery( "from Contact c join fetch c.org join fetch c.org.country" )
				.list().size() );
		s.close();
		testData.cleanData();
	}

	private class TestData{
		List list = new ArrayList();
		public void createData(){
			Session session = openSession();
			Transaction tx = session.beginTransaction();
			Country usa = new Country();
			session.save( usa );
			list.add( usa );
			Org disney = new Org();
			disney.setCountry( usa );
			session.save( disney );
			list.add( disney );
			Contact waltDisney = new Contact();
			waltDisney.setOrg( disney );
			session.save( waltDisney );
			list.add( waltDisney );
			tx.commit();
			session.close();
		}
		public void cleanData(){
			Session session = openSession();
			Transaction tx = session.beginTransaction();
			for(Object obj: list){
				session.delete( obj );
			}
			tx.commit();
			session.close();
		}
	}
}
