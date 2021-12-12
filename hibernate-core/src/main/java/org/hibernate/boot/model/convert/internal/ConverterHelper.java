/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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
		return GenericsHelper.extractParameterizedType( base );
	}
}
