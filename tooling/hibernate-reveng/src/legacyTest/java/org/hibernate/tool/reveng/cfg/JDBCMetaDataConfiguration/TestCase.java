/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.cfg.JDBCMetaDataConfiguration;

import org.hibernate.boot.Metadata;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tool.reveng.test.utils.HibernateUtil;
import org.hibernate.tool.reveng.test.utils.JdbcUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author max
 * @author koen
 */
public class TestCase {

	@BeforeEach
	public void setUp() {
		JdbcUtil.createDatabase(this);
	}

	@AfterEach
	public void tearDown() {
		JdbcUtil.dropDatabase(this);
	}

	@Test
	public void testReadFromJDBC() {
		Metadata metadata = MetadataDescriptorFactory
				.createReverseEngineeringDescriptor(null, null)
				.createMetadata();
		assertNotNull(metadata.getEntityBinding("WithRealTimestamp"), "WithRealTimestamp");
		assertNotNull(metadata.getEntityBinding("NoVersion"), "NoVersion");
		assertNotNull(metadata.getEntityBinding("WithFakeTimestamp"), "WithFakeTimestamp");
		assertNotNull(metadata.getEntityBinding("WithVersion"), "WithVersion");
	}

	@Test
	public void testGetTable() {
		assertNotNull(
				HibernateUtil.getTable(
						MetadataDescriptorFactory
							.createReverseEngineeringDescriptor(null, null)
							.createMetadata(),
						JdbcUtil.toIdentifier(this, "WITH_REAL_TIMESTAMP")));
	}

}
