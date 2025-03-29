/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemavalidation.matchingtablenames;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.tool.hbm2ddl.SchemaValidator;

import org.junit.Test;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;

/**
 * @author Andrea Boriero
 */
@JiraKey(value = "HHH-10718")
public class TableNamesWithUnderscoreTest extends BaseNonConfigCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Entity1.class,
				Entity01.class
		};
	}

	@Test
	public void testSchemaValidationDoesNotFailDueToAMoreThanOneTableFound() {
		new SchemaValidator().validate( metadata() );
	}

	@Entity(name = "Entity1")
	@Table(name = "Entity_1")
	public static class Entity1 {
		@Id
		@GeneratedValue
		private int id;
	}

	@Entity(name = "Entity01")
	@Table(name = "Entity01")
	public static class Entity01 {
		@Id
		@GeneratedValue
		private int id;
	}
}
