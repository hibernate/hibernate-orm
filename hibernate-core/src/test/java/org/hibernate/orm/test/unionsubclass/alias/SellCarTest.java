/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.unionsubclass.alias;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


/**
 * @author Strong Liu
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey( value = "HHH-4825" )
@DomainModel(xmlMappings = "org/hibernate/orm/test/unionsubclass/alias/mapping.hbm.xml")
@SessionFactory
public class SellCarTest {
	@BeforeEach
	void setUp(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			Seller stliu = new Seller();
			stliu.setId( createID( "stliu" ) );
			CarBuyer zd = new CarBuyer();
			zd.setId( createID( "zd" ) );
			zd.setSeller( stliu );
			zd.setSellerName( stliu.getId().getName() );
			stliu.getBuyers().add( zd );

			session.persist( stliu );
		} );
	}

	private PersonID createID( String name ) {
		PersonID id = new PersonID();
		id.setName( name );
		id.setNum(100L);
		return id;
	}

	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void testSellCar(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			Seller seller = session.createQuery( "from Seller", Seller.class ).uniqueResult();
			Assertions.assertNotNull( seller );
			Assertions.assertEquals( 1, seller.getBuyers().size() );
		} );
	}
}
