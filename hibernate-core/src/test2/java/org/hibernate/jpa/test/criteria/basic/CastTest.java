/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.criteria.basic;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.dialect.DerbyDialect;
import org.hibernate.jpa.test.metamodel.AbstractMetamodelSpecificTest;
import org.hibernate.jpa.test.metamodel.Product;
import org.hibernate.jpa.test.metamodel.Product_;

import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.TestForIssue;
import org.junit.Assert;
import org.junit.Test;

public class CastTest extends AbstractMetamodelSpecificTest {
	private static final int QUANTITY = 2;

	@Test
	@SkipForDialect(value = DerbyDialect.class,comment = "Derby does not support cast from INTEGER to VARCHAR")
	@TestForIssue( jiraKey = "HHH-5755" )
	public void testCastToString() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Product product = new Product();
		product.setId( "product1" );
		product.setPrice( 1.23d );
		product.setQuantity( QUANTITY );
		product.setPartNumber( ((long)Integer.MAX_VALUE) + 1 );
		product.setRating( 1.999f );
		product.setSomeBigInteger( BigInteger.valueOf( 987654321 ) );
		product.setSomeBigDecimal( BigDecimal.valueOf( 987654.321 ) );
		em.persist( product );
		em.getTransaction().commit();
		em.close();

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<Product> criteria = builder.createQuery( Product.class );
		Root<Product> root = criteria.from( Product.class );
		criteria.where( builder.equal(root.get(Product_.quantity).as( String.class ), builder.literal(String.valueOf(QUANTITY))) );
		List<Product> result = em.createQuery( criteria ).getResultList();
		Assert.assertEquals( 1, result.size() );
		em.getTransaction().commit();
		em.close();

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.createQuery( "delete Product" ).executeUpdate();
		em.getTransaction().commit();
		em.close();
	}
}
