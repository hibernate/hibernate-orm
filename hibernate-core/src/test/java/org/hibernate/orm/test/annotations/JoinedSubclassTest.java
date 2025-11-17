/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;

import org.hibernate.orm.test.annotations.inheritance.Carrot;
import org.hibernate.orm.test.annotations.inheritance.Tomato;
import org.hibernate.orm.test.annotations.inheritance.Vegetable;
import org.hibernate.orm.test.annotations.inheritance.VegetablePk;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Emmanuel Bernard
 */
@DomainModel(annotatedClasses = {
		Boat.class,
		Ferry.class,
		AmericaCupClass.class,
		Country.class,
		Vegetable.class,
		Carrot.class,
		Tomato.class
})
@SessionFactory
public class JoinedSubclassTest {
	@Test
	public void testDefaultValues(SessionFactoryScope scope) {
		Ferry ferry = new Ferry();
		scope.inTransaction(
				s -> {
					ferry.setSize( 2 );
					ferry.setSea( "Channel" );
					s.persist( ferry );
				}
		);

		scope.inTransaction(
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
	public void testDeclaredValues(SessionFactoryScope scope) {
		Country country = new Country();
		AmericaCupClass americaCupClass = new AmericaCupClass();
		scope.inTransaction(
				s -> {
					country.setName( "France" );
					americaCupClass.setSize( 2 );
					americaCupClass.setCountry( country );
					s.persist( country );
					s.persist( americaCupClass );
				}
		);

		scope.inTransaction(
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
	public void testCompositePk(SessionFactoryScope scope) {
		scope.inTransaction(
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

		scope.inTransaction(
				s -> {
					CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
					CriteriaQuery<Vegetable> criteria = criteriaBuilder.createQuery( Vegetable.class );
					criteria.from( Vegetable.class );
					Vegetable v = s.createQuery( criteria ).uniqueResult();
					assertInstanceOf( Carrot.class, v );
					Carrot result = (Carrot) v;
					assertEquals( 23, result.getLength() );
				}
		);
	}

}
