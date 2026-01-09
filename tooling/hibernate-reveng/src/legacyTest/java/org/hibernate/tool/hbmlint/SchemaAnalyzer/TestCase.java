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
package org.hibernate.tool.hbmlint.SchemaAnalyzer;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.id.SequenceMismatchStrategy;
import org.hibernate.mapping.Table;
import org.hibernate.tool.internal.export.lint.Issue;
import org.hibernate.tool.internal.export.lint.IssueCollector;
import org.hibernate.tool.internal.export.lint.SchemaByMetaDataDetector;
import org.hibernate.tool.test.utils.JdbcUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
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
	public void testSchemaAnalyzer() {

		StandardServiceRegistryBuilder ssrb = new StandardServiceRegistryBuilder();
		ssrb.applySetting(
				AvailableSettings.SEQUENCE_INCREMENT_SIZE_MISMATCH_STRATEGY,
				SequenceMismatchStrategy.NONE);
		MetadataSources metadataSources = new MetadataSources(ssrb.build());
		metadataSources.addResource("org/hibernate/tool/hbmlint/SchemaAnalyzer/SchemaIssues.hbm.xml");
		Metadata metadata = metadataSources.buildMetadata();
		SchemaByMetaDataDetector analyzer = new SchemaByMetaDataDetector();
		analyzer.initialize( metadata );


        for (Table table : metadata.collectTableMappings()) {
            MockCollector mc = new MockCollector();

            if (table.getName().equalsIgnoreCase("MISSING_TABLE")) {
                analyzer.visit(table, mc);
                assertEquals(1, mc.problems.size());
                Issue ap = mc.problems.get(0);
                assertTrue(ap.getDescription().contains("Missing table"));
            } else if (table.getName().equalsIgnoreCase("CATEGORY")) {
                analyzer.visit(table, mc);
                assertEquals(1, mc.problems.size());
                Issue ap = mc.problems.get(0);
                assertTrue(ap.getDescription().contains("missing column: name"));
            } else if (table.getName().equalsIgnoreCase("BAD_TYPE")) {
                analyzer.visit(table, mc);
                assertEquals(1, mc.problems.size());
                Issue ap = mc.problems.get(0);
                assertTrue(ap.getDescription().contains("wrong column type for name"));
            }
        }
		
		MockCollector mc = new MockCollector();
		analyzer.visitGenerators(mc);
		assertEquals(1,mc.problems.size());
		Issue issue = mc.problems.get( 0 );
		assertTrue(issue.getDescription().contains("does_not_exist"));

	}
			
	static class MockCollector implements IssueCollector {
		List<Issue> problems = new ArrayList<>();
		public void reportIssue(Issue analyze) {			
			problems.add(analyze);
		}		
	}		

}
