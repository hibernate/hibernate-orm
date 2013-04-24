/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.dialect.pagination;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.RowSelection;

/**
 * Limit handler for CUBRID
 *
 * @author Esen Sagynov (kadishmal at gmail dot com)
 */
public class CUBRIDLimitHandler extends AbstractLimitHandler {
	@SuppressWarnings("FieldCanBeLocal")
	private final Dialect dialect;

	/**
	 * Constructs a CUBRIDLimitHandler
	 *
	 * @param dialect Currently not used
	 * @param sql The SQL
	 * @param selection The row selection options
	 */
	public CUBRIDLimitHandler(Dialect dialect, String sql, RowSelection selection) {
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
			// useLimitOffset: whether "offset" is set or not;
			// if set, use "LIMIT offset, row_count" syntax;
			// if not, use "LIMIT row_count"
			final boolean useLimitOffset = LimitHelper.hasFirstRow( selection );
			return sql + (useLimitOffset ? " limit ?, ?" : " limit ?");
		}
		else {
			// or return unaltered SQL
			return sql;
		}
	}
}
