/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.internal.script;

import java.io.IOException;
import java.io.Reader;
import java.util.List;

import org.hibernate.dialect.Dialect;
import org.hibernate.grammars.importsql.SqlScriptLexer;
import org.hibernate.grammars.importsql.SqlScriptParser;
import org.hibernate.tool.schema.spi.SqlScriptCommandExtractor;
import org.hibernate.tool.schema.spi.SqlScriptException;

import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ConsoleErrorListener;
import org.antlr.v4.runtime.DefaultErrorStrategy;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.misc.ParseCancellationException;

/**
 * Class responsible for extracting SQL statements from import script. Supports instructions/comments and quoted
 * strings spread over multiple lines. Each statement must end with semicolon.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 * @author Steve Ebersole
 */
public class MultiLineSqlScriptExtracter implements SqlScriptCommandExtractor {
	public static final String SHORT_NAME = "multi-line";

	public static final MultiLineSqlScriptExtracter INSTANCE = new MultiLineSqlScriptExtracter();

	@Override
	public List<String> extractCommands(Reader reader, Dialect dialect) {
		try {
			final SqlScriptParser.ScriptContext scriptParseTree = buildScriptParseTree( reader );
			final SqlScriptVisitor visitor = new SqlScriptVisitor( dialect );
			return visitor.visitScript( scriptParseTree );
		}
		catch (Exception e) {
			throw new SqlScriptException( "Error during sql-script parsing.", e );
		}
	}

	private static SqlScriptParser.ScriptContext buildScriptParseTree(Reader reader) throws IOException {
		final SqlScriptLexer lexer = new SqlScriptLexer( CharStreams.fromReader( reader ) );
		return buildScriptParseTree( lexer );
	}

	private static SqlScriptParser.ScriptContext buildScriptParseTree(SqlScriptLexer lexer) {
		return buildScriptParseTree( lexer, new SqlScriptParser( new CommonTokenStream( lexer ) ) );
	}

	private static SqlScriptParser.ScriptContext buildScriptParseTree(SqlScriptLexer lexer, SqlScriptParser parser) {
		// try to use SLL(k)-based parsing first - its faster
		parser.getInterpreter().setPredictionMode( PredictionMode.SLL );
		parser.removeErrorListeners();
		parser.setErrorHandler( new BailErrorStrategy() );

		try {
			return parser.script();
		}
		catch ( ParseCancellationException e) {
			// reset the input token stream and parser state
			lexer.reset();
			parser.reset();

			// fall back to LL(k)-based parsing
			parser.getInterpreter().setPredictionMode( PredictionMode.LL );
			parser.addErrorListener( ConsoleErrorListener.INSTANCE );
			parser.setErrorHandler( new DefaultErrorStrategy() );

			return parser.script();
		}
	}
}
