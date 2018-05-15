/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.unique;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.metamodel.model.relational.spi.ExportableTable;
import org.hibernate.metamodel.model.relational.spi.UniqueKey;

/**
 * Informix requires the constraint name to come last on the alter table.
 * 
 * @author Brett Meyer
 */
public class InformixUniqueDelegate extends DefaultUniqueDelegate {
	
	public InformixUniqueDelegate( Dialect dialect ) {
		super( dialect );
	}

	// legacy model ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public String getAlterTableToAddUniqueKeyCommand(UniqueKey uniqueKey, JdbcServices jdbcServices) {
		final JdbcEnvironment jdbcEnvironment = jdbcServices.getJdbcEnvironment();

		// Do this here, rather than allowing UniqueKey/Constraint to do it.
		// We need full, simplified control over whether or not it happens.
		final String tableName = jdbcEnvironment.getQualifiedObjectNameFormatter().format(
				( (ExportableTable) uniqueKey.getTable() ).getQualifiedTableName(),
				jdbcEnvironment.getDialect()
		);
		final String constraintName = uniqueKey.getName().render( dialect );
		return dialect.getAlterTableString( tableName )
				+ " add constraint " + uniqueConstraintSql( uniqueKey ) + " constraint " + constraintName;
	}

}
