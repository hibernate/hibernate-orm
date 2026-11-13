package org.hibernate.dialect.temporaltype.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.MappingException;
import org.hibernate.dialect.temporaltype.spi.TemporalCallablePrecisionContext;
import org.hibernate.dialect.temporaltype.spi.TemporalPrecisionSupport;
import org.hibernate.type.descriptor.jdbc.TemporalCallablePrecisionJdbcType;

/// Callable-only contribution dispatch, with bounded recursive delegation and
/// diagnostics which contain contract metadata but never parameter values.
///
/// @author Steve Ebersole
public final class TemporalCallablePrecisionHelper {
	private static final ThreadLocal<List<TemporalCallablePrecisionContext>> ACTIVE = new ThreadLocal<>();
	private TemporalCallablePrecisionHelper() {}

	public static TemporalPrecisionSupport assess(TemporalCallablePrecisionContext context) {
		if ( !(context.type().getJdbcType() instanceof TemporalCallablePrecisionJdbcType contributor) ) { return null; }
		var active = ACTIVE.get();
		if ( active == null ) { active = new ArrayList<>(); ACTIVE.set( active ); }
		for ( var previous : active ) {
			if ( previous.type().getJdbcType() == context.type().getJdbcType()
					&& previous.type().getJdbcJavaType() == context.type().getJdbcJavaType()
					&& previous.dialect() == context.dialect() ) {
				throw failure( context, "recursive callable precision assessment" );
			}
		}
		active.add( context );
		try {
			final var result = contributor.getTemporalCallablePrecisionSupport( context );
			if ( result == null ) { throw failure( context, "null Optional; use Optional.empty() to decline" ); }
			return result.orElse( null );
		}
		catch (MappingException failure) { throw failure; }
		catch (RuntimeException failure) {
			// A callback message might contain values; retain its cause without copying it into our message.
			throw new MappingException( describe( context ) + ": callable precision callback failed", failure );
		}
		finally {
			active.remove( active.size() - 1 );
			if ( active.isEmpty() ) { ACTIVE.remove(); }
		}
	}

	public static String describe(TemporalCallablePrecisionContext context) {
		return "Callable temporal precision for '" + context.role() + "' (" + context.mode() + '/' + context.boundary()
				+ ", access=" + context.valueAccess() + ", registration=" + context.registrationAccess()
				+ ", " + context.type().getJdbcJavaType().getJavaTypeClass().getTypeName()
				+ '/' + context.type().getJdbcType().getClass().getName() + ", type=" + context.type().getClass().getName()
				+ ", JDBC=" + context.jdbcTypeCode() + ", policy=" + context.policy() + ", adjustment=" + context.adjustment() + ')';
	}

	public static MappingException failure(TemporalCallablePrecisionContext context, String reason) {
		return new MappingException( describe( context ) + ": " + reason
				+ "; select an evidenced callable descriptor/profile. Attribute precision does not declare a routine signature" );
	}
}
