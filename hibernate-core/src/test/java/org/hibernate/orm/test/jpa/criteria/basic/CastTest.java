/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.criteria.basic;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.hibernate.dialect.DerbyDialect;
import org.hibernate.orm.test.jpa.metamodel.Product;
import org.hibernate.orm.test.jpa.metamodel.Product_;
import org.hibernate.orm.test.jpa.criteria.AbstractCriteriaTest;

import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CastTest extends AbstractCriteriaTest {
	private static final int QUANTITY = 2;

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					entityManager.createQuery( "delete from Product" ).executeUpdate();
				}
		);
	}

	@Test
	@SkipForDialect(value = DerbyDialect.class, comment = "Derby does not support cast from INTEGER to VARCHAR")
	@JiraKey(value = "HHH-5755")
	public void testCastToString(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Product product = new Product();
					product.setId( "product1" );
					product.setPrice( 1.23d );
					product.setQuantity( QUANTITY );
					product.setPartNumber( ( (long) Integer.MAX_VALUE ) + 1 );
					product.setRating( 1.999f );
					product.setSomeBigInteger( BigInteger.valueOf( 987654321 ) );
					product.setSomeBigDecimal( BigDecimal.valueOf( 987654.321 ) );
					entityManager.persist( product );
				}
		);

		scope.inTransaction(
				entityManager -> {
					CriteriaBuilder builder = entityManager.getCriteriaBuilder();
					CriteriaQuery<Product> criteria = builder.createQuery( Product.class );
					Root<Product> root = criteria.from( Product.class );
					criteria.where( builder.equal(
							root.get( Product_.quantity ).as( String.class ),
							builder.literal( String.valueOf( QUANTITY ) )
					) );
					List<Product> result = entityManager.createQuery( criteria ).getResultList();
					Assertions.assertEquals( 1, result.size() );
				}
		);
	}
}
