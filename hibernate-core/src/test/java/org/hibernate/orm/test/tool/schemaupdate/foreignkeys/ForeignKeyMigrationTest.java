/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.tool.schemaupdate.foreignkeys;

import java.util.EnumSet;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.orm.test.tool.BaseSchemaUnitTestCase;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.DialectFeatureChecks;
import org.hibernate.testing.junit5.RequiresDialectFeature;
import org.hibernate.testing.junit5.schema.SchemaScope;
import org.hibernate.testing.junit5.schema.SchemaTest;

/**
 * @author Steve Ebersole
 */
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportCatalogCreation.class)
public class ForeignKeyMigrationTest extends BaseSchemaUnitTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Box.class, Thing.class };
	}

	@SchemaTest
	@TestForIssue(jiraKey = "HHH-9716")
	public void testMigrationOfForeignKeys(SchemaScope schemaScope) {
		// first create the schema...
		schemaScope.withSchemaExport( schemaExport ->
											  schemaExport.create( EnumSet.of( TargetType.DATABASE ) ) );

		// try to update the just created schema
		schemaScope.withSchemaUpdate( schemaUpdate -> schemaUpdate.execute( EnumSet.of( TargetType.DATABASE ) ) );
	}

	@Entity(name = "Box")
	@Table(name = "Box", schema = "PUBLIC", catalog = "DB1")
	public static class Box {
		@Id
		public Integer id;
		@ManyToOne
		@JoinColumn
		public Thing thing1;
	}

	@Entity(name = "Thing")
	@Table(name = "Thing", schema = "PUBLIC", catalog = "DB1")
	public static class Thing {
		@Id
		public Integer id;
	}
}
