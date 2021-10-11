/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.process.internal;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptorIndicators;

/**
 * @author Steve Ebersole
 */
public class ValueConverterTypeAdapter<J> extends AbstractSingleColumnStandardBasicType<J> {
	private final String description;
	private final BasicValueConverter<J,?> converter;

	@SuppressWarnings("rawtypes")
	private final ValueBinder valueBinder;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public ValueConverterTypeAdapter(
			String description,
			BasicValueConverter<J, ?> converter,
			JdbcTypeDescriptorIndicators indicators) {
		super(
				converter.getRelationalJavaDescriptor().getRecommendedJdbcType( indicators ),
				(JavaType) converter.getRelationalJavaDescriptor()
		);

		this.description = description;
		this.converter = converter;

		this.valueBinder = getJdbcTypeDescriptor().getBinder( converter.getRelationalJavaDescriptor() );
	}

	@Override
	public String getName() {
		return converter.getClass().getName();
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public void nullSafeSet(
			CallableStatement st,
			Object value,
			String name,
			SharedSessionContractImplementor session) throws SQLException {
		final Object converted = converter.toRelationalValue( (J) value );
		valueBinder.bind( st, converted, name, session );
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	protected void nullSafeSet(PreparedStatement st, Object value, int index, WrapperOptions options) throws SQLException {
		final Object converted = converter.toRelationalValue( (J) value );
		valueBinder.bind( st, converted, index, options );
	}

	@Override
	protected MutabilityPlan<J> getMutabilityPlan() {
		return converter.getDomainJavaDescriptor().getMutabilityPlan();
	}

	@Override
	public boolean isEqual(Object one, Object another) {
		//noinspection unchecked
		return ( (JavaType<Object>) converter.getDomainJavaDescriptor() ).areEqual( one, another );
	}

	@Override
	public String toString() {
		return description;
	}
}
