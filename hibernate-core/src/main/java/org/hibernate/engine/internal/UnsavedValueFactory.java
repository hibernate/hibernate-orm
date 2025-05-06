/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.internal;

import java.util.function.Supplier;

import org.hibernate.engine.spi.IdentifierValue;
import org.hibernate.engine.spi.VersionValue;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.KeyValue.NullValueSemantic;
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
		final NullValueSemantic nullValueSemantic = bootIdMapping.getNullValueSemantic();
		return nullValueSemantic == null
				? inferUnsavedIdentifierValue( idJavaType, getter, templateInstanceAccess )
				: switch ( nullValueSemantic ) {
					case UNDEFINED -> IdentifierValue.UNDEFINED;
					case NULL -> IdentifierValue.NULL;
					case ANY -> IdentifierValue.ANY;
					case NONE -> IdentifierValue.NONE;
					case VALUE -> new IdentifierValue( idJavaType.fromString( bootIdMapping.getNullValue() ) );
					default -> throw new IllegalArgumentException( "Illegal null-value semantic: " + nullValueSemantic );
				};
	}

	private static IdentifierValue inferUnsavedIdentifierValue(
			JavaType<?> idJavaType, Getter getter, Supplier<?> templateInstanceAccess) {
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
		final NullValueSemantic nullValueSemantic = bootVersionMapping.getNullValueSemantic();
		return nullValueSemantic == null
				? inferUnsavedVersionValue( versionJavaType, getter, templateInstanceAccess )
				: switch ( nullValueSemantic ) {
					case UNDEFINED -> VersionValue.UNDEFINED;
					case NULL -> VersionValue.NULL;
					case NEGATIVE -> VersionValue.NEGATIVE;
					// this should not happen since the DTD prevents it
					case VALUE -> new VersionValue( versionJavaType.fromString( bootVersionMapping.getNullValue() ) );
					default -> throw new IllegalArgumentException( "Illegal null-value semantic: " + nullValueSemantic );
				};
	}

	private static VersionValue inferUnsavedVersionValue(
			VersionJavaType<?> versionJavaType, Getter getter, Supplier<?> templateInstanceAccess) {
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

	private UnsavedValueFactory() {
	}
}
