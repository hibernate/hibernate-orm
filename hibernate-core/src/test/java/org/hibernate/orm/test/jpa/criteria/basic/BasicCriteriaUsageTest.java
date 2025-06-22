/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.basic;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.SingularAttribute;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Steve Ebersole
 */
public class BasicCriteriaUsageTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] { Wall.class, Payment.class };
	}

	@Test
	public void testParameterCollection() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		CriteriaQuery<Wall> criteria = em.getCriteriaBuilder().createQuery( Wall.class );
		Root<Wall> from = criteria.from( Wall.class );
		ParameterExpression param = em.getCriteriaBuilder().parameter( String.class );
		SingularAttribute<? super Wall, ?> colorAttribute = em.getMetamodel().entity( Wall.class ).getDeclaredSingularAttribute( "color" );
		assertNotNull( "metamodel returned null singular attribute", colorAttribute );
		Predicate predicate = em.getCriteriaBuilder().equal( from.get( colorAttribute ), param );
		criteria.where( predicate );
		assertEquals( 1, criteria.getParameters().size() );
		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testTrivialCompilation() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		CriteriaQuery<Wall> criteria = em.getCriteriaBuilder().createQuery( Wall.class );
		criteria.from( Wall.class );
		em.createQuery( criteria ).getResultList();
		em.getTransaction().commit();
		em.close();
	}

	@Test
	@JiraKey(value = "HHH-8283")
	public void testDateCompositeCustomType() {
		final Date date = Date.from( Instant.now() );
		final Payment payment = new Payment();
		payment.setAmount( new BigDecimal( 1000 ) );
		payment.setDate( date );

		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.persist( payment );

		CriteriaQuery<Payment> criteria = em.getCriteriaBuilder().createQuery( Payment.class );
		Root<Payment> rp = criteria.from( Payment.class );
		Predicate predicate = em.getCriteriaBuilder().equal( rp.get( Payment_.date ), date );
		criteria.where( predicate );

		TypedQuery<Payment> q = em.createQuery( criteria );
		List<Payment> payments = q.getResultList();

		assertEquals( 1, payments.size() );

		em.getTransaction().commit();
		em.close();
	}

	@Test
	@JiraKey(value = "HHH-8373")
	public void testFunctionCriteria() {
		Wall wall = new Wall();
		wall.setColor( "yellow" );
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.persist( wall );

		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Wall> query = cb.createQuery( Wall.class );
		Root<Wall> root = query.from( Wall.class );

		query.select( root ).where( cb.equal( root.get( "color" ), cb.lower( cb.literal( "YELLOW" ) ) ) );

		Wall resultItem = em.createQuery( query ).getSingleResult();
		assertNotNull( resultItem );

		em.getTransaction().commit();
		em.close();
	}

	@Test
	@JiraKey( value = "HHH-8914" )
	public void testDoubleNegation() {
		Wall wall1 = new Wall();
		wall1.setColor( "yellow" );
		Wall wall2 = new Wall();
		wall2.setColor( null );

		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.persist( wall1 );
		em.persist( wall2 );
		em.getTransaction().commit();
		em.clear();

		em.getTransaction().begin();

		// Although the examples are simplified and the usages appear pointless,
		// double negatives can occur in some dynamic applications (regardless
		// if it results from bad design or not).  Ensure we handle them as expected.

		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Wall> query = cb.createQuery( Wall.class );
		Root<Wall> root = query.from( Wall.class );
		query.select( root ).where(
				cb.not(
						cb.isNotNull( root.get( "color" ) ) ) );
		Wall result = em.createQuery( query ).getSingleResult();
		assertNotNull( result );
		assertEquals( null, result.getColor() );

		query = cb.createQuery( Wall.class );
		root = query.from( Wall.class );
		query.select( root ).where(
				cb.not(
						cb.not(
								cb.isNull( root.get( "color" ) ) ) ) );
		result = em.createQuery( query ).getSingleResult();
		assertNotNull( result );
		assertEquals( null, result.getColor() );

		query = cb.createQuery( Wall.class );
		root = query.from( Wall.class );
		query.select( root ).where(
				cb.not(
						cb.not(
								cb.isNotNull( root.get( "color" ) ) ) ) );
		result = em.createQuery( query ).getSingleResult();
		assertNotNull( result );
		assertEquals( "yellow", result.getColor() );

		em.getTransaction().commit();
		em.close();
	}
}
