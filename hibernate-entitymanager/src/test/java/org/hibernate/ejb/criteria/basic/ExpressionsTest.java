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
import java.util.Collections;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.hibernate.Query;
import org.hibernate.ejb.metamodel.AbstractMetamodelSpecificTest;
import org.hibernate.ejb.metamodel.Phone;
import org.hibernate.ejb.metamodel.Product;
import org.hibernate.ejb.metamodel.Product_;
import org.hibernate.ejb.test.mapping.Phone_;
import org.hibernate.impl.AbstractQueryImpl;

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
		product.setPartNumber( ((long)Integer.MAX_VALUE) + 1 );
		product.setRating( 1.999f );
		product.setSomeBigInteger( BigInteger.valueOf( 987654321 ) );
		product.setSomeBigDecimal( BigDecimal.valueOf( 987654.321 ) );
		em.persist( product );
		em.getTransaction().commit();
		em.close();
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

	public void testDiffWithQuotient() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		CriteriaQuery<Number> criteria = builder.createQuery( Number.class );
		criteria.from( Product.class );
		criteria.select(
				builder.quot(
						builder.diff(
								builder.literal( BigDecimal.valueOf( 2.0 ) ),
								builder.literal( BigDecimal.valueOf( 1.0 ) )
						),
						BigDecimal.valueOf( 2.0 )
				)
		);
		Number result = em.createQuery( criteria ).getSingleResult();
		assertEquals(0.5d, result.doubleValue(), 0.1d);
		em.getTransaction().commit();
		em.close();
	}

	public void testSumWithQuotient() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		CriteriaQuery<Number> criteria = builder.createQuery( Number.class );
		criteria.from( Product.class );
		criteria.select(
				builder.quot(
						builder.sum(
								builder.literal( BigDecimal.valueOf( 0.0 ) ),
								builder.literal( BigDecimal.valueOf( 1.0 ) )
						),
						BigDecimal.valueOf( 2.0 )
				)
		);
		Number result = em.createQuery( criteria ).getSingleResult();
		assertEquals(0.5d, result.doubleValue(), 0.1d);
		em.getTransaction().commit();
		em.close();
	}

	public void testQuotientAndMultiply() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		CriteriaQuery<Number> criteria = builder.createQuery( Number.class );
		criteria.from( Product.class );
		criteria.select(
				builder.quot(
						builder.prod(
								builder.literal( BigDecimal.valueOf( 10.0 ) ),
								builder.literal( BigDecimal.valueOf( 5.0 ) )
						),
						BigDecimal.valueOf( 2.0 )
				)
		);
		Number result = em.createQuery( criteria ).getSingleResult();
		assertEquals(25.0d, result.doubleValue(), 0.1d);

		criteria.select(
				builder.prod(
						builder.quot(
								builder.literal( BigDecimal.valueOf( 10.0 ) ),
								builder.literal( BigDecimal.valueOf( 5.0 ) )
						),
						BigDecimal.valueOf( 2.0 )
				)
		);
		result = em.createQuery( criteria ).getSingleResult();
		assertEquals(4.0d, result.doubleValue(), 0.1d);

		em.getTransaction().commit();
		em.close();
	}


	public void testParameterReuse() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		CriteriaQuery<Product> criteria = em.getCriteriaBuilder().createQuery( Product.class );
		Root<Product> from = criteria.from( Product.class );
		ParameterExpression<String> param = em.getCriteriaBuilder().parameter( String.class );
		Predicate predicate = em.getCriteriaBuilder().equal( from.get( Product_.id ), param );
		Predicate predicate2 = em.getCriteriaBuilder().equal( from.get( Product_.name ), param );
		criteria.where( em.getCriteriaBuilder().or( predicate, predicate2 ) );
		assertEquals( 1, criteria.getParameters().size() );
		TypedQuery<Product> query = em.createQuery( criteria );
		int hqlParamCount = countGeneratedParameters( query.unwrap( Query.class ) );
		assertEquals( 1, hqlParamCount );
		query.setParameter( param, "abc" ).getResultList();
		em.getTransaction().commit();
		em.close();
	}

	private int countGeneratedParameters(Query query) {
		AbstractQueryImpl hqlQueryImpl = (AbstractQueryImpl) query;
		return hqlQueryImpl.getParameterMetadata().getNamedParameterNames().size();
	}

	public void testInExplicitTupleList() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		CriteriaQuery<Product> criteria = builder.createQuery( Product.class );
		Root<Product> from = criteria.from( Product.class );
		criteria.where( from.get( Product_.partNumber ).in( Collections.singletonList( ((long)Integer.MAX_VALUE) + 1 ) ) );
		List<Product> result = em.createQuery( criteria ).getResultList();
		assertEquals( 1, result.size() );
		em.getTransaction().commit();
		em.close();
	}

	public void testInExplicitTupleListVarargs() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		CriteriaQuery<Product> criteria = builder.createQuery( Product.class );
		Root<Product> from = criteria.from( Product.class );
		criteria.where( from.get( Product_.partNumber ).in( ((long)Integer.MAX_VALUE) + 1 ) );
		List<Product> result = em.createQuery( criteria ).getResultList();
		assertEquals( 1, result.size() );
		em.getTransaction().commit();
		em.close();
	}

	public void testInExpressionVarargs() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		CriteriaQuery<Product> criteria = builder.createQuery( Product.class );
		Root<Product> from = criteria.from( Product.class );
		criteria.where( from.get( Product_.partNumber ).in( from.get( Product_.partNumber ) ) );
		List<Product> result = em.createQuery( criteria ).getResultList();
		assertEquals( 1, result.size() );
		em.getTransaction().commit();
		em.close();
	}

	public void testJoinedElementCollectionValuesInTupleList() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		CriteriaQuery<Phone> criteria = builder.createQuery( Phone.class );
		Root<Phone> from = criteria.from( Phone.class );
		criteria.where(
				from.join( "types" )
						.in( Collections.singletonList( Phone.Type.WORK ) )
		);
		em.createQuery( criteria ).getResultList();
		em.getTransaction().commit();
		em.close();
	}
}
