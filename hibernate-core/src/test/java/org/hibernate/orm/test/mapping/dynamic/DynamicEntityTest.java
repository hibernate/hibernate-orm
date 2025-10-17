/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.dynamic;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.Session;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

/**
 * @author Vlad Mihalcea
 */
@Jpa(
		xmlMappings = {"org/hibernate/orm/test/mapping/dynamic/Book.hbm.xml"},
		integrationSettings = {@Setting( name = "hibernate.default_entity_mode", value = "dynamic-map")}
)
public class DynamicEntityTest {

	// Preserved because of the doc inclusions
	protected void doesNothing() {
		Map settings = Collections.EMPTY_MAP;
		//tag::mapping-model-dynamic-setting-example[]
		settings.put("hibernate.default_entity_mode", "dynamic-map");
		//end::mapping-model-dynamic-setting-example[]
	}

	@Test
	public void test(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
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
