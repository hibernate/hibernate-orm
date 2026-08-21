/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate.inheritance.tableperclass;

import org.hamcrest.MatcherAssert;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.sql.Types;
import java.util.EnumSet;
import java.util.List;

import static org.hamcrest.core.Is.is;

/**
 * @author Andrea Boriero
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@ServiceRegistry
@DomainModel(annotatedClasses = {Element.class, Category.class})
public class SchemaCreationTest {

	@Test
	@JiraKey(value = "HHH-10553")
	public void testUniqueConstraintIsCorrectlyGenerated(
			ServiceRegistryScope registryScope,
			DomainModelScope modelScope,
			@TempDir File tmpDir) throws Exception {
		final var scriptFile = new File( tmpDir, "update_script.sql" );

		final var metadata = modelScope.getDomainModel();
		metadata.orderColumns( false );
		metadata.validate();

		final SchemaExport schemaExport = new SchemaExport(  )
				.setHaltOnError( true )
				.setOutputFile( scriptFile.getAbsolutePath() )
				.setFormat( false );
		schemaExport.create( EnumSet.of( TargetType.SCRIPT ), metadata );

		final List<String> sqlLines = Files.readAllLines( scriptFile.toPath(), Charset.defaultCharset() );

		final var dialect = registryScope.getRegistry().requireService( JdbcEnvironment.class ).getDialect();

		boolean isUniqueConstraintCreated = false;
		for ( String statement : sqlLines ) {
			statement = statement.toLowerCase();
			MatcherAssert.assertThat(
					"Should not try to create the unique constraint for the non existing table element",
					statement.matches( dialect.getAlterTableString( "element" ) ), is( false ) );
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

		MatcherAssert.assertThat( "Unique constraint for table category is not created", isUniqueConstraintCreated,
				is( true ) );
	}
}
