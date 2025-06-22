/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schema;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

/**
 * @author Vlad Mihalcea
 */

@RequiresDialect( H2Dialect.class )
@Jpa(
		annotatedClasses = {
				BaseSchemaGeneratorTest.Person.class,
				BaseSchemaGeneratorTest.Book.class,
				BaseSchemaGeneratorTest.Customer.class
		},
		integrationSettings = {
				@Setting( name = AvailableSettings.HBM2DDL_IMPORT_FILES, value = "schema-generation.sql"),
				@Setting( name = AvailableSettings.HBM2DDL_AUTO, value = "update")
		}
)
public class H2SchemaGenerationTest extends BaseSchemaGeneratorTest {

	@Test
	public void testH2() {
	}
}
