/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.internal.script;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.dialect.Dialect;
import org.hibernate.grammars.importsql.SqlScriptParser;
import org.hibernate.grammars.importsql.SqlScriptParserBaseVisitor;

/**
 * @author Steve Ebersole
 */
public class SqlScriptVisitor extends SqlScriptParserBaseVisitor<Object> {
	private final Dialect dialect;

	public SqlScriptVisitor(Dialect dialect) {
		this.dialect = dialect;
	}

	@Override
	public ArrayList<String> visitScript(SqlScriptParser.ScriptContext ctx) {
		final ArrayList<String> commands = new ArrayList<>();

		for ( int i = 0; i < ctx.commandBlock().size(); i++ ) {
			commands.add( visitCommandBlock( ctx.commandBlock().get( i ) ) );
		}

		return commands;
	}

	@Override
	public String visitCommandBlock(SqlScriptParser.CommandBlockContext ctx) {
		return visitCommand( ctx.command() );
	}

	@Override
	public String visitCommand(SqlScriptParser.CommandContext ctx) {
		final List<SqlScriptParser.CommandPartContext> commandParts = ctx.commandPart();

		final StringBuilder commandBuffer = new StringBuilder();
		String separator = "";

		for ( int i = 0; i < commandParts.size(); i++ ) {
			commandBuffer.append( separator );

			final SqlScriptParser.CommandPartContext commandPart = commandParts.get( i );

			if ( commandPart.notStmtEnd() != null ) {
				commandBuffer.append( commandPart.notStmtEnd().getText() );
			}
			else if ( commandPart.quotedText() != null ) {
				commandBuffer.append( visitQuotedText( commandPart.quotedText() ) );
			}

			separator = " ";
		}

		return commandBuffer.toString();
	}

	@Override
	public Object visitQuotedText(SqlScriptParser.QuotedTextContext ctx) {
		return dialect.quote( ctx.QUOTED_TEXT().getText() );
	}
}
