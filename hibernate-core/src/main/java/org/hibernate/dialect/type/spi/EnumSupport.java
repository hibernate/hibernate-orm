/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.type.spi;

import java.util.Collection;

import jakarta.annotation.Nullable;

import org.hibernate.SPI;
import org.hibernate.dialect.Dialect;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Defines declaration, lifecycle, and value-check behavior for finite
/// relational domains, including enum, Boolean, and discriminator mappings.
///
/// Pass the mapped relational values to the String-based operations. Use the
/// Class conveniences only when conversion does not change those values.
/// Returned command arrays are ordered and must not be mutated.
///
/// @author Steve Ebersole
/// @since 8.0
/// @see Dialect#getEnumSupport()
@SPI({ USE, IMPLEMENT, SUPPLY })
public interface EnumSupport {
	/// Resolve the complete declaration for the ordered relational values, or
	/// `null` when the profile supplies no enum declaration.
	@Nullable String getTypeDeclaration(String name, String[] relationalValues);

	/// Resolve a declaration from Java enum constant names.
	default @Nullable String getTypeDeclaration(Class<? extends Enum<?>> enumType) {
		return getTypeDeclaration( enumType.getSimpleName(), enumNames( enumType ) );
	}

	/// Produce every command required to create the named finite-domain type.
	String[] getCreateTypeCommands(String name, String[] relationalValues);

	/// Produce every command required to create a named ordinal finite-domain
	/// type. The default is appropriate when ordinal and textual domains use the
	/// same DDL representation.
	default String[] getCreateOrdinalTypeCommands(String name, String[] relationalValues) {
		return getCreateTypeCommands( name, relationalValues );
	}

	/// Produce every command required to drop the named finite-domain type.
	String[] getDropTypeCommands(String name);

	/// Produce drop commands using the Java enum's simple name.
	default String[] getDropTypeCommands(Class<? extends Enum<?>> enumType) {
		return getDropTypeCommands( enumType.getSimpleName() );
	}

	/// Render a complete finite-value check condition using the supplied JDBC
	/// representation. Preserve encounter order and represent a null value with
	/// one `or column is null` branch.
	String getCheckCondition(String columnName, Collection<?> relationalValues, JdbcType jdbcType);

	/// Render a complete inclusive integral-range check condition.
	String getCheckCondition(String columnName, long min, long max);

	private static String[] enumNames(Class<? extends Enum<?>> enumType) {
		final Enum<?>[] constants = enumType.getEnumConstants();
		final String[] names = new String[constants.length];
		for ( int i = 0; i < constants.length; i++ ) {
			names[i] = constants[i].name();
		}
		return names;
	}
}
