/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

/**
 * Dialect for Informix 10
 *
 * @deprecated use {@code InformixDialect(10)}
 */
@Deprecated
public class Informix10Dialect extends InformixDialect {

	public Informix10Dialect() {
		super(10);
	}

}
