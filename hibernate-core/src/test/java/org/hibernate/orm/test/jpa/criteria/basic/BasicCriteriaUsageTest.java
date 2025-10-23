/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.basic;

import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.SingularAttribute;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Steve Ebersole
 */
@Jpa(annotatedClasses = {Wall.class, Payment.class})
public class BasicCriteriaUsageTest {

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	public void testParameterCollection(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			CriteriaQuery<Wall> criteria = entityManager.getCriteriaBuilder().createQuery( Wall.class );
			Root<Wall> from = criteria.from( Wall.class );
			ParameterExpression<String> param = entityManager.getCriteriaBuilder().parameter( String.class );
			SingularAttribute<? super Wall, ?> colorAttribute = entityManager.getMetamodel().entity( Wall.class )
					.getDeclaredSingularAttribute( "color" );
			assertNotNull( colorAttribute, "metamodel returned null singular attribute" );
			Predicate predicate = entityManager.getCriteriaBuilder().equal( from.get( colorAttribute ), param );
			criteria.where( predicate );
			assertEquals( 1, criteria.getParameters().size() );
		} );
	}

	@Test
	public void testTrivialCompilation(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			CriteriaQuery<Wall> criteria = entityManager.getCriteriaBuilder().createQuery( Wall.class );
			criteria.from( Wall.class );
			entityManager.createQuery( criteria ).getResultList();
		} );
	}

	@Test
	@JiraKey(value = "HHH-8283")
	public void testDateCompositeCustomType(EntityManagerFactoryScope scope) {
		final Date date = Date.from( Instant.now() );
		final Payment payment = new Payment();
		payment.setAmount( new BigDecimal( 1000 ) );
		payment.setDate( date );

		scope.inTransaction( entityManager -> {
			entityManager.persist( payment );

			CriteriaQuery<Payment> criteria = entityManager.getCriteriaBuilder().createQuery( Payment.class );
			Root<Payment> rp = criteria.from( Payment.class );
			Predicate predicate = entityManager.getCriteriaBuilder().equal( rp.get( Payment_.date ), date );
			criteria.where( predicate );

			TypedQuery<Payment> q = entityManager.createQuery( criteria );
			List<Payment> payments = q.getResultList();

			assertEquals( 1, payments.size() );

		} );
	}

	@Test
	@JiraKey(value = "HHH-8373")
	public void testFunctionCriteria(EntityManagerFactoryScope scope) {
		Wall wall = new Wall();
		wall.setColor( "yellow" );
		scope.inTransaction( entityManager -> {
			entityManager.persist( wall );

			CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			CriteriaQuery<Wall> query = cb.createQuery( Wall.class );
			Root<Wall> root = query.from( Wall.class );

			query.select( root ).where( cb.equal( root.get( "color" ), cb.lower( cb.literal( "YELLOW" ) ) ) );

			Wall resultItem = entityManager.createQuery( query ).getSingleResult();
			assertNotNull( resultItem );

		} );
	}

	@Test
	@JiraKey(value = "HHH-8914")
	public void testDoubleNegation(EntityManagerFactoryScope scope) {
		Wall wall1 = new Wall();
		wall1.setColor( "yellow" );
		Wall wall2 = new Wall();
		wall2.setColor( null );

		scope.inTransaction( entityManager -> {
			entityManager.persist( wall1 );
			entityManager.persist( wall2 );
		} );

		scope.inTransaction( entityManager -> {

			// Although the examples are simplified and the usages appear pointless,
			// double negatives can occur in some dynamic applications (regardless
			// if it results from bad design or not).  Ensure we handle them as expected.

			CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			CriteriaQuery<Wall> query = cb.createQuery( Wall.class );
			Root<Wall> root = query.from( Wall.class );
			query.select( root ).where(
					cb.not(
							cb.isNotNull( root.get( "color" ) ) ) );
			Wall result = entityManager.createQuery( query ).getSingleResult();
			assertNotNull( result );
			assertNull( result.getColor() );

			query = cb.createQuery( Wall.class );
			root = query.from( Wall.class );
			query.select( root ).where(
					cb.not(
							cb.not(
									cb.isNull( root.get( "color" ) ) ) ) );
			result = entityManager.createQuery( query ).getSingleResult();
			assertNotNull( result );
			assertNull( result.getColor() );

			query = cb.createQuery( Wall.class );
			root = query.from( Wall.class );
			query.select( root ).where(
					cb.not(
							cb.not(
									cb.isNotNull( root.get( "color" ) ) ) ) );
			result = entityManager.createQuery( query ).getSingleResult();
			assertNotNull( result );
			assertEquals( "yellow", result.getColor() );

		} );
	}
}
