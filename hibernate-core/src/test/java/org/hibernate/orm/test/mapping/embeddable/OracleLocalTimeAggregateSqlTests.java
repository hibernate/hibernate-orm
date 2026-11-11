package org.hibernate.orm.test.mapping.embeddable;

import java.time.LocalTime;
import java.util.List;

import org.hibernate.dialect.aggregate.spi.AggregateWriteRendererRequest;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SelectablePath;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.type.descriptor.jdbc.AggregateJdbcType;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.aggregate.internal.OracleAggregateSupport;
import org.hibernate.dialect.type.internal.OracleLocalTimeJdbcType;
import org.hibernate.dialect.type.internal.OracleJsonJdbcType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.internal.SqlTypedMappingImpl;
import org.hibernate.sql.spi.StringBuilderSqlAppender;
import org.hibernate.type.descriptor.java.JdbcTimeJavaType;
import org.hibernate.type.descriptor.java.LocalDateTimeJavaType;
import org.hibernate.type.descriptor.java.LocalDateJavaType;
import org.hibernate.type.descriptor.java.OffsetDateTimeJavaType;
import org.hibernate.type.descriptor.jdbc.DateJdbcType;
import org.hibernate.type.descriptor.jdbc.TimestampWithTimeZoneJdbcType;
import org.hibernate.type.descriptor.java.LocalTimeJavaType;
import org.hibernate.type.descriptor.jdbc.LocalDateTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.TimeJdbcType;
import org.hibernate.type.internal.BasicTypeImpl;
import org.hibernate.type.spi.TypeConfiguration;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.ArgumentMatchers.any;
import static org.hibernate.type.SqlTypes.JSON;
import static org.hibernate.type.SqlTypes.JSON_ARRAY;
import static org.hibernate.type.SqlTypes.SQLXML;
import static org.hibernate.type.SqlTypes.XML_ARRAY;

/// Synthetic Oracle profiles verify SQL generation, not live old-server support.
///
/// @author Steve Ebersole
class OracleLocalTimeAggregateSqlTests {
	static final BasicTypeImpl<LocalTime> TIME =
			new BasicTypeImpl<>( LocalTimeJavaType.INSTANCE, OracleLocalTimeJdbcType.INSTANCE );

	static OracleAggregateSupport support(int version) {
		return (OracleAggregateSupport) new OracleDialect( DatabaseVersion.make( version ) ).getAggregateSupport();
	}

	static String read(int version, int format, String parent, String member, JdbcMapping mapping) {
		return read( version, format, parent, member, mapping, types( version >= 21 ) );
	}

	static TypeConfiguration types(boolean oson) {
		final var types = new TypeConfiguration();
		// Synthetic representation only; live/boot tests assert actual contributions.
		types.getJdbcTypeRegistry().addDescriptor( new OracleJsonJdbcType( null ) {
			@Override public boolean usesOsonTemporalValues() { return oson; }
		} );
		return types;
	}

	static String read(int version, int format, String parent, String member, JdbcMapping mapping, TypeConfiguration types) {
		return support( version ).aggregateComponentCustomReadExpression(
				"?", "?", parent, member, format, new SqlTypedMappingImpl( mapping ), types );
	}

	static String write(int version, String expression, JdbcMapping mapping) {
		return write( version, expression, mapping, types( version >= 21 ) );
	}

	static String write(int version, String expression, JdbcMapping mapping, TypeConfiguration types) {
		final var appender = new StringBuilderSqlAppender();
		support( version ).appendJsonWriteExpression(
				appender, () -> appender.appendSql( expression ), mapping, types );
		return appender.toString();
	}

	static String xmlWrite(int version, String valueSql) {
		final var member = mock( SelectableMapping.class );
		when( member.getJdbcMapping() ).thenReturn( TIME );
		when( member.getSelectablePath() ).thenReturn( SelectablePath.parse( "payload.value" ) );
		when( member.getSelectableName() ).thenReturn( "value" );
		when( member.getWriteExpression() ).thenReturn( "?" );
		final var embeddable = mock( EmbeddableMappingType.class );
		when( embeddable.getJdbcValueCount() ).thenReturn( 1 );
		when( embeddable.getJdbcValueSelectable( 0 ) ).thenReturn( member );
		final var jdbcType = mock( AggregateJdbcType.class );
		when( jdbcType.getDefaultSqlTypeCode() ).thenReturn( SQLXML );
		when( jdbcType.getEmbeddableMappingType() ).thenReturn( embeddable );
		final var mapping = mock( JdbcMapping.class );
		when( mapping.getJdbcType() ).thenReturn( jdbcType );
		final var aggregate = mock( SelectableMapping.class );
		when( aggregate.getJdbcMapping() ).thenReturn( mapping );
		when( aggregate.getSelectionExpression() ).thenReturn( "payload" );
		final var renderer = support( version ).aggregateCustomWriteExpressionRenderer(
				new AggregateWriteRendererRequest( aggregate, List.of( member ), new TypeConfiguration() ) );
		final var appender = new StringBuilderSqlAppender();
		final SqlAstTranslator<?> translator = mock( SqlAstTranslator.class );
		doAnswer( invocation -> { appender.appendSql( valueSql ); return null; } )
				.when( translator ).render( any(), any() );
		renderer.render( appender, translator, selectable -> mock( Expression.class ), null );
		return appender.toString();
	}

	@ParameterizedTest
	@ValueSource(ints = {19, 21, 23})
	void textProfilesRetainClockFields(int version) {
		for ( int format : new int[] { JSON, JSON_ARRAY, SQLXML, XML_ARRAY } ) {
			assertThat( read( version, format, "doc", "value", TIME, types( false ) ) )
					.contains( "1970-01-01", "case when", "HH24:MI:SS.FF9" );
		}
		assertThat( write( version, "value", TIME, types( false ) ) )
				.isEqualTo( "to_char(value,'HH24:MI:SS.FF9')" );
		assertThat( xmlWrite( version, "value" ) ).contains( "to_char(value,'HH24:MI:SS.FF9')" ).doesNotContain( "YYYY-MM-DD" );
	}

	@ParameterizedTest
	@ValueSource(ints = {21, 22, 23})
	void nativeJsonAndOtherTemporalTypesKeepTheirContracts(int version) {
		for ( int format : new int[] { JSON, JSON_ARRAY } ) {
			assertThat( read( version, format, "doc", "value", TIME ) )
					.isEqualTo( "json_value(doc,'$.value' returning timestamp(9))" );
		}
		assertThat( write( version, "value", TIME ) ).isEqualTo( "value" );
		for ( int format : new int[] { SQLXML, XML_ARRAY } ) {
			assertThat( read( version, format, "doc", "value", TIME ) ).contains( "1970-01-01", "HH24:MI:SS.FF9" );
		}
		final var timestamp = new BasicTypeImpl<>( LocalDateTimeJavaType.INSTANCE, LocalDateTimeJdbcType.INSTANCE );
		final var date = new BasicTypeImpl<>( LocalDateJavaType.INSTANCE, DateJdbcType.INSTANCE );
		final var zoned = new BasicTypeImpl<>( OffsetDateTimeJavaType.INSTANCE, TimestampWithTimeZoneJdbcType.INSTANCE );
		for ( int format : new int[] { JSON, JSON_ARRAY } ) {
			assertThat( read( version, format, "doc", "value", timestamp ) ).contains( "returning timestamp(9)" );
			assertThat( read( version, format, "doc", "value", date ) ).contains( "returning date" );
			assertThat( read( version, format, "doc", "value", zoned ) ).contains( "returning timestamp(9) with time zone" );
			assertThat( read( version, format, "doc", "value", timestamp, types( false ) ) ).contains( "to_timestamp(" );
			assertThat( read( version, format, "doc", "value", date, types( false ) ) ).contains( "to_date(" );
			assertThat( read( version, format, "doc", "value", zoned, types( false ) ) ).contains( "to_timestamp_tz(" );
		}
		assertThat( write( version, "value", timestamp ) ).isEqualTo( "value" );
		assertThat( read( 19, JSON, "doc", "value", timestamp ) ).contains( "YYYY-MM-DD\"T\"" )
				.doesNotContain( "1970-01-01" );
		assertThat( write( 19, "value", timestamp ) ).contains( "YYYY-MM-DD HH24" );
		final var legacy = new BasicTypeImpl<>( JdbcTimeJavaType.INSTANCE, TimeJdbcType.INSTANCE );
		assertThat( write( 19, "value", legacy ) ).isEqualTo( "to_char(value,'HH24:MI:SS')" );
		final var customJavaType = new BasicTypeImpl<>( new LocalTimeJavaType() {}, OracleLocalTimeJdbcType.INSTANCE );
		assertThat( read( 19, JSON, "doc", "value", customJavaType ) ).doesNotContain( "1970-01-01" );
	}
}
