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
import javax.persistence.TableGenerator;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Vlad Mihalcea
 */
public class TableGeneratorConfiguredTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Product.class
		};
	}

	@Test
	public void test() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::identifiers-generators-table-persist-example[]
			for ( long i = 1; i <= 3; i++ ) {
				Product product = new Product();
				product.setName( String.format( "Product %d", i ) );
				entityManager.persist( product );
			}
			//end::identifiers-generators-table-persist-example[]
		} );
	}

	//tag::identifiers-generators-table-mapping-example[]
	@Entity(name = "Product")
	public static class Product {

		@Id
		@GeneratedValue(
			strategy = GenerationType.TABLE,
			generator = "table-generator"
		)
		@TableGenerator(
			name =  "table-generator",
			table = "table_identifier",
			pkColumnName = "table_name",
			valueColumnName = "product_id",
			allocationSize = 5
		)
		private Long id;

		@Column(name = "product_name")
		private String name;

		//Getters and setters are omitted for brevity

	//end::identifiers-generators-table-mapping-example[]

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
		
	//tag::identifiers-generators-table-mapping-example[]
	}
	//end::identifiers-generators-table-mapping-example[]
}
