/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.jpa.test.criteria.basic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.SingularAttribute;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

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
	@TestForIssue(jiraKey = "HHH-8283")
	public void testDateCompositeCustomType() {
		Payment payment = new Payment();
		payment.setAmount( new BigDecimal( 1000 ) );
		payment.setDate( new Date() );

		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.persist( payment );

		CriteriaQuery<Payment> criteria = em.getCriteriaBuilder().createQuery( Payment.class );
		Root<Payment> rp = criteria.from( Payment.class );
		Predicate predicate = em.getCriteriaBuilder().equal( rp.get( Payment_.date ), new Date() );
		criteria.where( predicate );

		TypedQuery<Payment> q = em.createQuery( criteria );
		List<Payment> payments = q.getResultList();

		assertEquals( 1, payments.size() );

		em.getTransaction().commit();
		em.close();
	}

	@Test
	@TestForIssue(jiraKey = "HHH-8373")
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
	@TestForIssue( jiraKey = "HHH-8914" )
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
