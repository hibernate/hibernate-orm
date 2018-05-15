/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.hbm2ddl;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.tool.hbm2ddl.grammar.SqlStatementParser;
import org.hibernate.tool.hbm2ddl.grammar.SqlStatementParserBaseListener;

/**
 * @author Andrea Boriero
 */
public class SqlStatementParserListenerImpl extends SqlStatementParserBaseListener {

	List<String> statements = new ArrayList<String>(  );

	@Override
	public void exitStatement(SqlStatementParser.StatementContext ctx) {
		super.exitStatement( ctx );
		statements.add( ctx.getText().replace( System.lineSeparator(), " " ).trim() );
	}

	public List<String> getStatements(){
		return statements;
	}
}
