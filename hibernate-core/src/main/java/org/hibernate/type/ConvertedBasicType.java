/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type;

import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;

/**
 * Extension for implementations of {@link BasicType} which have an implied
 * {@linkplain BasicValueConverter conversion}.
 */
public interface ConvertedBasicType<J> extends BasicType<J> {
	BasicValueConverter<J,?> getValueConverter();
}
