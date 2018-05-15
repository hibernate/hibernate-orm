/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import javax.persistence.metamodel.Type;

/**
 * Indicates a problem during {@link ImpliedTypeSqmExpression#impliedType(Type)} execution
 *
 * @author Steve Ebersole
 */
public class TypeInferenceException extends RuntimeException {
	public TypeInferenceException(String message) {
		super( message );
	}

	public TypeInferenceException(String message, Throwable cause) {
		super( message, cause );
	}
}
