package org.hibernate.type.descriptor.jdbc;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.hibernate.dialect.type.internal.OracleLocalTimeJdbcType;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.LocalTimeJavaType;
import org.hibernate.type.spi.TypeConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Container conversion consumes the binder's value, including a different Java class.
///
/// @author Steve Ebersole
class TemporalAggregatePhysicalValueTest {
	@Test
	void convertsTheAnchoredJdbcTimestampWithoutWrappingItAsLocalTime() {
		var options = mock( WrapperOptions.class );
		when( options.getTypeConfiguration() ).thenReturn( new TypeConfiguration() );
		var carrier = LocalDateTime.of( 1970, 1, 1, 12, 34, 56, 123456789 );
		assertEquals( Timestamp.valueOf( carrier ), JavaTimeJdbcType.toPhysicalJdbcValue(
				OracleLocalTimeJdbcType.INSTANCE, carrier, LocalTimeJavaType.INSTANCE, options ) );
	}

	@Test
	void alreadyNativeValueIsNotReinterpreted() {
		var options = mock( WrapperOptions.class );
		when( options.getTypeConfiguration() ).thenReturn( new TypeConfiguration() );
		var carrier = Timestamp.valueOf( "1970-01-01 12:34:56.123456789" );
		assertSame( carrier, JavaTimeJdbcType.toPhysicalJdbcValue(
				OracleLocalTimeJdbcType.INSTANCE, carrier, LocalTimeJavaType.INSTANCE, options ) );
	}
}
