/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.jdbc2cfg.Versioning;

import org.hibernate.boot.Metadata;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptor;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tool.reveng.test.utils.JdbcUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * To be extended by VersioningForJDK50Test for the JPA generation part
 * @author max
 * @author koen
 */
public class TestCase {

	private Metadata metadata = null;
	private MetadataDescriptor metadataDescriptor = null;

	@BeforeEach
	public void setUp() {
		JdbcUtil.createDatabase(this);
		metadataDescriptor = MetadataDescriptorFactory
				.createReverseEngineeringDescriptor(null, null);
		metadata = metadataDescriptor
				.createMetadata();
	}

	@AfterEach
	public void tearDown() {
		JdbcUtil.dropDatabase(this);
	}

	@Test
	public void testVersion() {
		PersistentClass cl = metadata.getEntityBinding("WithVersion");
		Property version = cl.getVersion();
		assertNotNull(version);
		assertEquals("version", version.getName());
		cl = metadata.getEntityBinding("NoVersion");
		assertNotNull(cl);
		version = cl.getVersion();
		assertNull(version);
	}

}
