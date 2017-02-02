/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.unique;

import org.hibernate.boot.Metadata;
import org.hibernate.dialect.Dialect;
import org.hibernate.mapping.UniqueKey;

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
	public String getAlterTableToAddUniqueKeyCommand(UniqueKey uniqueKey, Metadata metadata) {
		// Do this here, rather than allowing UniqueKey/Constraint to do it.
		// We need full, simplified control over whether or not it happens.
		final String tableName = metadata.getDatabase().getJdbcEnvironment().getQualifiedObjectNameFormatter().format(
				uniqueKey.getTable().getQualifiedTableName(),
				metadata.getDatabase().getJdbcEnvironment().getDialect()
		);
		final String constraintName = dialect.quote( uniqueKey.getName() );
		return "alter table " + tableName + " add constraint " + uniqueConstraintSql( uniqueKey ) + " constraint " + constraintName;
	}

}
