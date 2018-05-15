/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.Types;

import org.hibernate.dialect.function.DB2SubstringFunctionTemplate;
import org.hibernate.query.sqm.consume.multitable.internal.StandardIdTableSupport;
import org.hibernate.query.sqm.consume.multitable.spi.IdTableStrategy;
import org.hibernate.query.sqm.consume.multitable.spi.idtable.GlobalTempTableExporter;
import org.hibernate.query.sqm.consume.multitable.spi.idtable.GlobalTemporaryTableStrategy;
import org.hibernate.query.sqm.consume.multitable.spi.idtable.IdTable;
import org.hibernate.query.sqm.consume.multitable.spi.idtable.IdTableSupport;
import org.hibernate.query.sqm.produce.function.SqmFunctionRegistry;
import org.hibernate.tool.schema.spi.Exporter;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.type.descriptor.sql.spi.CharSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.ClobSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.VarcharSqlDescriptor;
import org.hibernate.type.spi.StandardSpiBasicTypes;

/**
 * An SQL dialect for DB2 9.7.
 *
 * @author Gail Badner
 */
public class DB297Dialect extends DB2Dialect {

	public DB297Dialect() {
		super();
	}

	@Override
	public void initializeFunctionRegistry(SqmFunctionRegistry registry) {
		super.initializeFunctionRegistry( registry );

		registry.registerNamed( "chr", StandardSpiBasicTypes.CHARACTER );
		registry.register( "substring", new DB2SubstringFunctionTemplate() );
	}

	@Override
	public String getCrossJoinSeparator() {
		// DB2 9.7 and later support "cross join"
		return " cross join ";
	}

	@Override
	public IdTableStrategy getDefaultIdTableStrategy() {
		// Starting in DB2 9.7, "real" global temporary tables that can be shared between sessions
		// are supported; (obviously) data is not shared between sessions.
		return new GlobalTemporaryTableStrategy(
				generateIdTableSupport()
		);
	}

	@Override
	protected IdTableSupport generateIdTableSupport() {
		return new StandardIdTableSupport( new GlobalTempTableExporter() ) {
			@Override
			public Exporter<IdTable> getIdTableExporter() {
				return generateIdTableExporter();
			}
		};
	}

	@Override
	protected Exporter<IdTable> generateIdTableExporter() {
		return new GlobalTempTableExporter() {
			@Override
			protected String getCreateOptions() {
				return "not logged";
			}

			@Override
			protected String getCreateCommand() {
				return "create global temporary table";
			}
		};
	}

	@Override
	protected SqlTypeDescriptor getSqlTypeDescriptorOverride(int sqlCode) {
		// See HHH-12753
		// It seems that DB2's JDBC 4.0 support as of 9.5 does not support the N-variant methods like
		// NClob or NString.  Therefore here we overwrite the sql type descriptors to use the non-N variants
		// which are supported.
		switch ( sqlCode ) {
			case Types.NCHAR:
				return CharSqlDescriptor.INSTANCE;

			case Types.NCLOB:
				if ( useInputStreamToInsertBlob() ) {
					return ClobSqlDescriptor.STREAM_BINDING;
				}
				else {
					return ClobSqlDescriptor.CLOB_BINDING;
				}

			case Types.NVARCHAR:
				return VarcharSqlDescriptor.INSTANCE;

			default:
				return super.getSqlTypeDescriptorOverride( sqlCode );
		}
	}
}
