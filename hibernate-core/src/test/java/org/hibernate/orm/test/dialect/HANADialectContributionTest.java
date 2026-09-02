/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;

import java.sql.Types;

import org.hibernate.SPI;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.HANADialect;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.NVarcharJdbcType;
import org.hibernate.type.descriptor.jdbc.DecimalJdbcType;
import org.hibernate.type.spi.TypeConfiguration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;

/// Verifies that HANA resolves its bootstrap settings before inherited type
/// and column registration.
///
/// @author Steve Ebersole
public class HANADialectContributionTest {
	private static final String USE_LEGACY_BOOLEAN_TYPE = "hibernate.dialect.hana.use_legacy_boolean_type";
	private static final String USE_UNICODE_STRING_TYPES = "hibernate.dialect.hana.use_unicode_string_types";
	private static final String TREAT_DOUBLE_AS_DECIMAL = "hibernate.dialect.hana.treat_double_typed_fields_as_decimal";

	@Test
	void resolvesSettingsBeforeInheritedRegistrationAndPreservesTheAsciiGate() {
		final StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
				.applySetting( HANADialect.USE_DEFAULT_TABLE_TYPE_COLUMN, true )
				.applySetting( USE_LEGACY_BOOLEAN_TYPE, true )
				.applySetting( USE_UNICODE_STRING_TYPES, false )
				.applySetting( TREAT_DOUBLE_AS_DECIMAL, true )
				.build();
		try {
			final TypeConfiguration typeConfiguration = new TypeConfiguration();
			final ObservingHANADialect dialect = new ObservingHANADialect();

			assertThatCode( () -> dialect.contributeTypes( () -> typeConfiguration, serviceRegistry ) )
					.doesNotThrowAnyException();

			assertThat( dialect.settingsObservedDuringColumnRegistration ).isTrue();
			assertThat( typeConfiguration.getJdbcTypeRegistry().getDescriptor( Types.DOUBLE ) )
					.isSameAs( DecimalJdbcType.INSTANCE );
			assertThat( typeConfiguration.getJdbcTypeRegistry().getDescriptor( Types.VARCHAR ) )
					.isSameAs( NVarcharJdbcType.INSTANCE );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( serviceRegistry );
		}
	}

	private static class ObservingHANADialect extends HANADialect {
		private boolean settingsObservedDuringColumnRegistration;

		private ObservingHANADialect() {
			super( DatabaseVersion.make( 4 ) );
		}

		@Override
		@SPI({ IMPLEMENT, SUPPLY })
		protected void registerColumnTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
			settingsObservedDuringColumnRegistration = isDefaultTableTypeColumn()
					&& isUseUnicodeStringTypes()
					&& "tinyint".equals( columnType( SqlTypes.BOOLEAN ) );
			super.registerColumnTypes( typeContributions, serviceRegistry );
		}
	}
}
