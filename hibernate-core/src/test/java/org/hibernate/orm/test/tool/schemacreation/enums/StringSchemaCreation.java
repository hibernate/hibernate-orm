/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.tool.schemacreation.enums;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.orm.test.tool.schemacreation.BaseSchemaCreationTestCase;

import org.hibernate.testing.junit5.schema.SchemaScope;
import org.hibernate.testing.junit5.schema.SchemaTest;

/**
 * @author Andrea Boriero
 */
public class StringSchemaCreation extends BaseSchemaCreationTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Person.class };
	}

	@SchemaTest
	public void testTableIsCreated(SchemaScope scope) {

		assertThatTablesAreCreated(
				"person (gender varchar(255), id bigint not null, name varchar(255), primary key (id))"
		);
	}

	@Entity(name = "Person")
	@Table(name = "person")
	public static class Person {
		@Id
		public long id;

		String name;

		@Enumerated(EnumType.STRING)
		OrdinalSchemaCreation.Gender gender;
	}

	public enum Gender {
		MALE,
		FEMALE
	}
}
