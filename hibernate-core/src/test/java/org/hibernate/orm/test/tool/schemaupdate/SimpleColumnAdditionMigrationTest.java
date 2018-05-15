/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.tool.schemaupdate;

import java.util.EnumSet;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.metamodel.model.relational.spi.DatabaseModel;
import org.hibernate.orm.test.tool.BaseSchemaUnitTestCase;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.schema.internal.Helper;

import org.hibernate.testing.junit5.schema.SchemaScope;
import org.hibernate.testing.junit5.schema.SchemaTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Andrea Boriero
 */
public class SimpleColumnAdditionMigrationTest extends BaseSchemaUnitTestCase {
	private static final String RESOURCE_2 = "org/hibernate/orm/test/tool/schemaupdate/2_Version.hbm.xml";

	@Override
	protected String[] getHmbMappingFiles() {
		return new String[] { "tool/schemaupdate/1_Version.hbm.xml" };
	}

	@Override
	protected boolean dropSchemaAfterTest() {
		return false;
	}

	@SchemaTest
	public void testSimpleColumnAddition(SchemaScope scope) {
		scope.withSchemaUpdate( schemaUpdate -> {
			schemaUpdate.execute( EnumSet.of( TargetType.DATABASE, TargetType.STDOUT ) );
			assertEquals( 0, schemaUpdate.getExceptions().size() );
		} );

		final DatabaseModel v2DatabaseModel = createDatabaseModel( RESOURCE_2 );
		final SchemaUpdate v2schemaUpdate = new SchemaUpdate( v2DatabaseModel, getStandardServiceRegistry() );
		v2schemaUpdate.execute( EnumSet.of( TargetType.DATABASE, TargetType.STDOUT ) );
		assertEquals( 0, v2schemaUpdate.getExceptions().size() );

		new SchemaExport( v2DatabaseModel, getStandardServiceRegistry() ).drop( EnumSet.of( TargetType.DATABASE ) );
	}

	private DatabaseModel createDatabaseModel(String resource1) {
		final MetadataImplementor v1metadata = (MetadataImplementor) new MetadataSources( getStandardServiceRegistry() )
				.addResource( resource1 )
				.buildMetadata();

		return Helper.buildDatabaseModel( v1metadata );
	}

//	/**
//	 * 3_Version.hbm.xml contains a named unique constraint and an un-named
//	 * unique constraint (will receive a randomly-generated name).  Create
//	 * the original schema with 2_Version.hbm.xml.  Then, run SchemaUpdate
//	 * TWICE using 3_Version.hbm.xml.  Neither RECREATE_QUIETLY nor SKIP should
//	 * generate any exceptions.
//	 */
//	@Test
//	@TestForIssue( jiraKey = "HHH-8162" )
//	public void testConstraintUpdate() {
//		doConstraintUpdate(UniqueConstraintSchemaUpdateStrategy.DROP_RECREATE_QUIETLY);
//		doConstraintUpdate(UniqueConstraintSchemaUpdateStrategy.RECREATE_QUIETLY);
//		doConstraintUpdate(UniqueConstraintSchemaUpdateStrategy.SKIP);
//	}
//
//	private void doConstraintUpdate(UniqueConstraintSchemaUpdateStrategy strategy) {
//		// original
//		String resource1 = "org/hibernate/test/schemaupdate/2_Version.hbm.xml";
//		// adds unique constraint
//		String RESOURCE_2 = "org/hibernate/test/schemaupdate/3_Version.hbm.xml";
//
//		MetadataImplementor v1metadata = (MetadataImplementor) new MetadataSources( serviceRegistry )
//				.addResource( resource1 )
//				.buildMetadata();
//		MetadataImplementor v2metadata = (MetadataImplementor) new MetadataSources( serviceRegistry )
//				.addResource( RESOURCE_2 )
//				.buildMetadata();
//
//		new SchemaExport( v1metadata ).execute( false, true, true, false );
//
//		// adds unique constraint
//		Configuration v2cfg = new Configuration();
//		v2cfg.getProperties().put( AvailableSettings.UNIQUE_CONSTRAINT_SCHEMA_UPDATE_STRATEGY, strategy );
//		v2cfg.addResource( RESOURCE_2 );
//		SchemaUpdate v2schemaUpdate = new SchemaUpdate( serviceRegistry, v2cfg );
//		v2schemaUpdate.execute( true, true );
//		assertEquals( 0, v2schemaUpdate.getExceptions().size() );
//
//		Configuration v3cfg = new Configuration();
//		v3cfg.getProperties().put( AvailableSettings.UNIQUE_CONSTRAINT_SCHEMA_UPDATE_STRATEGY, strategy );
//		v3cfg.addResource( RESOURCE_2 );
//		SchemaUpdate v3schemaUpdate = new SchemaUpdate( serviceRegistry, v3cfg );
//		v3schemaUpdate.execute( true, true );
//		assertEquals( 0, v3schemaUpdate.getExceptions().size() );
//
//		new SchemaExport( serviceRegistry, v3cfg ).drop( false, true );
//	}
}
