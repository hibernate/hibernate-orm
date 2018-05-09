package org.hibernate.tool.ide.completion;

import java.io.CharArrayReader;

import org.hibernate.hql.internal.antlr.HqlBaseLexer;

import antlr.Token;
import antlr.TokenStreamException;

/**
 * A lexer implemented on top of the Antlr grammer implemented in core.
 * 
 * @author Max Rydahl Andersen
 *
 */
public class AntlrSimpleHQLLexer implements SimpleHQLLexer {

	private HqlBaseLexer lexer;
	private Token token;

	public AntlrSimpleHQLLexer(char[] cs, int length) {
		lexer = new HqlBaseLexer(new CharArrayReader(cs, 0, length)) {
			public void newline() {
				//super.newline();
			}
			
			public int getColumn() {
				return super.getColumn()-1;
			}
		};
		lexer.setTabSize(1);
	}

	public int getTokenLength() {
		if(token.getText()==null) {
			return 0;
		}
		return token.getText().length();
	}

	public int getTokenOffset() {
		return token.getColumn()-1;
	}

	public int nextTokenId() {
		try {
			token = lexer.nextToken();
			if(token==null) {
				System.out.println(token);
			}
		}
		catch (TokenStreamException e) {
			throw new SimpleLexerException(e);
		}
		return token.getType();
	}

}
