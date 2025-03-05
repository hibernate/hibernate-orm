/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
