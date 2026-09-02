/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.type.spi;

import org.hibernate.Incubating;
import org.hibernate.SPI;
import org.hibernate.type.descriptor.converter.internal.EnumHelper;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;

import static org.hibernate.SPI.Role.USE;

/// Resolves the relational strings represented by Java enum constants.
///
/// Results preserve declaration order. Apply database-specific ordering after
/// calling these methods when a database declaration requires it.
///
/// @author Steve Ebersole
/// @since 8.0
@Incubating
@SPI(USE)
public final class EnumRelationalValues {
	private EnumRelationalValues() {
	}

	/// Return a fresh array containing each enum constant's declared name.
	public static String[] names(Class<? extends Enum<?>> enumClass) {
		return EnumHelper.getEnumeratedValues( enumClass );
	}

	/// Return a fresh array containing each enum constant's converted relational
	/// value. A converter must not return `null` for an enum constant.
	public static String[] convertedValues(
			Class<? extends Enum<?>> enumClass,
			BasicValueConverter<Enum<?>, ?> converter) {
		return EnumHelper.getEnumeratedValues( enumClass, converter );
	}
}
