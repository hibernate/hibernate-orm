/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.spi;

/**
 * Defines the "callback" contract for {@link NativeQueryInterpreter#recognizeParameters}.
 */
public interface ParameterRecognizer {
	/**
	 * Called when an output parameter is recognized.  This should only ever be
	 * called once for a query in cases where the JDBC "function call" escape syntax is
	 * recognized, i.e. {@code "{?=call...}"}
	 *
	 * @param sourcePosition The position within the query
	 */
	void outParameter(int sourcePosition);

	/**
	 * Called when a JDBC-style ordinal parameter (?) is recognized
	 *
	 * @param sourcePosition The position within the query string
	 */
	void ordinalParameter(int sourcePosition);

	/**
	 * Called when a named parameter (:name) is recognized
	 *
	 * @param name The recognized parameter name
	 *
	 * @param sourcePosition The position within the query string
	 */
	void namedParameter(String name, int sourcePosition);

	/**
	 * Called when a JPA-style positional parameter (?1) is
	 * recognized.
	 *
	 * @param position The recognized position (the integer part of the
	 * param marker)
	 *
	 * @param sourcePosition The position within the query string
	 */
	void jpaPositionalParameter(int position, int sourcePosition);

	/**
	 * Called when a character that is not a parameter (nor part of a
	 * parameter definition) is recognized.
	 *
	 * @param character The recognized character
	 */
	void other(char character);
}
