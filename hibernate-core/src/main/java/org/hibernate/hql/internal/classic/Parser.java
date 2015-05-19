/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.classic;
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







