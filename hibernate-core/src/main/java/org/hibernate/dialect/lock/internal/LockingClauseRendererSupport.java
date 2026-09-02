/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock.internal;

import java.util.List;

import org.hibernate.dialect.lock.spi.LockingClauseRequest;

/// Internal helpers shared by built-in locking-clause renderers.
///
/// @author Steve Ebersole
final class LockingClauseRendererSupport {
	private LockingClauseRendererSupport() {
	}

	static void appendTargets(StringBuilder fragment, List<LockingClauseRequest.Target> targets) {
		boolean first = true;
		for ( LockingClauseRequest.Target target : targets ) {
			if ( first ) {
				first = false;
			}
			else {
				fragment.append( ',' );
			}
			if ( target instanceof LockingClauseRequest.TableTarget table ) {
				fragment.append( table.tableAlias() );
			}
			else if ( target instanceof LockingClauseRequest.ColumnTarget column ) {
				fragment.append( column.tableAlias() )
						.append( '.' )
						.append( column.columnName() );
			}
		}
	}
}
