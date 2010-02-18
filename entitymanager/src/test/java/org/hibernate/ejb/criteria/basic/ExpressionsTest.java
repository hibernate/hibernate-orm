/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.ejb.criteria.basic;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;

import org.hibernate.ejb.metamodel.AbstractMetamodelSpecificTest;
import org.hibernate.ejb.metamodel.Product;

/**
 * Tests that various expressions operate as expected
 *
 * @author Steve Ebersole
 */
public class ExpressionsTest extends AbstractMetamodelSpecificTest {
	private CriteriaBuilder builder;

	@Override
	public void setUp() throws Exception {
		super.setUp();
		builder = factory.getCriteriaBuilder();
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Product product = new Product();
		product.setId( "product1" );
		product.setPrice( 1.23d );
		product.setQuantity( 2 );
		product.setPartNumber( Integer.MAX_VALUE + 1 );
		product.setRating( 1.999f );
		product.setSomeBigInteger( BigInteger.valueOf( 987654321 ) );
		product.setSomeBigDecimal( BigDecimal.valueOf( 987654.321 ) );
		em.persist( product );
		em.getTransaction().commit();
		em.close();
	}

	@Override
	public void tearDown() throws Exception {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.createQuery( "delete Product" ).executeUpdate();
		em.getTransaction().commit();
		em.close();
		super.tearDown();
	}

	public void testEmptyConjunction() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		CriteriaQuery<Product> criteria = builder.createQuery( Product.class );
		criteria.from( Product.class );
		criteria.where( builder.and() );
		List<Product> result = em.createQuery( criteria ).getResultList();
		assertEquals( 1, result.size() );
		em.getTransaction().commit();
		em.close();
	}

	public void testEmptyConjunctionIsTrue() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		CriteriaQuery<Product> criteria = builder.createQuery( Product.class );
		criteria.from( Product.class );
		criteria.where( builder.isTrue( builder.and() ) );
		List<Product> result = em.createQuery( criteria ).getResultList();
		assertEquals( 1, result.size() );
		em.getTransaction().commit();
		em.close();
	}

	public void testEmptyConjunctionIsFalse() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		CriteriaQuery<Product> criteria = builder.createQuery( Product.class );
		criteria.from( Product.class );
		criteria.where( builder.isFalse( builder.and() ) );
		List<Product> result = em.createQuery( criteria ).getResultList();
		assertEquals( 0, result.size() );
		em.getTransaction().commit();
		em.close();
	}

	public void testEmptyDisjunction() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		CriteriaQuery<Product> criteria = builder.createQuery( Product.class );
		criteria.from( Product.class );
		criteria.where( builder.disjunction() );
		List<Product> result = em.createQuery( criteria ).getResultList();
		assertEquals( 0, result.size() );
		em.getTransaction().commit();
		em.close();
	}

	public void testEmptyDisjunctionIsTrue() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		CriteriaQuery<Product> criteria = builder.createQuery( Product.class );
		criteria.from( Product.class );
		criteria.where( builder.isTrue( builder.disjunction() ) );
		List<Product> result = em.createQuery( criteria ).getResultList();
		assertEquals( 0, result.size() );
		em.getTransaction().commit();
		em.close();
	}

	public void testEmptyDisjunctionIsFalse() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		CriteriaQuery<Product> criteria = builder.createQuery( Product.class );
		criteria.from( Product.class );
		criteria.where( builder.isFalse( builder.disjunction() ) );
		List<Product> result = em.createQuery( criteria ).getResultList();
		assertEquals( 1, result.size() );
		em.getTransaction().commit();
		em.close();
	}

	public void testDiff() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		CriteriaQuery<Integer> criteria = builder.createQuery( Integer.class );
		criteria.from( Product.class );
		criteria.select( builder.diff( builder.literal( 5 ), builder.literal( 2 ) ) );
		Integer result = em.createQuery( criteria ).getSingleResult();
		assertEquals( Integer.valueOf( 3 ), result );
		em.getTransaction().commit();
		em.close();
	}
}
