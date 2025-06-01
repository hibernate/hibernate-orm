/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.propertyref.basic;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;

import org.hibernate.Hibernate;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.PersistentClass;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
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
		xmlMappings = "org/hibernate/orm/test/propertyref/basic/Person.hbm.xml"

)
@SessionFactory(
		generateStatistics = true
)
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.DEFAULT_BATCH_FETCH_SIZE, value = "1")
		}
)
public class PropertyRefTest {

	@Test
	public void testNonLazyBagKeyPropertyRef(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Person p = new Person();
					p.setName( "Steve" );
					p.setUserId( "steve" );
					p.getSystems().add( "QA" );
					p.getSystems().add( "R&D" );
					session.persist( p );
				}
		);

		scope.inTransaction(
				session -> {
					session.createQuery( "from Person" ).list();
					session.clear();
					session.createNativeQuery( "select {p.*} from PROPREF_PERS {p}" )
							.addEntity( "p", Person.class.getName() )
							.list();
				}
		);

		scope.inTransaction(
				session -> {
					var results = session.createQuery( "from Person" ).list();
					for ( Object result : results ) {
						session.remove( result );
					}
				}
		);
	}

	@Test
	public void testManyToManyPropertyRef(SessionFactoryScope scope) {
		// prepare some test data relating to the Group->Person many-to-many association
		Group g = new Group();
		scope.inTransaction(
				session -> {
					Person p = new Person();
					p.setName( "Steve" );
					p.setUserId( "steve" );
					session.persist( p );
					g.setName( "Admins" );
					g.getUsers().add( p );
					session.persist( g );
					// force a flush and detachment here to test reattachment handling of the property-ref (HHH-1531)
				}
		);

		Person p2 = new Person();
		p2.setName( "Max" );
		p2.setUserId( "max" );
		g.getUsers().add( p2 );

		scope.inTransaction(
				session ->
						session.merge( g )
		);

		// test retrieval of the group
		scope.inTransaction(
				session -> {
					Group group = (Group) session.createQuery( "from Group g left join fetch g.users" ).uniqueResult();
					assertTrue( Hibernate.isInitialized( group.getUsers() ) );
					assertEquals( 2, group.getUsers().size() );
					session.remove( group );
					session.createQuery( "delete Person" ).executeUpdate();
				}
		);
	}

	@Test
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
					var l = session.createQuery( "from Person" ).list(); //pull address references for cache
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

					l = session.createQuery( "from Person p left join p.accounts a", Person.class ).list();
					for ( int i = 0; i < 2; i++ ) {
						Person px = (Person) l.get( i );
						assertFalse( Hibernate.isInitialized( px.getAccounts() ) );
						if ( px.getName().equals( "Max" ) ) {
							assertEquals( 1, px.getAccounts().size() );
						}
						else {
							assertEquals( 0, px.getAccounts().size() );

						}
					}
					session.clear();

					l = session.createQuery( "from Person p left join fetch p.accounts a order by p.name" ).list();
					Person p0 = (Person) l.get( 0 );
					assertTrue( Hibernate.isInitialized( p0.getAccounts() ) );
					assertEquals( 1, p0.getAccounts().size() );
					assertSame( p0, ( (Account) p0.getAccounts().iterator().next() ).getUser() );
					Person p1 = (Person) l.get( 1 );
					assertTrue( Hibernate.isInitialized( p1.getAccounts() ) );
					assertEquals( 0, p1.getAccounts().size() );
					session.clear();
					Account acc = (Account) session.createQuery( "from Account a left join fetch a.user" ).uniqueResult();
					assertTrue( Hibernate.isInitialized( acc.getUser() ) );
					assertNotNull( acc.getUser() );
					assertTrue( acc.getUser().getAccounts().contains( acc ) );

					session.createQuery( "delete from Address" ).executeUpdate();
					session.createQuery( "delete from Account" )
							.executeUpdate(); // to not break constraint violation between Person and Account
					session.createQuery( "delete from Person" ).executeUpdate();
				}
		);
	}

	@Test
	public void testJoinFetchPropertyRef(SessionFactoryScope scope) {
		scope.inTransaction(
				s -> {
					Person p = new Person();
					p.setName( "Steve" );
					p.setUserId( "steve" );
					Address a = new Address();
					a.setAddress( "Texas" );
					a.setCountry( "USA" );
					p.setAddress( a );
					a.setPerson( p );
					s.persist( p );

					s.flush();
					s.clear();

					final SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
					sessionFactory.getStatistics().clear();

					p = s.get( Person.class, p.getId() ); //get address reference by outer join

					assertTrue( Hibernate.isInitialized( p.getAddress() ) );
					assertNotNull( p.getAddress() );
					assertEquals( 1, sessionFactory.getStatistics().getPrepareStatementCount() );
					assertEquals( 0, sessionFactory.getStatistics().getEntityFetchCount() );

					s.clear();

					sessionFactory.getStatistics().clear();

					CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
					CriteriaQuery<Person> criteria = criteriaBuilder.createQuery( Person.class );
					criteria.from( Person.class ).join( "address" );

					p = s.createQuery( criteria ).uniqueResult();
//					p = (Person) s.createCriteria(Person.class)
//							.setFetchMode("address", FetchMode.SELECT)
//							.uniqueResult(); //get address reference by select

					assertTrue( Hibernate.isInitialized( p.getAddress() ) );
					assertNotNull( p.getAddress() );
					assertEquals( 2, sessionFactory.getStatistics().getPrepareStatementCount() );
					assertEquals( 0, sessionFactory.getStatistics().getEntityFetchCount() );

					s.createQuery( "delete from Address" ).executeUpdate();
					s.createQuery( "delete from Person" ).executeUpdate();
				}

		);
	}

	@Test
	public void testForeignKeyCreation(SessionFactoryScope scope) {
		PersistentClass classMapping = scope.getMetadataImplementor().getEntityBinding( Account.class.getName() );

		var foreignKeyIterator = classMapping.getTable().getForeignKeyCollection().iterator();
		boolean found = false;
		while ( foreignKeyIterator.hasNext() ) {
			final ForeignKey element = foreignKeyIterator.next();
			if ( element.getReferencedEntityName().equals( Person.class.getName() ) ) {

				if ( !element.isReferenceToPrimaryKey() ) {
					var referencedColumns = element.getReferencedColumns();
					Column column = referencedColumns.get( 0 );
					if ( column.getName().equals( "person_userid" ) ) {
						found = true; // extend test to include the columns
					}
				}
			}
		}

		assertTrue( found, "Property ref foreign key not found" );
	}
}
