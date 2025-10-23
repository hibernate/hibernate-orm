/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Parameter;
import jakarta.persistence.TemporalType;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.hibernate.query.Query;
import org.hibernate.query.SemanticException;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Jpa(annotatedClasses = {CriteriaQueryTypeQueryAdapterTest.Bid.class, CriteriaQueryTypeQueryAdapterTest.Item.class})
public class CriteriaQueryTypeQueryAdapterTest {

	@Test
	@JiraKey(value = "HHH-12685")
	public void testCriteriaQueryParameterIsBoundCheckNotFails(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			CriteriaBuilder builder = entityManager.getCriteriaBuilder();
			CriteriaQuery<Item> query = builder.createQuery( Item.class );
			Root<Item> root = query.from( Item.class );
			ParameterExpression<String> parameter = builder.parameter( String.class, "name" );
			Predicate predicate = builder.equal( root.get( "name" ), parameter );
			query.where( predicate );
			TypedQuery<Item> criteriaQuery = entityManager.createQuery( query );
			Parameter<?> dynamicParameter = criteriaQuery.getParameter( "name" );
			boolean bound = criteriaQuery.isBound( dynamicParameter );
			assertFalse( bound );
		} );
	}

	@Test
	@JiraKey(value = "HHH-12685")
	public void testCriteriaQueryGetParameters(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			CriteriaBuilder builder = entityManager.getCriteriaBuilder();
			CriteriaQuery<Item> query = builder.createQuery( Item.class );
			Root<Item> root = query.from( Item.class );
			ParameterExpression<String> parameter = builder.parameter( String.class, "name" );
			Predicate predicate = builder.equal( root.get( "name" ), parameter );
			query.where( predicate );

			TypedQuery<Item> criteriaQuery = entityManager.createQuery( query );

			Set<Parameter<?>> parameters = criteriaQuery.getParameters();
			assertEquals( 1, parameters.size() );

			Parameter<?> dynamicParameter = parameters.iterator().next();
			assertEquals( "name", dynamicParameter.getName() );
		} );
	}

	@JiraKey(value = "HHH-12685")
	@Test
	public void testCriteriaQueryGetParameterOfWrongType(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager ->
			assertThrows(
					IllegalArgumentException.class,
					() -> {
						CriteriaBuilder builder = entityManager.getCriteriaBuilder();
						CriteriaQuery<Item> query = builder.createQuery( Item.class );
						Root<Item> root = query.from( Item.class );
						ParameterExpression<String> parameter = builder.parameter( String.class, "name" );
						Predicate predicate = builder.equal( root.get( "name" ), parameter );
						query.where( predicate );
						TypedQuery<Item> criteriaQuery = entityManager.createQuery( query );
						criteriaQuery.getParameter( "name", Integer.class );
					}
			)
		);
	}

	@JiraKey(value = "HHH-12685")
	@Test
	public void testCriteriaQueryGetNonExistingParameter(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager ->
			assertThrows(
					IllegalArgumentException.class,
					() -> {
						CriteriaBuilder builder = entityManager.getCriteriaBuilder();
						CriteriaQuery<Item> query = builder.createQuery( Item.class );
						Root<Item> root = query.from( Item.class );
						ParameterExpression<String> parameter = builder.parameter( String.class, "name" );
						Predicate predicate = builder.equal( root.get( "name" ), parameter );
						query.where( predicate );
						TypedQuery<Item> criteriaQuery = entityManager.createQuery( query );
						criteriaQuery.getParameter( "placedAt" );
					}
			)
		);
	}

	@Test
	@JiraKey("HHH-13932")
	public void testCriteriaQuerySetNonExistingParameter(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager ->
			assertThrows(
					IllegalArgumentException.class,
					() -> {
						CriteriaBuilder builder = entityManager.getCriteriaBuilder();
						CriteriaQuery<Item> query = builder.createQuery( Item.class );
						Root<Item> root = query.from( Item.class );
						ParameterExpression<String> parameter = builder.parameter( String.class, "name" );
						Predicate predicate = builder.equal( root.get( "name" ), parameter );
						query.where( predicate );
						TypedQuery<Item> criteriaQuery = entityManager.createQuery( query );
						ParameterExpression<String> nonExistingParam = builder.parameter( String.class, "nonExistingParam" );
						criteriaQuery.setParameter( nonExistingParam, "George" );
					}
			)
		);
	}

	@Test
	public void testSetParameterPassingTypeNotFails(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			CriteriaBuilder builder = entityManager.getCriteriaBuilder();
			CriteriaQuery<Item> query = builder.createQuery( Item.class );

			Predicate predicate = builder.equal(
					query.from( Item.class ).get( "name" ),
					builder.parameter( String.class, "name" )
			);
			query.where( predicate );

			TypedQuery<?> criteriaQuery = entityManager.createQuery( query );

			criteriaQuery.setParameter( "name", "2" ).getResultList();
		} );
	}

	@Test
	public void testSetParameterTypeInstantNotFails(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			CriteriaBuilder builder = entityManager.getCriteriaBuilder();
			CriteriaQuery<Bid> query = builder.createQuery( Bid.class );

			Predicate predicate = builder.equal(
					query.from( Bid.class ).get( "placedAt" ),
					builder.parameter( Instant.class, "placedAt" )
			);
			query.where( predicate );

			Query<?> criteriaQuery = (Query<?>) entityManager.createQuery( query );

			criteriaQuery.setParameter( "placedAt", Instant.now() ).list();
		} );
	}

	@Test
	public void testSetParameterOfTypeInstantToAFloatParameterType(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager ->
			assertThrows(
					SemanticException.class,
					() -> {
						CriteriaBuilder builder = entityManager.getCriteriaBuilder();
						CriteriaQuery<Bid> query = builder.createQuery( Bid.class );

						Predicate predicate = builder.equal(
								query.from( Bid.class ).get( "amount" ),
								builder.parameter( Instant.class, "placedAt" )
						);
						query.where( predicate );

						Query<?> criteriaQuery = (Query<?>) entityManager.createQuery( query );

						criteriaQuery.setParameter( "placedAt", Instant.now() ).list();
					}
			)
		);
	}


	@Test
	public void testSetParameterOfTypeDateToAFloatParameterType(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager ->
			assertThrows(
					SemanticException.class,
					() -> {
						CriteriaBuilder builder = entityManager.getCriteriaBuilder();
						CriteriaQuery<Bid> query = builder.createQuery( Bid.class );

						Predicate predicate = builder.equal(
								query.from( Bid.class ).get( "amount" ),
								builder.parameter( Date.class, "placedAt" )
						);
						query.where( predicate );

						Query<?> criteriaQuery = (Query<?>) entityManager.createQuery( query );

						criteriaQuery.setParameter( "placedAt", Date.from(Instant.now()), TemporalType.DATE ).list();
					}
			)
		);
	}

	@Entity(name = "Bid")
	public static class Bid implements Serializable {
		@Id
		Long id;

		float amount;

		Instant placedAt;

		@Id
		@ManyToOne
		Item item;
	}

	@Entity(name = "Item")
	public static class Item implements Serializable {
		@Id
		Long id;

		String name;

		@OneToMany(mappedBy = "item")
		Set<Bid> bids = new HashSet<>();
	}
}
