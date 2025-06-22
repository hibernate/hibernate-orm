/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.metamodel.model.domain.BasicDomainType;
import org.hibernate.type.BasicType;
import org.hibernate.type.ConvertedBasicType;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Union of {@link ConvertedBasicType} and {@link BasicDomainType} capabilities.
 *
 * @implNote We need the {@link BasicDomainType} aspect for handling in SQM trees.
 *
 * @param <O> The Java type of the domain form of the discriminator.
 *
 * @author Steve Ebersole
 */
public interface DiscriminatorType<O> extends ConvertedBasicType<O>, BasicDomainType<O> {
	@Override
	DiscriminatorConverter<O, ?> getValueConverter();

	BasicType<?> getUnderlyingJdbcMapping();

	@Override
	default JavaType<O> getJavaTypeDescriptor() {
		return ConvertedBasicType.super.getJavaTypeDescriptor();
	}
}
