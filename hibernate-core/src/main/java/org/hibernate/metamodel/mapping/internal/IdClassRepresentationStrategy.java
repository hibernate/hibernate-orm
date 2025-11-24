/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.Locale;
import java.util.function.Supplier;

import org.hibernate.HibernateException;
import org.hibernate.bytecode.spi.ReflectionOptimizer;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.RepresentationMode;
import org.hibernate.metamodel.internal.EmbeddableInstantiatorPojoStandard;
import org.hibernate.metamodel.internal.EmbeddableInstantiatorRecordIndirecting;
import org.hibernate.metamodel.internal.EmbeddableInstantiatorRecordStandard;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.metamodel.spi.EmbeddableRepresentationStrategy;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * EmbeddableRepresentationStrategy for an IdClass mapping
 */
public class IdClassRepresentationStrategy implements EmbeddableRepresentationStrategy {
	private final JavaType<?> idClassType;
	private final EmbeddableInstantiator instantiator;

	public IdClassRepresentationStrategy(
			IdClassEmbeddable idClassEmbeddable,
			boolean simplePropertyOrder,
			Supplier<String[]> attributeNamesAccess) {
		idClassType = idClassEmbeddable.getMappedJavaType();
		final var javaTypeClass = idClassType.getJavaTypeClass();
		if ( javaTypeClass.isRecord() ) {
			instantiator = simplePropertyOrder
					? new EmbeddableInstantiatorRecordStandard( javaTypeClass )
					: EmbeddableInstantiatorRecordIndirecting.of( javaTypeClass, attributeNamesAccess.get() );
		}
		else {
			instantiator = new EmbeddableInstantiatorPojoStandard(
					idClassType.getJavaTypeClass(),
					() -> idClassEmbeddable
			);
		}
	}

	@Override
	public EmbeddableInstantiator getInstantiator() {
		return instantiator;
	}

	@Override
	public RepresentationMode getMode() {
		return RepresentationMode.POJO;
	}

	@Override
	public ReflectionOptimizer getReflectionOptimizer() {
		return null;
	}

	@Override
	public JavaType<?> getMappedJavaType() {
		return idClassType;
	}

	@Override
	public PropertyAccess resolvePropertyAccess(Property bootAttributeDescriptor) {
		final var strategy = bootAttributeDescriptor.getPropertyAccessStrategy( idClassType.getJavaTypeClass() );

		if ( strategy == null ) {
			throw new HibernateException(
					String.format(
							Locale.ROOT,
							"Could not resolve PropertyAccess for attribute `%s#%s`",
							idClassType.getTypeName(),
							bootAttributeDescriptor.getName()
					)
			);
		}

		return strategy.buildPropertyAccess(
				idClassType.getJavaTypeClass(),
				bootAttributeDescriptor.getName(),
				false
		);
	}
}
