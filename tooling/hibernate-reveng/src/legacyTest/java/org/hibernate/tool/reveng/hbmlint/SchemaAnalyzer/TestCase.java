/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.hbmlint.SchemaAnalyzer;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.id.SequenceMismatchStrategy;
import org.hibernate.mapping.Table;
import org.hibernate.tool.reveng.internal.export.lint.Issue;
import org.hibernate.tool.reveng.internal.export.lint.IssueCollector;
import org.hibernate.tool.reveng.internal.export.lint.SchemaByMetaDataDetector;
import org.hibernate.tool.reveng.test.utils.JdbcUtil;
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
		metadataSources.addResource("org/hibernate/tool/reveng/hbmlint/SchemaAnalyzer/SchemaIssues.hbm.xml");
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
