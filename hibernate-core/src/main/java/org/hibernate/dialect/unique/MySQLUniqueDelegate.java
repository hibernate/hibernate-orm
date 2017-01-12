/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.unique;

import org.hibernate.boot.Metadata;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.mapping.UniqueKey;

/**
 * @author Andrea Boriero
 */
public class MySQLUniqueDelegate extends DefaultUniqueDelegate {

	/**
	 * Constructs MySQLUniqueDelegate
	 *
	 * @param dialect The dialect for which we are handling unique constraints
	 */
	public MySQLUniqueDelegate(Dialect dialect) {
		super( dialect );
	}

	@Override
	protected String getDropUnique() {
		return " drop index ";
	}
}
