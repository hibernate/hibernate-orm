/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm;

import org.hibernate.query.PathException;

/**
 * Indicates a failure to resolve an element of a path expression in HQL/JPQL.
 *
 * @apiNote The JPA criteria API requires that this sort of problem be reported
 *          as an {@link IllegalArgumentException} or {@link IllegalStateException},
 *          and so we usually throw {@link PathElementException} or
 *          {@link TerminalPathException} from the SQM objects, and then wrap
 *          as an instance of this exception type in the
 *          {@link org.hibernate.query.hql.HqlTranslator}.
 *
 * @author Steve Ebersole
 *
 * @see PathElementException
 * @see TerminalPathException
 */
public class UnknownPathException extends PathException {

	public UnknownPathException(String message) {
		super( message );
	}

	public UnknownPathException(String message, String hql, Exception cause) {
		super( message, hql, cause );
	}

}
