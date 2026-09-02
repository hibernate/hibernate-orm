/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.sql.DdlType;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Verifies representative community Dialect registrations through the
/// supported DDL-type construction surface.
///
/// @author Steve Ebersole
public class DdlTypeConstructionTest {
	@Test
	void preservesMySqlCapacityThresholdsAndLobClassification() {
		final MySQLLegacyDialect dialect = new MySQLLegacyDialect();
		final DdlTypeRegistry registry = contributeTypes( dialect );
		final int maxVarcharLength = dialect.getTypeSizingProfile().maxVarcharLength();
		final DdlType varchar = registry.getDescriptor( SqlTypes.VARCHAR );

		assertThat( registry.getTypeName( SqlTypes.VARCHAR, Size.length( maxVarcharLength ), null ) )
				.isEqualTo( "varchar(" + maxVarcharLength + ")" );
		assertThat( registry.getTypeName( SqlTypes.VARCHAR, Size.length( maxVarcharLength + 1L ), null ) )
				.isEqualTo( "text" );
		assertThat( varchar.isLob( Size.length( maxVarcharLength ) ) ).isFalse();
		assertThat( varchar.isLob( Size.length( 16_777_216 ) ) ).isTrue();
	}

	@Test
	void preservesCubridCapacityAndBinaryFloatRegistrations() {
		final CUBRIDDialect dialect = new CUBRIDDialect();
		final DdlTypeRegistry registry = contributeTypes( dialect );
		final int maxVarbinaryLength = dialect.getTypeSizingProfile().maxVarbinaryLength();

		assertThat( registry.getTypeName( SqlTypes.VARBINARY, Size.length( maxVarbinaryLength ), null ) )
				.isEqualTo( "bit varying(" + maxVarbinaryLength + ")" );
		assertThat( registry.getTypeName( SqlTypes.VARBINARY, Size.length( maxVarbinaryLength + 1L ), null ) )
				.isEqualTo( "blob" );
		assertThat( registry.getDescriptor( SqlTypes.VARBINARY ).isLob( Size.length( maxVarbinaryLength + 1L ) ) )
				.isTrue();
		assertThat( registry.getDescriptor( SqlTypes.FLOAT ) ).isNotNull();
	}

	@Test
	void preservesArrayIntervalAndNamedEnumRegistrations() {
		final TypeConfiguration typeConfiguration = new TypeConfiguration();
		new TestPostgreSQLLegacyDialect().registerColumnTypes( () -> typeConfiguration, null );
		final DdlTypeRegistry registry = typeConfiguration.getDdlTypeRegistry();

		assertThat( registry.getDescriptor( SqlTypes.ARRAY ).getRawTypeNames() ).containsExactly( "array" );
		assertThat( registry.getTypeName( SqlTypes.INTERVAL_SECOND, Size.precision( 2, 6 ), null ) )
				.isEqualTo( "interval second(6)" );
		assertThat( registry.getDescriptor( SqlTypes.NAMED_ENUM ).getSqlTypeCode() ).isEqualTo( SqlTypes.NAMED_ENUM );
		assertThat( registry.getDescriptor( SqlTypes.NAMED_ORDINAL_ENUM ).getSqlTypeCode() )
				.isEqualTo( SqlTypes.NAMED_ORDINAL_ENUM );
	}

	@Test
	void preservesNativeEnumAndBinaryPrecisionRegistrations() {
		final DdlTypeRegistry h2Registry = contributeTypes( new H2LegacyDialect() );
		assertThat( h2Registry.getTypeName( SqlTypes.ENUM, Size.length( 12 ), null ) ).isEqualTo( "varchar(12)" );
		assertThat( h2Registry.getTypeName( SqlTypes.ORDINAL_ENUM, Size.nil(), null ) ).isEqualTo( "int" );

		final DdlTypeRegistry mimerRegistry = contributeTypes( new MimerSQLDialect() );
		assertThat( mimerRegistry.getTypeName( SqlTypes.FLOAT, Size.precision( 10 ), null ) ).isEqualTo( "float(3)" );
	}

	private static DdlTypeRegistry contributeTypes(Dialect dialect) {
		final TypeConfiguration typeConfiguration = new TypeConfiguration();
		dialect.contributeTypes( () -> typeConfiguration, null );
		return typeConfiguration.getDdlTypeRegistry();
	}

	private static class TestPostgreSQLLegacyDialect extends PostgreSQLLegacyDialect {
		@Override
		public void registerColumnTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
			super.registerColumnTypes( typeContributions, serviceRegistry );
		}
	}
}
