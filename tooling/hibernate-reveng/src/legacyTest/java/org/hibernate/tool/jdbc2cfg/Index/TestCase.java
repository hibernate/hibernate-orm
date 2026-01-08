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
package org.hibernate.tool.jdbc2cfg.Index;

import org.hibernate.boot.Metadata;
import org.hibernate.mapping.*;
import org.hibernate.tool.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tool.test.utils.HibernateUtil;
import org.hibernate.tool.test.utils.JdbcUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.*;

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
	public void testUniqueKey() {	
		Table table = HibernateUtil.getTable(
				metadata, 
				JdbcUtil.toIdentifier(this, "WITH_INDEX") );		
		UniqueKey uniqueKey = table.getUniqueKey(
				JdbcUtil.toIdentifier(this, "OTHER_IDX") );
		assertNotNull(uniqueKey);
		assertEquals(1, uniqueKey.getColumnSpan() );	
		Column keyCol = uniqueKey.getColumn(0);
		assertTrue(keyCol.isUnique() );
		assertSame(keyCol, table.getColumn(keyCol) );
	}
	
	@Test
	public void testWithIndex() {		
		Table table = HibernateUtil.getTable(
				metadata, 
				JdbcUtil.toIdentifier(this, "WITH_INDEX"));
		assertEquals(
				JdbcUtil.toIdentifier(this, "WITH_INDEX"), 
				JdbcUtil.toIdentifier(this, table.getName()));	
		assertNull(table.getPrimaryKey(), "there should be no pk" );
		Iterator<Index> iterator = table.getIndexes().values().iterator();
		int cnt=0;
		while(iterator.hasNext() ) {
			iterator.next();
			cnt++;
		}
		assertEquals(1, cnt);	
		Index index = table.getIndex(JdbcUtil.toIdentifier(this, "MY_INDEX") );
		assertNotNull(index, "No index ?");
		assertEquals(
				JdbcUtil.toIdentifier(this, "MY_INDEX"), 
				JdbcUtil.toIdentifier(this, index.getName()));	
		assertEquals(2, index.getColumnSpan() );	
		assertSame(index.getTable(), table);
		Iterator<Selectable> cols = index.getSelectables().iterator();
		Column col1 = (Column)cols.next();
		Column col2 = (Column)cols.next();
		assertEquals(
				JdbcUtil.toIdentifier(this, "ONE"), 
				JdbcUtil.toIdentifier(this, col1.getName()));
		assertEquals(
				JdbcUtil.toIdentifier(this, "THREE"), 
				JdbcUtil.toIdentifier(this, col2.getName()));		
		Column example = new Column();
		example.setName(col2.getName() );
		assertSame(
				table.getColumn(example), col2, "column with same name should be same instance!");			
	}
	
}
