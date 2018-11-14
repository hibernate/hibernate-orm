/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.tool.schemacreation.index;

import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.annotations.Index;
import org.hibernate.orm.test.tool.schemacreation.BaseSchemaCreationTestCase;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.schema.SchemaScope;
import org.hibernate.testing.junit5.schema.SchemaTest;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-11815")
public class ComponentIndexTest extends BaseSchemaCreationTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { User.class };
	}

	@SchemaTest
	public void testTheIndexIsGenerated(SchemaScope schemaScope) {
		assertThatActionIsGenerated( "create index city_index .*" );
	}

	@Entity(name = "user")
	public class User {
		@Id
		private Long id;
		@Embedded
		private Address address;
	}

	@Embeddable
	public class Address {
		@Index(name = "city_index")
		private String city;
		private String street;
		private String postalCode;
	}
}
