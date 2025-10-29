/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.schema.TargetType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hibernate.cfg.SchemaToolingSettings.JAKARTA_HBM2DDL_CREATE_SCHEMAS;

/**
 * @author Andrea Boriero
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@ServiceRegistry(settings = @Setting(name = JAKARTA_HBM2DDL_CREATE_SCHEMAS, value = "true"))
public class QuotedTableNameSchemaUpdateTest {

	@Test
	@JiraKey(value = "HHH-10820")
	@DisabledOnOs(value = OS.WINDOWS, disabledReason = "On Windows, MySQL is case insensitive!")
	public void testSchemaUpdateWithQuotedTableName(
			ServiceRegistryScope registryScope,
			@TempDir File tempDir) throws Exception {
		var output = new File( tempDir, "update_script.sql" );

		var metadata = (MetadataImplementor) new MetadataSources( registryScope.getRegistry() )
				.addAnnotatedClass( QuotedTable.class )
				.buildMetadata();
		metadata.orderColumns( false );
		metadata.validate();

		new SchemaExport()
				.setOutputFile( output.getAbsolutePath() )
				.setFormat( false )
				.create( EnumSet.of( TargetType.DATABASE ), metadata );

		new SchemaUpdate().setHaltOnError( true )
				.setOutputFile( output.getAbsolutePath() )
				.setFormat( false )
				.execute( EnumSet.of( TargetType.DATABASE, TargetType.SCRIPT ), metadata );

		final List<String> sqlLines = Files.readAllLines( output.toPath(), Charset.defaultCharset() );
		assertThat( "The update should recognize the existing table", sqlLines.isEmpty(), is( true ) );

		new SchemaExport().setHaltOnError( true )
				.setOutputFile( output.getAbsolutePath() )
				.setFormat( false )
				.drop( EnumSet.of( TargetType.DATABASE ), metadata );
	}

	@Entity(name = "QuotedTable")
	@Table(name = "\"QuotedTable\"")
	public static class QuotedTable {
		@Id
		long id;
	}
}
