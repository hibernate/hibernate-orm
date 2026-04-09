/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2024-2025 Red Hat, Inc.
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
package org.hibernate.tool.orm.jbt.api.wrp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hibernate.tool.reveng.internal.reveng.strategy.TableFilter;
import org.hibernate.tool.orm.jbt.internal.factory.TableFilterWrapperFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TableFilterWrapperTest {

	private TableFilter wrappedTableFilter = null;
	private TableFilterWrapper tableFilterWrapper = null;
	
	@BeforeEach
	public void beforeEach() {
		tableFilterWrapper = TableFilterWrapperFactory.createTableFilterWrapper();
		wrappedTableFilter = (TableFilter)tableFilterWrapper.getWrappedObject();
	}
	
	@Test
	public void testConstruction() {
		assertNotNull(wrappedTableFilter);
		assertNotNull(tableFilterWrapper);
	}
	
	@Test
	public void testSetExclude() {
		assertNull(wrappedTableFilter.getExclude());
		tableFilterWrapper.setExclude(true);
		assertTrue(wrappedTableFilter.getExclude());
	}
	
	@Test
	public void testSetMatchCatalog() {
		assertNotEquals("foo", wrappedTableFilter.getMatchCatalog());
		tableFilterWrapper.setMatchCatalog("foo");
		assertEquals("foo", wrappedTableFilter.getMatchCatalog());
	}
		
	@Test
	public void testSetMatchSchema() {
		assertNotEquals("foo", wrappedTableFilter.getMatchSchema());
		tableFilterWrapper.setMatchSchema("foo");
		assertEquals("foo", wrappedTableFilter.getMatchSchema());
	}
		
	@Test
	public void testSetMatchName() {
		assertNotEquals("foo", wrappedTableFilter.getMatchName());
		tableFilterWrapper.setMatchName("foo");
		assertEquals("foo", wrappedTableFilter.getMatchName());
	}
		
	@Test
	public void testGetExclude() {
		assertNull(tableFilterWrapper.getExclude());
		wrappedTableFilter.setExclude(true);
		assertTrue(tableFilterWrapper.getExclude());
	}
		
	@Test
	public void testGetMatchCatalog() {
		assertNotEquals("foo", tableFilterWrapper.getMatchCatalog());
		wrappedTableFilter.setMatchCatalog("foo");
		assertEquals("foo", tableFilterWrapper.getMatchCatalog());
	}
		
	@Test
	public void testGetMatchSchema() {
		assertNotEquals("foo", tableFilterWrapper.getMatchSchema());
		wrappedTableFilter.setMatchSchema("foo");
		assertEquals("foo", tableFilterWrapper.getMatchSchema());
	}
		
	@Test
	public void testGetMatchName() {
		assertNotEquals("foo", tableFilterWrapper.getMatchName());
		wrappedTableFilter.setMatchName("foo");
		assertEquals("foo", tableFilterWrapper.getMatchName());
	}
		
}
