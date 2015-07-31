/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.namingstrategy.ejb3joincolumn;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

/**
 * @author Anton Wimmer
 * @author Steve Ebersole
 */
public class PhysicalNamingStrategyImpl extends PhysicalNamingStrategyStandardImpl {
	/**
	 * Singleton access
	 */
	public static final PhysicalNamingStrategyImpl INSTANCE = new PhysicalNamingStrategyImpl();

	@Override
	public Identifier toPhysicalTableName(Identifier name, JdbcEnvironment jdbcEnvironment) {
		return Identifier.toIdentifier(makeCleanIdentifier("tbl_" + name.getText()), name.isQuoted());
	}

	@Override
	public Identifier toPhysicalColumnName(Identifier name, JdbcEnvironment jdbcEnvironment) {
		if ( name.getText().equals("DTYPE") ) {
			return name;
		}

		return Identifier.toIdentifier(makeCleanIdentifier("c_" + name.getText()), name.isQuoted());
	}

	private String makeCleanIdentifier(String s) {
		return s.substring(0, Math.min(s.length(), 63)).toLowerCase();
	}
}
