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
package org.hibernate.jpa.test.criteria.basic;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.junit.Assert;
import org.junit.Test;

import org.hibernate.jpa.test.metamodel.AbstractMetamodelSpecificTest;
import org.hibernate.jpa.test.metamodel.Product;
import org.hibernate.jpa.test.metamodel.Product_;

import org.hibernate.testing.TestForIssue;

public class CastTest extends AbstractMetamodelSpecificTest {
	private static final int QUANTITY = 2;

	@Test
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
