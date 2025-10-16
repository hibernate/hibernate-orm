/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.subclassfilter;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;


/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsTemporaryTable.class)
@DomainModel(xmlMappings = "org/hibernate/orm/test/subclassfilter/joined-subclass.hbm.xml")
@SessionFactory
public class JoinedSubclassFilterTest {

	@Test
	@SuppressWarnings( {"unchecked"})
	public void testFiltersWithJoinedSubclass(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (s) -> {
			s.enableFilter( "region" ).setParameter( "userRegion", "US" );

			List<?> results = s.createQuery( "from Person" ).list();
			Assertions.assertEquals( 4, results.size(), "Incorrect qry result count" );
			s.clear();

			results = s.createQuery( "from Employee" ).list();
			Assertions.assertEquals( 2, results.size(), "Incorrect qry result count" );
			Iterator<?> itr = results.iterator();
			while ( itr.hasNext() ) {
				// find john
				final Person p = ( Person ) itr.next();
				if ( p.getName().equals( "John Doe" ) ) {
					Employee john = ( Employee ) p;
					Assertions.assertEquals( 2, john.getMinions().size(), "Incorrect fecthed minions count" );
					break;
				}
			}
			s.clear();

			// TODO : currently impossible to define a collection-level filter w/ joined-subclass elements that will filter based on a superclass column and function correctly in (theta only?) outer joins;
			// this is consistent with the behaviour of a collection-level where.
			// this might be one argument for "pulling" the attached class-level filters into collection assocations,
			// although we'd need some way to apply the appropriate alias in that scenario.
			results = new ArrayList( new HashSet( s.createQuery( "from Person as p left join fetch p.minions" ).list() ) );
			Assertions.assertEquals( 4, results.size(), "Incorrect qry result count" );
			itr = results.iterator();
			while ( itr.hasNext() ) {
				// find john
				final Person p = ( Person ) itr.next();
				if ( p.getName().equals( "John Doe" ) ) {
					Employee john = ( Employee ) p;
					Assertions.assertEquals( 2, john.getMinions().size(), "Incorrect fecthed minions count" );
					break;
				}
			}
			s.clear();

			results = new ArrayList( new HashSet( s.createQuery( "from Employee as p left join fetch p.minions" ).list() ) );
			Assertions.assertEquals( 2, results.size(), "Incorrect qry result count" );
			itr = results.iterator();
			while ( itr.hasNext() ) {
				// find john
				final Person p = ( Person ) itr.next();
				if ( p.getName().equals( "John Doe" ) ) {
					Employee john = ( Employee ) p;
					Assertions.assertEquals( 2, john.getMinions().size(), "Incorrect fecthed minions count" );
					break;
				}
			}
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@BeforeEach
	@SuppressWarnings( {"unchecked"})
	void prepareTestData(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (s) -> {
			Employee john = new Employee("John Doe");
			john.setCompany( "JBoss" );
			john.setDepartment( "hr" );
			john.setTitle( "hr guru" );
			john.setRegion( "US" );

			Employee polli = new Employee("Polli Wog");
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
		} );
	}

}
