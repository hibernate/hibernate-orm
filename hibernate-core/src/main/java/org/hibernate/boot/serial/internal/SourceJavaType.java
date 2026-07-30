/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.serial.internal;

import java.io.Serializable;

import org.hibernate.models.spi.TypeDetails;
import org.hibernate.type.internal.ParameterizedTypeImpl;

/// Declarative Java type source retained by a basic mapping for archive restoration.
///
/// @since 9.0
/// @author Steve Ebersole
public interface SourceJavaType extends Serializable {
	TypeDetails typeDetails();

	Class<?> rawJavaClass();

	java.lang.reflect.Type asReflectType();

	static SourceJavaType from(TypeDetails typeDetails, Class<?> explicitJavaType) {
		return new SourceJavaType() {
			@Override
			public TypeDetails typeDetails() {
				return typeDetails;
			}

			@Override
			public Class<?> rawJavaClass() {
				if ( explicitJavaType != null ) {
					return explicitJavaType;
				}
				if ( typeDetails == null ) {
					return null;
				}
				return typeDetails.determineRawClass().toJavaClass();
			}

			@Override
			public java.lang.reflect.Type asReflectType() {
				if ( explicitJavaType != null ) {
					return explicitJavaType;
				}
				if ( typeDetails == null ) {
					return null;
				}
				if ( typeDetails.getTypeKind() == TypeDetails.Kind.PARAMETERIZED_TYPE ) {
					return ParameterizedTypeImpl.from( typeDetails.asParameterizedType() );
				}
				return rawJavaClass();
			}
		};
	}
}
