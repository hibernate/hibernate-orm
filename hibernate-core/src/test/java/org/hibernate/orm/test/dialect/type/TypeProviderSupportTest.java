/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.type;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.ResultSet;
import java.time.Instant;

import javax.sql.rowset.serial.SerialClob;

import org.hibernate.HibernateException;
import org.hibernate.dialect.lob.spi.LobDataExtraction;
import org.hibernate.dialect.type.spi.DB2JdbcTypes;
import org.hibernate.dialect.type.spi.EnumRelationalValues;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.InstantJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Verifies provider-facing type facilities introduced for Dialect type
/// contributions.
///
/// @author Steve Ebersole
public class TypeProviderSupportTest {
	private enum ExampleEnum {
		SECOND,
		FIRST
	}

	@Test
	void db2TemporalDescriptorsAreStableAndUseTheirSemanticTypeCodes() {
		assertThat( DB2JdbcTypes.instant().getDefaultSqlTypeCode() ).isEqualTo( SqlTypes.INSTANT );
		assertThat( DB2JdbcTypes.localDate().getDefaultSqlTypeCode() ).isEqualTo( SqlTypes.LOCAL_DATE );
		assertThat( DB2JdbcTypes.localTime().getDefaultSqlTypeCode() ).isEqualTo( SqlTypes.LOCAL_TIME );
		assertThat( DB2JdbcTypes.localDateTime().getDefaultSqlTypeCode() ).isEqualTo( SqlTypes.LOCAL_DATE_TIME );
		assertThat( DB2JdbcTypes.offsetTime().getDefaultSqlTypeCode() ).isEqualTo( SqlTypes.OFFSET_TIME );
		assertThat( DB2JdbcTypes.offsetDateTime().getDefaultSqlTypeCode() ).isEqualTo( SqlTypes.OFFSET_DATE_TIME );
		assertThat( DB2JdbcTypes.zonedDateTime().getDefaultSqlTypeCode() ).isEqualTo( SqlTypes.ZONED_DATE_TIME );

		assertThat( DB2JdbcTypes.instant() ).isSameAs( DB2JdbcTypes.instant() );
		assertThat( DB2JdbcTypes.localDate() ).isSameAs( DB2JdbcTypes.localDate() );
		assertThat( DB2JdbcTypes.localTime() ).isSameAs( DB2JdbcTypes.localTime() );
		assertThat( DB2JdbcTypes.localDateTime() ).isSameAs( DB2JdbcTypes.localDateTime() );
		assertThat( DB2JdbcTypes.offsetTime() ).isSameAs( DB2JdbcTypes.offsetTime() );
		assertThat( DB2JdbcTypes.offsetDateTime() ).isSameAs( DB2JdbcTypes.offsetDateTime() );
		assertThat( DB2JdbcTypes.zonedDateTime() ).isSameAs( DB2JdbcTypes.zonedDateTime() );
	}

	@Test
	void db2ExtractionTreatsTheDriverNullPointerFailureAsNullOnlyForNullValues() throws Exception {
		final ResultSet resultSet = mock( ResultSet.class );
		when( resultSet.getObject( 1, Instant.class ) ).thenThrow( new NullPointerException( "driver" ) );
		when( resultSet.getObject( 1 ) ).thenReturn( null );
		assertThat( DB2JdbcTypes.instant().getExtractor( InstantJavaType.INSTANCE ).extract( resultSet, 1, null ) )
				.isNull();

		final ResultSet nonNullResultSet = mock( ResultSet.class );
		when( nonNullResultSet.getObject( 1, Instant.class ) ).thenThrow( new NullPointerException( "driver" ) );
		when( nonNullResultSet.getObject( 1 ) ).thenReturn( "not null" );
		assertThatExceptionOfType( NullPointerException.class )
				.isThrownBy( () -> DB2JdbcTypes.instant()
						.getExtractor( InstantJavaType.INSTANCE )
						.extract( nonNullResultSet, 1, null ) )
				.withMessage( "driver" );
	}

	@Test
	void db2CallableExtractionCoversIndexedAndNamedNullValues() throws Exception {
		final CallableStatement statement = mock( CallableStatement.class );
		when( statement.getObject( 1, Instant.class ) ).thenThrow( new NullPointerException( "indexed" ) );
		when( statement.getObject( 1 ) ).thenReturn( null );
		when( statement.getObject( "value", Instant.class ) ).thenThrow( new NullPointerException( "named" ) );
		when( statement.getObject( "value" ) ).thenReturn( null );

		final var extractor = DB2JdbcTypes.instant().getExtractor( InstantJavaType.INSTANCE );
		assertThat( extractor.extract( statement, 1, null ) ).isNull();
		assertThat( extractor.extract( statement, "value", null ) ).isNull();
	}

	@Test
	void lobExtractionMaterializesAndClosesTheSuppliedResource() throws Exception {
		final CloseTrackingInputStream inputStream = new CloseTrackingInputStream( "binary" );
		assertThat( LobDataExtraction.extractBytes( inputStream ) )
				.isEqualTo( "binary".getBytes( StandardCharsets.UTF_8 ) );
		assertThat( inputStream.closed ).isTrue();

		final SerialClob clob = new SerialClob( "characters".toCharArray() );
		assertThat( LobDataExtraction.extractString( clob ) ).isEqualTo( "characters" );
		assertThat( clob.length() ).isEqualTo( 10 );
	}

	@Test
	void lobExtractionPreservesHibernateExceptionConversion() throws Exception {
		final Clob clob = mock( Clob.class );
		when( clob.getCharacterStream() ).thenThrow( new java.sql.SQLException( "driver" ) );
		assertThatExceptionOfType( HibernateException.class )
				.isThrownBy( () -> LobDataExtraction.extractString( clob ) )
				.withMessage( "Unable to access lob stream" );
	}

	@Test
	void enumValuesPreserveDeclarationOrderAndConverterSemantics() {
		assertThat( EnumRelationalValues.names( ExampleEnum.class ) ).containsExactly( "SECOND", "FIRST" );
		assertThat( EnumRelationalValues.convertedValues( ExampleEnum.class, converter( false ) ) )
				.containsExactly( "second", "first" );
		assertThatExceptionOfType( HibernateException.class )
				.isThrownBy( () -> EnumRelationalValues.convertedValues( ExampleEnum.class, converter( true ) ) )
				.withMessageContaining( ExampleEnum.class.getName() );
	}

	private static BasicValueConverter<Enum<?>, String> converter(boolean returnNull) {
		return new BasicValueConverter<>() {
			@Override
			public Enum<?> toDomainValue(String relationalForm) {
				return null;
			}

			@Override
			public String toRelationalValue(Enum<?> domainForm) {
				return returnNull ? null : domainForm.name().toLowerCase();
			}

			@Override
			public JavaType<Enum<?>> getDomainJavaType() {
				return null;
			}

			@Override
			public JavaType<String> getRelationalJavaType() {
				return null;
			}
		};
	}

	private static class CloseTrackingInputStream extends ByteArrayInputStream {
		private boolean closed;

		private CloseTrackingInputStream(String value) {
			super( value.getBytes( StandardCharsets.UTF_8 ) );
		}

		@Override
		public void close() throws IOException {
			closed = true;
			super.close();
		}
	}
}
