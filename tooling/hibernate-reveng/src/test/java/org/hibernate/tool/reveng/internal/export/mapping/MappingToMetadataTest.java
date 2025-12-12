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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class MappingToMetadataTest {

	@TempDir
	private File tempDir;

	private File simpleMappingFile;

	@BeforeEach
	public void beforeEach() throws Exception {
		simpleMappingFile = new File( tempDir, "simple.mapping.hbm" );
		Files.writeString(simpleMappingFile.toPath(), SIMPLE_MAPPING_XML);
	}


	@Disabled
	@Test
	public void testBuildMetadata() throws Exception {
		StandardServiceRegistryBuilder ssrb = new StandardServiceRegistryBuilder();
		ssrb.clearSettings();
		ssrb.applySetting( JdbcSettings.ALLOW_METADATA_ON_BOOT, JdbcMetadataOnBoot.DISALLOW );
		ssrb.applySetting(JdbcSettings.DIALECT, DummyDialect.class.getName());
		MetadataSources sources = new MetadataSources(ssrb.build());
		sources.addFile(  simpleMappingFile );
		assertNotNull(sources.buildMetadata());
	}

	private static final String SIMPLE_MAPPING_XML =
			"""
					<?xml version="1.0" encoding="UTF-8"?>
					<entity-mappings xmlns="http://www.hibernate.org/xsd/orm/mapping">
						<attribute-accessor>property</attribute-accessor>
						<default-cascade>none</default-cascade>
						<default-lazy>true</default-lazy>
						<entity class="Foo" metadata-complete="false">
							<table name="Foo"/>
							<dynamic-insert>false</dynamic-insert>
							<dynamic-update>false</dynamic-update>
							<select-before-update>false</select-before-update>
							<batch-size>-1</batch-size>
							<mutable>true</mutable>
							<optimistic-locking>VERSION</optimistic-locking>
							<attributes>
								<id name="id">
									<column insertable="true" name="id" nullable="false" unique="false" updatable="false"/>
									<java-type>org.hibernate.type.descriptor.java.LongJavaType</java-type>
									<jdbc-type>org.hibernate.type.descriptor.jdbc.BigIntJdbcType</jdbc-type>
								</id>
								<basic fetch="EAGER" name="name" optimistic-lock="true" optional="true">
									<java-type>org.hibernate.type.descriptor.java.StringJavaType</java-type>
									<jdbc-type>org.hibernate.type.descriptor.jdbc.VarcharJdbcType</jdbc-type>
								</basic>
							</attributes>
						</entity>
					</entity-mappings>""";

}
