/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.mapping.identifier;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Vlad Mihalcea
 */
public class PooledOptimizerTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Product.class
		};
	}

	@Test
	public void test() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::identifiers-generators-pooled-lo-optimizer-persist-example[]
			for ( long i = 1; i <= 5; i++ ) {
				if(i % 3 == 0) {
					entityManager.flush();
				}
				Product product = new Product();
				product.setName( String.format( "Product %d", i ) );
				product.setNumber( String.format( "P_100_%d", i ) );
				entityManager.persist( product );
			}
			//end::identifiers-generators-pooled-lo-optimizer-persist-example[]
		} );
	}

	//tag::identifiers-generators-pooled-lo-optimizer-mapping-example[]
	@Entity(name = "Product")
	public static class Product {

		@Id
		@GeneratedValue(
			strategy = GenerationType.SEQUENCE,
			generator = "product_generator"
		)
		@GenericGenerator(
			name = "product_generator",
			strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
			parameters = {
				@Parameter(name = "sequence_name", value = "product_sequence"),
				@Parameter(name = "initial_value", value = "1"),
				@Parameter(name = "increment_size", value = "3"),
				@Parameter(name = "optimizer", value = "pooled-lo")
			}
		)
		private Long id;

		@Column(name = "p_name")
		private String name;

		@Column(name = "p_number")
		private String number;

		//Getters and setters are omitted for brevity

	//end::identifiers-generators-pooled-lo-optimizer-mapping-example[]

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getNumber() {
			return number;
		}

		public void setNumber(String number) {
			this.number = number;
		}
	//tag::identifiers-generators-pooled-lo-optimizer-mapping-example[]
	}
	//end::identifiers-generators-pooled-lo-optimizer-mapping-example[]
}
