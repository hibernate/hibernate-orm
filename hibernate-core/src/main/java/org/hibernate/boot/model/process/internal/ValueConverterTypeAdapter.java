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
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;

/**
 * @author Steve Ebersole
 */
public class ValueConverterTypeAdapter<J> extends AbstractSingleColumnStandardBasicType<J> {
	private final String description;
	private final BasicValueConverter<J, Object> converter;

	private final ValueBinder<Object> valueBinder;

	@SuppressWarnings("unchecked")
	public ValueConverterTypeAdapter(
			String description,
			BasicValueConverter<J, ?> converter,
			JdbcTypeIndicators indicators) {
		super(
				converter.getRelationalJavaType().getRecommendedJdbcType( indicators ),
				(JavaType<J>) converter.getRelationalJavaType()
		);

		this.description = description;
		this.converter = (BasicValueConverter<J, Object>) converter;
		this.valueBinder = getJdbcType().getBinder( this.converter.getRelationalJavaType() );
	}

	@Override
	public String getName() {
		return converter.getClass().getName();
	}

	@Override
	public void nullSafeSet(
			CallableStatement st,
			J value,
			String name,
			SharedSessionContractImplementor session) throws SQLException {
		final Object converted = converter.toRelationalValue( value );
		valueBinder.bind( st, converted, name, session );
	}

	@Override
	protected void nullSafeSet(PreparedStatement st, J value, int index, WrapperOptions options) throws SQLException {
		final Object converted = converter.toRelationalValue( value );
		valueBinder.bind( st, converted, index, options );
	}

	@Override
	protected MutabilityPlan<J> getMutabilityPlan() {
		return converter.getDomainJavaType().getMutabilityPlan();
	}

	@Override
	public boolean isEqual(Object one, Object another) {
		//noinspection unchecked
		return ( (JavaType<Object>) converter.getDomainJavaType() ).areEqual( one, another );
	}

	@Override
	public String toString() {
		return description;
	}
}
