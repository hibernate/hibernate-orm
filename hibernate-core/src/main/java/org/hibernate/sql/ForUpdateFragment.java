/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.RowLockStrategy;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;

import java.util.Map;

/**
 * A SQL {@code FOR UPDATE} clause.
 *
 * @author Gavin King
 */
public class ForUpdateFragment {
	private final StringBuilder lockItemFragment = new StringBuilder();
	private final Dialect dialect;
	private final LockOptions lockOptions;

	public ForUpdateFragment(Dialect dialect, LockOptions lockOptions, Map<String, String[]> keyColumnNameMap) {
		this.dialect = dialect;
		this.lockOptions =  lockOptions;

		if ( lockOptions.getLockMode() == LockMode.NONE ) {
			return;
		}

		if ( CollectionHelper.isEmpty( keyColumnNameMap ) ) {
			return;
		}

		final RowLockStrategy lockStrategy = dialect.getWriteRowLockStrategy();
		if ( lockStrategy == RowLockStrategy.NONE ) {
			return;
		}

		keyColumnNameMap.forEach( (tableAlias, keyColumnNames) -> {
			if ( lockStrategy == RowLockStrategy.TABLE ) {
				addLockItem( tableAlias );
			}
			else {
				assert lockStrategy == RowLockStrategy.COLUMN;
				for ( String keyColumnReference : StringHelper.qualify( tableAlias, keyColumnNames ) ) {
					addLockItem( keyColumnReference );
				}
			}
		} );
	}

	public ForUpdateFragment addTableAlias(String alias) {
		addLockItem( alias );
		return this;
	}

	public ForUpdateFragment addLockItem(String itemText) {
		if ( !lockItemFragment.isEmpty() ) {
			lockItemFragment.append( ", " );
		}
		lockItemFragment.append( itemText );
		return this;
	}

	public String toFragmentString() {
		if ( lockItemFragment.isEmpty() ) {
			return dialect.getForUpdateString( lockOptions );
		}
		else {
			return dialect.getForUpdateString( lockItemFragment.toString(), lockOptions );
		}
	}


}
