/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.internal;

import java.util.function.Supplier;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.IdentifierValue;
import org.hibernate.engine.spi.VersionValue;
import org.hibernate.mapping.KeyValue;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.VersionJavaType;
import org.hibernate.type.descriptor.java.spi.PrimitiveJavaType;

import static org.hibernate.engine.internal.Versioning.isNullInitialVersion;

/**
 * Helper for dealing with unsaved value handling
 *
 * @author Gavin King
 */
public class UnsavedValueFactory {

	/**
	 * Return the UnsavedValueStrategy for determining whether an entity instance is
	 * unsaved based on the identifier.  If an explicit strategy is not specified, determine
	 * the unsaved value by instantiating an instance of the entity and reading the value of
	 * its id property, or if that is not possible, using the java default value for the type
	 */
	public static IdentifierValue getUnsavedIdentifierValue(
			KeyValue bootIdMapping,
			JavaType<?> idJavaType,
			Getter getter,
			Supplier<?> templateInstanceAccess) {
		final String unsavedValue = bootIdMapping.getNullValue();
		if ( unsavedValue == null ) {
			if ( getter != null && templateInstanceAccess != null ) {
				// use the id value of a newly instantiated instance as the unsaved-value
				final Object defaultValue = getter.get( templateInstanceAccess.get() );
				return new IdentifierValue( defaultValue );
			}
			else if ( idJavaType instanceof PrimitiveJavaType<?> primitiveJavaType ) {
				return new IdentifierValue( primitiveJavaType.getDefaultValue() );
			}
			else {
				return IdentifierValue.NULL;
			}
		}
		else {
			return switch ( unsavedValue ) {
				case "null" -> IdentifierValue.NULL;
				case "undefined" -> IdentifierValue.UNDEFINED;
				case "none" -> IdentifierValue.NONE;
				case "any" -> IdentifierValue.ANY;
				default -> new IdentifierValue( idJavaType.fromString( unsavedValue ) );
			};
		}
	}

	/**
	 * Return the {@link org.hibernate.engine.spi.UnsavedValueStrategy} for determining
	 * whether an entity instance is unsaved based on the version.  If an explicit strategy
	 * is not specified, determine the unsaved value by instantiating an instance of the
	 * entity and reading the value of its version property, or if that is not possible,
	 * using the java default value for the type.
	 */
	public static <T> VersionValue getUnsavedVersionValue(
			KeyValue bootVersionMapping,
			VersionJavaType<T> versionJavaType,
			Getter getter,
			Supplier<?> templateInstanceAccess) {
		final String unsavedValue = bootVersionMapping.getNullValue();
		if ( unsavedValue == null ) {
			if ( getter != null && templateInstanceAccess != null ) {
				final Object defaultValue = getter.get( templateInstanceAccess.get() );
				// if the version of a newly instantiated object is null
				// or a negative number, use that value as the unsaved-value,
				// otherwise assume it's the initial version set by program
				return isNullInitialVersion( defaultValue )
						? new VersionValue( defaultValue )
						: VersionValue.UNDEFINED;
			}
			else {
				return VersionValue.UNDEFINED;
			}
		}
		else {
			// this should not happen since the DTD prevents it
			return switch ( unsavedValue ) {
				case "undefined" -> VersionValue.UNDEFINED;
				case "null" -> VersionValue.NULL;
				case "negative" -> VersionValue.NEGATIVE;
				default -> throw new MappingException( "Could not parse version unsaved-value: " + unsavedValue );
			};
		}

	}

	private UnsavedValueFactory() {
	}
}
