/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate.inheritance.tableperclass;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.sql.Types;
import java.util.EnumSet;
import java.util.List;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.util.ServiceRegistryUtil;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
public class SchemaCreationTest {
	private File output;
	private StandardServiceRegistry ssr;
	private MetadataImplementor metadata;
	private Dialect dialect;

	@Before
	public void setUp() throws IOException {
		output = File.createTempFile( "update_script", ".sql" );
		output.deleteOnExit();
		ssr = ServiceRegistryUtil.serviceRegistry();
		dialect = ssr.getService(JdbcEnvironment.class).getDialect();
	}

	@After
	public void tearsDown() {
		StandardServiceRegistryBuilder.destroy( ssr );
	}

	@Test
	@JiraKey(value = "HHH-10553")
	public void testUniqueConstraintIsCorrectlyGenerated() throws Exception {

		final MetadataSources metadataSources = new MetadataSources( ssr );

		metadataSources.addAnnotatedClass( Element.class );
		metadataSources.addAnnotatedClass( Category.class );
		metadata = (MetadataImplementor) metadataSources.buildMetadata();
		metadata.orderColumns( false );
		metadata.validate();
		final SchemaExport schemaExport = new SchemaExport(  )
				.setHaltOnError( true )
				.setOutputFile( output.getAbsolutePath() )
				.setFormat( false );
		schemaExport.create( EnumSet.of( TargetType.SCRIPT ), metadata );

		final List<String> sqlLines = Files.readAllLines( output.toPath(), Charset.defaultCharset() );

		boolean isUniqueConstraintCreated = false;
		for ( String statement : sqlLines ) {
			statement = statement.toLowerCase();
			assertThat(
					"Should not try to create the unique constraint for the non existing table element",
					statement.matches( dialect.getAlterTableString( "element" ) ),
					is( false )
			);
			String varchar255 = metadata.getTypeConfiguration().getDdlTypeRegistry()
					.getTypeName(Types.VARCHAR,255L,0,0);
			isUniqueConstraintCreated = isUniqueConstraintCreated
					|| statement.startsWith("create unique index")
						&& statement.contains("category (code)")
					|| statement.startsWith("create unique nonclustered index")
					&& statement.contains("category (code)")
					|| statement.startsWith("alter table if exists category add constraint ")
						&& statement.contains("unique (code)")
					|| statement.startsWith("alter table category add constraint ")
						&& statement.contains("unique (code)")
					|| statement.startsWith("create table category")
						&& statement.contains("code " + varchar255 + " not null unique")
					|| statement.startsWith("create table category")
						&& statement.contains("unique(code)");
		}

		assertThat(
				"Unique constraint for table category is not created",
				isUniqueConstraintCreated,
				is( true )
		);
	}
}
