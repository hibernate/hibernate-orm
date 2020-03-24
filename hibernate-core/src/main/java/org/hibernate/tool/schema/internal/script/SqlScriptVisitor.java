/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.internal.script;

import java.util.ArrayList;

import org.hibernate.grammars.importsql.SqlScriptParser;
import org.hibernate.grammars.importsql.SqlScriptParserBaseVisitor;

/**
 * @author Steve Ebersole
 */
public class SqlScriptVisitor extends SqlScriptParserBaseVisitor {
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
		return ctx.getText();
	}
}
