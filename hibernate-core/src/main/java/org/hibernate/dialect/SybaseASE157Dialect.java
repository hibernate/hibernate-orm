/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

/**
 * An SQL dialect targeting Sybase Adaptive Server Enterprise (ASE) 15.7 and higher.
 *
 * @author Junyan Ren
 *
 * @deprecated use {@code SybaseASEDialect(1570)}
 */
@Deprecated
public class SybaseASE157Dialect extends SybaseASEDialect {

	public SybaseASE157Dialect() {
		super(1570);
	}

}
