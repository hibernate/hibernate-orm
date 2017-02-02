/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.schemavalidation.matchingtablenames;


import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.tool.hbm2ddl.SchemaValidator;

import org.junit.Test;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-10718")
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
