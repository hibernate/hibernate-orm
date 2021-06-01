/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.criteria.basic;

import java.math.BigDecimal;
import java.math.BigInteger;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import junit.framework.AssertionFailedError;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.jpa.test.metamodel.AbstractMetamodelSpecificTest;
import org.hibernate.jpa.test.metamodel.Product;
import org.hibernate.jpa.test.metamodel.Product_;

/**
 * @author Steve Ebersole
 */
public class AggregationResultTest extends AbstractMetamodelSpecificTest {
	private CriteriaBuilder builder;

	@Before
	public void createTestData() {
		builder = entityManagerFactory().getCriteriaBuilder();

		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Product product = new Product();
		product.setId( "product1" );
		product.setPrice( 1.23d );
		product.setQuantity( 1000 );
		product.setPartNumber( ( (long) Integer.MAX_VALUE ) + 1 );
		product.setRating( 1.999f );
		product.setSomeBigInteger( BigInteger.valueOf( 987654321 ) );
		product.setSomeBigDecimal( BigDecimal.valueOf( 987654.32 ) );
		em.persist( product );
		em.getTransaction().commit();
		em.close();
	}

	@After
	public void cleanUpTestData() throws Exception {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.createQuery( "delete Product" ).executeUpdate();
		em.getTransaction().commit();
		em.close();
	}

	/**
	 * Sum of Integers should return an Integer; note that this is distinctly different than JPAQL
	 */
	@Test
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
	 * Sum of Floats should return a Float; note that this is distinctly different than JPAQL
	 */
	@Test
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

	private void assertReturnType(Class expectedType, Object value) {
		if ( value != null && ! expectedType.isInstance( value ) ) {
			throw new AssertionFailedError(
					"Result value was not of expected type: expected [" + expectedType.getName()
							+ "] but found [" + value.getClass().getName() + "]"
			);
		}
	}
}
