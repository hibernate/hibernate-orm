package org.hibernate.orm.test.mapping.javatime.redesign;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.dialect.jdbc.spi.SybaseDriverKind;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Component;
import org.hibernate.testing.orm.junit.DialectFeatureCheck;
import org.hibernate.type.descriptor.java.InstantJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.TimestampUtcAsPlainJdbcTimestampJdbcType;

import static org.assertj.core.api.Assertions.assertThat;

/// Test-only expectation for the observed jConnect BIGDATETIME microsecond path.
/// Does not change input values or general Sybase precision semantics.
///
/// @author Steve Ebersole
public final class SybaseCachePrecisionSupport {
	private SybaseCachePrecisionSupport() { }

	public static class JConnect implements DialectFeatureCheck {
		@Override
		public boolean apply(Dialect dialect) {
			return dialect instanceof SybaseASEDialect sybase && sybase.getDriverKind() == SybaseDriverKind.JCONNECT;
		}
	}

	static boolean observedPath(Dialect dialect, JavaType<?> javaType, JdbcType jdbcType, String ddl, Integer precision) {
		return new JConnect().apply( dialect )
				&& javaType.getClass() == InstantJavaType.class
				&& jdbcType.getClass() == TimestampUtcAsPlainJdbcTimestampJdbcType.class
				&& "bigdatetime".equalsIgnoreCase( ddl ) && Integer.valueOf( 6 ).equals( precision );
	}

	static Instant databaseValue(MetadataImplementor metadata, Class<?> entityType, Instant input) {
		final var dialect = metadata.getDatabase().getDialect();
		assertThat( new JConnect().apply( dialect ) ).as( "jConnect BIGDATETIME prerequisite" ).isTrue();
		final var entity = metadata.getEntityBinding( entityType.getName() );
		for ( String property : List.of( "adjusted", "unadjusted" ) ) {
			final var component = (Component) entity.getProperty( property ).getValue();
			final var primary = (BasicValue) component.getProperty( "instant" ).getValue();
			final var mapping = primary.resolve().getJdbcMapping();
			final var column = primary.getColumns().get( 0 );
			final String ddl = column.getSqlType( metadata );
			final Integer precision = column.getColumnSize( dialect, metadata ).getPrecision();
			final String description = property + " " + mapping.getJavaTypeDescriptor().getClass().getName()
					+ "/" + mapping.getJdbcType().getClass().getName() + " " + ddl + " precision=" + precision;
			System.out.println( "SYBASE_CACHE_PRECISION " + description );
			assertThat( observedPath( dialect, mapping.getJavaTypeDescriptor(), mapping.getJdbcType(), ddl, precision ) )
					.as( description ).isTrue();
		}
		return input.truncatedTo( ChronoUnit.MICROS );
	}
}
