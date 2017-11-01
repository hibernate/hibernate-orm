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
import javax.persistence.Table;

import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

/**
 * @author Max Rydahl Andersen
 * @author Brett Meyer
 */
public class MigrationTest extends BaseSchemaUnitTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{CustomerInfo.class, PersonInfo.class};
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9550")
	public void testSameTableNameDifferentExplicitSchemas() {
		// drop and then create the schema
		createSchemaExport().execute( EnumSet.of( TargetType.DATABASE ), SchemaExport.Action.BOTH );

		// update the schema
		createSchemaUpdate().setHaltOnError( true ).execute( EnumSet.of( TargetType.DATABASE ) );
	}

	@Entity
	@Table(name = "PERSON", schema = "CRM")
	public static class CustomerInfo {
		@Id
		private Integer id;
	}

	@Entity
	@Table(name = "PERSON", schema = "ERP")
	public static class PersonInfo {
		@Id
		private Integer id;
	}
}

