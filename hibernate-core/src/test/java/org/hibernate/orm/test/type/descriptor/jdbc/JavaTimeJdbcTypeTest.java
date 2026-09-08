/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.type.descriptor.jdbc;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.type.spi.DirectJavaTimeJdbcSupport;
import org.hibernate.dialect.type.spi.DirectJavaTimeJdbcSupports;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.ArrayJavaType;
import org.hibernate.type.descriptor.java.LocalDateJavaType;
import org.hibernate.type.descriptor.java.ZonedDateTimeJavaType;
import org.hibernate.type.descriptor.jdbc.ArrayJdbcType;
import org.hibernate.type.descriptor.jdbc.JavaTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.LocalDateJdbcType;
import org.hibernate.type.descriptor.jdbc.LocalDateTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.OffsetDateTimeJdbcType;
import org.hibernate.type.spi.TypeConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Tests the physical representation and encoded forms of direct Java Time
/// JDBC descriptors.
///
/// @author Steve Ebersole
class JavaTimeJdbcTypeTest {
	@Test
	void arrayContainerCapabilityControlsTheJdbcCarrier() throws Exception {
		final var jdbcType = new ArrayJdbcType( LocalDateJdbcType.INSTANCE );
		final var javaType = new ArrayJavaType<>( LocalDateJavaType.INSTANCE );
		final LocalDate[] value = { LocalDate.of( 2000, 1, 1 ) };

		final Object[] physicalValues = (Object[]) jdbcType.getBinder( javaType )
				.getBindValue( value, options( DirectJavaTimeJdbcSupports.none() ) );
		assertInstanceOf( java.sql.Date[].class, physicalValues );

		final DirectJavaTimeJdbcSupport directArraySupport = DirectJavaTimeJdbcSupports.of(
				Set.of(),
				Set.of(),
				Set.of( LocalDate.class )
		);
		final Object[] directValues = (Object[]) jdbcType.getBinder( javaType )
				.getBindValue( value, options( directArraySupport ) );
		assertInstanceOf( LocalDate[].class, directValues );
	}

	@Test
	void resolvesPhysicalTypeCodes() {
		assertEquals(
				SqlTypes.DATE,
				JavaTimeJdbcType.getPhysicalJdbcTypeCode( LocalDateJdbcType.INSTANCE )
		);
		assertEquals(
				SqlTypes.TIMESTAMP,
				JavaTimeJdbcType.getPhysicalJdbcTypeCode( LocalDateTimeJdbcType.INSTANCE )
		);
	}

	@Test
	void convertsToPhysicalContainerRepresentation() {
		final var options = mock( WrapperOptions.class );
		when( options.getTypeConfiguration() ).thenReturn( new TypeConfiguration() );
		final LocalDate date = LocalDate.of( 2000, 1, 1 );
		assertEquals(
				java.sql.Date.valueOf( date ),
				JavaTimeJdbcType.toPhysicalJdbcValue(
						LocalDateJdbcType.INSTANCE,
						date,
						LocalDateJavaType.INSTANCE,
						options
				)
		);
	}

	@Test
	void retainsAnAlreadyPhysicalContainerRepresentation() {
		final var options = mock( WrapperOptions.class );
		when( options.getTypeConfiguration() ).thenReturn( new TypeConfiguration() );
		final OffsetDateTime offsetDateTime = OffsetDateTime.of(
				2000,
				1,
				1,
				12,
				34,
				56,
				0,
				ZoneOffset.ofHours( 2 )
		);
		assertEquals(
				offsetDateTime,
				JavaTimeJdbcType.toPhysicalJdbcValue(
						OffsetDateTimeJdbcType.INSTANCE,
						offsetDateTime,
						ZonedDateTimeJavaType.INSTANCE,
						options
				)
		);
	}

	private WrapperOptions options(DirectJavaTimeJdbcSupport support) {
		final var options = mock( WrapperOptions.class );
		when( options.getTypeConfiguration() ).thenReturn( new TypeConfiguration() );
		final var dialect = mock( Dialect.class );
		when( dialect.getDirectJavaTimeJdbcSupport() ).thenReturn( support );
		when( options.getDialect() ).thenReturn( dialect );
		return options;
	}
}
