/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate.idgenerator;

import java.io.File;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Andrea Boriero
 */
@RequiresDialect(H2Dialect.class)
public class TableGeneratorTest extends BaseUnitTestCase {

	private StandardServiceRegistry ssr;
	private File output;
	private MetadataImplementor metadata;
	private static final int INITIAL_VALUE = 5;
	private static final int EXPECTED_DB_INSERTED_VALUE = INITIAL_VALUE;

	@Before
	public void setUp() throws Exception {
		ssr = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( Environment.HBM2DDL_AUTO, "none" )
				.build();

		output = File.createTempFile( "update_script", ".sql" );
		output.deleteOnExit();

		metadata = (MetadataImplementor) new MetadataSources( ssr )
				.addAnnotatedClass( TestEntity.class )
				.buildMetadata();
		metadata.orderColumns( true );
		metadata.validate();
	}

	@Test
	public void testTableGeneratorIsGenerated() throws Exception {
		new SchemaExport()
				.setOutputFile( output.getAbsolutePath() )
				.create( EnumSet.of( TargetType.SCRIPT, TargetType.DATABASE ), metadata );

		final List<String> commands = Files.readAllLines( output.toPath() );

		final String expectedTestEntityTableCreationCommand = "CREATE TABLE TEST_ENTITY \\(ID .*, PRIMARY KEY \\(ID\\)\\);";
		assertTrue(
				"The command '" + expectedTestEntityTableCreationCommand + "' has not been correctly generated",
				isCommandGenerated( commands, expectedTestEntityTableCreationCommand )
		);

		final String expectedIdTableGeneratorCreationCommand = "CREATE TABLE ID_TABLE_GENERATOR \\(VALUE .*, PK .*, PRIMARY KEY \\(PK\\)\\);";

		assertTrue(
				"The command '" + expectedIdTableGeneratorCreationCommand + "' has not been correctly generated",

				isCommandGenerated(
						commands,
						expectedIdTableGeneratorCreationCommand
				)
		);

		final String expectedInsertIntoTableGeneratorCommand = "INSERT INTO ID_TABLE_GENERATOR\\(PK, VALUE\\) VALUES \\('TEST_ENTITY_ID'," + EXPECTED_DB_INSERTED_VALUE + "\\);";

		assertTrue(
				"The command '" + expectedInsertIntoTableGeneratorCommand + "' has not been correctly generated",
				isCommandGenerated(
						commands,
						expectedInsertIntoTableGeneratorCommand
				)
		);
	}

	@After
	public void tearDown() {
		try {
			new SchemaExport().drop( EnumSet.of( TargetType.DATABASE ), metadata );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Entity(name = "TestEntity")
	@Table(name = "TEST_ENTITY")
	@TableGenerator(name = "tableGenerator",
			table = "ID_TABLE_GENERATOR",
			pkColumnName = "PK",
			pkColumnValue = "TEST_ENTITY_ID",
			valueColumnName = "VALUE",
			allocationSize = 3,
			initialValue = INITIAL_VALUE)
	public static class TestEntity {
		Long id;

		@Id
		@GeneratedValue(strategy = GenerationType.TABLE, generator = "tableGenerator")
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
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

}
