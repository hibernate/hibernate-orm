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
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import junit.framework.AssertionFailedError;

import org.hibernate.ejb.metamodel.AbstractMetamodelSpecificTest;
import org.hibernate.ejb.metamodel.Product;
import org.hibernate.ejb.metamodel.Product_;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class AggregationResultTest extends AbstractMetamodelSpecificTest {
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
		product.setQuantity( 1000 );
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

	/**
	 * Sum of Longs should return a Long
	 */
	public void testSumOfLongs() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		CriteriaQuery<Long> criteria = builder.createQuery( Long.class );
		Root<Product> productRoot = criteria.from( Product.class );
		criteria.select( builder.sum( productRoot.get( Product_.partNumber ) ) );
		Object sumResult = em.createQuery( criteria ).getSingleResult();
		assertReturnType( Long.class, sumResult );
		em.getTransaction().commit();
		em.close();
	}

	/**
	 * Sum of Integers should return an Integer; note that this is distinctly different than JPAQL
	 */
	public void testSumOfIntegers() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		CriteriaQuery<Integer> criteria = builder.createQuery( Integer.class );
		Root<Product> productRoot = criteria.from( Product.class );
		criteria.select( builder.sum( productRoot.get( Product_.quantity ) ) );
		Object sumResult = em.createQuery( criteria ).getSingleResult();
		assertReturnType( Integer.class, sumResult );
		em.getTransaction().commit();
		em.close();
	}

	/**
	 * Sum of Doubles should return a Double
	 */
	public void testSumOfDoubles() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		CriteriaQuery<Double> criteria = builder.createQuery( Double.class );
		Root<Product> productRoot = criteria.from( Product.class );
		criteria.select( builder.sum( productRoot.get( Product_.price ) ) );
		Object sumResult = em.createQuery( criteria ).getSingleResult();
		assertReturnType( Double.class, sumResult );
		em.getTransaction().commit();
		em.close();
	}

	/**
	 * Sum of Floats should return a Float; note that this is distinctly different than JPAQL
	 */
	public void testSumOfFloats() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		CriteriaQuery<Float> criteria = builder.createQuery( Float.class );
		Root<Product> productRoot = criteria.from( Product.class );
		criteria.select( builder.sum( productRoot.get( Product_.rating ) ) );
		Object sumResult = em.createQuery( criteria ).getSingleResult();
		assertReturnType( Float.class, sumResult );
		em.getTransaction().commit();
		em.close();
	}

	/**
	 * Sum of BigInteger should return a BigInteger
	 */
	public void testSumOfBigIntegers() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		CriteriaQuery<BigInteger> criteria = builder.createQuery( BigInteger.class );
		Root<Product> productRoot = criteria.from( Product.class );
		criteria.select( builder.sum( productRoot.get( Product_.someBigInteger ) ) );
		Object sumResult = em.createQuery( criteria ).getSingleResult();
		assertReturnType( BigInteger.class, sumResult );
		em.getTransaction().commit();
		em.close();
	}

	/**
	 * Sum of BigDecimal should return a BigDecimal
	 */
	public void testSumOfBigDecimals() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		CriteriaQuery<BigDecimal> criteria = builder.createQuery( BigDecimal.class );
		Root<Product> productRoot = criteria.from( Product.class );
		criteria.select( builder.sum( productRoot.get( Product_.someBigDecimal ) ) );
		Object sumResult = em.createQuery( criteria ).getSingleResult();
		assertReturnType( BigDecimal.class, sumResult );
		em.getTransaction().commit();
		em.close();
	}

	public void assertReturnType(Class expectedType, Object value) {
		if ( value != null && ! expectedType.isInstance( value ) ) {
			throw new AssertionFailedError(
					"Result value was not of expected type: expected [" + expectedType.getName()
							+ "] but found [" + value.getClass().getName() + "]"
			);
		}
	}
}
