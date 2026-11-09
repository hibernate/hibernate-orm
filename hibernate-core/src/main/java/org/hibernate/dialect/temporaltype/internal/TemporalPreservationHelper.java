package org.hibernate.dialect.temporaltype.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Internal;
import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.temporaltype.spi.TemporalPreservation;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.TemporalJdbcType;

/// Dispatches optional descriptor assessments without caching their results.
/// The thread-local stack exists only during callbacks and detects identity-based
/// recursion, including delegation cycles involving more than one pair.
///
/// @author Steve Ebersole
@Internal
public final class TemporalPreservationHelper {
	private static final ThreadLocal<List<Assessment>> ACTIVE = new ThreadLocal<>();

	private TemporalPreservationHelper() {
	}

	/// Returns null when no descriptor assessment applies; a non-null report is final.
	public static TemporalPreservation assess(JavaType<?> javaType, JdbcType jdbcType, Dialect dialect) {
		if ( !(jdbcType instanceof TemporalJdbcType temporal) ) {
			return null;
		}
		var active = ACTIVE.get();
		if ( active == null ) {
			active = new ArrayList<>();
			ACTIVE.set( active );
		}
		for ( var assessment : active ) {
			if ( assessment.javaType == javaType && assessment.jdbcType == jdbcType && assessment.dialect == dialect ) {
				throw failure( javaType, jdbcType, "recursive assessment through " + dialect.getClass().getName(), null );
			}
		}
		active.add( new Assessment( javaType, jdbcType, dialect ) );
		try {
			final var result = temporal.getTemporalPreservation( javaType, dialect );
			if ( result == null ) {
				throw failure( javaType, jdbcType, "returned a null Optional; return Optional.empty() to decline", null );
			}
			return result.orElse( null );
		}
		catch (RuntimeException failure) {
			throw failure( javaType, jdbcType, "callback failed: " + failure.getMessage(), failure );
		}
		finally {
			active.remove( active.size() - 1 );
			if ( active.isEmpty() ) {
				ACTIVE.remove();
			}
		}
	}

	private static MappingException failure(JavaType<?> javaType, JdbcType jdbcType, String detail, Throwable cause) {
		return new MappingException( "TemporalJdbcType SPI assessment for " + javaType.getClass().getName()
				+ " + " + jdbcType.getClass().getName() + " " + detail, cause );
	}

	private record Assessment(JavaType<?> javaType, JdbcType jdbcType, Dialect dialect) {
	}
}
