package org.hibernate.tool.ide.completion;

/**
 * Minimal lexer interface that allows HqlAnalyzer to work.
 * 
 * @author Max Rydahl Andersen
 */
public interface SimpleHQLLexer {

	
	int nextTokenId() throws SimpleLexerException;

	int getTokenOffset();

	int getTokenLength();

}
