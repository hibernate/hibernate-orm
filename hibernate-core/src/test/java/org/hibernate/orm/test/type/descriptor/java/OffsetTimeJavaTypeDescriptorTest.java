/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.type.descriptor.java;

import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.util.TimeZone;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.jdbc.env.internal.NonContextualLobCreator;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.OffsetTimeJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.type.format.FormatMapper;
import org.hibernate.type.spi.TypeConfiguration;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@BaseUnitTest
public class OffsetTimeJavaTypeDescriptorTest {

	@Test
	@JiraKey("HHH-17229")
	public void testWrap() {
		final OffsetTimeJavaType javaType = OffsetTimeJavaType.INSTANCE;
		final WrapperOptions wrapperOptions = new WrapperOptions() {
			@Override
			public SharedSessionContractImplementor getSession() {
				return null;
			}

			public boolean useStreamForLobBinding() {
				return false;
			}

			@Override
			public int getPreferredSqlTypeCodeForBoolean() {
				return 0;
			}

			@Override
			public boolean useLanguageTagForLocale() {
				return true;
			}

			public LobCreator getLobCreator() {
				return NonContextualLobCreator.INSTANCE;
			}

			public JdbcType remapSqlTypeDescriptor(JdbcType sqlTypeDescriptor) {
				return sqlTypeDescriptor;
			}

			@Override
			public TimeZone getJdbcTimeZone() {
				return null;
			}

			@Override
			public Dialect getDialect() {
				return null;
			}

			@Override
			public TypeConfiguration getTypeConfiguration() {
				return null;
			}

			@Override
			public FormatMapper getXmlFormatMapper() {
				return null;
			}

			@Override
			public FormatMapper getJsonFormatMapper() {
				return null;
			}
		};

		final Time sqlTime = new Time(
				LocalDate.EPOCH.atTime( LocalTime.of( 0, 1, 2, 0 ) )
						.toInstant( ZoneOffset.ofHours( 4 ) )
						.plusMillis( 123 )
						.toEpochMilli()
		);
		final OffsetTime wrappedSqlTime = javaType.wrap( sqlTime, wrapperOptions );
		assertThat( wrappedSqlTime ).isEqualTo( LocalTime.of( 20, 1, 2, 123_000_000 ).atOffset( OffsetDateTime.now().getOffset() ) );
	}
}
