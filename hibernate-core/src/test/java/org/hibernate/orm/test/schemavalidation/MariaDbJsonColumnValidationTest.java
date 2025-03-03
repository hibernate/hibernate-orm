/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemavalidation;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.tool.hbm2ddl.SchemaValidator;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;

@JiraKey(value = "HHH-18869")
@RequiresDialect(value = MariaDBDialect.class)
public class MariaDbJsonColumnValidationTest extends BaseNonConfigCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {Foo.class};
	}

	@Before
	public void init() {
		try {
			inTransaction( session -> {
						try {
							session.createNativeMutationQuery( "drop table Foo" ).executeUpdate();
						}
						catch (Exception e) {
							throw new RuntimeException( e );
						}
					}
			);
			inTransaction( session ->
					session.createNativeMutationQuery(
							"create table Foo (id integer not null, bigDecimals json, primary key (id)) engine=InnoDB"
					).executeUpdate()
			);
		}
		catch (Exception ignored) {
		}
	}

	@Test
	public void testSchemaValidation() {
		new SchemaValidator().validate( metadata() );
	}

	@Entity(name = "Foo")
	@Table(name = "Foo")
	public static class Foo {
		@Id
		public Integer id;
		public BigDecimal[] bigDecimals;
	}
}
