/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm;

import java.util.Locale;

import org.hibernate.query.SemanticException;
import org.hibernate.query.sqm.tree.domain.SqmPath;

/**
 *
 *
 * todo (6.0) : account for this in the "exception conversion" handling
 *
 * @author Steve Ebersole
 */
public class UnknownPathException extends SemanticException {
	public static UnknownPathException unknownSubPath(SqmPath base, String name) {
		return new UnknownPathException(
				String.format(
						Locale.ROOT,
						"Could not resolve path `%s` relative to %s (%s)",
						name,
						base.getReferencedPathSource().getSqmPathType().getTypeName(),
						base.getNavigablePath().getFullPath()
				)
		);
	}

	private UnknownPathException(String message) {
		super( message );
	}
}
