/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.jdbc2cfg.Performance;

import org.hibernate.boot.Metadata;
import org.hibernate.mapping.Table;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tool.reveng.test.utils.JUnitUtil;
import org.hibernate.tool.reveng.test.utils.JdbcUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author max
 * @author koen
 */
public class TestCase {

	private static final int TABLECOUNT = 200;
	private static final int COLCOUNT = 10;

	@BeforeEach
	public void setUp() {
		JdbcUtil.createDatabase(this);
	}

	@AfterEach
	public void tearDown() {
		JdbcUtil.dropDatabase(this);
	}

	@Test
	public void testBasic() {
		Metadata metadata = MetadataDescriptorFactory
				.createReverseEngineeringDescriptor(null, null)
				.createMetadata();
		JUnitUtil.assertIteratorContainsExactly(
				"There should be " + TABLECOUNT + " tables!",
				metadata.collectTableMappings().iterator(),
				TABLECOUNT);
		Table tab = metadata.collectTableMappings().iterator().next();
		assertEquals(COLCOUNT+1, tab.getColumnSpan());
	}

}
