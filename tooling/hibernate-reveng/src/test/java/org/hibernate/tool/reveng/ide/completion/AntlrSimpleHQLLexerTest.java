/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.ide.completion;

import org.antlr.v4.runtime.Token;
import org.hibernate.grammars.hql.HqlLexer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AntlrSimpleHQLLexerTest {

	@Test
	public void testLexSimpleSelect() {
		AntlrSimpleHQLLexer lexer = new AntlrSimpleHQLLexer("from Product".toCharArray());

		int firstToken = lexer.nextTokenId();
		assertEquals(HqlLexer.FROM, firstToken);
		assertEquals(4, lexer.getTokenLength()); // "from"
		assertEquals(0, lexer.getTokenOffset());

		int secondToken = lexer.nextTokenId();
		assertEquals(HqlLexer.IDENTIFIER, secondToken);
		assertEquals(7, lexer.getTokenLength()); // "Product"
		assertEquals(5, lexer.getTokenOffset());
	}

	@Test
	public void testLexEof() {
		AntlrSimpleHQLLexer lexer = new AntlrSimpleHQLLexer("from".toCharArray());
		lexer.nextTokenId(); // FROM
		int eof = lexer.nextTokenId();
		assertEquals(Token.EOF, eof);
	}

	@Test
	public void testLexDottedPath() {
		AntlrSimpleHQLLexer lexer = new AntlrSimpleHQLLexer("from com.example.Product".toCharArray());
		assertEquals(HqlLexer.FROM, lexer.nextTokenId());
		assertEquals(HqlLexer.IDENTIFIER, lexer.nextTokenId()); // "com"
		assertEquals(HqlLexer.DOT, lexer.nextTokenId());
		assertEquals(HqlLexer.IDENTIFIER, lexer.nextTokenId()); // "example"
		assertEquals(HqlLexer.DOT, lexer.nextTokenId());
		assertEquals(HqlLexer.IDENTIFIER, lexer.nextTokenId()); // "Product"
	}

	@Test
	public void testLexWhereClause() {
		AntlrSimpleHQLLexer lexer = new AntlrSimpleHQLLexer("from Product where id = 1".toCharArray());
		assertEquals(HqlLexer.FROM, lexer.nextTokenId());
		assertEquals(HqlLexer.IDENTIFIER, lexer.nextTokenId()); // Product
		assertEquals(HqlLexer.WHERE, lexer.nextTokenId());
	}

	@Test
	public void testEmptyInput() {
		AntlrSimpleHQLLexer lexer = new AntlrSimpleHQLLexer("".toCharArray());
		int token = lexer.nextTokenId();
		assertEquals(Token.EOF, token);
	}

	@Test
	public void testTokenLength() {
		AntlrSimpleHQLLexer lexer = new AntlrSimpleHQLLexer("select".toCharArray());
		lexer.nextTokenId();
		assertEquals(6, lexer.getTokenLength());
	}

	@Test
	public void testJoinQuery() {
		AntlrSimpleHQLLexer lexer = new AntlrSimpleHQLLexer("from Product p join p.categories c".toCharArray());
		assertEquals(HqlLexer.FROM, lexer.nextTokenId());
		assertEquals(HqlLexer.IDENTIFIER, lexer.nextTokenId()); // Product
		assertEquals(HqlLexer.IDENTIFIER, lexer.nextTokenId()); // p
		assertEquals(HqlLexer.JOIN, lexer.nextTokenId());
	}

	@Test
	public void testDeleteQuery() {
		AntlrSimpleHQLLexer lexer = new AntlrSimpleHQLLexer("delete Product".toCharArray());
		assertEquals(HqlLexer.DELETE, lexer.nextTokenId());
		assertEquals(HqlLexer.IDENTIFIER, lexer.nextTokenId());
	}

	@Test
	public void testUpdateQuery() {
		AntlrSimpleHQLLexer lexer = new AntlrSimpleHQLLexer("update Product set name = 'x'".toCharArray());
		assertEquals(HqlLexer.UPDATE, lexer.nextTokenId());
		assertEquals(HqlLexer.IDENTIFIER, lexer.nextTokenId());
		assertEquals(HqlLexer.SET, lexer.nextTokenId());
	}

	@Test
	public void testTokenOffset() {
		AntlrSimpleHQLLexer lexer = new AntlrSimpleHQLLexer("from Product".toCharArray());
		lexer.nextTokenId(); // from
		assertEquals(0, lexer.getTokenOffset());
		lexer.nextTokenId(); // Product
		assertEquals(5, lexer.getTokenOffset());
	}
}
