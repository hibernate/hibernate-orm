/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java.internal;

import java.util.Comparator;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.spi.JdbcRecommendedSqlTypeMappingContext;
import org.hibernate.type.descriptor.java.spi.AbstractBasicJavaDescriptor;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * AbstractBasicTypeDescriptor adapter for cases where we do not know a proper JavaTypeDescriptor
 * for a given Java type.
 *
 * @author Steve Ebersole
 */
public class JavaTypeDescriptorBasicAdaptor<T> extends AbstractBasicJavaDescriptor<T> {
	public JavaTypeDescriptorBasicAdaptor(Class<T> type) {
		super( type );
	}

	protected JavaTypeDescriptorBasicAdaptor(
			Class<T> type,
			MutabilityPlan<T> mutabilityPlan) {
		super( type, mutabilityPlan );
	}

	public JavaTypeDescriptorBasicAdaptor(
			Class<T> type,
			MutabilityPlan<T> mutabilityPlan,
			Comparator comparator) {
		super( type, mutabilityPlan, comparator );
	}

	@Override
	public SqlTypeDescriptor getJdbcRecommendedSqlType(JdbcRecommendedSqlTypeMappingContext context) {
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
