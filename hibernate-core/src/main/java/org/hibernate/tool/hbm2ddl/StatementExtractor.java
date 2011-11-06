package org.hibernate.tool.hbm2ddl;

import org.hibernate.hql.internal.antlr.SqlStatementLexer;
import org.hibernate.hql.internal.antlr.SqlStatementParser;

import java.io.Reader;
import java.util.List;

/**
 * Class responsible for extracting SQL statements from import script. Supports single and multiple line
 * instructions/comments and quoted strings.
 * 
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class StatementExtractor {
	/**
	 * @param reader Character stream reader of entire SQL script. Each statement should end with semicolon.
	 * @return List of single SQL statements (each without semicolon at the end).
	 */
	List<String> retrieveStatements(final Reader reader) {
		final SqlStatementLexer lexer = new SqlStatementLexer( reader );
		final SqlStatementParser parser = new SqlStatementParser( lexer );
		try {
			parser.script(); // Parse script.
		}
		catch ( Exception e ) {
			throw new ImportScriptException( "Error during import script parsing.", e );
		}
		return parser.getStatementList();
	}
}
