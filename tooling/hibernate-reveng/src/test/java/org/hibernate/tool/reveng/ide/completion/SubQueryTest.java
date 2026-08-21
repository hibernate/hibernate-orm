/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.ide.completion;

import org.hibernate.grammars.hql.HqlLexer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SubQueryTest {

	private SubQuery createSubQuery(int startOffset) {
		SubQuery sq = new SubQuery();
		sq.startOffset = startOffset;
		return sq;
	}

	private void addToken(SubQuery sq, int tokenId, String text) {
		sq.tokenIds.add(tokenId);
		sq.tokenText.add(text);
	}

	@Test
	public void testGetTokenCount() {
		SubQuery sq = createSubQuery(0);
		assertEquals(0, sq.getTokenCount());
		addToken(sq, HqlLexer.IDENTIFIER, "Product");
		assertEquals(1, sq.getTokenCount());
	}

	@Test
	public void testGetTokenAndText() {
		SubQuery sq = createSubQuery(0);
		addToken(sq, HqlLexer.FROM, "from");
		addToken(sq, HqlLexer.IDENTIFIER, "Product");
		assertEquals(HqlLexer.FROM, sq.getToken(0));
		assertEquals(HqlLexer.IDENTIFIER, sq.getToken(1));
		assertEquals("from", sq.getTokenText(0));
		assertEquals("Product", sq.getTokenText(1));
	}

	@Test
	public void testCompareTo() {
		SubQuery a = createSubQuery(10);
		SubQuery b = createSubQuery(20);
		assertTrue(a.compareTo(b) < 0);
		assertTrue(b.compareTo(a) > 0);
		assertEquals(0, a.compareTo(createSubQuery(10)));
	}

	@Test
	public void testEqualsAndHashCode() {
		SubQuery a = createSubQuery(5);
		SubQuery b = createSubQuery(5);
		SubQuery c = createSubQuery(10);

		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertNotEquals(a, c);
		assertFalse(a.equals("not a subquery"));
	}

	@Test
	public void testGetEntityNamesSimpleFrom() {
		SubQuery sq = createSubQuery(0);
		addToken(sq, HqlLexer.FROM, "from");
		addToken(sq, HqlLexer.IDENTIFIER, "Product");

		List<EntityNameReference> names = sq.getEntityNames();
		assertEquals(1, names.size());
		assertEquals("Product", names.get(0).getEntityName());
		assertEquals("Product", names.get(0).getAlias());
	}

	@Test
	public void testGetEntityNamesWithAlias() {
		SubQuery sq = createSubQuery(0);
		addToken(sq, HqlLexer.FROM, "from");
		addToken(sq, HqlLexer.IDENTIFIER, "Product");
		addToken(sq, HqlLexer.IDENTIFIER, "p");

		List<EntityNameReference> names = sq.getEntityNames();
		assertEquals(1, names.size());
		assertEquals("Product", names.get(0).getEntityName());
		assertEquals("p", names.get(0).getAlias());
	}

	@Test
	public void testGetEntityNamesMultipleEntities() {
		SubQuery sq = createSubQuery(0);
		addToken(sq, HqlLexer.FROM, "from");
		addToken(sq, HqlLexer.IDENTIFIER, "Product");
		addToken(sq, HqlLexer.IDENTIFIER, "p");
		addToken(sq, HqlLexer.COMMA, ",");
		addToken(sq, HqlLexer.IDENTIFIER, "Category");
		addToken(sq, HqlLexer.IDENTIFIER, "c");

		List<EntityNameReference> names = sq.getEntityNames();
		assertEquals(2, names.size());
		assertEquals("Product", names.get(0).getEntityName());
		assertEquals("p", names.get(0).getAlias());
		assertEquals("Category", names.get(1).getEntityName());
		assertEquals("c", names.get(1).getAlias());
	}

	@Test
	public void testGetEntityNamesWithWhere() {
		SubQuery sq = createSubQuery(0);
		addToken(sq, HqlLexer.FROM, "from");
		addToken(sq, HqlLexer.IDENTIFIER, "Product");
		addToken(sq, HqlLexer.WHERE, "where");
		addToken(sq, HqlLexer.IDENTIFIER, "id");

		List<EntityNameReference> names = sq.getEntityNames();
		assertEquals(1, names.size());
		assertEquals("Product", names.get(0).getEntityName());
	}

	@Test
	public void testGetEntityNamesWithOrderBy() {
		SubQuery sq = createSubQuery(0);
		addToken(sq, HqlLexer.FROM, "from");
		addToken(sq, HqlLexer.IDENTIFIER, "Product");
		addToken(sq, HqlLexer.ORDER, "order");

		List<EntityNameReference> names = sq.getEntityNames();
		assertEquals(1, names.size());
	}

	@Test
	public void testGetEntityNamesWithGroupBy() {
		SubQuery sq = createSubQuery(0);
		addToken(sq, HqlLexer.FROM, "from");
		addToken(sq, HqlLexer.IDENTIFIER, "Product");
		addToken(sq, HqlLexer.GROUP, "group");

		List<EntityNameReference> names = sq.getEntityNames();
		assertEquals(1, names.size());
	}

	@Test
	public void testGetEntityNamesWithHaving() {
		SubQuery sq = createSubQuery(0);
		addToken(sq, HqlLexer.FROM, "from");
		addToken(sq, HqlLexer.IDENTIFIER, "Product");
		addToken(sq, HqlLexer.HAVING, "having");

		List<EntityNameReference> names = sq.getEntityNames();
		assertEquals(1, names.size());
	}

	@Test
	public void testGetEntityNamesWithDottedName() {
		SubQuery sq = createSubQuery(0);
		addToken(sq, HqlLexer.FROM, "from");
		addToken(sq, HqlLexer.IDENTIFIER, "com");
		addToken(sq, HqlLexer.DOT, ".");
		addToken(sq, HqlLexer.IDENTIFIER, "example");
		addToken(sq, HqlLexer.DOT, ".");
		addToken(sq, HqlLexer.IDENTIFIER, "Product");

		List<EntityNameReference> names = sq.getEntityNames();
		assertEquals(1, names.size());
		assertEquals("com.example.Product", names.get(0).getEntityName());
	}

	@Test
	public void testGetEntityNamesUpdate() {
		SubQuery sq = createSubQuery(0);
		addToken(sq, HqlLexer.UPDATE, "update");
		addToken(sq, HqlLexer.IDENTIFIER, "Product");
		addToken(sq, HqlLexer.SET, "set");
		addToken(sq, HqlLexer.IDENTIFIER, "name");

		List<EntityNameReference> names = sq.getEntityNames();
		assertEquals(1, names.size());
		assertEquals("Product", names.get(0).getEntityName());
	}

	@Test
	public void testGetEntityNamesDelete() {
		SubQuery sq = createSubQuery(0);
		addToken(sq, HqlLexer.DELETE, "delete");
		addToken(sq, HqlLexer.IDENTIFIER, "Product");
		addToken(sq, HqlLexer.WHERE, "where");

		List<EntityNameReference> names = sq.getEntityNames();
		assertEquals(1, names.size());
		assertEquals("Product", names.get(0).getEntityName());
	}

	@Test
	public void testGetEntityNamesWithJoin() {
		SubQuery sq = createSubQuery(0);
		addToken(sq, HqlLexer.FROM, "from");
		addToken(sq, HqlLexer.IDENTIFIER, "Product");
		addToken(sq, HqlLexer.IDENTIFIER, "p");
		addToken(sq, HqlLexer.JOIN, "join");
		addToken(sq, HqlLexer.IDENTIFIER, "p");
		addToken(sq, HqlLexer.DOT, ".");
		addToken(sq, HqlLexer.IDENTIFIER, "categories");
		addToken(sq, HqlLexer.IDENTIFIER, "c");

		List<EntityNameReference> names = sq.getEntityNames();
		// Product p from the from clause, plus join entries
		assertFalse(names.isEmpty());
	}

	@Test
	public void testGetEntityNamesWithLeftJoin() {
		SubQuery sq = createSubQuery(0);
		addToken(sq, HqlLexer.FROM, "from");
		addToken(sq, HqlLexer.IDENTIFIER, "Product");
		addToken(sq, HqlLexer.IDENTIFIER, "p");
		addToken(sq, HqlLexer.JOIN, "join");
		addToken(sq, HqlLexer.LEFT, "left");
		addToken(sq, HqlLexer.IDENTIFIER, "Category");
		addToken(sq, HqlLexer.IDENTIFIER, "c");
		addToken(sq, HqlLexer.WHERE, "where");

		List<EntityNameReference> names = sq.getEntityNames();
		assertFalse(names.isEmpty());
	}

	@Test
	public void testGetEntityNamesWithInnerJoin() {
		SubQuery sq = createSubQuery(0);
		addToken(sq, HqlLexer.FROM, "from");
		addToken(sq, HqlLexer.IDENTIFIER, "Product");
		addToken(sq, HqlLexer.JOIN, "join");
		addToken(sq, HqlLexer.INNER, "inner");
		addToken(sq, HqlLexer.IDENTIFIER, "Category");
		addToken(sq, HqlLexer.ORDER, "order");

		List<EntityNameReference> names = sq.getEntityNames();
		assertFalse(names.isEmpty());
	}

	@Test
	public void testGetEntityNamesEmpty() {
		SubQuery sq = createSubQuery(0);
		List<EntityNameReference> names = sq.getEntityNames();
		assertTrue(names.isEmpty());
	}

	@Test
	public void testGetEntityNamesNoFromClause() {
		SubQuery sq = createSubQuery(0);
		addToken(sq, HqlLexer.IDENTIFIER, "select");
		addToken(sq, HqlLexer.IDENTIFIER, "count");

		List<EntityNameReference> names = sq.getEntityNames();
		assertTrue(names.isEmpty());
	}

	@Test
	public void testGetEntityNamesJoinWithComma() {
		SubQuery sq = createSubQuery(0);
		addToken(sq, HqlLexer.FROM, "from");
		addToken(sq, HqlLexer.IDENTIFIER, "Product");
		addToken(sq, HqlLexer.JOIN, "join");
		addToken(sq, HqlLexer.IDENTIFIER, "Category");
		addToken(sq, HqlLexer.COMMA, ",");
		addToken(sq, HqlLexer.IDENTIFIER, "Brand");

		List<EntityNameReference> names = sq.getEntityNames();
		assertFalse(names.isEmpty());
	}

	@Test
	public void testGetEntityNamesRightJoin() {
		SubQuery sq = createSubQuery(0);
		addToken(sq, HqlLexer.FROM, "from");
		addToken(sq, HqlLexer.IDENTIFIER, "Product");
		addToken(sq, HqlLexer.JOIN, "join");
		addToken(sq, HqlLexer.RIGHT, "right");
		addToken(sq, HqlLexer.IDENTIFIER, "Category");

		List<EntityNameReference> names = sq.getEntityNames();
		assertFalse(names.isEmpty());
	}

	@Test
	public void testGetEntityNamesOuterJoin() {
		SubQuery sq = createSubQuery(0);
		addToken(sq, HqlLexer.FROM, "from");
		addToken(sq, HqlLexer.IDENTIFIER, "Product");
		addToken(sq, HqlLexer.JOIN, "join");
		addToken(sq, HqlLexer.OUTER, "outer");
		addToken(sq, HqlLexer.IDENTIFIER, "Category");

		List<EntityNameReference> names = sq.getEntityNames();
		assertFalse(names.isEmpty());
	}

	@Test
	public void testGetEntityNamesJoinWithDottedPath() {
		SubQuery sq = createSubQuery(0);
		addToken(sq, HqlLexer.FROM, "from");
		addToken(sq, HqlLexer.IDENTIFIER, "Product");
		addToken(sq, HqlLexer.JOIN, "join");
		addToken(sq, HqlLexer.IDENTIFIER, "com");
		addToken(sq, HqlLexer.DOT, ".");
		addToken(sq, HqlLexer.IDENTIFIER, "example");
		addToken(sq, HqlLexer.DOT, ".");
		addToken(sq, HqlLexer.IDENTIFIER, "Category");

		List<EntityNameReference> names = sq.getEntityNames();
		assertFalse(names.isEmpty());
	}
}
