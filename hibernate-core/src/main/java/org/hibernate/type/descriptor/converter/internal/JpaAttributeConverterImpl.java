/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.converter.internal;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.PersistenceException;

import org.hibernate.boot.model.convert.spi.JpaAttributeConverterCreationContext;
import org.hibernate.type.descriptor.converter.spi.JpaAttributeConverter;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.java.spi.JavaTypeRegistry;
import org.hibernate.type.descriptor.java.spi.RegistryHelper;

/**
 * Standard implementation of {@link JpaAttributeConverter}.
 *
 * @see AttributeConverterMutabilityPlanImpl
 *
 * @author Steve Ebersole
 */
public class JpaAttributeConverterImpl<O,R> implements JpaAttributeConverter<O,R> {
	private final ManagedBean<? extends AttributeConverter<O,R>> attributeConverterBean;
	private final JavaType<? extends AttributeConverter<O, R>> converterJtd;
	private final JavaType<O> domainJtd;
	private final JavaType<R> jdbcJtd;

	public JpaAttributeConverterImpl(
			ManagedBean<? extends AttributeConverter<O, R>> attributeConverterBean,
			JavaType<? extends AttributeConverter<O,R>> converterJtd,
			JavaType<O> domainJtd,
			JavaType<R> jdbcJtd) {
		this.attributeConverterBean = attributeConverterBean;
		this.converterJtd = converterJtd;
		this.domainJtd = domainJtd;
		this.jdbcJtd = jdbcJtd;
	}

	public JpaAttributeConverterImpl(
			ManagedBean<? extends AttributeConverter<O,R>> attributeConverterBean,
			JavaType<? extends AttributeConverter<O,R>> converterJtd,
			Class<O> domainJavaType,
			Class<R> jdbcJavaType,
			JpaAttributeConverterCreationContext context) {
		this.attributeConverterBean = attributeConverterBean;
		this.converterJtd = converterJtd;

		final JavaTypeRegistry jtdRegistry = context.getJavaTypeRegistry();

		jdbcJtd = jtdRegistry.getDescriptor( jdbcJavaType );
		//noinspection unchecked
		domainJtd = (JavaType<O>) jtdRegistry.resolveDescriptor(
				domainJavaType,
				() -> RegistryHelper.INSTANCE.createTypeDescriptor(
						domainJavaType,
						() -> {
							final Class<? extends AttributeConverter<O, R>> converterClass = attributeConverterBean.getBeanClass();
							final MutabilityPlan<Object> mutabilityPlan = RegistryHelper.INSTANCE.determineMutabilityPlan(
									converterClass,
									context.getTypeConfiguration()
							);

							if ( mutabilityPlan != null ) {
								return mutabilityPlan;
							}

							return new AttributeConverterMutabilityPlanImpl<>( this, true );
						},
						context.getTypeConfiguration()
				)
		);
	}

	@Override
	public ManagedBean<? extends AttributeConverter<O, R>> getConverterBean() {
		return attributeConverterBean;
	}

	@Override
	public O toDomainValue(R relationalForm) {
		try {
			return attributeConverterBean.getBeanInstance().convertToEntityAttribute( relationalForm );
		}
		catch (PersistenceException pe) {
			throw pe;
		}
		catch (RuntimeException re) {
			throw new PersistenceException( "Error attempting to apply AttributeConverter", re );
		}
	}

	@Override
	public R toRelationalValue(O domainForm) {
		try {
			return attributeConverterBean.getBeanInstance().convertToDatabaseColumn( domainForm );
		}
		catch (PersistenceException pe) {
			throw pe;
		}
		catch (RuntimeException re) {
			throw new PersistenceException( "Error attempting to apply AttributeConverter: " + re.getMessage(), re );
		}
	}

	@Override
	public JavaType<? extends AttributeConverter<O, R>> getConverterJavaType() {
		return converterJtd;
	}

	@Override
	public JavaType<O> getDomainJavaType() {
		return domainJtd;
	}

	@Override
	public JavaType<R> getRelationalJavaType() {
		return jdbcJtd;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		JpaAttributeConverterImpl<?, ?> that = (JpaAttributeConverterImpl<?, ?>) o;

		if ( !attributeConverterBean.equals( that.attributeConverterBean ) ) {
			return false;
		}
		if ( !converterJtd.equals( that.converterJtd ) ) {
			return false;
		}
		if ( !domainJtd.equals( that.domainJtd ) ) {
			return false;
		}
		return jdbcJtd.equals( that.jdbcJtd );
	}

	@Override
	public int hashCode() {
		int result = attributeConverterBean.hashCode();
		result = 31 * result + converterJtd.hashCode();
		result = 31 * result + domainJtd.hashCode();
		result = 31 * result + jdbcJtd.hashCode();
		return result;
	}
}
