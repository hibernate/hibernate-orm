/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.tool.schemaupdate;

import java.util.EnumSet;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

import org.hibernate.orm.test.tool.BaseSchemaUnitTestCase;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.schema.SchemaScope;
import org.hibernate.testing.junit5.schema.SchemaTest;

/**
 * @author Andrea Boriero
 */
public class IndexCreationSchemaUpdateTest extends BaseSchemaUnitTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { EntityWithIndex.class };
	}

	@SchemaTest
	@TestForIssue(jiraKey = "HHH-9713")
	public void testIndexCreationViaSchemaUpdate(SchemaScope schemaScope) {
		// drop and then create the schema
		schemaScope.withSchemaExport( schemaExport ->
								  schemaExport.execute( EnumSet.of( TargetType.DATABASE ), SchemaExport.Action.BOTH ) );

		// update the schema
		schemaScope.withSchemaUpdate( schemaUpdate ->
								  schemaUpdate.setHaltOnError( true ).execute( EnumSet.of( TargetType.DATABASE ) ) );
	}

	@Entity(name = "EntityWithIndex")
	@Table(name = "T_Entity_With_Index", indexes = @Index(columnList = "name"))
	public static class EntityWithIndex {
		@Id
		public Integer id;
		public String name;
	}
}
