/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.hbm.uk;

import java.util.Collections;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.unique.DefaultUniqueDelegate;
import org.hibernate.dialect.unique.UniqueDelegate;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;
import org.hibernate.tool.schema.spi.SchemaCreator;
import org.hibernate.tool.schema.spi.SchemaDropper;
import org.hibernate.tool.schema.spi.SchemaManagementTool;

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
		ssr = new StandardServiceRegistryBuilder()
				.applySetting( AvailableSettings.DIALECT, MyDialect.class )
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

		final JournalingSchemaToolingTarget target = new JournalingSchemaToolingTarget();
		final SchemaCreator schemaCreator = ssr.getService( SchemaManagementTool.class ).getSchemaCreator( Collections.emptyMap() );
		schemaCreator.doCreation( metadata, false, target );

		assertThat( getAlterTableToAddUniqueKeyCommandCallCount, equalTo( 1 ) );
		assertThat( getColumnDefinitionUniquenessFragmentCallCount, equalTo( 1 ) );
		assertThat( getTableCreationUniqueConstraintsFragmentCallCount, equalTo( 1 ) );

		final SchemaDropper schemaDropper = ssr.getService( SchemaManagementTool.class ).getSchemaDropper( Collections.emptyMap() );
		schemaDropper.doDrop( metadata, false, target );

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
		public String getTableCreationUniqueConstraintsFragment(Table table) {
			getTableCreationUniqueConstraintsFragmentCallCount++;
			return super.getTableCreationUniqueConstraintsFragment( table );
		}

		@Override
		public String getAlterTableToAddUniqueKeyCommand(
				UniqueKey uniqueKey, Metadata metadata) {
			getAlterTableToAddUniqueKeyCommandCallCount++;
			return super.getAlterTableToAddUniqueKeyCommand( uniqueKey, metadata );
		}

		@Override
		public String getAlterTableToDropUniqueKeyCommand(
				UniqueKey uniqueKey, Metadata metadata) {
			getAlterTableToDropUniqueKeyCommandCallCount++;
			return super.getAlterTableToDropUniqueKeyCommand( uniqueKey, metadata );
		}
	}
}
