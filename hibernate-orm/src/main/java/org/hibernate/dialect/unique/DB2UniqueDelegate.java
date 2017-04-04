/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.unique;

import java.util.Iterator;

import org.hibernate.boot.Metadata;
import org.hibernate.dialect.Dialect;
import org.hibernate.mapping.UniqueKey;

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
	public String getAlterTableToAddUniqueKeyCommand(UniqueKey uniqueKey, Metadata metadata) {
		if ( hasNullable( uniqueKey ) ) {
			return org.hibernate.mapping.Index.buildSqlCreateIndexString(
					dialect,
					uniqueKey.getName(),
					uniqueKey.getTable(),
					uniqueKey.columnIterator(),
					uniqueKey.getColumnOrderMap(),
					true,
					metadata
			);
		}
		else {
			return super.getAlterTableToAddUniqueKeyCommand( uniqueKey, metadata );
		}
	}
	
	@Override
	public String getAlterTableToDropUniqueKeyCommand(UniqueKey uniqueKey, Metadata metadata) {
		if ( hasNullable( uniqueKey ) ) {
			return org.hibernate.mapping.Index.buildSqlDropIndexString(
					uniqueKey.getName(),
					metadata.getDatabase().getJdbcEnvironment().getQualifiedObjectNameFormatter().format(
							uniqueKey.getTable().getQualifiedTableName(),
							metadata.getDatabase().getJdbcEnvironment().getDialect()
					)
			);
		}
		else {
			return super.getAlterTableToDropUniqueKeyCommand( uniqueKey, metadata );
		}
	}
	
	private boolean hasNullable(org.hibernate.mapping.UniqueKey uniqueKey) {
		final Iterator<org.hibernate.mapping.Column> iter = uniqueKey.columnIterator();
		while ( iter.hasNext() ) {
			if ( iter.next().isNullable() ) {
				return true;
			}
		}
		return false;
	}
}
