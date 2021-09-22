/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.convert.internal;

import jakarta.persistence.AttributeConverter;

import org.hibernate.annotations.Immutable;
import org.hibernate.boot.model.convert.spi.JpaAttributeConverterCreationContext;
import org.hibernate.metamodel.model.convert.spi.JpaAttributeConverter;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.type.descriptor.converter.AttributeConverterMutabilityPlanImpl;
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptorRegistry;
import org.hibernate.type.descriptor.java.spi.RegistryHelper;

/**
 * Standard implementation of JpaAttributeConverter
 *
 * @author Steve Ebersole
 */
public class JpaAttributeConverterImpl<O,R> implements JpaAttributeConverter<O,R> {
	private final ManagedBean<? extends AttributeConverter<O,R>> attributeConverterBean;
	private final JavaTypeDescriptor<? extends AttributeConverter<O, R>> converterJtd;
	private final JavaTypeDescriptor<O> domainJtd;
	private final JavaTypeDescriptor<R> jdbcJtd;

	public JpaAttributeConverterImpl(
			ManagedBean<? extends AttributeConverter<O, R>> attributeConverterBean,
			JavaTypeDescriptor<? extends AttributeConverter<O,R>> converterJtd,
			JavaTypeDescriptor<O> domainJtd,
			JavaTypeDescriptor<R> jdbcJtd) {
		this.attributeConverterBean = attributeConverterBean;
		this.converterJtd = converterJtd;
		this.domainJtd = domainJtd;
		this.jdbcJtd = jdbcJtd;
	}

	public JpaAttributeConverterImpl(
			ManagedBean<? extends AttributeConverter<O,R>> attributeConverterBean,
			JavaTypeDescriptor<? extends AttributeConverter<O,R>> converterJtd,
			Class<O> domainJavaType,
			Class<R> jdbcJavaType,
			JpaAttributeConverterCreationContext context) {
		this.attributeConverterBean = attributeConverterBean;
		this.converterJtd = converterJtd;

		final JavaTypeDescriptorRegistry jtdRegistry = context.getJavaTypeDescriptorRegistry();

		jdbcJtd = jtdRegistry.getDescriptor( jdbcJavaType );
		//noinspection unchecked
		domainJtd = (JavaTypeDescriptor<O>) jtdRegistry.resolveDescriptor(
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
		return attributeConverterBean.getBeanInstance().convertToEntityAttribute( relationalForm );
	}

	@Override
	public R toRelationalValue(O domainForm) {
		return attributeConverterBean.getBeanInstance().convertToDatabaseColumn( domainForm );
	}

	@Override
	public JavaTypeDescriptor<? extends AttributeConverter<O, R>> getConverterJavaTypeDescriptor() {
		return converterJtd;
	}

	@Override
	public JavaTypeDescriptor<O> getDomainJavaDescriptor() {
		return getDomainJavaTypeDescriptor();
	}

	@Override
	public JavaTypeDescriptor<R> getRelationalJavaDescriptor() {
		return getRelationalJavaTypeDescriptor();
	}

	@Override
	public JavaTypeDescriptor<O> getDomainJavaTypeDescriptor() {
		return domainJtd;
	}

	@Override
	public JavaTypeDescriptor<R> getRelationalJavaTypeDescriptor() {
		return jdbcJtd;
	}
}
