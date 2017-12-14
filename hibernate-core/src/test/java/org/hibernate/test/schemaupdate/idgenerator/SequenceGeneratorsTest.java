/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.schemaupdate.idgenerator;

import java.io.File;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.SequenceGenerators;
import javax.persistence.Table;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
@RequiresDialect(H2Dialect.class)
public class SequenceGeneratorsTest extends BaseUnitTestCase {
	private StandardServiceRegistry ssr;
	private File output;
	private MetadataImplementor metadata;

	@Before
	public void setUp() throws Exception {
		ssr = new StandardServiceRegistryBuilder()
				.applySetting( Environment.HBM2DDL_AUTO, "none" )
				.build();
		output = File.createTempFile( "update_script", ".sql" );
		output.deleteOnExit();
		metadata = (MetadataImplementor) new MetadataSources( ssr )
				.addAnnotatedClass( TestEntity.class )
				.buildMetadata();
		metadata.validate();
	}

	@Test
	public void testSequenceIsGenerated() throws Exception {
		new SchemaExport()
				.setOutputFile( output.getAbsolutePath() )
				.create( EnumSet.of( TargetType.SCRIPT, TargetType.DATABASE ), metadata );

		List<String> commands = Files.readAllLines( output.toPath() );

		assertThat(
				isCommandGenerated( commands, "CREATE TABLE TEST_ENTITY \\(ID .*, PRIMARY KEY \\(ID\\)\\)" ),
				is( true )
		);

		assertThat(
				isCommandGenerated( commands, "CREATE SEQUENCE SEQUENCE_GENERATOR START WITH 5 INCREMENT BY 3" ),
				is( true )
		);
	}

	@After
	public void tearDown() {
		try {
			new SchemaExport()
					.drop( EnumSet.of( TargetType.DATABASE ), metadata );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}

	}

	private boolean isCommandGenerated(List<String> commands, String expectedCommnad) {
		final Pattern pattern = Pattern.compile( expectedCommnad.toLowerCase() );
		for ( String command : commands ) {
			Matcher matcher = pattern.matcher( command.toLowerCase() );
			if ( matcher.matches() ) {
				return true;
			}
		}
		return false;
	}


	@Entity(name = "TestEntity")
	@Table(name = "TEST_ENTITY")
	public static class TestEntity {
		Long id;

		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQUENCEGENERATOR")
		@SequenceGenerators({
				@SequenceGenerator(
						name = "SEQUENCEGENERATOR",
						allocationSize = 3,
						initialValue = 5,
						sequenceName = "SEQUENCE_GENERATOR")
		})
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}
}
