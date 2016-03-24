/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.schemaupdate.inheritance.tableperclass;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.List;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.testing.TestForIssue;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
public class SchemaCreationTest {
	private File output;
	private StandardServiceRegistry ssr;
	private MetadataImplementor metadata;

	@Before
	public void setUp() throws IOException {
		output = File.createTempFile( "update_script", ".sql" );
		output.deleteOnExit();
		ssr = new StandardServiceRegistryBuilder().build();
	}

	@After
	public void tearsDown() {
		StandardServiceRegistryBuilder.destroy( ssr );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-10553")
	public void testUniqueConstraintIsCorrectlyGenerated() throws Exception {

		final MetadataSources metadataSources = new MetadataSources( ssr );

		metadataSources.addAnnotatedClass( Element.class );
		metadataSources.addAnnotatedClass( Category.class );
		metadata = (MetadataImplementor) metadataSources.buildMetadata();
		metadata.validate();
		final SchemaExport schemaExport = new SchemaExport(  )
				.setHaltOnError( true )
				.setOutputFile( output.getAbsolutePath() )
				.setFormat( false );
		schemaExport.create( EnumSet.of( TargetType.SCRIPT ), metadata );

		final List<String> sqlLines = Files.readAllLines( output.toPath(), Charset.defaultCharset() );

		boolean isUniqueConstraintCreated = false;
		for ( String statement : sqlLines ) {
			assertThat(
					"Should not try to create the unique constraint for the non existing table element",
					statement.toLowerCase().contains( "alter table element" ),
					is( false )
			);
			if (ssr.getService(JdbcEnvironment.class).getDialect() instanceof DB2Dialect) {
				if (statement.toLowerCase().startsWith("create unique index")
						&& statement.toLowerCase().contains("category (code)")) {
					isUniqueConstraintCreated = true;
				}
			} else {
				if (statement.toLowerCase().startsWith("alter table category add constraint")
						&& statement.toLowerCase().contains("unique (code)")) {
					isUniqueConstraintCreated = true;
				}
			}
		}

		assertThat(
				"Unique constraint for table category is not created",
				isUniqueConstraintCreated,
				is( true )
		);
	}
}
