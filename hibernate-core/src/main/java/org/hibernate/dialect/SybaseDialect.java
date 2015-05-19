/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.Types;

import org.hibernate.type.descriptor.sql.BlobTypeDescriptor;
import org.hibernate.type.descriptor.sql.ClobTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;


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
			return BlobTypeDescriptor.PRIMITIVE_ARRAY_BINDING;
		case Types.CLOB:
			// Some Sybase drivers cannot support getClob.  See HHH-7889
			return ClobTypeDescriptor.STREAM_BINDING_EXTRACTING;
		default:
			return super.getSqlTypeDescriptorOverride( sqlCode );
		}
	}
	
	@Override
	public String getNullColumnString() {
		return " null";
	}
}
