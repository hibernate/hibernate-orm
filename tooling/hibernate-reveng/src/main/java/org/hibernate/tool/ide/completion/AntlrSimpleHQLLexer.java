/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2010-2025 Red Hat, Inc.
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
package org.hibernate.tool.ide.completion;

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
