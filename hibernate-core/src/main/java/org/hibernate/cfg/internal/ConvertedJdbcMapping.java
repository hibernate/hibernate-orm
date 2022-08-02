/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cfg.internal;

import java.lang.reflect.ParameterizedType;

import org.hibernate.Remove;
import org.hibernate.internal.util.GenericsHelper;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.model.convert.internal.JpaAttributeConverterImpl;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.spi.JavaTypeRegistry;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.spi.TypeConfiguration;

import jakarta.persistence.AttributeConverter;

/**
 * Ad-hoc JdbcMapping implementation for cases where we only have a converter
 *
 * @author Steve Ebersole
 * @deprecated remove
 */
@Remove
@Deprecated(forRemoval = true)
public class ConvertedJdbcMapping<T> implements JdbcMapping {

	private final JavaType<T> domainJtd;
	private final JavaType<?> relationalJtd;
	private final JdbcType jdbcType;

	public ConvertedJdbcMapping(
			ManagedBean<AttributeConverter<?, ?>> converterBean,
			TypeConfiguration typeConfiguration) {

		final JavaTypeRegistry jtdRegistry = typeConfiguration.getJavaTypeRegistry();
		final JavaType<? extends AttributeConverter<?,?>> converterJtd = jtdRegistry.resolveDescriptor( converterBean.getBeanClass() );

		final ParameterizedType converterParameterizedType = GenericsHelper.extractParameterizedType( converterBean.getBeanClass() );
		final Class<?> domainJavaClass = GenericsHelper.extractClass( converterParameterizedType.getActualTypeArguments()[0] );
		final Class<?> relationalJavaClass = GenericsHelper.extractClass( converterParameterizedType.getActualTypeArguments()[1] );

		this.domainJtd = jtdRegistry.resolveDescriptor( domainJavaClass );
		this.relationalJtd = jtdRegistry.resolveDescriptor( relationalJavaClass );

		@SuppressWarnings({ "unchecked", "rawtypes" })
		final JpaAttributeConverterImpl converterDescriptor = new JpaAttributeConverterImpl(
				converterBean,
				converterJtd,
				domainJtd,
				relationalJtd
		);

		this.jdbcType = null;
//		new AttributeConverterJdbcTypeAdapter(
//				converterDescriptor,
//				relationalJtd.getRecommendedJdbcType( typeConfiguration.getCurrentBaseSqlTypeIndicators() ),
//				relationalJtd
//		);
	}

	@Override
	public JavaType<T> getJavaTypeDescriptor() {
		return domainJtd;
	}

	@Override
	public JdbcType getJdbcType() {
		return jdbcType;
	}

	@Override
	public JavaType<?> getJdbcJavaType() {
		return relationalJtd;
	}

	@Override
	public ValueExtractor<?> getJdbcValueExtractor() {
		return jdbcType.getExtractor( domainJtd );
	}

	@Override
	public ValueBinder<T> getJdbcValueBinder() {
		return jdbcType.getBinder( domainJtd );
	}
}
