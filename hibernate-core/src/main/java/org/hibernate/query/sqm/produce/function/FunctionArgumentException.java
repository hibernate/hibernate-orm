/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.sqm.produce.function;

import org.hibernate.query.SemanticException;

/**
 * Represents a problem with the argument list of a function in HQL/JPQL.
 *
 * @author Gavin King
 *
 * @since 6.3
 */
public class FunctionArgumentException extends SemanticException {
	public FunctionArgumentException(String message) {
		super(message);
	}
}
