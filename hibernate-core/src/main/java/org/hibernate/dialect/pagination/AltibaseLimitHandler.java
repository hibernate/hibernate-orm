/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.pagination;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.RowSelection;

/**
 * Limit handler for Altibase
 *
 * @author YounJung Park
 */
public class AltibaseLimitHandler extends AbstractLimitHandler {
	@SuppressWarnings("FieldCanBeLocal")
	private final Dialect dialect;

	/**
	 * Constructs a Altibase LimitHandler
	 *
	 * @param dialect Currently not used
	 * @param sql The SQL
	 * @param selection The row selection options
	 */
	public AltibaseLimitHandler(Dialect dialect, String sql, RowSelection selection) {
		super( sql, selection );
		this.dialect = dialect;
	}

	@Override
	public boolean supportsLimit() {
		return true;
	}

	@Override
	public String getProcessedSql() {
		if ( LimitHelper.useLimit( this, selection ) ) {
			final boolean useLimitOffset = LimitHelper.hasFirstRow( selection );
			return sql + (useLimitOffset ? " limit ?, ?" : " limit ?");
		}
		else {
			// or return unaltered SQL
			return sql;
		}
	}
}
