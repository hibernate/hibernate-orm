/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.sql.spi;

import org.hibernate.sql.JdbcValueBinder;
import org.hibernate.sql.JdbcValueExtractor;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;

/**
 * @author Steve Ebersole
 */
public class StandardSqlExpressableTypeImpl implements SqlExpressableType {
	private final BasicJavaDescriptor javaTypeDescriptor;
	private final SqlTypeDescriptor sqlTypeDescriptor;
	private final JdbcValueExtractor valueExtractor;
	private final JdbcValueBinder valueBinder;

	public StandardSqlExpressableTypeImpl(
			BasicJavaDescriptor javaTypeDescriptor,
			SqlTypeDescriptor sqlTypeDescriptor,
			JdbcValueExtractor valueExtractor,
			JdbcValueBinder valueBinder) {
		this.javaTypeDescriptor = javaTypeDescriptor;
		this.sqlTypeDescriptor = sqlTypeDescriptor;
		this.valueExtractor = valueExtractor;
		this.valueBinder = valueBinder;
	}

	@Override
	public BasicJavaDescriptor getJavaTypeDescriptor() {
		return javaTypeDescriptor;
	}

	@Override
	public SqlTypeDescriptor getSqlTypeDescriptor() {
		return sqlTypeDescriptor;
	}

	@Override
	public JdbcValueExtractor getJdbcValueExtractor() {
		return valueExtractor;
	}

	@Override
	public JdbcValueBinder getJdbcValueBinder() {
		return valueBinder;
	}
}
