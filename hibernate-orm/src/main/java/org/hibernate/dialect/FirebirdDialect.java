/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.dialect.pagination.AbstractLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.LimitHelper;
import org.hibernate.engine.spi.RowSelection;
import org.hibernate.type.StandardBasicTypes;

/**
 * An SQL dialect for Firebird.
 *
 * @author Reha CENANI
 */
public class FirebirdDialect extends InterbaseDialect {

	private static final AbstractLimitHandler LIMIT_HANDLER = new AbstractLimitHandler() {
		@Override
		public String processSql(String sql, RowSelection selection) {
			final boolean hasOffset = LimitHelper.hasFirstRow( selection );
			return new StringBuilder( sql.length() + 20 )
					.append( sql )
					.insert( 6, hasOffset ? " first ? skip ?" : " first ?" )
					.toString();
		}

		@Override
		public boolean supportsLimit() {
			return true;
		}

		@Override
		public boolean bindLimitParametersFirst() {
			return true;
		}

		@Override
		public boolean bindLimitParametersInReverseOrder() {
			return true;
		}
	};

	public FirebirdDialect() {
		super();
		registerFunction( "replace", new StandardSQLFunction( "replace", StandardBasicTypes.STRING ) );
	}
	
	@Override
	public String getDropSequenceString(String sequenceName) {
		return "drop generator " + sequenceName;
	}

	@Override
	public String getLimitString(String sql, boolean hasOffset) {
		return new StringBuilder( sql.length() + 20 )
				.append( sql )
				.insert( 6, hasOffset ? " first ? skip ?" : " first ?" )
				.toString();
	}

	@Override
	public boolean bindLimitParametersFirst() {
		return true;
	}

	@Override
	public boolean bindLimitParametersInReverseOrder() {
		return true;
	}

	@Override
	public LimitHandler getLimitHandler() {
		return LIMIT_HANDLER;
	}
}
