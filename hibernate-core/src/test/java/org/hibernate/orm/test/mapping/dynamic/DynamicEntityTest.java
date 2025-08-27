/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.dynamic;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Vlad Mihalcea
 */
public class DynamicEntityTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected String[] getMappings() {
		return new String[] {
				"org/hibernate/orm/test/mapping/dynamic/Book.hbm.xml"
		};
	}

	@Override
	protected Map buildSettings() {
		Map settings = super.buildSettings();
		//tag::mapping-model-dynamic-setting-example[]
		settings.put("hibernate.default_entity_mode", "dynamic-map");
		//end::mapping-model-dynamic-setting-example[]
		return settings;
	}

	@Test
	public void test() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::mapping-model-dynamic-example[]

			Map<String, String> book = new HashMap<>();
			book.put("isbn", "978-9730228236");
			book.put("title", "High-Performance Java Persistence");
			book.put("author", "Vlad Mihalcea");

			entityManager
				.unwrap(Session.class)
				.persist("Book", book);
			//end::mapping-model-dynamic-example[]
		});
	}
}
