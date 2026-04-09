/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2004-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.jdbc2cfg.Performance;

import org.hibernate.boot.Metadata;
import org.hibernate.mapping.Table;
import org.hibernate.tool.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tool.test.utils.JUnitUtil;
import org.hibernate.tool.test.utils.JdbcUtil;
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
