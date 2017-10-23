/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.schemaupdate;

import java.util.EnumSet;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

/**
 * @author Andrea Boriero
 */
public class IndexCreationSchemaUpdateTest extends BaseSchemaUnitTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{EntityWithIndex.class};
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9713")
	public void testIndexCreationViaSchemaUpdate() {
		// drop and then create the schema
		createSchemaExport().execute( EnumSet.of( TargetType.DATABASE ), SchemaExport.Action.BOTH );

		// update the schema
		createSchemaUpdate().execute( EnumSet.of( TargetType.DATABASE ) );
	}

	@Entity(name = "EntityWithIndex")
	@Table(name = "T_Entity_With_Index", indexes = @Index(columnList = "name"))
	public static class EntityWithIndex {
		@Id
		public Integer id;
		public String name;
	}
}
