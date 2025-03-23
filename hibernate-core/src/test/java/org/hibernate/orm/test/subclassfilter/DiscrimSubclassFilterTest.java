/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.subclassfilter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.hibernate.Session;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Steve Ebersole
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/subclassfilter/discrim-subclass.hbm.xml"
)
@SessionFactory
public class DiscrimSubclassFilterTest {

	@Test
	@SuppressWarnings("unchecked")
	public void testFiltersWithSubclass(SessionFactoryScope scope) {
		scope.inTransaction(
				s -> {
					s.enableFilter( "region" ).setParameter( "userRegion", "US" );

					prepareTestData( s );
					s.clear();

					List results;
					Iterator itr;

					results = s.createQuery( "from Person" ).list();
					assertEquals( 4, results.size(), "Incorrect qry result count" );
					s.clear();

					results = s.createQuery( "from Employee" ).list();
					assertEquals( 2, results.size(), "Incorrect qry result count" );
					s.clear();

					results = new ArrayList( new HashSet( s.createQuery( "from Person as p left join fetch p.minions" )
																.list() ) );
					assertEquals( 4, results.size(), "Incorrect qry result count" );
					itr = results.iterator();
					while ( itr.hasNext() ) {
						// find john
						final Person p = (Person) itr.next();
						if ( p.getName().equals( "John Doe" ) ) {
							Employee john = (Employee) p;
							assertEquals( 1, john.getMinions().size(), "Incorrect fecthed minions count" );
							break;
						}
					}
					s.clear();

					results = new ArrayList( new HashSet( s.createQuery( "from Employee as p left join fetch p.minions" )
																.list() ) );
					assertEquals( 2, results.size(), "Incorrect qry result count" );
					itr = results.iterator();
					while ( itr.hasNext() ) {
						// find john
						final Person p = (Person) itr.next();
						if ( p.getName().equals( "John Doe" ) ) {
							Employee john = (Employee) p;
							assertEquals( 1, john.getMinions().size(), "Incorrect fecthed minions count" );
							break;
						}
					}

				}
		);

		scope.inTransaction(
				s -> {
					s.createQuery( "delete Customer where contactOwner is not null" ).executeUpdate();
					s.createQuery( "delete Employee where manager is not null" ).executeUpdate();
					s.createQuery( "delete Person" ).executeUpdate();
				}
		);
	}

	@SuppressWarnings("unchecked")
	private void prepareTestData(Session s) {
		Employee john = new Employee( "John Doe" );
		john.setCompany( "JBoss" );
		john.setDepartment( "hr" );
		john.setTitle( "hr guru" );
		john.setRegion( "US" );

		Employee polli = new Employee( "Polli Wog" );
		polli.setCompany( "JBoss" );
		polli.setDepartment( "hr" );
		polli.setTitle( "hr novice" );
		polli.setRegion( "US" );
		polli.setManager( john );
		john.getMinions().add( polli );

		Employee suzie = new Employee( "Suzie Q" );
		suzie.setCompany( "JBoss" );
		suzie.setDepartment( "hr" );
		suzie.setTitle( "hr novice" );
		suzie.setRegion( "EMEA" );
		suzie.setManager( john );
		john.getMinions().add( suzie );

		Customer cust = new Customer( "John Q Public" );
		cust.setCompany( "Acme" );
		cust.setRegion( "US" );
		cust.setContactOwner( john );

		Person ups = new Person( "UPS guy" );
		ups.setCompany( "UPS" );
		ups.setRegion( "US" );

		s.persist( john );
		s.persist( cust );
		s.persist( ups );

		s.flush();
	}

}
