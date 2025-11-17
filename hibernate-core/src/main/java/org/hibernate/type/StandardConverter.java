/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type;

import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;

import jakarta.persistence.AttributeConverter;

/**
 * Marker for Hibernate supplied {@linkplain AttributeConverter converter} classes.
 * <p/>
 * Also implements the Hibernate-specific BasicValueConverter contract
 *
 * @author Steve Ebersole
 */
public interface StandardConverter<O,R> extends AttributeConverter<O,R>, BasicValueConverter<O,R> {
}
