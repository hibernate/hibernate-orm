/*
 * SPDX-License-Identifier: Apache-2.0
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
import org.hibernate.type.descriptor.java.spi.RegistryHelper;

import java.util.Objects;

/**
 * Standard implementation of {@link JpaAttributeConverter} backed by
 * a {@link ManagedBean}.
 * <p>
 * JPA requires support for injection into {@link AttributeConverter}
 * instances via CDI.
 *
 * @see AttributeConverterMutabilityPlan
 *
 * @author Steve Ebersole
 */
public final class AttributeConverterBean<O,R> implements JpaAttributeConverter<O,R> {
	private final ManagedBean<? extends AttributeConverter<O,R>> attributeConverterBean;
	private final JavaType<? extends AttributeConverter<O, R>> converterJavaType;
	private final JavaType<O> domainJavaType;
	private final JavaType<R> jdbcJavaType;

	public AttributeConverterBean(
			ManagedBean<? extends AttributeConverter<O, R>> attributeConverterBean,
			JavaType<? extends AttributeConverter<O,R>> converterJavaType,
			JavaType<O> domainJavaType,
			JavaType<R> jdbcJavaType) {
		this.attributeConverterBean = attributeConverterBean;
		this.converterJavaType = converterJavaType;
		this.domainJavaType = domainJavaType;
		this.jdbcJavaType = jdbcJavaType;
	}

	public AttributeConverterBean(
			ManagedBean<? extends AttributeConverter<O,R>> attributeConverterBean,
			JavaType<? extends AttributeConverter<O,R>> converterJavaType,
			Class<O> domainJavaType,
			Class<R> jdbcJavaType,
			JpaAttributeConverterCreationContext context) {
		this.attributeConverterBean = attributeConverterBean;
		this.converterJavaType = converterJavaType;

		final var jtdRegistry = context.getJavaTypeRegistry();
		this.jdbcJavaType = jtdRegistry.resolveDescriptor( jdbcJavaType );
		this.domainJavaType =
				jtdRegistry.resolveDescriptor( domainJavaType,
						() -> getTypeDescriptor( attributeConverterBean, domainJavaType, context ) );
	}

	private JavaType<O> getTypeDescriptor(
			ManagedBean<? extends AttributeConverter<O, R>> attributeConverterBean,
			Class<O> domainJavaType,
			JpaAttributeConverterCreationContext context) {
		return RegistryHelper.INSTANCE.createTypeDescriptor(
				domainJavaType,
				() -> getMutabilityPlan( attributeConverterBean, context ),
				context.getTypeConfiguration()
		);
	}

	private MutabilityPlan<O> getMutabilityPlan(
			ManagedBean<? extends AttributeConverter<O,R>> attributeConverterBean,
			JpaAttributeConverterCreationContext context) {
		final MutabilityPlan<O> mutabilityPlan =
				RegistryHelper.INSTANCE.determineMutabilityPlan(
						attributeConverterBean.getBeanClass(),
						context.getTypeConfiguration()
				);
		return mutabilityPlan == null
				? new AttributeConverterMutabilityPlan<>( this, true )
				: mutabilityPlan;
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
		return converterJavaType;
	}

	@Override
	public JavaType<O> getDomainJavaType() {
		return domainJavaType;
	}

	@Override
	public JavaType<R> getRelationalJavaType() {
		return jdbcJavaType;
	}

	@Override
	public boolean equals(Object object) {
		if ( this == object ) {
			return true;
		}
		else {
			return object instanceof AttributeConverterBean<?, ?> that
				&& Objects.equals( this.attributeConverterBean, that.attributeConverterBean )
				&& Objects.equals( this.converterJavaType, that.converterJavaType )
				&& Objects.equals( this.domainJavaType, that.domainJavaType )
				&& Objects.equals( this.jdbcJavaType, that.jdbcJavaType );
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash( attributeConverterBean, converterJavaType, domainJavaType, jdbcJavaType );
	}
}
