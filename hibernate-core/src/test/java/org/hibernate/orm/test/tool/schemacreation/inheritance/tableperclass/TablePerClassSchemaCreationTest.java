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

import org.hibernate.dialect.DB2Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.orm.test.tool.schemacreation.BaseSchemaCreationTestCase;

import org.hibernate.testing.junit5.schema.SchemaScope;
import org.hibernate.testing.junit5.schema.SchemaTest;

/**
 * @author Andrea Boriero
 */
public class TablePerClassSchemaCreationTest extends BaseSchemaCreationTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { AbstractParent.class, Child.class };
	}

	@SchemaTest
	public void testTableIsCreated(SchemaScope scope) {

		assertThatTablesAreCreated(
				"child (firstindex varchar(255), id bigint not null, secondindex varchar(255), seconduniquefield varchar(255), uniquefield varchar(255), primary key (id))"
		);

		if ( getStandardServiceRegistry().getService( JdbcEnvironment.class ).getDialect() instanceof DB2Dialect ) {
			assertThatActionIsGenerated( "create unique index (.*) on table child \\(uniquefield\\)" );
			assertThatActionIsGenerated( "create unique index (.*) on table child \\(secondUniqueField\\)" );
		}
		else {
			assertThatActionIsGenerated( "alter table child add constraint (.*) unique \\(uniquefield\\)" );
			assertThatActionIsGenerated( "alter table child add constraint (.*) unique \\(secondUniqueField\\)" );
		}

		assertThatActionIsGenerated( "create index index_1 on child \\(firstindex\\)" );
		assertThatActionIsGenerated( "create index index_2 on child \\(secondindex\\)" );
	}

	@Entity
	@Table(name = "entity", indexes = @Index(columnList = "firstIndex", name = "index_1"))
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	public abstract class AbstractParent {
		@Id
		@GeneratedValue
		private Long id;

		@Column(unique = true)
		private String uniqueField;

		private String firstIndex;
	}

	@Entity
	@Table(name = "child", indexes = @Index(columnList = "secondIndex", name = "index_2"))
	public class Child extends AbstractParent {
		@Column(unique = true)
		private String secondUniqueField;

		private String secondIndex;
	}
}
