/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.schemaupdate;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.EnumSet;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.junit4.CustomRunner;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.schema.spi.SchemaManagementException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Vlad Mihalcea
 * @author Gail Badner
 */
@SkipForDialect(value = DB2Dialect.class, comment = "DB2 is far more resistant to the reserved keyword usage. See HHH-12832.")
@RunWith(CustomRunner.class)
public class SchemaUpdateHaltOnErrorTest {

	private File output;
	private StandardServiceRegistry ssr;
	private MetadataImplementor metadata;

	@Before
	public void setUp() throws IOException {
		output = File.createTempFile( "update_script", ".sql" );
		output.deleteOnExit();
		ssr = new StandardServiceRegistryBuilder().build();

		final MetadataSources metadataSources = new MetadataSources( ssr )
				.addAnnotatedClass( From.class );
		metadata = (MetadataImplementor) metadataSources.buildMetadata();
	}

	@After
	public void tearsDown() {
		// there shouldn't be anything to clean up
		StandardServiceRegistryBuilder.destroy( ssr );
	}

	@Test
	public void testHaltOnError() {
		try {
			new SchemaUpdate().setHaltOnError( true )
					.execute( EnumSet.of( TargetType.DATABASE ), metadata );
			fail( "Should halt on error!" );
		}
		catch ( Exception e ) {
			SchemaManagementException cause = (SchemaManagementException) e;

			assertTrue( cause.getMessage().startsWith( "Halting on error : Error executing DDL" ) );
			assertTrue( cause.getMessage().endsWith( "via JDBC Statement" ) );
		}
	}

	@Entity(name = "From")
	public class From {

		@Id
		private Integer id;

		private String table;

		private String select;
	}
}