/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.typeoverride;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.VarcharTypeDescriptor;

/**
 *
 * @author Gail Badner
 */
public class H2DialectOverridePrefixedVarcharSqlTypeDesc extends H2Dialect {
	@Override
    public JdbcTypeDescriptor remapSqlTypeDescriptor(JdbcTypeDescriptor jdbcTypeDescriptor) {
		return  jdbcTypeDescriptor == StoredPrefixedStringType.INSTANCE.getJdbcTypeDescriptor() ?
				VarcharTypeDescriptor.INSTANCE :
				super.remapSqlTypeDescriptor( jdbcTypeDescriptor );
	}
}

