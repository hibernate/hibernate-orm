/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.tool.schemacreation.components;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.orm.test.tool.schemacreation.BaseSchemaCreationTestCase;

import org.hibernate.testing.junit5.schema.SchemaScope;
import org.hibernate.testing.junit5.schema.SchemaTest;

/**
 * @author Andrea Boriero
 */
public class ComponentAttributeOverrideSchemaCreationTest extends BaseSchemaCreationTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Person.class };
	}

	@SchemaTest
	public void testTableIsCreated(SchemaScope scope) {

		assertThatTablesAreCreated(
				"person (id bigint not null, name varchar(255), postcode varchar(255), work_address_name varchar(255) not null, work_address_postcode varchar(255), primary key (id))"
		);
	}

	@Entity(name = "Person")
	public static class Person {
		@Id
		private long id;

		private Address home;

		@Embedded
		@AttributeOverrides({
				@AttributeOverride(name = "name", column = @Column(name = "work_address_name", nullable = false)),
				@AttributeOverride(name = "postcode", column = @Column(name = "work_address_postcode"))
		})
		private Address work;
	}

	@Embeddable
	public static class Address {
		private String name;
		private String postcode;
	}
}
