/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.sql.NClob;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

import org.hibernate.annotations.Nationalized;
import org.hibernate.engine.jdbc.NClobProxy;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
@Jpa(annotatedClasses = NClobTest.Product.class)
@RequiresDialectFeature(
		feature = DialectFeatureChecks.SupportsNationalizedDataTypes.class,
		comment = "This is different from other tests checking generalized nationalization support; " +
				"because we explicitly map this attribute to the `NClob` java type the database really" +
				" has to support those types"
)
public class NClobTest {
	@Test
	public void test(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				(entityManager) -> {
					//tag::basic-nclob-persist-example[]
					String warranty = "My product warranty";

					final Product product = new Product();
					product.setId(1);
					product.setName("Mobile phone");

					product.setWarranty(NClobProxy.generateProxy(warranty));

					entityManager.persist(product);
					//end::basic-nclob-persist-example[]
				}
	);

		scope.inTransaction(
				(entityManager) -> {
					try {
						//tag::basic-nclob-find-example[]
						Product product = entityManager.find(Product.class, 1);

						try (Reader reader = product.getWarranty().getCharacterStream()) {
							assertEquals("My product warranty", toString(reader));
						}
						//end::basic-nclob-find-example[]
					}
					catch (Exception e) {
						fail(e.getMessage());
					}
				}
	);
	}

	private String toString(Reader reader) throws IOException {
		BufferedReader bufferedReader = new BufferedReader(reader);
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

		int result = bufferedReader.read();

		while (result != -1) {
			byteArrayOutputStream.write((byte) result);
			result = bufferedReader.read();
		}

		return byteArrayOutputStream.toString();
	}


	//tag::basic-nclob-example[]
	@Entity(name = "Product")
	public static class Product {

		@Id
		private Integer id;

		private String name;

		@Lob
		@Nationalized
		// Clob also works, because NClob extends Clob.
		// The database type is still NCLOB either way and handled as such.
		private NClob warranty;

		//Getters and setters are omitted for brevity

		//end::basic-nclob-example[]
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

		public NClob getWarranty() {
			return warranty;
		}

		public void setWarranty(NClob warranty) {
			this.warranty = warranty;
		}

		//tag::basic-nclob-example[]
	}
	//end::basic-nclob-example[]
}
