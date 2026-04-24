/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.hbmlint.SchemaAnalyzer;

import org.hibernate.cfg.Environment;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.tool.reveng.internal.builder.hbm.HbmClassDetailsBuilder;
import org.hibernate.tool.reveng.internal.exporter.lint.Issue;
import org.hibernate.tool.reveng.internal.exporter.lint.IssueCollector;
import org.hibernate.tool.reveng.internal.exporter.lint.SchemaByMetaDataDetector;
import org.hibernate.tool.reveng.test.utils.JdbcUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author koen
 */
public class TestCase {

	private List<ClassDetails> entities;

	@BeforeEach
	public void setUp() {
		JdbcUtil.createDatabase(this);
		File hbmFile = new File(
				"src/test/resources/org/hibernate/tool/reveng/hbmlint/SchemaAnalyzer/SchemaIssues.hbm.xml");
		HbmClassDetailsBuilder builder = new HbmClassDetailsBuilder();
		entities = builder.buildFromFiles(hbmFile);
	}

	@AfterEach
	public void tearDown() {
		JdbcUtil.dropDatabase(this);
	}

	@Test
	public void testSchemaAnalyzer() {
		SchemaByMetaDataDetector analyzer = new SchemaByMetaDataDetector();
		Properties properties = Environment.getProperties();
		analyzer.initialize(entities, properties);

		MockCollector mc = new MockCollector();
		analyzer.visit(mc);

		// Check for missing table (MISSING_TABLE)
		assertTrue(
				mc.problems.stream().anyMatch(
						i -> i.getDescription().contains("Missing table")
								&& i.getDescription().contains("MISSING_TABLE")),
				"Should detect missing table MISSING_TABLE");

		// Check for missing column (CATEGORY.name)
		assertTrue(
				mc.problems.stream().anyMatch(
						i -> i.getDescription().contains("missing column")),
				"Should detect missing column");

		// Check for wrong column type (BAD_TYPE.name)
		assertTrue(
				mc.problems.stream().anyMatch(
						i -> i.getDescription().contains("wrong column type")),
				"Should detect wrong column type");

		// Check for missing generator (does_not_exist)
		MockCollector genCollector = new MockCollector();
		analyzer.visitGenerators(genCollector);
		assertTrue(
				genCollector.problems.stream().anyMatch(
						i -> i.getDescription().contains("does_not_exist")),
				"Should detect missing generator table 'does_not_exist'");
	}

	static class MockCollector implements IssueCollector {
		List<Issue> problems = new ArrayList<>();
		public void reportIssue(Issue analyze) {
			problems.add(analyze);
		}
	}

}
