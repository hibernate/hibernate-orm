/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.tool.schemaupdate;

import java.util.EnumSet;
import javax.persistence.ConstraintMode;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.orm.test.tool.BaseSchemaUnitTestCase;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.junit5.schema.SchemaScope;
import org.hibernate.testing.junit5.schema.SchemaTest;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(value = H2Dialect.class)
@TestForIssue(jiraKey = "HHH-8805")
public class SchemaUpdateJoinColumnNoConstraintTest extends BaseSchemaUnitTestCase {

	private static final String DELIMITER = ";";

	@Override
	protected boolean createSqlScriptTempOutputFile() {
		return true;
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Parent.class };
	}

	@SchemaTest
	public void test(SchemaScope scope) throws Exception {
		scope.withSchemaUpdate(
				schemaUpdate ->
						schemaUpdate.setHaltOnError( true )
								.setDelimiter( DELIMITER )
								.setFormat( true )
								.execute( EnumSet.of( TargetType.SCRIPT ) ) );


		assertFalse( getSqlScriptOutputFileContent().toLowerCase().contains( "foreign key" ) );
	}

	@Entity(name = "Parent")
	public static class Parent {

		@Id
		private Long id;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}

	@Entity(name = "Child")
	public static class Child {

		@Id
		private Long id;

		@ManyToOne
		@JoinColumn(
				name = "some_fk",
				foreignKey = @ForeignKey(name = "none", value = ConstraintMode.NO_CONSTRAINT)
		)
		private Parent parent;
	}

}
