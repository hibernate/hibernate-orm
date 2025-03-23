/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.unionsubclass.alias;

import org.junit.Test;

import org.hibernate.query.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Strong Liu
 */
@JiraKey( value = "HHH-4825" )
public class SellCarTest extends BaseCoreFunctionalTestCase {

	@Override
	protected String getBaseForMappings() {
		return "org/hibernate/orm/test/";
	}

	@Override
	public String[] getMappings() {
		return new String[] { "unionsubclass/alias/mapping.hbm.xml" };
	}

	@Test
	public void testSellCar() {
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
		session.persist( createData() );
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
		id.setNum(100L);
		return id;
	}
}
