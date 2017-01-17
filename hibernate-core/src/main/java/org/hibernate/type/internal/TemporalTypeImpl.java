/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.internal;

import java.util.Comparator;

import org.hibernate.type.descriptor.java.spi.MutabilityPlan;
import org.hibernate.type.descriptor.java.spi.TemporalJavaDescriptor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.type.spi.BasicTypeRegistry;
import org.hibernate.type.spi.ColumnMapping;
import org.hibernate.type.spi.TemporalType;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class TemporalTypeImpl<T> extends BasicTypeImpl<T> implements TemporalType<T> {
	private final javax.persistence.TemporalType precision;

	public TemporalTypeImpl(
			TemporalJavaDescriptor javaDescriptor,
			MutabilityPlan mutabilityPlan,
			Comparator comparator,
			ColumnMapping columnMapping,
			javax.persistence.TemporalType precision) {
		this( javaDescriptor, mutabilityPlan, comparator, columnMapping.getSqlTypeDescriptor(), precision );
	}

	public TemporalTypeImpl(
			TemporalJavaDescriptor javaDescriptor,
			MutabilityPlan mutabilityPlan,
			Comparator comparator,
			SqlTypeDescriptor sqlTypeDescriptor,
			javax.persistence.TemporalType precision) {
		super( javaDescriptor, mutabilityPlan, comparator, new ColumnMapping( sqlTypeDescriptor ) );
		this.precision = precision == null ? javaDescriptor.getPrecision() : precision;
	}

	@Override
	public TemporalJavaDescriptor<T> getJavaTypeDescriptor() {
		return (TemporalJavaDescriptor<T>) super.getJavaTypeDescriptor();
	}

	@Override
	public javax.persistence.TemporalType getPrecision() {
		return precision;
	}

	@Override
	public <X> TemporalType<X> resolveTypeForPrecision(
			javax.persistence.TemporalType precision,
			TypeConfiguration typeConfiguration) {
		return (TemporalType<X>) typeConfiguration.getBasicTypeRegistry().getBasicType(
				BasicTypeRegistry.Key.from(
						getJavaTypeDescriptor().resolveTypeForPrecision( precision, typeConfiguration ),
						getColumnMapping().getSqlTypeDescriptor()
				)
		);
	}
}
