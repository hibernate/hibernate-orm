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
package org.hibernate.tool.hbm2x.DefaultSchemaCatalog;

import org.hibernate.boot.Metadata;
import org.hibernate.cfg.Environment;
import org.hibernate.mapping.Table;
import org.hibernate.tool.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tool.api.reveng.RevengStrategy;
import org.hibernate.tool.api.reveng.RevengStrategy.SchemaSelection;
import org.hibernate.tool.api.reveng.TableIdentifier;
import org.hibernate.tool.internal.reveng.strategy.DefaultStrategy;
import org.hibernate.tool.internal.reveng.strategy.OverrideRepository;
import org.hibernate.tool.test.utils.JdbcUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

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
	public void testReadOnlySpecificSchema() {
		OverrideRepository or = new OverrideRepository();
		or.addSchemaSelection(createSchemaSelection("OVRTEST", null));
		RevengStrategy res = or.getReverseEngineeringStrategy(new DefaultStrategy());
		List<Table> tables = getTables(MetadataDescriptorFactory
				.createReverseEngineeringDescriptor(res, null)
				.createMetadata());
		assertEquals(2,tables.size());	
		Table catchild = tables.get(0);
		Table catmaster = tables.get(1);
		if(catchild.getName().equals("CATMASTER")) {
			catchild = tables.get(1);
			catmaster = tables.get(0);
		} 	
		TableIdentifier masterid = TableIdentifier.create(catmaster);
		TableIdentifier childid = TableIdentifier.create(catchild);
		assertEquals(TableIdentifier.create(null, "OVRTEST", "CATMASTER"), masterid);
		assertEquals(TableIdentifier.create(null, "OVRTEST", "CATCHILD"), childid);	
	}

	@Test
	public void testOverlapping() {	
		OverrideRepository or = new OverrideRepository();
		or.addSchemaSelection(createSchemaSelection("OVRTEST", null));
		or.addSchemaSelection(createSchemaSelection(null, "MASTER"));
		or.addSchemaSelection(createSchemaSelection(null, "CHILD"));
		RevengStrategy res = 
				or.getReverseEngineeringStrategy(new DefaultStrategy());
		Metadata metadata = MetadataDescriptorFactory
				.createReverseEngineeringDescriptor(res, null)
				.createMetadata();
		Set<TableIdentifier> tables = new HashSet<>();
        for (Table element : metadata.collectTableMappings()) {
            boolean added = tables.add(TableIdentifier.create(element));
            if (!added)
                fail("duplicate table found for " + element);
        }
		assertEquals(4,tables.size());
	}
	
	@Test
	public void testUseDefault() {
		Properties properties = new Properties();
		properties.setProperty(Environment.DEFAULT_SCHEMA, "OVRTEST");
		properties.setProperty(Environment.DEFAULT_SCHEMA, "OVRTEST");
		List<Table> tables = getTables(MetadataDescriptorFactory
				.createReverseEngineeringDescriptor(null, properties)
				.createMetadata());
		assertEquals(2,tables.size());
		Table catchild = tables.get(0);
		Table catmaster = tables.get(1);
		if(catchild.getName().equals("CATMASTER")) {
			catchild = tables.get(1);
			catmaster = tables.get(0);
		} 	
		TableIdentifier masterid = TableIdentifier.create(catmaster);
		TableIdentifier childid = TableIdentifier.create(catchild);
		assertEquals(TableIdentifier.create(null, null, "CATMASTER"), masterid, "jdbcreader has not nulled out according to default schema");
		assertEquals(TableIdentifier.create(null, null, "CATCHILD"), childid, "jdbcreader has not nulled out according to default schema");
	}

	private List<Table> getTables(Metadata metadata) {
        return new ArrayList<>(metadata.collectTableMappings());
	}

	private SchemaSelection createSchemaSelection(String matchSchema, String matchTable) {
		return new SchemaSelection() {
			@Override
			public String getMatchCatalog() {
				return null;
			}
			@Override
			public String getMatchSchema() {
				return matchSchema;
			}
			@Override
			public String getMatchTable() {
				return matchTable;
			}		
		};
	}
	
}
