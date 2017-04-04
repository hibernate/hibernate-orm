/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.schemaupdate.foreignkeys.definition;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.EnumSet;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Vlad MIhalcea
 */

public abstract class AbstractForeignKeyDefinitionTest extends BaseUnitTestCase {

	private File output;

	private StandardServiceRegistry ssr;

	private MetadataImplementor metadata;

	@Before
	public void setUp() throws IOException {
		output = File.createTempFile( "update_script", ".sql" );
		output.deleteOnExit();
		ssr = new StandardServiceRegistryBuilder().build();
		createSchema();
	}

	@After
	public void tearsDown() {
		StandardServiceRegistryBuilder.destroy( ssr );
	}

	private void createSchema() {
		final MetadataSources metadataSources = new MetadataSources( ssr );

		for ( Class c : getAnnotatedClasses() ) {
			metadataSources.addAnnotatedClass( c );
		}
		metadata = (MetadataImplementor) metadataSources.buildMetadata();
		metadata.validate();
		new SchemaExport()
				.setHaltOnError( true )
				.setOutputFile( output.getAbsolutePath() )
				.setFormat( false )
				.create( EnumSet.of( TargetType.SCRIPT ), metadata );
	}

	protected abstract Class<?>[] getAnnotatedClasses();

	protected abstract boolean validate(String fileContent);

	@Test
	@TestForIssue(jiraKey = "HHH-10643")
	public void testForeignKeyDefinitionOverridesDefaultNamingStrategy()
			throws Exception {
		String fileContent = new String( Files.readAllBytes( output.toPath() ) );
		assertTrue( "Script file : " + fileContent, validate( fileContent ) );
	}

}
