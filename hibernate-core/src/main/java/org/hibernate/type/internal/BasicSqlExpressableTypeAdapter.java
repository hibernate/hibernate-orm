/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.internal;

import org.hibernate.sql.JdbcValueBinder;
import org.hibernate.sql.JdbcValueExtractor;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class BasicSqlExpressableTypeAdapter implements SqlExpressableType {
	private final BasicJavaDescriptor domainJavaType;
	private final SqlTypeDescriptor sqlTypeDescriptor;

	private final BasicType basicType;

	private final BasicTypeBinderAdapter binder;
	private final BasicTypeExtractorAdaptor extractor;

	public BasicSqlExpressableTypeAdapter(
			BasicJavaDescriptor domainJavaType,
			SqlTypeDescriptor sqlTypeDescriptor,
			BasicType basicType) {
		this.domainJavaType = domainJavaType;
		this.sqlTypeDescriptor = sqlTypeDescriptor;
		this.basicType = basicType;

		this.binder = new BasicTypeBinderAdapter( basicType );
		this.extractor = new BasicTypeExtractorAdaptor( basicType );
	}

	@Override
	public BasicJavaDescriptor getJavaTypeDescriptor() {
		return domainJavaType;
	}

	@Override
	public SqlTypeDescriptor getSqlTypeDescriptor() {
		return sqlTypeDescriptor;
	}

	@Override
	public JdbcValueExtractor getJdbcValueExtractor() {
		return extractor;
	}

	@Override
	public JdbcValueBinder getJdbcValueBinder() {
		return binder;
	}
}
