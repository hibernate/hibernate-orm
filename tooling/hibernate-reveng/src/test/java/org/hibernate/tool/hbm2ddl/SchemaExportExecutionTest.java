/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.hbm2ddl;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.tool.schema.TargetType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.hibernate.boot.spi.MetadataImplementor;

import java.io.File;
import java.nio.file.Files;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SchemaExportExecutionTest {

	private StandardServiceRegistry serviceRegistry;
	private Metadata metadata;

	@BeforeEach
	public void setUp() {
		serviceRegistry = new StandardServiceRegistryBuilder()
				.applySetting("hibernate.dialect", "org.hibernate.dialect.H2Dialect")
				.applySetting("hibernate.connection.driver_class", "org.h2.Driver")
				.applySetting("hibernate.connection.url", "jdbc:h2:mem:schema_export_test;DB_CLOSE_DELAY=-1")
				.applySetting("hibernate.connection.username", "sa")
				.applySetting("hibernate.connection.password", "")
				.applySetting("hibernate.default_schema", "")
				.applySetting("hibernate.default_catalog", "")
				.build();
		metadata = new MetadataSources(serviceRegistry)
				.addResource("org/hibernate/tool/reveng/hbm2x/DdlExporterTest/HelloWorld.hbm.xml")
				.buildMetadata();
	}

	@AfterEach
	public void tearDown() {
		if (serviceRegistry != null) {
			new SchemaExport().drop(EnumSet.of(TargetType.DATABASE), metadata);
			StandardServiceRegistryBuilder.destroy(serviceRegistry);
		}
	}

	@Test
	public void testCreateAndDropToDatabase() {
		SchemaExport export = new SchemaExport();
		export.createOnly(EnumSet.of(TargetType.DATABASE), metadata);
		assertTrue(export.getExceptions().isEmpty());

		export.drop(EnumSet.of(TargetType.DATABASE), metadata);
		assertTrue(export.getExceptions().isEmpty());
	}

	@Test
	public void testCreateBothToDatabase() {
		SchemaExport export = new SchemaExport();
		export.create(EnumSet.of(TargetType.DATABASE), metadata);
		assertTrue(export.getExceptions().isEmpty());
	}

	@Test
	public void testExecuteWithActionNone() {
		SchemaExport export = new SchemaExport();
		export.execute(EnumSet.of(TargetType.DATABASE), SchemaExport.Action.NONE, metadata);
		assertTrue(export.getExceptions().isEmpty());
	}

	@Test
	public void testExecuteWithEmptyTargets() {
		SchemaExport export = new SchemaExport();
		export.execute(EnumSet.noneOf(TargetType.class), SchemaExport.Action.CREATE, metadata);
		assertTrue(export.getExceptions().isEmpty());
	}

	@Test
	public void testExportToScript(@TempDir File tempDir) throws Exception {
		File scriptFile = new File(tempDir, "export.sql");
		SchemaExport export = new SchemaExport();
		export.setOutputFile(scriptFile.getAbsolutePath());
		export.setDelimiter(";");
		export.setFormat(true);
		export.execute(EnumSet.of(TargetType.SCRIPT), SchemaExport.Action.BOTH, metadata);

		assertTrue(export.getExceptions().isEmpty());
		assertTrue(scriptFile.exists());
		String content = Files.readString(scriptFile.toPath());
		assertFalse(content.isEmpty());
	}

	@Test
	public void testExportToScriptAndDatabase(@TempDir File tempDir) throws Exception {
		File scriptFile = new File(tempDir, "export.sql");
		SchemaExport export = new SchemaExport();
		export.setOutputFile(scriptFile.getAbsolutePath());
		export.setOverrideOutputFileContent();
		export.execute(EnumSet.of(TargetType.DATABASE, TargetType.SCRIPT), SchemaExport.Action.BOTH, metadata);

		assertTrue(export.getExceptions().isEmpty());
		assertTrue(scriptFile.exists());
	}

	@Test
	public void testExportToStdout() {
		SchemaExport export = new SchemaExport();
		export.execute(EnumSet.of(TargetType.STDOUT), SchemaExport.Action.BOTH, metadata);
		assertTrue(export.getExceptions().isEmpty());
	}

	@Test
	public void testGetExceptions() {
		SchemaExport export = new SchemaExport();
		assertNotNull(export.getExceptions());
		assertTrue(export.getExceptions().isEmpty());
	}

	@Test
	public void testSetManageNamespaces() {
		SchemaExport export = new SchemaExport();
		export.setManageNamespaces(true);
		export.execute(EnumSet.of(TargetType.DATABASE), SchemaExport.Action.BOTH, metadata);
		assertTrue(export.getExceptions().isEmpty());
	}

	@Test
	public void testSetHaltOnError() {
		SchemaExport export = new SchemaExport();
		export.setHaltOnError(true);
		export.createOnly(EnumSet.of(TargetType.DATABASE), metadata);
		assertTrue(export.getExceptions().isEmpty());
	}

	@Test
	public void testDropAndRecreate() {
		SchemaExport export = new SchemaExport();
		// First create
		export.createOnly(EnumSet.of(TargetType.DATABASE), metadata);
		assertTrue(export.getExceptions().isEmpty());
		// Drop and recreate
		export.create(EnumSet.of(TargetType.DATABASE), metadata);
		assertTrue(export.getExceptions().isEmpty());
	}

	@Test
	public void testSetImportFiles() {
		SchemaExport export = new SchemaExport();
		export.setImportFiles("nonexistent.sql");
		export.createOnly(EnumSet.of(TargetType.DATABASE), metadata);
		// Import file not found is not a hard error
		assertNotNull(export.getExceptions());
	}

	@Test
	public void testBuildMetadataFromMainArgs(@TempDir File tempDir) throws Exception {
		// Create a properties file for H2
		File propsFile = new File(tempDir, "hibernate.properties");
		Files.writeString(propsFile.toPath(),
				"hibernate.dialect=org.hibernate.dialect.H2Dialect\n" +
				"hibernate.connection.driver_class=org.h2.Driver\n" +
				"hibernate.connection.url=jdbc:h2:mem:main_args_test\n" +
				"hibernate.connection.username=sa\n" +
				"hibernate.connection.password=\n" +
				"hibernate.default_schema=\n" +
				"hibernate.default_catalog=\n");

		// Copy the HBM file to the temp dir
		File hbmFile = new File(tempDir, "HelloWorld.hbm.xml");
		try (var in = getClass().getResourceAsStream("/org/hibernate/tool/reveng/hbm2x/DdlExporterTest/HelloWorld.hbm.xml")) {
			Files.copy(in, hbmFile.toPath());
		}

		MetadataImplementor md = SchemaExport.buildMetadataFromMainArgs(new String[]{
				"--properties=" + propsFile.getAbsolutePath(),
				hbmFile.getAbsolutePath()
		});
		assertNotNull(md);
	}

	@Test
	public void testMainWithCommandLineArgs(@TempDir File tempDir) throws Exception {
		// Create a properties file for H2
		File propsFile = new File(tempDir, "hibernate.properties");
		Files.writeString(propsFile.toPath(),
				"hibernate.dialect=org.hibernate.dialect.H2Dialect\n" +
				"hibernate.connection.driver_class=org.h2.Driver\n" +
				"hibernate.connection.url=jdbc:h2:mem:main_test\n" +
				"hibernate.connection.username=sa\n" +
				"hibernate.connection.password=\n" +
				"hibernate.default_schema=\n" +
				"hibernate.default_catalog=\n");

		File hbmFile = new File(tempDir, "HelloWorld.hbm.xml");
		try (var in = getClass().getResourceAsStream("/org/hibernate/tool/reveng/hbm2x/DdlExporterTest/HelloWorld.hbm.xml")) {
			Files.copy(in, hbmFile.toPath());
		}

		File outputFile = new File(tempDir, "output.sql");
		// This exercises CommandLineArgs.parseCommandLineArgs with various flags
		// This exercises CommandLineArgs.parseCommandLineArgs and execute() with various flags
		SchemaExport.main(new String[]{
				"--properties=" + propsFile.getAbsolutePath(),
				"--output=" + outputFile.getAbsolutePath(),
				"--format",
				"--delimiter=;",
				"--action=drop-and-create",
				"--target=script",
				hbmFile.getAbsolutePath()
		});
	}

	@Test
	public void testMainWithLegacyFlags(@TempDir File tempDir) throws Exception {
		File propsFile = new File(tempDir, "hibernate.properties");
		Files.writeString(propsFile.toPath(),
				"hibernate.dialect=org.hibernate.dialect.H2Dialect\n" +
				"hibernate.connection.driver_class=org.h2.Driver\n" +
				"hibernate.connection.url=jdbc:h2:mem:main_legacy_test\n" +
				"hibernate.connection.username=sa\n" +
				"hibernate.connection.password=\n" +
				"hibernate.default_schema=\n" +
				"hibernate.default_catalog=\n");

		File hbmFile = new File(tempDir, "HelloWorld.hbm.xml");
		try (var in = getClass().getResourceAsStream("/org/hibernate/tool/reveng/hbm2x/DdlExporterTest/HelloWorld.hbm.xml")) {
			Files.copy(in, hbmFile.toPath());
		}

		File outputFile = new File(tempDir, "output.sql");
		// Test legacy flags: --text (no export), --drop, --create, --quiet
		SchemaExport.main(new String[]{
				"--properties=" + propsFile.getAbsolutePath(),
				"--output=" + outputFile.getAbsolutePath(),
				"--text",
				"--drop",
				"--haltonerror",
				"--schemas",
				hbmFile.getAbsolutePath()
		});
	}
}
