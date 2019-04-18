/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.Types;

import org.hibernate.dialect.function.LocateEmulationFunction;
import org.hibernate.query.sqm.produce.function.SqmFunctionRegistry;
import org.hibernate.type.descriptor.sql.spi.BlobSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.ClobSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.type.spi.StandardSpiBasicTypes;


/**
 * All Sybase dialects share an IN list size limit.
 *
 * @author Brett Meyer
 */
public class SybaseDialect extends AbstractTransactSQLDialect {
	private static final int PARAM_LIST_SIZE_LIMIT = 250000;

	@Override
	public int getInExpressionCountLimit() {
		return PARAM_LIST_SIZE_LIMIT;
	}
	
	@Override
	protected SqlTypeDescriptor getSqlTypeDescriptorOverride(int sqlCode) {
		switch (sqlCode) {
		case Types.BLOB:
			return BlobSqlDescriptor.PRIMITIVE_ARRAY_BINDING;
		case Types.CLOB:
			// Some Sybase drivers cannot support getClob.  See HHH-7889
			return ClobSqlDescriptor.STREAM_BINDING_EXTRACTING;
		default:
			return super.getSqlTypeDescriptorOverride( sqlCode );
		}
	}

	@Override
	public void initializeFunctionRegistry(SqmFunctionRegistry registry) {
		super.initializeFunctionRegistry(registry);

		registry.register(
				"locate",
				new LocateEmulationFunction(
						registry.patternTemplateBuilder( "locate/2", "locate(?2, ?1)" )
								.setExactArgumentCount( 2 )
								.setInvariantType( StandardSpiBasicTypes.INTEGER )
								.register(),
						registry.patternTemplateBuilder( "locate/3", "locate(?2, ?1, ?3)" )
								.setExactArgumentCount( 3 )
								.setInvariantType( StandardSpiBasicTypes.INTEGER )
								.register()
				)
		);

	}

	@Override
	public String getNullColumnString() {
		return " null";
	}

	@Override
	public String getCurrentSchemaCommand() {
		return "select db_name()";
	}
}
