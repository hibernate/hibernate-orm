/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.Parameter;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.usertype.UserTypeLegacyBridge;

import org.junit.jupiter.api.Test;


/**
 * @author Vlad Mihalcea
 */
@Jpa( annotatedClasses = {ExplicitTypeTest.Product.class} )
public class ExplicitTypeTest {

	@Test
	public void test(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {

		});
	}

	//tag::basic-type-annotation-example[]
	@Entity(name = "Product")
	public static class Product {

		@Id
		private Integer id;

		private String sku;

		@Type(
				value = UserTypeLegacyBridge.class,
				parameters = @Parameter(name = UserTypeLegacyBridge.TYPE_NAME_PARAM_KEY, value = "nstring")
		)
		private String name;

		@Type(
				value = UserTypeLegacyBridge.class,
				parameters = @Parameter(name = UserTypeLegacyBridge.TYPE_NAME_PARAM_KEY, value = "materialized_nclob")
		)
		private String description;
	}
	//end::basic-type-annotation-example[]
}
