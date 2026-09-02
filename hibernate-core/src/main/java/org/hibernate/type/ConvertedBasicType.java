/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type;

import org.hibernate.SPI;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.USE;

import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;

/**
 * Extension for implementations of {@link BasicType} which have an implied
 * {@linkplain BasicValueConverter conversion}.
 */
@SPI({ USE, IMPLEMENT })
public interface ConvertedBasicType<J> extends BasicType<J> {
	BasicValueConverter<J,?> getValueConverter();
}
