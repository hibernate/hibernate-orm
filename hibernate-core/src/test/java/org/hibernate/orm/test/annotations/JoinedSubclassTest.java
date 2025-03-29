/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.orm.test.annotations.inheritance.Carrot;
import org.hibernate.orm.test.annotations.inheritance.Tomato;
import org.hibernate.orm.test.annotations.inheritance.Vegetable;
import org.hibernate.orm.test.annotations.inheritance.VegetablePk;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Emmanuel Bernard
 */
public class JoinedSubclassTest extends BaseCoreFunctionalTestCase {
	@Test
	public void testDefaultValues() {
		Ferry ferry = new Ferry();
		inTransaction(
				s -> {
					ferry.setSize( 2 );
					ferry.setSea( "Channel" );
					s.persist( ferry );
				}
		);

		inTransaction(
				s -> {
					Ferry f = s.get( Ferry.class, ferry.getId() );
					assertNotNull( f );
					assertEquals( "Channel", f.getSea() );
					assertEquals( 2, f.getSize() );
					s.remove( f );
				}
		);
	}

	@Test
	public void testDeclaredValues() {
		Country country = new Country();
		AmericaCupClass americaCupClass = new AmericaCupClass();
		inTransaction(
				s -> {
					country.setName( "France" );
					americaCupClass.setSize( 2 );
					americaCupClass.setCountry( country );
					s.persist( country );
					s.persist( americaCupClass );
				}
		);

		inTransaction(
				s -> {
					AmericaCupClass f = s.get( AmericaCupClass.class, americaCupClass.getId() );
					assertNotNull( f );
					assertEquals( country, f.getCountry() );
					assertEquals( 2, f.getSize() );
					s.remove( f );
					s.remove( f.getCountry() );
				}
		);
	}

	@Test
	public void testCompositePk() {
		inTransaction(
				s -> {
					Carrot c = new Carrot();
					VegetablePk pk = new VegetablePk();
					pk.setFarmer( "Bill" );
					pk.setHarvestDate( "2004-08-15" );
					c.setId( pk );
					c.setLength( 23 );
					s.persist( c );
				}
		);

		inTransaction(
				s -> {
					CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
					CriteriaQuery<Vegetable> criteria = criteriaBuilder.createQuery( Vegetable.class );
					criteria.from( Vegetable.class );
					Vegetable v = s.createQuery( criteria ).uniqueResult();
//					Vegetable v = (Vegetable) s.createCriteria( Vegetable.class ).uniqueResult();
					assertTrue( v instanceof Carrot );
					Carrot result = (Carrot) v;
					assertEquals( 23, result.getLength() );
				}
		);
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Boat.class,
				Ferry.class,
				AmericaCupClass.class,
				Country.class,
				Vegetable.class,
				Carrot.class,
				Tomato.class
		};
	}
}
