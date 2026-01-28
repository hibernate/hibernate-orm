/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.jdbc2cfg.SearchEscapeString;

import org.hibernate.boot.Metadata;
import org.hibernate.mapping.Table;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tool.reveng.test.utils.HibernateUtil;
import org.hibernate.tool.reveng.test.utils.JUnitUtil;
import org.hibernate.tool.reveng.test.utils.JdbcUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author max
 * @author koen
 */
public class TestCase {

	private Metadata metadata = null;

	@BeforeEach
	public void setUp() {
		JdbcUtil.createDatabase(this);
		metadata = MetadataDescriptorFactory
				.createReverseEngineeringDescriptor(null, null)
				.createMetadata();
	}

	@AfterEach
	public void tearDown() {
		JdbcUtil.dropDatabase(this);
	}

	@Test
	public void testBasic() {

		JUnitUtil.assertIteratorContainsExactly(
				"There should be 2 tables!",
				metadata.collectTableMappings().iterator(),
				2);

		Table table = HibernateUtil.getTable(metadata, JdbcUtil.toIdentifier(this, "B_TAB" ) );
		Table table2 = HibernateUtil.getTable(metadata, JdbcUtil.toIdentifier(this, "B2TAB" ) );

		assertNotNull(table);
		assertNotNull(table2);

		assertEquals(2, table.getColumnSpan());
		assertEquals(2, table2.getColumnSpan());

	}

}
