/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.sql.JoinFragment;
import org.hibernate.sql.Sybase11JoinFragment;

/**
 * A SQL dialect suitable for use with Sybase 11.9.2 (specifically: avoids ANSI JOIN syntax)
 *
 * @author Colm O' Flaherty
 */
public class Sybase11Dialect extends SybaseDialect  {
	/**
	 * Constructs a Sybase11Dialect
	 */
	public Sybase11Dialect() {
		super();
	}

	@Override
	public JoinFragment createOuterJoinFragment() {
		return new Sybase11JoinFragment();
	}

	@Override
	public String getCrossJoinSeparator() {
		return ", ";
	}
}
