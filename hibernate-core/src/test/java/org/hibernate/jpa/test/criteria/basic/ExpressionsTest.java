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

import org.hibernate.jpa.test.metamodel.AbstractMetamodelSpecificTest;
import org.hibernate.jpa.test.metamodel.Product;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests that various expressions operate as expected
 *
 * @author Steve Ebersole
 */
public class ExpressionsTest extends AbstractMetamodelSpecificTest {
	private CriteriaBuilder builder;

	@Before
	public void prepareTestData() {
		builder = entityManagerFactory().getCriteriaBuilder();

		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Product product = new Product();
		product.setId( "product1" );
		product.setPrice( 1.23d );
		product.setQuantity( 2 );
		product.setPartNumber( ((long)Integer.MAX_VALUE) + 1 );
		product.setRating( 1.999f );
		product.setSomeBigInteger( BigInteger.valueOf( 987654321 ) );
		product.setSomeBigDecimal( BigDecimal.valueOf( 987654.32 ) );
		em.persist( product );
		em.getTransaction().commit();
		em.close();
	}

	@After
	public void cleanupTestData() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.remove( em.find( Product.class, "product1" ) );
		em.getTransaction().commit();
		em.close();
	}

	@Test
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

}