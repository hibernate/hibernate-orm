/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.ServiceRegistryFunctionalTesting;
import org.hibernate.testing.orm.junit.ServiceRegistryProducer;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.schema.JdbcMetadataAccessStrategy;
import org.hibernate.tool.schema.TargetType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;


import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.cfg.MappingSettings.KEYWORD_AUTO_QUOTING_ENABLED;
import static org.hibernate.cfg.SchemaToolingSettings.HBM2DDL_JDBC_METADATA_EXTRACTOR_STRATEGY;

/**
 * @author Andrea Boriero
 */
@ParameterizedClass
@MethodSource("parameters")
@TestInstance( TestInstance.Lifecycle.PER_METHOD )
@ServiceRegistryFunctionalTesting
@DomainModel(annotatedClasses = ColumnNamesTest.Employee.class)
public class ColumnNamesTest implements ServiceRegistryProducer {
	public static Collection<JdbcMetadataAccessStrategy> parameters() {
		return List.of(
				JdbcMetadataAccessStrategy.GROUPED,
				JdbcMetadataAccessStrategy.INDIVIDUALLY
		);
	}

	private final File output;
	private final JdbcMetadataAccessStrategy jdbcMetadataExtractorStrategy;

	public ColumnNamesTest(
			JdbcMetadataAccessStrategy jdbcMetadataExtractorStrategy,
			@TempDir File outputDir) {
		this.jdbcMetadataExtractorStrategy = jdbcMetadataExtractorStrategy;
		this.output = new File( outputDir, "update_script.sql" );
	}

	@Override
	public StandardServiceRegistry produceServiceRegistry(StandardServiceRegistryBuilder builder) {
		return ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( KEYWORD_AUTO_QUOTING_ENABLED, "true" )
				.applySetting( HBM2DDL_JDBC_METADATA_EXTRACTOR_STRATEGY, jdbcMetadataExtractorStrategy )
				.build();
	}

	@BeforeEach
	public void setUp(DomainModelScope modelScope) {
		new SchemaExport().create( EnumSet.of( TargetType.DATABASE ), modelScope.getDomainModel() );
	}

	@AfterEach
	public void tearDown(DomainModelScope modelScope) {
		new SchemaExport().drop( EnumSet.of( TargetType.DATABASE ), modelScope.getDomainModel() );
	}

	@Test
	public void testSchemaUpdateWithQuotedColumnNames(DomainModelScope modelScope) throws IOException {
		new SchemaUpdate()
				.setOutputFile( output.getAbsolutePath() )
				.execute( EnumSet.of( TargetType.SCRIPT ), modelScope.getDomainModel() );

		final String fileContent = new String( Files.readAllBytes( output.toPath() ) );
		assertThat( fileContent ).as( "The update output file should be empty" ).isEmpty();
	}

	@Entity
	@Table(name = "Employee")
	public static class Employee {
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
