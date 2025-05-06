/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ConsoleErrorListener;
import org.antlr.v4.runtime.DefaultErrorStrategy;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.misc.ParseCancellationException;

/**
 * Class responsible for extracting SQL statements from import script. Supports instructions/comments and quoted
 * strings spread over multiple lines. Each statement must end with semicolon.
 *
 * @author Lukasz Antoniak
 * @author Steve Ebersole
 */
public class MultiLineSqlScriptExtractor implements SqlScriptCommandExtractor {
	public static final String SHORT_NAME = "multi-line";

	public static final MultiLineSqlScriptExtractor INSTANCE = new MultiLineSqlScriptExtractor();

	@Override
	public List<String> extractCommands(Reader reader, Dialect dialect) {
		try {
			final SqlScriptParser.ScriptContext scriptParseTree = buildScriptParseTree( reader );
			final SqlScriptVisitor visitor = new SqlScriptVisitor( dialect );
			return visitor.visitScript( scriptParseTree );
		}
		catch (Exception exception) {
			if ( exception instanceof SqlScriptException sqlScriptException ) {
				throw sqlScriptException;
			}
			throw new SqlScriptException( "Error during sql-script parsing.", exception );
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
		parser.addErrorListener( new VerboseListener() );

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

	public static class VerboseListener extends BaseErrorListener {
		@Override
		public void syntaxError(
				Recognizer<?, ?> recognizer,
				Object offendingSymbol,
				int line,
				int charPositionInLine,
				String msg,
				RecognitionException e) {
			if ( msg.contains( "missing ';'" ) ) {
				throw new SqlScriptException( "Import script Sql statements must terminate with a ';' char" );
			}
			super.syntaxError( recognizer, offendingSymbol, line, charPositionInLine, msg, e );
		}
	}
}
