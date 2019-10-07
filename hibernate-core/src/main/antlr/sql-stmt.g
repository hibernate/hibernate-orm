header
{
package org.hibernate.hql.internal.antlr;

import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;

import org.hibernate.hql.internal.ast.ErrorReporter;
}
/**
 * Lexer and parser used to extract single statements from import SQL script. Supports instructions/comments and quoted
 * strings spread over multiple lines. Each statement must end with semicolon.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
class SqlStatementParser extends Parser;

options {
    buildAST = false;
}

{
    private ErrorHandler errorHandler = new ErrorHandler();

    @Override
    public void reportError(RecognitionException e) {
        errorHandler.reportError( e );
    }

    @Override
    public void reportError(String s) {
        errorHandler.reportError( s );
    }

    @Override
    public void reportWarning(String s) {
        errorHandler.reportWarning( s );
    }

    public void throwExceptionIfErrorOccurred() {
        if ( errorHandler.hasErrors() ) {
        	String errorMessage = errorHandler.getErrorMessage();
        	if(errorMessage.contains("expecting STMT_END")) {
        		throw new StatementParserException( "Import script Sql statements must terminate with a ';' char"  );
        	}
            throw new StatementParserException( errorHandler.getErrorMessage() );
        }
    }

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

    public class StatementParserException extends RuntimeException {
        public StatementParserException(String message) {
            super( message );
        }
    }

    private class ErrorHandler implements ErrorReporter {
        private List<String> errorList = new LinkedList<String>();

        @Override
        public void reportError(RecognitionException e) {
            reportError( e.toString() );
        }

        @Override
        public void reportError(String s) {
            errorList.add( s );
        }

        @Override
        public void reportWarning(String s) {
        }

        public boolean hasErrors() {
            return !errorList.isEmpty();
        }

        public String getErrorMessage() {
            StringBuilder buf = new StringBuilder();
            for ( Iterator iterator = errorList.iterator(); iterator.hasNext(); ) {
                buf.append( (String) iterator.next() );
                if ( iterator.hasNext() ) {
                    buf.append( "\n" );
                }
            }
            return buf.toString();
        }
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
    : ';' ( '\t' | ' ' | '\r' | '\n' )*
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