/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.export.mapping;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.engine.jdbc.env.JdbcMetadataOnBoot;
import org.hibernate.tool.reveng.internal.util.DummyDialect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class HbmToMetadataTest {

	@TempDir
	private File tempDir;

	private File simpleHbmFile;

	@BeforeEach
	public void beforeEach() throws Exception {
		simpleHbmFile = new File( tempDir, "simple.mapping.hbm" );
		Files.writeString(simpleHbmFile.toPath(), SIMPLE_HBM_XML);
	}

	@Test
	public void testBuildMetadata() throws Exception {
		StandardServiceRegistryBuilder ssrb = new StandardServiceRegistryBuilder();
		ssrb.clearSettings();
		ssrb.applySetting( JdbcSettings.ALLOW_METADATA_ON_BOOT, JdbcMetadataOnBoot.DISALLOW );
		ssrb.applySetting(JdbcSettings.DIALECT, DummyDialect.class.getName());
		MetadataSources sources = new MetadataSources(ssrb.build());
		sources.addFile(  simpleHbmFile );
		assertNotNull(sources.buildMetadata());
	}


	private static final String SIMPLE_HBM_XML =
			"""
					<hibernate-mapping>
						<class name="Foo">
							<id name="id" type="long"/>
							<property name="name" type="string"/>
						</class>
					</hibernate-mapping>
				""";

}
