/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.propertyref.component.complete;


import org.hibernate.Hibernate;
import org.hibernate.cache.spi.CacheImplementor;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * @author Gavin King
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/propertyref/component/complete/Mapping.hbm.xml",
		concurrencyStrategy = "nonstrict-read-write"
)
@SessionFactory
public class CompleteComponentPropertyRefTest {

	@Test
	public void testComponentPropertyRef(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Person p = new Person();
					p.setIdentity( new Identity() );
					Account a = new Account();
					a.setNumber( "123-12345-1236" );
					a.setOwner( p );
					p.getIdentity().setName( "Gavin" );
					p.getIdentity().setSsn( "123-12-1234" );
					session.persist( p );
					session.persist( a );
				}
		);

		scope.inTransaction(
				session -> {
					Account a = (Account) session.createQuery( "from Account a left join fetch a.owner" )
							.uniqueResult();
					assertTrue( Hibernate.isInitialized( a.getOwner() ) );
					assertNotNull( a.getOwner() );
					assertEquals( "Gavin", a.getOwner().getIdentity().getName() );
					session.clear();

					a = session.get( Account.class, "123-12345-1236" );
					assertFalse( Hibernate.isInitialized( a.getOwner() ) );
					assertNotNull( a.getOwner() );
					assertEquals( "Gavin", a.getOwner().getIdentity().getName() );
					assertTrue( Hibernate.isInitialized( a.getOwner() ) );

					session.clear();

					final CacheImplementor cache = scope.getSessionFactory().getCache();
					cache.evictEntityData( Account.class );
					cache.evictEntityData( Person.class );

					a = session.get( Account.class, "123-12345-1236" );
					assertTrue( Hibernate.isInitialized( a.getOwner() ) );
					assertNotNull( a.getOwner() );
					assertEquals( "Gavin", a.getOwner().getIdentity().getName() );
					assertTrue( Hibernate.isInitialized( a.getOwner() ) );

					session.remove( a );
					session.remove( a.getOwner() );
				}
		);
	}
}
