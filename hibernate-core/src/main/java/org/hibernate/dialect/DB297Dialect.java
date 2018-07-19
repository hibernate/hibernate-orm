/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.Types;

import org.hibernate.dialect.function.DB2SubstringFunction;
import org.hibernate.hql.spi.id.IdTableSupportStandardImpl;
import org.hibernate.hql.spi.id.MultiTableBulkIdStrategy;
import org.hibernate.hql.spi.id.global.GlobalTemporaryTableBulkIdStrategy;
import org.hibernate.hql.spi.id.local.AfterUseAction;
import org.hibernate.type.descriptor.sql.CharTypeDescriptor;
import org.hibernate.type.descriptor.sql.ClobTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;
import org.hibernate.type.descriptor.sql.VarcharTypeDescriptor;

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

	@Override
	protected SqlTypeDescriptor getSqlTypeDescriptorOverride(int sqlCode) {
		// See HHH-12753
		// It seems that DB2's JDBC 4.0 support as of 9.5 does not support the N-variant methods like
		// NClob or NString.  Therefore here we overwrite the sql type descriptors to use the non-N variants
		// which are supported.
		switch ( sqlCode ) {
			case Types.NCHAR:
				return CharTypeDescriptor.INSTANCE;

			case Types.NCLOB:
				if ( useInputStreamToInsertBlob() ) {
					return ClobTypeDescriptor.STREAM_BINDING;
				}
				else {
					return ClobTypeDescriptor.CLOB_BINDING;
				}

			case Types.NVARCHAR:
				return VarcharTypeDescriptor.INSTANCE;

			default:
				return super.getSqlTypeDescriptorOverride( sqlCode );
		}
	}
}
