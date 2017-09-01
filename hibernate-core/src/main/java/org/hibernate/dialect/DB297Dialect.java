/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.dialect.function.DB2SubstringFunction;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.hql.spi.id.IdTableSupportStandardImpl;
import org.hibernate.hql.spi.id.MultiTableBulkIdStrategy;
import org.hibernate.hql.spi.id.global.GlobalTemporaryTableBulkIdStrategy;
import org.hibernate.hql.spi.id.local.AfterUseAction;
import org.hibernate.hql.spi.id.local.LocalTemporaryTableBulkIdStrategy;
import org.hibernate.type.StandardBasicTypes;

/**
 * An SQL dialect for DB2 9.7.
 *
 * @author Gail Badner
 */
public class DB297Dialect extends DB2Dialect {

	public DB297Dialect() {
		super();
		registerFunction( "substring", new DB2SubstringFunction() );
	}

	@Override
	public String getCrossJoinSeparator() {
		// DB2 9.7 and later support "cross join"
		return " cross join ";
	}

	@Override
	public MultiTableBulkIdStrategy getDefaultMultiTableBulkIdStrategy() {
		// Starting in DB2 9.7, "real" global temporary tables that can be shared between sessions
		// are supported; (obviously) data is not shared between sessions.
		return new GlobalTemporaryTableBulkIdStrategy(
				new IdTableSupportStandardImpl() {
					@Override
					public String generateIdTableName(String baseName) {
						return super.generateIdTableName( baseName );
					}

					@Override
					public String getCreateIdTableCommand() {
						return "create global temporary table";
					}

					@Override
					public String getCreateIdTableStatementOptions() {
						return "not logged";
					}
				},
				AfterUseAction.CLEAN
		);
	}
}
