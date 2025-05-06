/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.internal.script;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.dialect.Dialect;
import org.hibernate.grammars.importsql.SqlScriptLexer;
import org.hibernate.grammars.importsql.SqlScriptParser;
import org.hibernate.grammars.importsql.SqlScriptParserBaseVisitor;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

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
			return Collections.emptyList();
		}
		final ArrayList<String> commands = new ArrayList<>( children.size() );
		final StringBuilder commandBuffer = new StringBuilder();
		for ( int i = 0; i < children.size(); i++ ) {
			final ParseTree parseTree = children.get( i );
			if ( parseTree instanceof SqlScriptParser.CommandBlockContext blockContext ) {
				commandBuffer.setLength( 0 );
				final List<ParseTree> terminalNodes = blockContext.command().children;
				for ( int j = 0; j < terminalNodes.size(); j++ ) {
					final TerminalNode terminalNode = (TerminalNode) terminalNodes.get( j );
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
