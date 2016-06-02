/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.schemaupdate.idbag;

import java.io.File;
import java.nio.file.Files;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.Environment;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.hbm2ddl.Target;

import org.junit.Test;

import org.hibernate.testing.TestForIssue;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-10373")
public class IdBagSequenceTest {

	@Test
	public void testIdBagSequenceGeneratorIsCreated() throws Exception {
		StandardServiceRegistry ssr = new StandardServiceRegistryBuilder()
				.applySetting( Environment.HBM2DDL_AUTO, "none" )
				.build();
		try {
			File output = File.createTempFile( "update_script", ".sql" );
			output.deleteOnExit();

			final MetadataImplementor metadata = (MetadataImplementor) new MetadataSources( ssr )
					.addResource( "org/hibernate/test/schemaupdate/idbag/Mappings.hbm.xml" )
					.buildMetadata();
			metadata.validate();

			final SchemaUpdate schemaUpdate = new SchemaUpdate( metadata );
			schemaUpdate.setHaltOnError( true );
			schemaUpdate.setOutputFile( output.getAbsolutePath() );
			schemaUpdate.setDelimiter( ";" );
			schemaUpdate.setFormat( true );
			schemaUpdate.execute( Target.SCRIPT );

			String fileContent = new String( Files.readAllBytes( output.toPath() ) );
			assertThat( fileContent.toLowerCase().contains( "create sequence seq_child_id" ), is( true ) );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

}
