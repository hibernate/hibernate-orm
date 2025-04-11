/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm;

import java.util.Locale;

import org.hibernate.query.PathException;
import org.hibernate.query.sqm.tree.domain.SqmPath;

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


	public static UnknownPathException unknownSubPath(SqmPath base, String name) {
		return new UnknownPathException(
				String.format(
						Locale.ROOT,
						"Could not resolve path element '%s' relative to '%s' (%s)",
						name,
						base.getReferencedPathSource().getPathType().getTypeName(),
						base.getNavigablePath()
				)
		);
	}
}
