/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.hql;

import org.hibernate.HibernateException;

/**
 * Base of exception hierarchy for exceptions stemming from
 * producing SQM AST trees
 *
 * @author Steve Ebersole
 */
public class HqlInterpretationException extends HibernateException {
	public HqlInterpretationException(String message) {
		super( message );
	}

	public HqlInterpretationException(String message, Throwable cause) {
		super( message, cause );
	}
}
