/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.hibernate.Incubating;
import org.hibernate.boot.beanvalidation.BeanValidationIntegrator;
import org.hibernate.boot.beanvalidation.ValidationMode;
import org.hibernate.cfg.SchemaToolingSettings;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.service.ServiceRegistry;

/// Controls whether Jakarta Validation constraints (such as
/// {@link jakarta.validation.constraints.NotNull @NotNull},
/// {@link jakarta.validation.constraints.Size @Size},
/// {@link jakarta.validation.constraints.Digits @Digits} and others)
/// influence the generated DDL schema.
///
/// @see SchemaToolingSettings#APPLY_VALIDATION_CONSTRAINTS
///
/// @since 8.0
@Incubating
public enum ValidationConstraintDdlInfluence {
	/// Apply validation constraints to the DDL schema if a Jakarta Validation
	/// provider is available on the classpath; silently skip otherwise.
	AUTO,
	/// Apply validation constraints to the DDL schema, failing with an error
	/// if no Jakarta Validation provider is available.
	REQUIRED,
	/// Do not apply validation constraints to the DDL schema.
	DISABLED;

	@SuppressWarnings( "removal" )
	public static ValidationConstraintDdlInfluence resolve(ServiceRegistry serviceRegistry, Set<ValidationMode> validationModes) {
		final var configurationService = serviceRegistry.requireService( ConfigurationService.class );
		final var settings = configurationService.getSettings();
		if ( settings.containsKey( SchemaToolingSettings.APPLY_VALIDATION_CONSTRAINTS ) ) {
			return resolveSettingValue( settings );
		}
		// legacy: treat deprecated ValidationMode.DDL as REQUIRED
		for ( var mode : validationModes ) {
			if ( mode == ValidationMode.DDL ) {
				return REQUIRED;
			}
		}
		// legacy boolean setting fallback
		if ( !configurationService.getSetting( BeanValidationIntegrator.APPLY_CONSTRAINTS, StandardConverters.BOOLEAN, true ) ) {
			return DISABLED;
		}
		return AUTO;
	}

	private static ValidationConstraintDdlInfluence resolveSettingValue(Map<String, Object> configurationValues) {
		final Object setting = configurationValues.get( SchemaToolingSettings.APPLY_VALIDATION_CONSTRAINTS );
		if ( setting == null ) {
			return AUTO;
		}

		if ( setting instanceof ValidationConstraintDdlInfluence type ) {
			return type;
		}

		return valueOf( setting.toString().trim().toUpperCase( Locale.ROOT ) );
	}
}
