/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.internal.script;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.dialect.Dialect;
import org.hibernate.grammars.importsql.SqlScriptLexer;
import org.hibernate.grammars.importsql.SqlScriptParser;
import org.hibernate.grammars.importsql.SqlScriptParserBaseVisitor;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import static java.util.Collections.emptyList;

/**
 * @author Steve Ebersole
 * @author Christian Beikov
 */
public class SqlScriptVisitor extends SqlScriptParserBaseVisitor<Object> {
	private final Dialect dialect;

	public SqlScriptVisitor(Dialect dialect) {
		this.dialect = dialect;
	}

	@Override
	public List<String> visitScript(SqlScriptParser.ScriptContext ctx) {
		final List<ParseTree> children = ctx.children;
		if ( children == null ) {
			return emptyList();
		}
		final ArrayList<String> commands = new ArrayList<>( children.size() );
		final var commandBuffer = new StringBuilder();
		for ( int i = 0; i < children.size(); i++ ) {
			if ( children.get( i ) instanceof SqlScriptParser.CommandBlockContext blockContext ) {
				commandBuffer.setLength( 0 );
				final List<ParseTree> terminalNodes = blockContext.command().children;
				for ( int j = 0; j < terminalNodes.size(); j++ ) {
					final var terminalNode = (TerminalNode) terminalNodes.get( j );
					switch ( terminalNode.getSymbol().getType() ) {
						case SqlScriptLexer.CHAR:
						case SqlScriptLexer.SPACE:
						case SqlScriptLexer.TAB:
							commandBuffer.append( terminalNode.getText() );
							break;
						case SqlScriptLexer.QUOTED_TEXT:
							commandBuffer.append( dialect.quote( terminalNode.getText() ) );
							break;
						case SqlScriptLexer.NEWLINE:
							commandBuffer.append( ' ' );
							break;
						default:
							throw new IllegalArgumentException( "Unsupported token: " + terminalNode );
					}
				}
				commands.add( commandBuffer.toString() );
			}
		}

		return commands;
	}

}
