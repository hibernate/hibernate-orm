/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.hbm2ddl;

import java.io.Reader;
import java.util.List;

import org.hibernate.tool.hbm2ddl.grammar.SqlStatementLexer;
import org.hibernate.tool.hbm2ddl.grammar.SqlStatementParser;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;


/**
 * Class responsible for extracting SQL statements from import script. Supports instructions/comments and quoted
 * strings spread over multiple lines. Each statement must end with semicolon.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class MultipleLinesSqlCommandExtractor implements ImportSqlCommandExtractor {
	@Override
	public String[] extractCommands(Reader reader) {
		try {
			final SqlStatementLexer lexer;
			lexer = new SqlStatementLexer( new ANTLRInputStream( reader ) );
			final SqlStatementParser parser = new SqlStatementParser( new CommonTokenStream( lexer ) );
			SqlStatementParserListenerImpl listener = new SqlStatementParserListenerImpl();
			ParseTreeWalker parseTreeWalker = new ParseTreeWalker();
			parseTreeWalker.walk( listener, parser.statements() );
			final List<String> statements = listener.getStatements();
			return statements.toArray( new String[statements.size()] );
		}
		catch (Exception e) {
			throw new ImportScriptException( "Error during import script parsing.", e );
		}
	}
}
