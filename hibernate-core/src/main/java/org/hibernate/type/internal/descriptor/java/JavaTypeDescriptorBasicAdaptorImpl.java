/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.internal.descriptor.java;

import java.util.Comparator;

import org.hibernate.type.spi.descriptor.JdbcRecommendedSqlTypeMappingContext;
import org.hibernate.type.spi.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.spi.AbstractBasicTypeDescriptor;
import org.hibernate.type.descriptor.java.spi.MutabilityPlan;
import org.hibernate.type.spi.descriptor.sql.SqlTypeDescriptor;

/**
 * AbstractTypeDescriptorBasicImpl adaptor for cases where we do not know a
 * proper JavaTypeDescriptor for a given Java type.
 *
 * @author Steve Ebersole
 */
public class JavaTypeDescriptorBasicAdaptorImpl<T> extends AbstractBasicTypeDescriptor<T> {
	public JavaTypeDescriptorBasicAdaptorImpl(Class<T> type) {
		super( type );
	}

	protected JavaTypeDescriptorBasicAdaptorImpl(
			Class<T> type,
			MutabilityPlan<T> mutabilityPlan) {
		super( type, mutabilityPlan );
	}

	public JavaTypeDescriptorBasicAdaptorImpl(
			Class<T> type,
			MutabilityPlan<T> mutabilityPlan,
			Comparator comparator) {
		super( type, mutabilityPlan, comparator );
	}

	@Override
	public SqlTypeDescriptor getJdbcRecommendedSqlType(JdbcRecommendedSqlTypeMappingContext context) {
		throw new UnsupportedOperationException(
				"Recommended SqlTypeDescriptor not known for this Java type : " + getJavaTypeClass().getName()
		);
	}

	@Override
	public String toString(T value) {
		return value.toString();
	}

	@Override
	public T fromString(String string) {
		throw new UnsupportedOperationException(
				"Conversion from String strategy not known for this Java type : " + getJavaTypeClass().getName()
		);
	}

	@Override
	public <X> X unwrap(T value, Class<X> type, WrapperOptions options) {
		throw new UnsupportedOperationException(
				"Unwrap strategy not known for this Java type : " + getJavaTypeClass().getName()
		);
	}

	@Override
	public <X> T wrap(X value, WrapperOptions options) {
		throw new UnsupportedOperationException(
				"Wrap strategy not known for this Java type : " + getJavaTypeClass().getName()
		);
	}
}
