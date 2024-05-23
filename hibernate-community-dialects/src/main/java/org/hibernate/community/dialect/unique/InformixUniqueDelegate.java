/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.community.dialect.unique;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.unique.SkipNullableUniqueDelegate;
import org.hibernate.mapping.UniqueKey;

/**
 * Informix requires the constraint name to come last.
 *
 * @author Brett Meyer
 */
public class InformixUniqueDelegate extends SkipNullableUniqueDelegate {

	public InformixUniqueDelegate(Dialect dialect) {
		super( dialect );
	}

	@Override
	protected void appendUniqueConstraint(StringBuilder fragment, UniqueKey uniqueKey) {
		if ( !uniqueKey.hasNullableColumn() ) {
			fragment.append( ", " );
			fragment.append( uniqueConstraintSql( uniqueKey ) );
			if ( uniqueKey.isNameExplicit() ) {
				fragment.append( " constraint " ).append( uniqueKey.getName() );
			}
		}
	}

	@Override
	public String getAlterTableToAddUniqueKeyCommand(
			UniqueKey uniqueKey, Metadata metadata,
			SqlStringGenerationContext context) {
		if ( uniqueKey.hasNullableColumn() || !context.isMigration() ) {
			return "";
		}
		else {
			final String tableName = context.format( uniqueKey.getTable().getQualifiedTableName() );
			final String constraintName = dialect.quote( uniqueKey.getName() );
			return dialect.getAlterTableString( tableName )
					+ " add constraint " + uniqueConstraintSql( uniqueKey ) + " constraint " + constraintName;
		}
	}

}
