/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.java.internal;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.java.spi.AbstractBasicJavaDescriptor;
import org.hibernate.type.descriptor.spi.JdbcRecommendedSqlTypeMappingContext;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class NonStandardBasicJavaTypeDescriptor<T>
		extends AbstractBasicJavaDescriptor<T> {
	public NonStandardBasicJavaTypeDescriptor(Class<T> type) {
		super( type );
	}

	@Override
	public SqlTypeDescriptor getJdbcRecommendedSqlType(JdbcRecommendedSqlTypeMappingContext context) {
		// none
		return null;
	}

	@Override
	public String toString(T value) {
		return null;
	}

	@Override
	public T fromString(String string) {
		return null;
	}

	@Override
	public <X> X unwrap(T value, Class<X> type, SharedSessionContractImplementor session) {
		return null;
	}

	@Override
	public <X> T wrap(X value, SharedSessionContractImplementor session) {
		return null;
	}

	public String asLoggableText() {
		return "{non-standard-basic-type(" + getJavaType().getName() + "}";
	}

}
