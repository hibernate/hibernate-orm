// $Id: QuerySyntaxException.java 9242 2006-02-09 12:37:36Z steveebersole $
package org.hibernate.hql.ast;

import antlr.RecognitionException;
import org.hibernate.QueryException;

/**
 * Exception thrown when there is a syntax error in the HQL.
 *
 * @author josh
 */
public class QuerySyntaxException extends QueryException {

	public QuerySyntaxException(String message) {
		super( message );
	}

	public QuerySyntaxException(String message, String hql) {
		this( message );
		setQueryString( hql );
	}

	public static QuerySyntaxException convert(RecognitionException e) {
		return convert( e, null );
	}

	public static QuerySyntaxException convert(RecognitionException e, String hql) {
		String positionInfo = e.getLine() > 0 && e.getColumn() > 0
				? " near line " + e.getLine() + ", column " + e.getColumn()
				: "";
		return new QuerySyntaxException( e.getMessage() + positionInfo, hql );
	}

}
