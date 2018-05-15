/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.schemaupdate;

import java.sql.SQLException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.id.enhanced.TableStructure;
import org.hibernate.mapping.Table;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.schema.internal.ExceptionHandlerLoggedImpl;
import org.hibernate.tool.schema.spi.ExceptionHandler;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.hibernate.tool.schema.spi.ScriptTargetOutput;
import org.hibernate.tool.schema.spi.TargetDescriptor;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
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
		ssr = new StandardServiceRegistryBuilder()
				.applySetting( AvailableSettings.FORMAT_SQL, false )
				.build();
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

		final TargetImpl target = new TargetImpl();

		ssr.getService( SchemaManagementTool.class ).getSchemaMigrator( Collections.emptyMap() ).doMigration(
				metadata,
				new ExecutionOptions() {
					@Override
					public boolean shouldManageNamespaces() {
						return true;
					}

					@Override
					public Map getConfigurationValues() {
						return ssr.getService( ConfigurationService.class ).getSettings();
					}

					@Override
					public ExceptionHandler getExceptionHandler() {
						return ExceptionHandlerLoggedImpl.INSTANCE;
					}
				},
				new TargetDescriptor() {
					@Override
					public EnumSet<TargetType> getTargetTypes() {
						return EnumSet.of( TargetType.SCRIPT, TargetType.DATABASE );
					}

					@Override
					public ScriptTargetOutput getScriptTargetOutput() {
						return target;
					}
				}
		);

		assertTrue( target.found );

		new SchemaExport().drop( EnumSet.of( TargetType.DATABASE ), metadata );
	}

	class TargetImpl implements ScriptTargetOutput {
		boolean found = false;

		@Override
		public void prepare() {

		}

		@Override
		public void accept(String action) {
			if ( action.startsWith( "insert into test_seq" ) ) {
				found = true;
			}
		}

		@Override
		public void release() {

		}
	}
}
