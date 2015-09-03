/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.identity;

/**
 * @author Andrea Boriero
 */
public class DB2390IdentityColumnSupport extends DB2IdentityColumnSupport {
	@Override
	public String getIdentitySelectString(String table, String column, int type) {
		return "select identity_val_local() from sysibm.sysdummy1";
	}
}
