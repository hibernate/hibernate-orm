/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.schema.JdbcMetadataAccessStrategy;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
@RunWith(Parameterized.class)
public class ColumnNamesTest {
	@Parameterized.Parameters
	public static Collection<String> parameters() {
		return Arrays.asList(
				new String[] {JdbcMetadataAccessStrategy.GROUPED.toString(), JdbcMetadataAccessStrategy.INDIVIDUALLY.toString()}
		);
	}

	@Parameterized.Parameter
	public String jdbcMetadataExtractorStrategy;

	private StandardServiceRegistry ssr;
	private Metadata metadata;
	private File output;

	@Before
	public void setUp() throws IOException {
		ssr = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.KEYWORD_AUTO_QUOTING_ENABLED, "true" )
				.applySetting( AvailableSettings.HBM2DDL_JDBC_METADATA_EXTRACTOR_STRATEGY, jdbcMetadataExtractorStrategy )
				.build();
		output = File.createTempFile( "update_script", ".sql" );
		output.deleteOnExit();

		metadata = new MetadataSources( ssr )
				.addAnnotatedClass( Employee.class )
				.buildMetadata();
		new SchemaExport().create( EnumSet.of( TargetType.DATABASE ), metadata );
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

	@Test
	public void testSchemaUpdateWithQuotedColumnNames() throws Exception {
		new SchemaUpdate()
				.setOutputFile( output.getAbsolutePath() )
				.execute(
						EnumSet.of( TargetType.SCRIPT ),
						metadata
				);

		final String fileContent = new String( Files.readAllBytes( output.toPath() ) );
		assertThat( "The update output file should be empty", fileContent, is( "" ) );
	}

	@Entity
	@Table(name = "Employee")
	public class Employee {
		@Id
		private long id;

		@Column(name = "`Age`")
		public String age;

		@Column(name = "Name")
		private String name;

		private String match;

		private String birthday;

		private String homeAddress;
	}
}
