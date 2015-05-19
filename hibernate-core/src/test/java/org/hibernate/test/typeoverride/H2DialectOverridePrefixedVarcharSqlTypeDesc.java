/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.typeoverride;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;
import org.hibernate.type.descriptor.sql.VarcharTypeDescriptor;

/**
 *
 * @author Gail Badner
 */
public class H2DialectOverridePrefixedVarcharSqlTypeDesc extends H2Dialect {
	@Override
    public SqlTypeDescriptor remapSqlTypeDescriptor(SqlTypeDescriptor sqlTypeDescriptor) {
		return  sqlTypeDescriptor == StoredPrefixedStringType.INSTANCE.getSqlTypeDescriptor() ?
				VarcharTypeDescriptor.INSTANCE :
				super.remapSqlTypeDescriptor( sqlTypeDescriptor );
	}
}

