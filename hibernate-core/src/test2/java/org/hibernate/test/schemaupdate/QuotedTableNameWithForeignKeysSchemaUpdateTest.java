/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.schemaupdate;

import java.util.EnumSet;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-10197")
public class QuotedTableNameWithForeignKeysSchemaUpdateTest extends BaseUnitTestCase {

	@Before
	public void setUp() {
		StandardServiceRegistry ssr = new StandardServiceRegistryBuilder().build();
		try {
			final MetadataImplementor metadata = (MetadataImplementor) new MetadataSources( ssr )
					.addResource( "org/hibernate/test/schemaupdate/UserGroup.hbm.xml" )
					.buildMetadata();
			metadata.validate();
			new SchemaUpdate().execute( EnumSet.of( TargetType.DATABASE ), metadata );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Test
	public void testUpdateExistingSchema() {
		StandardServiceRegistry ssr = new StandardServiceRegistryBuilder().build();
		try {
			final MetadataImplementor metadata = (MetadataImplementor) new MetadataSources( ssr )
					.addResource( "org/hibernate/test/schemaupdate/UserGroup.hbm.xml" )
					.buildMetadata();
			new SchemaUpdate().execute( EnumSet.of( TargetType.DATABASE ), metadata );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Test
	public void testGeneratingUpdateScript() {
		StandardServiceRegistry ssr = new StandardServiceRegistryBuilder().build();
		try {

			final MetadataImplementor metadata = (MetadataImplementor) new MetadataSources( ssr )
					.addResource( "org/hibernate/test/schemaupdate/UserGroup.hbm.xml" )
					.buildMetadata();
			new SchemaUpdate().execute( EnumSet.of( TargetType.STDOUT ), metadata );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@After
	public void tearDown() {
		StandardServiceRegistry ssr = new StandardServiceRegistryBuilder().build();
		try {

			final MetadataImplementor metadata = (MetadataImplementor) new MetadataSources( ssr )
					.addResource( "org/hibernate/test/schemaupdate/UserGroup.hbm.xml" )
					.buildMetadata();
			new SchemaExport().drop( EnumSet.of( TargetType.STDOUT, TargetType.DATABASE ), metadata );

		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}
}
