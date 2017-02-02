/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast;
import antlr.RecognitionException;

/**
 * Implementations will report or handle errors invoked by an ANTLR base parser.
 *
 * @author josh
 */
public interface ErrorReporter {
	void reportError(RecognitionException e);

	void reportError(String s);

	void reportWarning(String s);
}
