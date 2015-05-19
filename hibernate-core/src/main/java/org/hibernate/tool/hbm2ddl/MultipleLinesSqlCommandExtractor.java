/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.hbm2ddl;

import java.io.Reader;
import java.util.List;

import org.hibernate.hql.internal.antlr.SqlStatementLexer;
import org.hibernate.hql.internal.antlr.SqlStatementParser;

/**
 * Class responsible for extracting SQL statements from import script. Supports instructions/comments and quoted
 * strings spread over multiple lines. Each statement must end with semicolon.
 * 
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class MultipleLinesSqlCommandExtractor implements ImportSqlCommandExtractor {
	@Override
	public String[] extractCommands(Reader reader) {
		final SqlStatementLexer lexer = new SqlStatementLexer( reader );
		final SqlStatementParser parser = new SqlStatementParser( lexer );
		try {
			parser.script(); // Parse script.
			parser.throwExceptionIfErrorOccurred();
		}
		catch ( Exception e ) {
			throw new ImportScriptException( "Error during import script parsing.", e );
		}
		List<String> statementList = parser.getStatementList();
		return statementList.toArray( new String[statementList.size()] );
	}
}
