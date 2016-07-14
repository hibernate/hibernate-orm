/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.sqm.convert.spi;

import org.hibernate.sql.sqm.ast.from.TableJoin;

/**
 * Indicates a problem in the definition of a {@link TableJoin}
 *
 * @author Steve Ebersole
 */
public class IllegalJoinSpecificationException extends SqlTreeException {
	public IllegalJoinSpecificationException(String message) {
		super( message );
	}
}
