package org.hibernate.tool.ide.completion;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;
import org.hibernate.grammars.hql.HqlLexer;


/**
 * A lexer implemented on top of the Antlr grammer implemented in core.
 * 
 * @author Max Rydahl Andersen
 *
 */
public class AntlrSimpleHQLLexer implements SimpleHQLLexer {

	private HqlLexer lexer;
	private Token token;

	public AntlrSimpleHQLLexer(char[] cs, int length) {
		lexer = new HqlLexer(CharStreams.fromString(new String(cs)));
		// Commenting out, not sure if this is still relevant and/or needed
//		{
//			public void newline() {
//				//super.newline();
//			}
//			
//			public int getColumn() {
//				return super.getCharPositionInLine();
//			}
//		};
//		lexer.setTabSize(1);
	}

	public int getTokenLength() {
		if(token.getText()==null) {
			return 0;
		}
		return token.getText().length();
	}

	public int getTokenOffset() {
		return token.getCharPositionInLine();
	}

	public int nextTokenId() {
		token = lexer.nextToken();
		if(token==null) {
			System.out.println(token);
		}
		return token.getType();
	}

}
