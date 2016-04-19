/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;


import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.identity.SybaseAnywhereIdentityColumnSupport;

/**
 * SQL Dialect for Sybase Anywhere
 * extending Sybase (Enterprise) Dialect
 * (Tested on ASA 8.x)
 */
public class SybaseAnywhereDialect extends SybaseDialect {
	/**
	 * Sybase Anywhere syntax would require a "DEFAULT" for each column specified,
	 * but I suppose Hibernate use this syntax only with tables with just 1 column
	 * <p/>
	 * {@inheritDoc}
	 */
	@Override
	public String getNoColumnsInsertString() {
		return "values (default)";
	}

	/**
	 * ASA does not require to drop constraint beforeQuery dropping tables, so disable it.
	 * <p/>
	 * NOTE : Also, the DROP statement syntax used by Hibernate to drop constraints is 
	 * not compatible with ASA.
	 * <p/>
	 * {@inheritDoc}
	 */
	@Override
	public boolean dropConstraints() {
		return false;
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return new SybaseAnywhereIdentityColumnSupport();
	}
}
