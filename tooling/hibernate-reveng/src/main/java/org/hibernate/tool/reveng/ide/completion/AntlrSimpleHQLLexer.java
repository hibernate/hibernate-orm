/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.ide.completion;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;
import org.hibernate.grammars.hql.HqlLexer;


/**
 * A lexer implemented on top of the Antlr grammer implemented in core.
 *
 * @author Max Rydahl Andersen
 *
 */
public class AntlrSimpleHQLLexer implements SimpleHQLLexer {

	private final HqlLexer lexer;
	private Token token;

	public AntlrSimpleHQLLexer(char[] cs) {
		lexer = new HqlLexer(CharStreams.fromString(new String(cs)));
	}

	public int getTokenLength() {
		if(token.getText()==null) {
			return 0;
		}
		return token.getText().length();
	}

	public int getTokenOffset() {
		return token.getCharPositionInLine();
	}

	public int nextTokenId() {
		token = lexer.nextToken();
		return token.getType();
	}

}
