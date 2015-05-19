/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.Types;

/**
 * Dialect for Derby 10.7
 *
 * @author Strong Liu
 */
public class DerbyTenSevenDialect extends DerbyTenSixDialect {
	/**
	 * Constructs a DerbyTenSevenDialect
	 */
	public DerbyTenSevenDialect() {
		super();
		registerColumnType( Types.BOOLEAN, "boolean" );
	}

	@Override
	public String toBooleanValueString(boolean bool) {
		return String.valueOf( bool );
	}
}
