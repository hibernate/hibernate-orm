/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.unique;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.mapping.UniqueKey;

import static org.hibernate.mapping.Index.buildSqlCreateIndexString;
import static org.hibernate.mapping.Index.buildSqlDropIndexString;

/**
 * A {@link UniqueDelegate} which uses {@code create unique index} commands when necessary.
 * <ul>
 * <li>DB2 does not allow unique constraints on nullable columns, but it does allow the creation
 *     of unique indexes instead, using {@code create unique index ... exclude null keys} or
 *     {@code create unique where not null index}, depending on flavor.
 * <li>SQL Server <em>does</em> allow unique constraints on nullable columns, but the semantics
 *     are that two null values are non-unique. So here we need to jump through hoops with the
 *     {@code create unique nonclustered index ... where ...} command.
 * </ul>
 * 
 * @author Brett Meyer
 */
public class AlterTableUniqueIndexDelegate extends AlterTableUniqueDelegate {
	public AlterTableUniqueIndexDelegate(Dialect dialect ) {
		super( dialect );
	}

	@Override
	public String getAlterTableToAddUniqueKeyCommand(UniqueKey uniqueKey, Metadata metadata,
			SqlStringGenerationContext context) {
		if ( uniqueKey.hasNullableColumn() ) {
			return buildSqlCreateIndexString(
					context,
					uniqueKey.getName(),
					uniqueKey.getTable(),
					uniqueKey.getColumns(),
					uniqueKey.getColumnOrderMap(),
					true,
					metadata
			);
		}
		else {
			return super.getAlterTableToAddUniqueKeyCommand( uniqueKey, metadata, context );
		}
	}
	
	@Override
	public String getAlterTableToDropUniqueKeyCommand(UniqueKey uniqueKey, Metadata metadata,
			SqlStringGenerationContext context) {
		if ( uniqueKey.hasNullableColumn() ) {
			return buildSqlDropIndexString(
					uniqueKey.getName(),
					context.format( uniqueKey.getTable().getQualifiedTableName() )
			);
		}
		else {
			return super.getAlterTableToDropUniqueKeyCommand( uniqueKey, metadata, context );
		}
	}
}
