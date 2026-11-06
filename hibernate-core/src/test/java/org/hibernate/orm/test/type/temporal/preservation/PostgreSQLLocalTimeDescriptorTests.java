package org.hibernate.orm.test.type.temporal.preservation;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.time.LocalTime;
import java.util.stream.Stream;
import org.hibernate.dialect.type.internal.PostgreSQLLocalTimeJdbcType;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.LocalTimeJavaType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/// Sentinel reconstruction covers result sets, positional/named callables and
/// nulls, while retaining the last real microsecond before midnight.
///
/// @author Steve Ebersole
class PostgreSQLLocalTimeDescriptorTests {
	static Stream<LocalTime> values() { return Stream.of( null, LocalTime.MAX, LocalTime.MIDNIGHT, LocalTime.parse( "23:59:59.999999" ) ); }
	@ParameterizedTest
	@MethodSource("values")
	void reconstructsAllGetters(LocalTime value) throws Exception {
		final var result = mock( ResultSet.class );
		final var callable = mock( CallableStatement.class );
		when( result.getObject( 1, LocalTime.class ) ).thenReturn( value );
		when( callable.getObject( 1, LocalTime.class ) ).thenReturn( value );
		when( callable.getObject( "value", LocalTime.class ) ).thenReturn( value );
		final var extractor = PostgreSQLLocalTimeJdbcType.INSTANCE.getExtractor( LocalTimeJavaType.INSTANCE );
		final var options = mock( WrapperOptions.class );
		final var expected = LocalTime.MAX.equals( value ) ? LocalTime.MIDNIGHT : value;
		assertThat( extractor.extract( result, 1, options ) ).isEqualTo( expected );
		assertThat( extractor.extract( callable, 1, options ) ).isEqualTo( expected );
		assertThat( extractor.extract( callable, "value", options ) ).isEqualTo( expected );
	}
}
