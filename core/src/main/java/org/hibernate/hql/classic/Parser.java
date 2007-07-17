//$Id: Parser.java 4907 2004-12-08 00:24:14Z oneovthafew $
package org.hibernate.hql.classic;

import org.hibernate.QueryException;

/**
 * A parser is a state machine that accepts a string of tokens,
 * bounded by start() and end() and modifies a QueryTranslator. Parsers
 * are NOT intended to be threadsafe. They SHOULD be reuseable
 * for more than one token stream.
 */

public interface Parser {
	public void token(String token, QueryTranslatorImpl q) throws QueryException;

	public void start(QueryTranslatorImpl q) throws QueryException;

	public void end(QueryTranslatorImpl q) throws QueryException;
}







