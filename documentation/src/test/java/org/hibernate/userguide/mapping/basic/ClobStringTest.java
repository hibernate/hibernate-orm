/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.mapping.basic;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class ClobStringTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Product.class
		};
	}

	@Test
	public void test() {
		Integer productId = doInJPA( this::entityManagerFactory, entityManager -> {
			final Product product = new Product( );
			product.setId( 1 );
			product.setName( "Mobile phone" );
			product.setWarranty( "My product warranty" );

			entityManager.persist( product );
			return product.getId();
		} );
		doInJPA( this::entityManagerFactory, entityManager -> {
			Product product = entityManager.find( Product.class, productId );
			assertEquals( "My product warranty", product.getWarranty() );
		} );
	}

	//tag::basic-clob-string-example[]
	@Entity(name = "Product")
	public static class Product {

		@Id
		private Integer id;

		private String name;

		@Lob
		private String warranty;

		//Getters and setters are omitted for brevity

	//end::basic-clob-string-example[]
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getWarranty() {
			return warranty;
		}

		public void setWarranty(String warranty) {
			this.warranty = warranty;
		}

		//tag::basic-clob-string-example[]
	}
	//end::basic-clob-string-example[]
}
