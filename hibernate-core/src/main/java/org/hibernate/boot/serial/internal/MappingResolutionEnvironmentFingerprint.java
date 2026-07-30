/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.serial.internal;

import java.io.Serializable;

import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.pipeline.internal.MappingResolutionOptions;
import org.hibernate.type.TimeZoneStorageStrategy;
import org.hibernate.type.WrapperArrayHandling;

/// Type-affecting environment facts which must agree when resolutions are restored.
///
/// @since 9.0
/// @author Steve Ebersole
public record MappingResolutionEnvironmentFingerprint(
		String dialectClassName,
		int preferredBooleanJdbcType,
		int preferredDurationJdbcType,
		int preferredUuidJdbcType,
		int preferredInstantJdbcType,
		int preferredArrayJdbcType,
		boolean preferJavaTimeJdbcTypes,
		boolean preferNativeEnumTypes,
		boolean preferLocaleLanguageTag,
		TimeZoneStorageStrategy defaultTimeZoneStorage,
		WrapperArrayHandling wrapperArrayHandling,
		boolean nationalizedCharacterData,
		boolean legacyXmlFormat) implements Serializable {

	public static MappingResolutionEnvironmentFingerprint from(MetadataImplementor metadata) {
		final MappingResolutionOptions options = metadata.getMappingResolutionOptions();
		return new MappingResolutionEnvironmentFingerprint(
				metadata.getDatabase().getDialect().getClass().getName(),
				options.getPreferredSqlTypeCodeForBoolean(),
				options.getPreferredSqlTypeCodeForDuration(),
				options.getPreferredSqlTypeCodeForUuid(),
				options.getPreferredSqlTypeCodeForInstant(),
				options.getPreferredSqlTypeCodeForArray(),
				options.isPreferJavaTimeJdbcTypesEnabled(),
				options.isPreferNativeEnumTypesEnabled(),
				options.isPreferLocaleLanguageTagEnabled(),
				options.getDefaultTimeZoneStorage(),
				options.getWrapperArrayHandling(),
				options.useNationalizedCharacterData(),
				options.isXmlFormatMapperLegacyFormatEnabled()
		);
	}

	public void validate(MetadataImplementor metadata) {
		final MappingResolutionEnvironmentFingerprint actual = from( metadata );
		if ( !equals( actual ) ) {
			throw new IllegalStateException(
					"Incompatible mapping-resolution environment; archived=" + this + ", restoration=" + actual
			);
		}
	}
}
