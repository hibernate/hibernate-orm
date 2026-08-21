/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.ide.completion;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HQLAnalyzerTest {

	private final HQLAnalyzer analyzer = new HQLAnalyzer();

	@Test
	public void testShouldShowEntityNamesAfterFrom() {
		assertTrue(analyzer.shouldShowEntityNames("from ", 5));
	}

	@Test
	public void testShouldShowEntityNamesAfterUpdate() {
		assertTrue(analyzer.shouldShowEntityNames("update ", 7));
	}

	@Test
	public void testShouldShowEntityNamesAfterDelete() {
		assertTrue(analyzer.shouldShowEntityNames("delete ", 7));
	}

	@Test
	public void testShouldNotShowEntityNamesInWhere() {
		assertFalse(analyzer.shouldShowEntityNames("from Foo where ", 15));
	}

	@Test
	public void testShouldNotShowEntityNamesInSelect() {
		assertFalse(analyzer.shouldShowEntityNames("select ", 7));
	}

	@Test
	public void testGetVisibleEntityNames() {
		String query = "from Product as p where p.name = 'test'";
		List<EntityNameReference> names = analyzer.getVisibleEntityNames(query.toCharArray(), query.length());
		assertFalse(names.isEmpty());
		assertEquals("p", names.get(0).getAlias());
		assertEquals("Product", names.get(0).getEntityName());
	}

	@Test
	public void testGetVisibleEntityNamesMultiple() {
		String query = "from Product as p, Category as c where ";
		List<EntityNameReference> names = analyzer.getVisibleEntityNames(query.toCharArray(), query.length());
		assertTrue(names.size() >= 2);
	}

	@Test
	public void testGetVisibleEntityNamesWithJoin() {
		String query = "from Product as p join p.categories as c where ";
		List<EntityNameReference> names = analyzer.getVisibleEntityNames(query.toCharArray(), query.length());
		assertFalse(names.isEmpty());
	}

	@Test
	public void testGetVisibleSubQueries() {
		String query = "from Product as p where p.id in (select c.id from Category as c)";
		List<SubQuery> subQueries = analyzer.getVisibleSubQueries(query.toCharArray(), query.length());
		assertNotNull(subQueries);
	}

	@Test
	public void testGetEntityNamePrefix() {
		assertEquals("Pro", HQLAnalyzer.getEntityNamePrefix("from Pro".toCharArray(), 8));
		assertEquals("", HQLAnalyzer.getEntityNamePrefix("from ".toCharArray(), 5));
	}

	@Test
	public void testGetHQLKeywords() {
		String[] keywords = HQLAnalyzer.getHQLKeywords();
		assertNotNull(keywords);
		assertTrue(keywords.length > 0);
		// Verify known keywords are present
		boolean foundWhere = false;
		boolean foundFrom = false;
		for (String keyword : keywords) {
			if ("where".equals(keyword)) foundWhere = true;
			if ("from".equals(keyword)) foundFrom = true;
		}
		assertTrue(foundWhere);
		assertTrue(foundFrom);
	}

	@Test
	public void testGetHQLFunctionNames() {
		String[] functions = HQLAnalyzer.getHQLFunctionNames();
		assertNotNull(functions);
		assertTrue(functions.length > 0);
	}

	@Test
	public void testShouldShowEntityNamesWithCharArray() {
		assertTrue(analyzer.shouldShowEntityNames("from ".toCharArray(), 5));
		assertFalse(analyzer.shouldShowEntityNames("select ".toCharArray(), 7));
	}

	@Test
	public void testEntityNameReferenceToString() {
		EntityNameReference ref = new EntityNameReference("Product", "p");
		assertNotNull(ref.toString());
		assertEquals("Product", ref.getEntityName());
		assertEquals("p", ref.getAlias());
	}
}
