/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.ast;

import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.hql.internal.ast.util.ASTUtil;
import org.hibernate.internal.util.StringHelper;

import antlr.RecognitionException;
import antlr.Token;
import antlr.TokenStream;

/**
 * @author Steve Ebersole
 */
public class SqlScriptParser extends GeneratedSqlScriptParser {
	private static String[] TOKEN_NAMES = ASTUtil.generateTokenNameCache( GeneratedSqlScriptParserTokenTypes .class );

	public static List<String> extractCommands(Reader reader) {
		final List<String> statementList = new ArrayList<>();

		final SqlScriptLexer lexer = new SqlScriptLexer( reader );
		final SqlScriptParser parser = new SqlScriptParser( statementList::add, lexer );

		parser.parseScript();

		return statementList;
	}

	private final List<String> errorList = new LinkedList<>();
	private final Consumer<String> commandConsumer;

	private StringBuilder currentStatementBuffer;

	public SqlScriptParser(Consumer<String> commandConsumer, TokenStream lexer) {
		super( lexer );
		this.commandConsumer = commandConsumer;

	}

	private void parseScript() {
		try {
			// trigger the top-level grammar rule
			script();
		}
		catch ( Exception e ) {
			throw new SqlScriptParserException( "Error during import script parsing.", e );
		}

		failIfAnyErrors();
	}

	/**
	 * Semantic action outputting text to the current statement buffer
	 */
	@Override
	protected void out(String text) {
		SqlScriptLogging.SCRIPT_LOGGER.tracef( "#out(`%s`) [text]", text );
		currentStatementBuffer.append( text );
	}

	/**
	 * Semantic action outputting a token to the current statement buffer
	 */
	@Override
	protected void out(Token token) {
		SqlScriptLogging.SCRIPT_LOGGER.tracef( "#out(`%s`) [token]", token.getText() );

		currentStatementBuffer.append( token.getText() );
	}

	@Override
	protected void statementStarted() {
		if ( currentStatementBuffer != null ) {
			SqlScriptLogging.SCRIPT_LOGGER.debugf( "`#currentStatementBuffer` was not null at `#statementStart`" );
		}
		currentStatementBuffer = new StringBuilder();
	}

	/**
	 * Semantic action signifying the end of a statement (delimiter recognized)
	 */
	@Override
	protected void statementEnded() {
		final String statementText = currentStatementBuffer.toString().trim();
		SqlScriptLogging.AST_LOGGER.debugf( "Import statement : %s", statementText );
		commandConsumer.accept( statementText );

		currentStatementBuffer = null;
	}

	private void failIfAnyErrors() {
		if ( errorList.isEmpty() ) {
			return;
		}

		throw new SqlScriptParserException( buildErrorMessage() );
	}

	public String buildErrorMessage() {
		final StringBuilder buf = new StringBuilder();
		for ( int i = 0; i < errorList.size(); i++ ) {
			buf.append( errorList.get( i ) );
			if ( i < errorList.size() - 1 ) {
				buf.append( System.lineSeparator() );
			}
		}
		return buf.toString();
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// error handling hooks

	@Override
	public void reportError(RecognitionException e) {
		final String textBase = "RecognitionException(@" + e.getLine() + ":" + e.getColumn() + ")";

		String message = e.toString();
		if ( message.contains( "expecting DELIMITER" ) ) {
			message = "Import script Sql statements must terminate with a ';' char";
		}

		errorList.add( textBase + " : " + message );
	}

	@Override
	public void reportError(String message) {
		if ( message.contains( "expecting DELIMITER" ) ) {
			message = "Import script Sql statements must terminate with a ';' char";
		}

		errorList.add( message );
	}

	@Override
	public void reportWarning(String message) {
		SqlScriptLogging.SCRIPT_LOGGER.debugf( "SqlScriptParser recognition warning : " + message );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// trace logging hooks

	private static final int depthIndent = 2;
	private int traceDepth;

	@Override
	public void traceIn(String ruleName) {
		if ( ! SqlScriptLogging.AST_TRACE_ENABLED ) {
			return;
		}

		if ( inputState.guessing > 0 ) {
			return;
		}

		final String prefix = StringHelper.repeat( '-', ( traceDepth++ * depthIndent ) );
		SqlScriptLogging.AST_LOGGER.tracef( "%s-> %s", prefix, ruleName );
	}

	@Override
	public void traceOut(String ruleName) {
		if ( ! SqlScriptLogging.AST_TRACE_ENABLED ) {
			return;
		}

		if ( inputState.guessing > 0 ) {
			return;
		}

		final String prefix = StringHelper.repeat( '-', ( --traceDepth * depthIndent ) );
		SqlScriptLogging.AST_LOGGER.tracef( "<-%s %s", prefix, ruleName );
	}
}
