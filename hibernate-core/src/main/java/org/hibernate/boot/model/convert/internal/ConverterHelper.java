/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.convert.internal;

import java.lang.reflect.ParameterizedType;

import org.hibernate.internal.util.GenericsHelper;

import jakarta.persistence.AttributeConverter;


/**
 * Helpers related to handling converters
 */
public class ConverterHelper {
	public static ParameterizedType extractAttributeConverterParameterizedType(Class<? extends AttributeConverter<?,?>> base) {
		return GenericsHelper.extractParameterizedType( base, AttributeConverter.class );
	}
}
