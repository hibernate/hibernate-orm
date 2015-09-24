/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.schemaupdate;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.id.enhanced.TableStructure;
import org.hibernate.mapping.Table;
import org.hibernate.tool.schema.extract.internal.DatabaseInformationImpl;
import org.hibernate.tool.schema.extract.spi.DatabaseInformation;
import org.hibernate.tool.schema.internal.TargetDatabaseImpl;
import org.hibernate.tool.schema.internal.TargetStdoutImpl;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.hibernate.tool.schema.spi.Target;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
public class SchemaUpdateTableBackedSequenceTest extends BaseUnitTestCase {
	private StandardServiceRegistry ssr;

	@Before
	public void before() {
		ssr = new StandardServiceRegistryBuilder().build();
	}

	@After
	public void after() {
		StandardServiceRegistryBuilder.destroy( ssr );
	}

	@Test
	public void testCreateTableOnUpdate() throws SQLException {
		Metadata metadata = new MetadataSources( ssr ).buildMetadata();

		Database database = metadata.getDatabase();

		TableStructure tableStructure = new TableStructure(
				database.getJdbcEnvironment(),
				new QualifiedTableName( null, null, Identifier.toIdentifier( "test_seq" ) ),
				Identifier.toIdentifier( "nextval" ),
				20,
				30,
				Long.class
		);
		tableStructure.registerExportables( database );

		// lets make sure the InitCommand is there
		assertEquals( 1, database.getDefaultNamespace().getTables().size() );
		Table table = database.getDefaultNamespace().getTables().iterator().next();
		assertEquals( 1, table.getInitCommands().size() );


		class TargetImpl extends TargetStdoutImpl {
			boolean found = false;
			@Override
			public void accept(String action) {
				super.accept( action );
				if ( action.startsWith( "insert into test_seq" ) ) {
					found = true;
				}

			}
		}

		TargetImpl target = new TargetImpl();

		DatabaseInformation dbInfo = new DatabaseInformationImpl(
				ssr,
				database.getJdbcEnvironment(),
				ssr.getService( JdbcServices.class ).getBootstrapJdbcConnectionAccess(),
				database.getDefaultNamespace().getPhysicalName().getCatalog(),
				database.getDefaultNamespace().getPhysicalName().getSchema()
		);

		ssr.getService( SchemaManagementTool.class ).getSchemaMigrator( Collections.emptyMap() ).doMigration(
				metadata,
				dbInfo,
				true,
				Arrays.asList( target, new TargetDatabaseImpl( ssr.getService( JdbcServices.class ).getBootstrapJdbcConnectionAccess() ) )
		);

		assertTrue( target.found );
		
		ssr.getService( SchemaManagementTool.class ).getSchemaDropper( null ).doDrop(
				metadata,
				false,
				Arrays.asList( target, new TargetDatabaseImpl( ssr.getService( JdbcServices.class ).getBootstrapJdbcConnectionAccess() ) )
		);
	}
}
