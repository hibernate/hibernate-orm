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

import org.hibernate.cfg.Environment;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.tool.internal.reveng.models.builder.hbm.HbmClassDetailsBuilder;
import org.hibernate.tool.internal.exporter.lint.Issue;
import org.hibernate.tool.internal.exporter.lint.IssueCollector;
import org.hibernate.tool.internal.exporter.lint.SchemaByMetaDataDetector;
import org.hibernate.tool.test.utils.JdbcUtil;
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
				"src/legacyTest/resources/org/hibernate/tool/hbmlint/SchemaAnalyzer/SchemaIssues.hbm.xml");
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
