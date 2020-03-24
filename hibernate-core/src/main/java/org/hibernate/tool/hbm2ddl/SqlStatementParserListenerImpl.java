/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.tool.hbm2ddl;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.grammars.importsql.SqlScriptParser;
import org.hibernate.grammars.importsql.SqlScriptParserBaseListener;

/**
 * @author Andrea Boriero
 */
public class SqlStatementParserListenerImpl extends SqlScriptParserBaseListener {
	private final List<String> statements = new ArrayList<>();

	@Override
	public void exitCommand(SqlScriptParser.CommandContext ctx) {
		statements.add( ctx.getText() );
	}

	public List<String> getStatements(){
		return statements;
	}
}
