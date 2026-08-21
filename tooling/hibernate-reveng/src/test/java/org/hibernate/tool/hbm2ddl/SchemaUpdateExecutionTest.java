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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SchemaUpdateExecutionTest {

	private StandardServiceRegistry serviceRegistry;
	private Metadata metadata;

	@BeforeEach
	public void setUp() {
		serviceRegistry = new StandardServiceRegistryBuilder()
				.applySetting("hibernate.dialect", "org.hibernate.dialect.H2Dialect")
				.applySetting("hibernate.connection.driver_class", "org.h2.Driver")
				.applySetting("hibernate.connection.url", "jdbc:h2:mem:schema_update_test;DB_CLOSE_DELAY=-1")
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
	public void testUpdateToDatabase() {
		SchemaUpdate update = new SchemaUpdate();
		update.execute(EnumSet.of(TargetType.DATABASE), metadata);
		assertTrue(update.getExceptions().isEmpty());
	}

	@Test
	public void testUpdateToDatabaseTwice() {
		SchemaUpdate update = new SchemaUpdate();
		// First update creates the schema
		update.execute(EnumSet.of(TargetType.DATABASE), metadata);
		assertTrue(update.getExceptions().isEmpty());
		// Second update should be a no-op (schema already exists)
		update.execute(EnumSet.of(TargetType.DATABASE), metadata);
		assertTrue(update.getExceptions().isEmpty());
	}

	@Test
	public void testUpdateToScript(@TempDir File tempDir) throws Exception {
		File scriptFile = new File(tempDir, "update.sql");
		SchemaUpdate update = new SchemaUpdate();
		update.setOutputFile(scriptFile.getAbsolutePath());
		update.setDelimiter(";");
		update.setFormat(true);
		update.execute(EnumSet.of(TargetType.SCRIPT), metadata);

		assertTrue(update.getExceptions().isEmpty());
		assertTrue(scriptFile.exists());
	}

	@Test
	public void testUpdateToScriptAndDatabase(@TempDir File tempDir) throws Exception {
		File scriptFile = new File(tempDir, "update.sql");
		SchemaUpdate update = new SchemaUpdate();
		update.setOutputFile(scriptFile.getAbsolutePath());
		update.execute(EnumSet.of(TargetType.DATABASE, TargetType.SCRIPT), metadata);

		assertTrue(update.getExceptions().isEmpty());
		assertTrue(scriptFile.exists());
	}

	@Test
	public void testUpdateToStdout() {
		SchemaUpdate update = new SchemaUpdate();
		update.execute(EnumSet.of(TargetType.STDOUT), metadata);
		assertTrue(update.getExceptions().isEmpty());
	}

	@Test
	public void testUpdateWithEmptyTargets() {
		SchemaUpdate update = new SchemaUpdate();
		update.execute(EnumSet.noneOf(TargetType.class), metadata);
		assertTrue(update.getExceptions().isEmpty());
	}

	@Test
	public void testGetExceptions() {
		SchemaUpdate update = new SchemaUpdate();
		assertNotNull(update.getExceptions());
		assertTrue(update.getExceptions().isEmpty());
	}

	@Test
	public void testSetHaltOnError() {
		SchemaUpdate update = new SchemaUpdate();
		update.setHaltOnError(true);
		update.execute(EnumSet.of(TargetType.DATABASE), metadata);
		assertTrue(update.getExceptions().isEmpty());
	}

	@Test
	public void testSetOverrideOutputFileContent(@TempDir File tempDir) throws Exception {
		File scriptFile = new File(tempDir, "update.sql");
		SchemaUpdate update = new SchemaUpdate();
		update.setOutputFile(scriptFile.getAbsolutePath());
		update.setOverrideOutputFileContent();
		update.execute(EnumSet.of(TargetType.SCRIPT), metadata);
		assertTrue(update.getExceptions().isEmpty());
	}

	@Test
	public void testFluentApi() {
		SchemaUpdate update = new SchemaUpdate();
		SchemaUpdate result = update
				.setHaltOnError(false)
				.setFormat(true)
				.setDelimiter(";")
				.setOutputFile("test.sql")
				.setOverrideOutputFileContent();
		assertNotNull(result);
	}

	@Test
	public void testBuildMetadataFromMainArgs(@TempDir File tempDir) throws Exception {
		File propsFile = new File(tempDir, "hibernate.properties");
		Files.writeString(propsFile.toPath(),
				"hibernate.dialect=org.hibernate.dialect.H2Dialect\n" +
				"hibernate.connection.driver_class=org.h2.Driver\n" +
				"hibernate.connection.url=jdbc:h2:mem:update_main_args_test\n" +
				"hibernate.connection.username=sa\n" +
				"hibernate.connection.password=\n" +
				"hibernate.default_schema=\n" +
				"hibernate.default_catalog=\n");

		File hbmFile = new File(tempDir, "HelloWorld.hbm.xml");
		try (var in = getClass().getResourceAsStream("/org/hibernate/tool/reveng/hbm2x/DdlExporterTest/HelloWorld.hbm.xml")) {
			Files.copy(in, hbmFile.toPath());
		}

		MetadataImplementor md = SchemaUpdate.buildMetadataFromMainArgs(new String[]{
				"--properties=" + propsFile.getAbsolutePath(),
				hbmFile.getAbsolutePath()
		});
		assertNotNull(md);
	}

	@Test
	public void testMainWithCommandLineArgs(@TempDir File tempDir) throws Exception {
		File propsFile = new File(tempDir, "hibernate.properties");
		Files.writeString(propsFile.toPath(),
				"hibernate.dialect=org.hibernate.dialect.H2Dialect\n" +
				"hibernate.connection.driver_class=org.h2.Driver\n" +
				"hibernate.connection.url=jdbc:h2:mem:update_main_test\n" +
				"hibernate.connection.username=sa\n" +
				"hibernate.connection.password=\n" +
				"hibernate.default_schema=\n" +
				"hibernate.default_catalog=\n");

		File hbmFile = new File(tempDir, "HelloWorld.hbm.xml");
		try (var in = getClass().getResourceAsStream("/org/hibernate/tool/reveng/hbm2x/DdlExporterTest/HelloWorld.hbm.xml")) {
			Files.copy(in, hbmFile.toPath());
		}

		File outputFile = new File(tempDir, "output.sql");
		SchemaUpdate.main(new String[]{
				"--properties=" + propsFile.getAbsolutePath(),
				"--output=" + outputFile.getAbsolutePath(),
				"--delimiter=;",
				"--target=script",
				hbmFile.getAbsolutePath()
		});
		assertTrue(outputFile.exists());
	}

	@Test
	public void testMainWithLegacyFlags(@TempDir File tempDir) throws Exception {
		File propsFile = new File(tempDir, "hibernate.properties");
		Files.writeString(propsFile.toPath(),
				"hibernate.dialect=org.hibernate.dialect.H2Dialect\n" +
				"hibernate.connection.driver_class=org.h2.Driver\n" +
				"hibernate.connection.url=jdbc:h2:mem:update_legacy_test\n" +
				"hibernate.connection.username=sa\n" +
				"hibernate.connection.password=\n" +
				"hibernate.default_schema=\n" +
				"hibernate.default_catalog=\n");

		File hbmFile = new File(tempDir, "HelloWorld.hbm.xml");
		try (var in = getClass().getResourceAsStream("/org/hibernate/tool/reveng/hbm2x/DdlExporterTest/HelloWorld.hbm.xml")) {
			Files.copy(in, hbmFile.toPath());
		}

		// Test legacy flags: --text, --quiet
		SchemaUpdate.main(new String[]{
				"--properties=" + propsFile.getAbsolutePath(),
				"--text",
				"--quiet",
				hbmFile.getAbsolutePath()
		});
	}
}
