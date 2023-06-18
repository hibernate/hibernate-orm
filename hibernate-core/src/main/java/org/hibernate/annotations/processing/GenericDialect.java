/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations.processing;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;

/**
 * A generic {@linkplain Dialect dialect} for ANSI-like SQL.
 * Used by default in the HQL Query Validator.
 *
 * @author Gavin King
 *
 * @see CheckHQL#dialect
 */
public class GenericDialect extends Dialect {
	public GenericDialect() {
		super( (DatabaseVersion) null );
	}
}
