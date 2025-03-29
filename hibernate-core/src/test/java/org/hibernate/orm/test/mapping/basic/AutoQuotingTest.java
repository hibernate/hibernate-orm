/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic;

import java.util.Map;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Vlad Mihalcea
 */
public class AutoQuotingTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Product.class
		};
	}

	@Override
	protected Map buildSettings() {
		Map settings = super.buildSettings();
		settings.put(AvailableSettings.GLOBALLY_QUOTED_IDENTIFIERS, true);
		return settings;
	}

	@Test
	public void test() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::basic-auto-quoting-persistence-example[]
			Product product = new Product();
			product.setId(1L);
			product.setName("Mobile phone");
			product.setNumber("123-456-7890");
			entityManager.persist(product);
			//end::basic-auto-quoting-persistence-example[]
		});
	}

	//tag::basic-auto-quoting-example[]
	@Entity(name = "Product")
	public static class Product {

		@Id
		private Long id;

		private String name;

		private String number;

		//Getters and setters are omitted for brevity

	//end::basic-auto-quoting-example[]

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


		//tag::basic-auto-quoting-example[]
	}
	//end::basic-auto-quoting-example[]
}
