/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

/**
 * @author Emmanuel Bernard
 */
public class MyNamingStrategy extends PhysicalNamingStrategyStandardImpl {
	@Override
	public Identifier toPhysicalTableName(Identifier name, JdbcEnvironment jdbcEnvironment) {
		return jdbcEnvironment.getIdentifierHelper().toIdentifier( "tbl_" + name.getText() );
	}
}
