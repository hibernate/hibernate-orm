/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.hbm.uk;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.unique.DefaultUniqueDelegate;
import org.hibernate.dialect.unique.UniqueDelegate;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.relational.spi.DatabaseModel;
import org.hibernate.metamodel.model.relational.spi.ExportableTable;
import org.hibernate.metamodel.model.relational.spi.UniqueKey;
import org.hibernate.tool.schema.internal.Helper;
import org.hibernate.tool.schema.internal.SchemaCreatorImpl;
import org.hibernate.tool.schema.internal.SchemaDropperImpl;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.test.hbm.index.JournalingSchemaToolingTarget;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * @author Steve Ebersole
 */
public class UniqueDelegateTest extends BaseUnitTestCase {
	private static int getColumnDefinitionUniquenessFragmentCallCount = 0;
	private static int getTableCreationUniqueConstraintsFragmentCallCount = 0;
	private static int getAlterTableToAddUniqueKeyCommandCallCount = 0;
	private static int getAlterTableToDropUniqueKeyCommandCallCount = 0;

	private StandardServiceRegistry ssr;

	@Before
	public void before() {
		final StandardServiceRegistryBuilder standardServiceRegistryBuilder = new StandardServiceRegistryBuilder()
				.applySetting( AvailableSettings.DIALECT, MyDialect.class );
		standardServiceRegistryBuilder.getSettings();
		ssr = standardServiceRegistryBuilder
				.build();
	}

	@After
	public void after() {
		if ( ssr != null ) {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-10203" )
	public void testUniqueDelegateConsulted() {
		final Metadata metadata = new MetadataSources( ssr )
				.addResource( "org/hibernate/test/hbm/uk/person_unique.hbm.xml" )
				.buildMetadata();

		final DatabaseModel databaseModel = Helper.buildDatabaseModel( (MetadataImplementor) metadata );

		final JournalingSchemaToolingTarget target = new JournalingSchemaToolingTarget();
		new SchemaCreatorImpl( databaseModel, ssr ).doCreation( false, target );

		assertThat( getAlterTableToAddUniqueKeyCommandCallCount, equalTo( 1 ) );
		assertThat( getColumnDefinitionUniquenessFragmentCallCount, equalTo( 1 ) );
		assertThat( getTableCreationUniqueConstraintsFragmentCallCount, equalTo( 1 ) );

		new SchemaDropperImpl( databaseModel, ssr ).doDrop( false, target );

		// unique keys are not dropped explicitly
		assertThat( getAlterTableToAddUniqueKeyCommandCallCount, equalTo( 1 ) );
		assertThat( getColumnDefinitionUniquenessFragmentCallCount, equalTo( 1 ) );
		assertThat( getTableCreationUniqueConstraintsFragmentCallCount, equalTo( 1 ) );
	}

	public static class MyDialect extends H2Dialect {
		private MyUniqueDelegate myUniqueDelegate;

		public MyDialect() {
			this.myUniqueDelegate = new MyUniqueDelegate( this );
		}

		@Override
		public UniqueDelegate getUniqueDelegate() {
			return myUniqueDelegate;
		}
	}

	public static class MyUniqueDelegate extends DefaultUniqueDelegate {

		/**
		 * Constructs DefaultUniqueDelegate
		 *
		 * @param dialect The dialect for which we are handling unique constraints
		 */
		public MyUniqueDelegate(Dialect dialect) {
			super( dialect );
		}

		@Override
		public String getColumnDefinitionUniquenessFragment(Column column) {
			getColumnDefinitionUniquenessFragmentCallCount++;
			return super.getColumnDefinitionUniquenessFragment( column );
		}

		@Override
		public String getTableCreationUniqueConstraintsFragment(ExportableTable table) {
			getTableCreationUniqueConstraintsFragmentCallCount++;
			return super.getTableCreationUniqueConstraintsFragment( table );
		}

		@Override
		public String getAlterTableToAddUniqueKeyCommand(UniqueKey uniqueKey, JdbcServices jdbcServices) {
			getAlterTableToAddUniqueKeyCommandCallCount++;
			return super.getAlterTableToAddUniqueKeyCommand( uniqueKey, jdbcServices );
		}

		@Override
		public String getAlterTableToDropUniqueKeyCommand(UniqueKey uniqueKey, JdbcServices jdbcServices) {
			getAlterTableToDropUniqueKeyCommandCallCount++;
			return super.getAlterTableToDropUniqueKeyCommand( uniqueKey, jdbcServices );
		}
	}
}
