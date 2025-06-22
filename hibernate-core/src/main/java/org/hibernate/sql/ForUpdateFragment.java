/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql;

import java.util.Map;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.QueryException;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.RowLockStrategy;
import org.hibernate.internal.util.StringHelper;

/**
 * A SQL {@code FOR UPDATE} clause.
 *
 * @author Gavin King
 */
public class ForUpdateFragment {
	private final StringBuilder aliases = new StringBuilder();
	private final Dialect dialect;
	private final LockOptions lockOptions;

	public ForUpdateFragment(Dialect dialect, LockOptions lockOptions, Map<String, String[]> keyColumnNames) {
		this.dialect = dialect;
		LockMode upgradeType = null;
		this.lockOptions =  lockOptions;

		if ( !lockOptions.getAliasSpecificLocks().iterator().hasNext() ) {  // no tables referenced
			final LockMode lockMode = lockOptions.getLockMode();
			if ( LockMode.READ.lessThan(lockMode) ) {
				upgradeType = lockMode;
			}
		}
		else {
			for ( Map.Entry<String, LockMode> me : lockOptions.getAliasSpecificLocks() ) {
				final LockMode lockMode = me.getValue();
				if ( LockMode.READ.lessThan(lockMode) ) {
					final String tableAlias = me.getKey();
					if ( dialect.getWriteRowLockStrategy() == RowLockStrategy.COLUMN ) {
						String[] keyColumns = keyColumnNames.get( tableAlias ); //use the id column alias
						if ( keyColumns == null ) {
							throw new IllegalArgumentException( "alias not found: " + tableAlias );
						}
						keyColumns = StringHelper.qualify( tableAlias, keyColumns );
						for ( String keyColumn : keyColumns ) {
							addTableAlias( keyColumn );
						}
					}
					else {
						addTableAlias( tableAlias );
					}
					if ( upgradeType != null && lockMode != upgradeType ) {
						throw new QueryException( "Mixed LockModes" );
					}
					upgradeType = lockMode;
				}
			}
		}
	}

	public ForUpdateFragment addTableAlias(String alias) {
		if ( !aliases.isEmpty() ) {
			aliases.append( ", " );
		}
		aliases.append( alias );
		return this;
	}

	public String toFragmentString() {
		if ( aliases.isEmpty() ) {
			return dialect.getForUpdateString( lockOptions );
		}
		else {
			return dialect.getForUpdateString( aliases.toString(), lockOptions );
		}
	}


}
