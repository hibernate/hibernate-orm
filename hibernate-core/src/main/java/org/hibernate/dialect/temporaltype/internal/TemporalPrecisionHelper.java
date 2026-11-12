package org.hibernate.dialect.temporaltype.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.MappingException;
import org.hibernate.dialect.temporaltype.spi.TemporalPrecisionContext;
import org.hibernate.dialect.temporaltype.spi.TemporalPrecisionSupport;
import org.hibernate.type.descriptor.jdbc.TemporalPrecisionJdbcType;

/// Dispatches an optional precision contribution without boot JDBC or a shared
/// plan cache. The temporary stack detects recursive descriptor assessment, not
/// runtime precision context; it is always released after the callback.
///
/// @author Steve Ebersole
public final class TemporalPrecisionHelper {
	private static final ThreadLocal<List<TemporalPrecisionContext>> ACTIVE = new ThreadLocal<>();

	private TemporalPrecisionHelper() {
	}

	/// Returns null only when the descriptor declines the context. An explicit
	/// unsupported result remains authoritative to the caller.
	public static TemporalPrecisionSupport assess(TemporalPrecisionContext context) {
		if ( !(context.jdbcType() instanceof TemporalPrecisionJdbcType precision) ) {
			return null;
		}
		var active = ACTIVE.get();
		if ( active == null ) {
			active = new ArrayList<>();
			ACTIVE.set( active );
		}
		for ( var previous : active ) {
			if ( previous.javaType() == context.javaType() && previous.jdbcType() == context.jdbcType()
					&& previous.dialect() == context.dialect() ) {
				throw failure( context, "recursive precision assessment", null );
			}
		}
		active.add( context );
		try {
			final var result = precision.getTemporalPrecisionSupport( context );
			if ( result == null ) {
				throw failure( context, "returned a null Optional; use Optional.empty() to decline", null );
			}
			return result.orElse( null );
		}
		catch (RuntimeException exception) {
			throw failure( context, "callback failed: " + exception.getMessage(), exception );
		}
		finally {
			active.remove( active.size() - 1 );
			if ( active.isEmpty() ) {
				ACTIVE.remove();
			}
		}
	}

	public static MappingException failure(TemporalPrecisionContext context, String reason, Throwable cause) {
		return new MappingException( "Temporal precision for '" + context.role() + "' ("
				+ context.javaType().getClass().getName() + " + " + context.jdbcType().getClass().getName()
				+ ", DDL=" + context.ddlTypeName() + ", declared=" + context.declaredPrecision()
				+ ", resolved=" + context.precision() + ", operation=" + context.operation()
				+ ", policy=" + context.policy() + ", representation=" + context.representation()
				+ "): " + reason, cause );
	}
}
