/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.namingstrategy;

import org.hibernate.boot.model.naming.ColumnNamingContext;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

public class ContextNamingStrategy extends PhysicalNamingStrategyStandardImpl {
	@Override
	public Identifier toPhysicalColumnName(
			Identifier logicalName,
			JdbcEnvironment jdbcEnvironment,
			ColumnNamingContext columnNamingContext) {
		return new Identifier(
				simpleName( columnNamingContext.entityName() ) + '_' + logicalName.getText(),
				logicalName.isQuoted()
		);
	}

	@Override
	public Identifier toPhysicalColumnName(Identifier logicalName, JdbcEnvironment context) {
		return new Identifier( "legacy_" + logicalName.getText(), logicalName.isQuoted() );
	}

	private String simpleName(String typeName) {
		if ( typeName == null ) {
			return null;
		}
		final int separator = Math.max( typeName.lastIndexOf( '.' ), typeName.lastIndexOf( '$' ) );
		return separator < 0 ? typeName : typeName.substring( separator + 1 );
	}
}
