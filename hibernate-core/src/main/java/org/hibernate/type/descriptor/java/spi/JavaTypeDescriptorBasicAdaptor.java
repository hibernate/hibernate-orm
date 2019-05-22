/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.java.spi;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.java.AbstractTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptorIndicators;

/**
 * AbstractBasicTypeDescriptor adapter for cases where we do not know a proper JavaTypeDescriptor
 * for a given Java type.
 *
 * @author Steve Ebersole
 */
public class JavaTypeDescriptorBasicAdaptor<T> extends AbstractTypeDescriptor<T> {
	public JavaTypeDescriptorBasicAdaptor(Class<T> type) {
		super( type );
	}

	@Override
	public SqlTypeDescriptor getJdbcRecommendedSqlType(SqlTypeDescriptorIndicators context) {
		throw new UnsupportedOperationException(
				"Recommended SqlTypeDescriptor not known for this Java type : " + getJavaType().getName()
		);
	}

	@Override
	public String toString(T value) {
		return value.toString();
	}

	@Override
	public T fromString(String string) {
		throw new UnsupportedOperationException(
				"Conversion from String strategy not known for this Java type : " + getJavaType().getName()
		);
	}

	@Override
	public <X> X unwrap(T value, Class<X> type, SharedSessionContractImplementor session) {
		throw new UnsupportedOperationException(
				"Unwrap strategy not known for this Java type : " + getJavaType().getName()
		);
	}

	@Override
	public <X> T wrap(X value, SharedSessionContractImplementor session) {
		throw new UnsupportedOperationException(
				"Wrap strategy not known for this Java type : " + getJavaType().getName()
		);
	}
}
