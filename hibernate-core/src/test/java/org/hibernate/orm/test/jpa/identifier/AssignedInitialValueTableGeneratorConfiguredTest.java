/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.identifier;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.TableGenerator;

import org.hibernate.Session;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * @author Andrea Boriero
 */
@Jpa(annotatedClasses = {
		AssignedInitialValueTableGeneratorConfiguredTest.Product.class
})
public class AssignedInitialValueTableGeneratorConfiguredTest {

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
		scope.releaseEntityManagerFactory();
	}

	@Test
	public void testTheFirstGeneratedIdIsEqualToTableGeneratorInitialValuePlusOne(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Product product = new Product();
					product.setName( "Hibernate" );
					entityManager.persist( product );
				}
		);

		scope.inEntityManager(
				entityManager -> {
					Product product = entityManager.find( Product.class, 3L );
					assertThat( product, notNullValue() );
				}
		);
	}

	@Test
	public void testTheGeneratedIdValuesAreCorrect(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					for ( long i = 0; i < 3; i++ ) {
						Product product = new Product();
						product.setName( "Hibernate " + i );
						entityManager.persist( product );
					}
				}
		);

		scope.inEntityManager(
				entityManager -> {
					Session session = entityManager.unwrap( Session.class );
					session.doWork(
							connection -> {
								try (Statement statement = connection.createStatement()) {
									ResultSet resultSet = statement.executeQuery(
											"select product_id from table_identifier" );
									resultSet.next();
									int productIdValue = resultSet.getInt( 1 );
									assertThat( productIdValue, is( 12 ) );
								}
							}
					);
				}
		);

		scope.inEntityManager(
				entityManager -> {
					List<Product> products = entityManager.createQuery( "from Product p order by id " ).getResultList();
					assertThat( products.size(), is( 3 ) );
					assertThat( products.get( 0 ).getId(), is( 3L ) );
					assertThat( products.get( 1 ).getId(), is( 4L ) );
					assertThat( products.get( 2 ).getId(), is( 5L ) );
				}
		);
	}

	@Entity(name = "Product")
	public static class Product {

		@Id
		@GeneratedValue(
				strategy = GenerationType.TABLE,
				generator = "table-generator"
		)
		@TableGenerator(
				name = "table-generator",
				table = "table_identifier",
				pkColumnName = "table_name",
				valueColumnName = "product_id",
				allocationSize = 5,
				initialValue = 2
		)
		private Long id;

		private String name;

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
	}
}
