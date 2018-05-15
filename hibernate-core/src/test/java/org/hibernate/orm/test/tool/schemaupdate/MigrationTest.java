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
import javax.persistence.Table;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.orm.test.tool.BaseSchemaUnitTestCase;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.DialectFeatureChecks;
import org.hibernate.testing.junit5.RequiresDialectFeature;
import org.hibernate.testing.junit5.schema.SchemaScope;
import org.hibernate.testing.junit5.schema.SchemaTest;

/**
 * @author Max Rydahl Andersen
 * @author Brett Meyer
 */
@RequiresDialectFeature( feature = DialectFeatureChecks.SupportSchemaCreation.class)
public class MigrationTest extends BaseSchemaUnitTestCase {

	@Override
	protected void applySettings(StandardServiceRegistryBuilder serviceRegistryBuilder) {
		serviceRegistryBuilder.applySetting( AvailableSettings.HBM2DLL_CREATE_SCHEMAS, "true" );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { CustomerInfo.class, PersonInfo.class };
	}

	@SchemaTest
	@TestForIssue(jiraKey = "HHH-9550")
	public void testSameTableNameDifferentExplicitSchemas(SchemaScope schemaScope) {
		// drop and then create the schema
		schemaScope.withSchemaExport( schemaExport ->
								  schemaExport.execute( EnumSet.of( TargetType.DATABASE ), SchemaExport.Action.BOTH ) );

		// update the schema
		schemaScope.withSchemaUpdate( schemaUpdate ->
								  schemaUpdate.setHaltOnError( true ).execute( EnumSet.of( TargetType.DATABASE ) ) );
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

