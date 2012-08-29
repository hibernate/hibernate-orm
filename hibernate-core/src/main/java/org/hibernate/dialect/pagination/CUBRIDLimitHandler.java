package org.hibernate.dialect.pagination;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.RowSelection;

/**
 * Limit handler that delegates all operations to the underlying dialect.
 *
 * @author Esen Sagynov (kadishmal at gmail dot com)
 */
public class CUBRIDLimitHandler extends AbstractLimitHandler {
	private final Dialect dialect;

	public CUBRIDLimitHandler(Dialect dialect, String sql, RowSelection selection) {
		super( sql, selection );
		this.dialect = dialect;
	}

	public boolean supportsLimit() {
		return true;
	}

	public String getProcessedSql() {
		if (LimitHelper.useLimit(this, selection)) {
			// useLimitOffset: whether "offset" is set or not;
			// if set, use "LIMIT offset, row_count" syntax;
			// if not, use "LIMIT row_count"
			boolean useLimitOffset = LimitHelper.hasFirstRow(selection);

			return new StringBuilder(sql.length() + 20).append(sql)
							.append(useLimitOffset ? " limit ?, ?" : " limit ?").toString();
		}
		else {
			return sql; // or return unaltered SQL
		}
	}
}
