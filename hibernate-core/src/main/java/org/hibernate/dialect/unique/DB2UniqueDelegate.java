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
import org.hibernate.mapping.Column;
import org.hibernate.mapping.UniqueKey;

import static org.hibernate.mapping.Index.buildSqlCreateIndexString;

/**
 * DB2 does not allow unique constraints on nullable columns.  Rather than
 * forcing "not null", use unique *indexes* instead.
 * 
 * @author Brett Meyer
 */
public class DB2UniqueDelegate extends DefaultUniqueDelegate {
	/**
	 * Constructs a DB2UniqueDelegate
	 *
	 * @param dialect The dialect
	 */
	public DB2UniqueDelegate( Dialect dialect ) {
		super( dialect );
	}

	@Override
	public String getAlterTableToAddUniqueKeyCommand(UniqueKey uniqueKey, Metadata metadata,
			SqlStringGenerationContext context) {
		if ( hasNullable( uniqueKey ) ) {
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
		if ( hasNullable( uniqueKey ) ) {
			return org.hibernate.mapping.Index.buildSqlDropIndexString(
					uniqueKey.getName(),
					context.format( uniqueKey.getTable().getQualifiedTableName() )
			);
		}
		else {
			return super.getAlterTableToDropUniqueKeyCommand( uniqueKey, metadata, context );
		}
	}
	
	private boolean hasNullable(UniqueKey uniqueKey) {
		for ( Column column : uniqueKey.getColumns() ) {
			if ( column.isNullable() ) {
				return true;
			}
		}
		return false;
	}
}
