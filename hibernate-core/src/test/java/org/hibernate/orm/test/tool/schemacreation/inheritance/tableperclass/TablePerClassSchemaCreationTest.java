/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.tool.schemacreation.inheritance.tableperclass;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

import org.hibernate.orm.test.tool.schemacreation.BaseSchemaCreationTestCase;

import org.hibernate.testing.junit5.schema.SchemaScope;
import org.hibernate.testing.junit5.schema.SchemaTest;

/**
 * @author Andrea Boriero
 */
public class TablePerClassSchemaCreationTest extends BaseSchemaCreationTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { AbstractParent.class, Child.class, SecondChild.class };
	}

	@SchemaTest
	public void testTableIsCreated(SchemaScope scope) {

		assertThatTablesAreCreated(
				"child (childIndex varchar(255), childUniqueField varchar(255), id bigint not null, parentIndex varchar(255), uniqueField varchar(255), primary key (id))",
				"second_child (id bigint not null, parentIndex varchar(255), secondChildIndex varchar(255), secondChildUniqueField varchar(255), uniqueField varchar(255), primary key (id))"
		);

		assertThatActionIsGenerated( "alter table child add constraint (.*) unique \\(uniquefield\\)" );
		assertThatActionIsGenerated( "alter table child add constraint (.*) unique \\(childUniqueField\\)" );

		assertThatActionIsGenerated( "alter table second_child add constraint (.*) unique \\(uniquefield\\)" );
		assertThatActionIsGenerated( "alter table second_child add constraint (.*) unique \\(secondChildUniqueField\\)" );

		assertThatActionIsGenerated( "create index index_1 on child \\(parentIndex\\)" );
		assertThatActionIsGenerated( "create index index_2 on child \\(childIndex\\)" );

		assertThatActionIsGenerated( "create index index_1 on second_child \\(parentIndex\\)" );
		assertThatActionIsGenerated( "create index index_2 on second_child \\(secondChildIndex\\)" );
	}

	@Entity
	@Table(name = "entity", indexes = @Index(columnList = "parentIndex", name = "index_1"))
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	public abstract class AbstractParent {
		@Id
		@GeneratedValue
		private Long id;

		@Column(unique = true)
		private String uniqueField;

		private String parentIndex;
	}

	@Entity
	@Table(name = "child", indexes = @Index(columnList = "childIndex", name = "index_2"))
	public class Child extends AbstractParent {
		@Column(unique = true)
		private String childUniqueField;

		private String childIndex;
	}

	@Entity
	@Table(name = "second_child", indexes = @Index(columnList = "secondChildIndex", name = "index_2"))
	public class SecondChild extends AbstractParent {
		@Column(unique = true)
		private String secondChildUniqueField;

		private String secondChildIndex;
	}
}
