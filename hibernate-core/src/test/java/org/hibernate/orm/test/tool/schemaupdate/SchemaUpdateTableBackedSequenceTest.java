/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.tool.schemaupdate;

import java.util.EnumSet;
import java.util.Map;

import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.MappedTable;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.id.enhanced.TableStructure;
import org.hibernate.naming.Identifier;
import org.hibernate.naming.QualifiedTableName;
import org.hibernate.orm.test.tool.BaseSchemaUnitTestCase;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.schema.internal.ExceptionHandlerLoggedImpl;
import org.hibernate.tool.schema.spi.ExceptionHandler;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.ScriptTargetOutput;
import org.hibernate.tool.schema.spi.TargetDescriptor;

import org.hibernate.testing.junit5.schema.SchemaScope;
import org.hibernate.testing.junit5.schema.SchemaTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
public class SchemaUpdateTableBackedSequenceTest extends BaseSchemaUnitTestCase {

	@Override
	protected void applySettings(StandardServiceRegistryBuilder serviceRegistryBuilder) {
		serviceRegistryBuilder.applySetting( AvailableSettings.FORMAT_SQL, false );
	}

	@Override
	protected void afterMetadataCreation(MetadataImplementor metadata) {
		Database database = getMetadata().getDatabase();

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
		MappedTable table = database.getDefaultNamespace().getTables().iterator().next();
		assertEquals( 1, table.getInitCommands().size() );
	}

	@SchemaTest
	public void testCreateTableOnUpdate(SchemaScope schemaScope) {
		final TargetImpl target = new TargetImpl();
		schemaScope.withSchemaMigrator( schemaMigrator ->
									schemaMigrator.doMigration(
											new TestExecutionOptions(),
											new TestTargetDescriptor( target )
									) );

		assertTrue( target.found );
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

	class TestExecutionOptions implements ExecutionOptions {
		@Override
		public boolean shouldManageNamespaces() {
			return true;
		}

		@Override
		public Map getConfigurationValues() {
			return getStandardServiceRegistry().getService( ConfigurationService.class ).getSettings();
		}

		@Override
		public ExceptionHandler getExceptionHandler() {
			return ExceptionHandlerLoggedImpl.INSTANCE;
		}
	}

	class TestTargetDescriptor implements TargetDescriptor {
		private TargetImpl target;

		public TestTargetDescriptor(TargetImpl target) {
			this.target = target;
		}

		@Override
		public EnumSet<TargetType> getTargetTypes() {
			return EnumSet.of( TargetType.SCRIPT, TargetType.DATABASE );
		}

		@Override
		public ScriptTargetOutput getScriptTargetOutput() {
			return target;
		}
	}
}
