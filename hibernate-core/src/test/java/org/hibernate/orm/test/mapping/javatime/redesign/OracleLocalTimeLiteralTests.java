package org.hibernate.orm.test.mapping.javatime.redesign;

import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.TimeZone;

import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.type.internal.OracleLocalTimeJdbcType;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/// Literal and bound values share the same zone-less timestamp carrier, including fractions.
/// @author Steve Ebersole
class OracleLocalTimeLiteralTests {
	@ParameterizedTest
	@ValueSource(strings={"00:00:00", "12:34:56.123456", "23:59:59.999999"})
	@SuppressWarnings("unchecked")
	void literalMatchesBinderThroughJavaType(String text) throws Exception {
		final var value=LocalTime.parse(text);
		final var carrier=value.atDate(java.time.LocalDate.of(1970,1,1));
		final var descriptor=OracleLocalTimeJdbcType.INSTANCE;
		// Deliberately use a non-LocalTime input to exercise the JavaType contract.
		final JavaType<String> javaType=mock(JavaType.class);
		when(javaType.unwrap(eq(text),eq(LocalTime.class),any())).thenReturn(value);
		String previous=null;
		for (String zone : new String[]{"UTC","Asia/Tokyo","America/Denver"}) {
			final var options=mock(WrapperOptions.class);
			when(options.getJdbcTimeZone()).thenReturn(TimeZone.getTimeZone(zone));
			final var statement=mock(PreparedStatement.class);
			descriptor.getBinder(javaType).bind(statement,text,1,options);
			verify(statement).setObject(1,carrier,java.sql.Types.TIMESTAMP);
			final var literal=descriptor.getJdbcLiteralFormatter(javaType).toJdbcLiteral(text,new OracleDialect(),options);
			assertThat(literal).contains("1970-01-01", value.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")));
			assertThat(literal).doesNotStartWith("{t '");
			if(value.getNano()!=0) assertThat(literal).contains(text);
			if(previous!=null) assertThat(literal).isEqualTo(previous);
			previous=literal;
		}
	}
}
