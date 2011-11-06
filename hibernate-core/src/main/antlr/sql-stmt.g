header
{
package org.hibernate.hql.internal.antlr;

import java.util.List;
import java.util.LinkedList;
}
/**
 * Lexer and parser used to extract single statements from import SQL script. Supports single and multiple line
 * instructions/comments and quoted strings. Each statement should end with semicolon.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
class SqlStatementParser extends Parser;

options {
    buildAST = false;
}

{
    /** List of all SQL statements. */
	private List<String> statementList = new LinkedList<String>();

	/** Currently processing SQL statement. */
	private StringBuilder current = new StringBuilder();

	protected void out(String stmt) {
		current.append( stmt );
	}

	protected void out(Token token) {
		out( token.getText() );
	}

    public List<String> getStatementList() {
        return statementList;
    }

    protected void statementEnd() {
        statementList.add( current.toString().trim() );
        current = new StringBuilder();
    }
}

script
    :   ( statement )*
    ;

statement
    :   ( s:NOT_STMT_END { out( s ); } | q:QUOTED_STRING { out( q ); } )* STMT_END { statementEnd(); }
    ;

class SqlStatementLexer extends Lexer;

options {
    k = 2;
    charVocabulary = '\u0000'..'\uFFFE';
}

STMT_END
    : ';'
    ;

NOT_STMT_END
    : ~( ';' )
    ;

QUOTED_STRING
	: '\'' ( (ESCqs)=> ESCqs | ~'\'' )* '\''
	;

protected
ESCqs
	: '\'' '\''
	;

LINE_COMMENT
    : ( "//" | "--" ) ( ~('\n'|'\r') )* { $setType(Token.SKIP); }
	;

MULTILINE_COMMENT
    : "/*" ( options {greedy=false;} : . )* "*/" { $setType(Token.SKIP); }
    ;