/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cuk;

import java.util.List;
import java.util.Set;

import org.hibernate.Hibernate;
import org.hibernate.cfg.Environment;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * @author Gavin King
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/cuk/Person.hbm.xml"
)
@SessionFactory
@ServiceRegistry(
		settings = @Setting(name = Environment.DEFAULT_BATCH_FETCH_SIZE, value = "1")
)
public class CompositePropertyRefTest {

	@Test
	@SuppressWarnings({ "unchecked", "unused" })
	public void testOneToOnePropertyRef(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Person p = new Person();
					p.setName( "Steve" );
					p.setUserId( "steve" );
					Address a = new Address();
					a.setAddress( "Texas" );
					a.setCountry( "USA" );
					p.setAddress( a );
					a.setPerson( p );
					session.persist( p );

					Person p2 = new Person();
					p2.setName( "Max" );
					p2.setUserId( "max" );
					session.persist( p2 );
					Account act = new Account();
					act.setType( 'c' );
					act.setUser( p2 );
					p2.getAccounts().add( act );
					session.persist( act );
					session.flush();
					session.clear();

					p = session.get( Person.class, p.getId() ); //get address reference by outer join
					p2 = session.get( Person.class, p2.getId() ); //get null address reference by outer join
					assertNull( p2.getAddress() );
					assertNotNull( p.getAddress() );
					List l = session.createQuery( "from Person" ).list(); //pull address references for cache
					assertEquals( 2, l.size() );
					assertTrue( l.contains( p ) && l.contains( p2 ) );
					session.clear();

					l = session.createQuery( "from Person p order by p.name" )
							.list(); //get address references by sequential selects
					assertEquals( 2, l.size() );
					assertNull( ( (Person) l.get( 0 ) ).getAddress() );
					assertNotNull( ( (Person) l.get( 1 ) ).getAddress() );
					session.clear();

					l = session.createQuery( "from Person p left join fetch p.address a order by a.country" )
							.list(); //get em by outer join
					assertEquals( 2, l.size() );
					if ( ( (Person) l.get( 0 ) ).getName().equals( "Max" ) ) {
						assertNull( ( (Person) l.get( 0 ) ).getAddress() );
						assertNotNull( ( (Person) l.get( 1 ) ).getAddress() );
					}
					else {
						assertNull( ( (Person) l.get( 1 ) ).getAddress() );
						assertNotNull( ( (Person) l.get( 0 ) ).getAddress() );
					}
					session.clear();

					l = session.createQuery( "from Person p left join p.accounts", Person.class ).list();
					for ( int i = 0; i < 2; i++ ) {
						Person px = (Person) l.get( i );
						Set accounts = px.getAccounts();
						assertFalse( Hibernate.isInitialized( accounts ) );
//			assertTrue( px.getAccounts().size()>0 || row[1]==null );
					}
					session.clear();

					l = session.createQuery( "from Person p left join fetch p.accounts a order by p.name" ).list();
					Person p0 = (Person) l.get( 0 );
					assertTrue( Hibernate.isInitialized( p0.getAccounts() ) );
					assertEquals( 1, p0.getAccounts().size() );
					assertSame( ( (Account) p0.getAccounts().iterator().next() ).getUser(), p0 );
					Person p1 = (Person) l.get( 1 );
					assertTrue( Hibernate.isInitialized( p1.getAccounts() ) );
					assertEquals( 0, p1.getAccounts().size() );
					session.clear();

					l = session.createQuery( "from Account a join fetch a.user" ).list();

					session.clear();

					l = session.createQuery( "from Person p left join fetch p.address" ).list();

					session.clear();
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

}
