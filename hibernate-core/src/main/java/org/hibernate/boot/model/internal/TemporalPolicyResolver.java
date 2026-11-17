package org.hibernate.boot.model.internal;

import java.util.HashSet;
import java.util.Map;

import jakarta.annotation.Nullable;

import org.hibernate.Internal;
import org.hibernate.MappingException;
import org.hibernate.annotations.TemporalPreservationPolicies;
import org.hibernate.annotations.TemporalPreservationPolicy;
import org.hibernate.models.spi.AnnotationTarget;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.type.internal.TemporalPolicySettings;
import org.hibernate.type.internal.TemporalPolicySettings.Selection;

/// Resolves the [TemporalPreservationPolicy] of a mapped value.
///
/// The policy is taken from the first of the following which declares one:
/// the mapped member, the class declaring it, each superclass of that class
/// in turn, the package of that class, the module of that class, and finally
/// the configuration settings. On a class, package or module, a declaration
/// naming the Java type takes precedence over one naming no types.
///
/// Every annotated element on the way is checked for malformed declarations,
/// including those beyond the one which supplies the policy.
///
/// @since 8.1
/// @author Steve Ebersole
@Internal
public final class TemporalPolicyResolver {
	private TemporalPolicyResolver() {
	}

	/// Resolve the policy for a value of the given Java class, which must be
	/// one a policy [applies to][TemporalPolicySettings#appliesTo].
	///
	/// @param member the mapped member, or `null` when the value has none
	public static Selection resolve(@Nullable MemberDetails member, Class<?> javaClass, Map<?, ?> settings, String role) {
		Selection selected = null;
		if ( member != null ) {
			selected = atScope( member, javaClass, true, "'" + role + "'" );
			final var declaring = member.getDeclaringType();
			for ( var type = declaring; type != null; type = type.getSuperClass() ) {
				final var candidate = atScope( type, javaClass, false, "class '" + type.getName() + "'" );
				if ( selected == null ) {
					selected = candidate;
				}
			}
			if ( declaring.getPackage() != null ) {
				final var candidate = atScope(
						declaring.getPackage(),
						javaClass,
						false,
						"the package of '" + declaring.getName() + "'"
				);
				if ( selected == null ) {
					selected = candidate;
				}
			}
			if ( declaring.getModule() != null ) {
				final var candidate = atScope(
						declaring.getModule(),
						javaClass,
						false,
						"the module of '" + declaring.getName() + "'"
				);
				if ( selected == null ) {
					selected = candidate;
				}
			}
		}
		return selected != null ? selected : TemporalPolicySettings.resolve( settings, javaClass );
	}

	/// Whether the member itself declares a policy.
	public static boolean declaresPolicy(@Nullable MemberDetails member) {
		return member != null
			&& ( member.hasDirectAnnotationUsage( TemporalPreservationPolicy.class )
				|| member.hasDirectAnnotationUsage( TemporalPreservationPolicies.class ) );
	}

	private static @Nullable Selection atScope(
			AnnotationTarget target,
			Class<?> javaClass,
			boolean member,
			String source) {
		final var single = target.getDirectAnnotationUsage( TemporalPreservationPolicy.class );
		final var container = target.getDirectAnnotationUsage( TemporalPreservationPolicies.class );
		if ( single != null && container != null ) {
			throw failure( source, "both the annotation and its container are present" );
		}
		final TemporalPreservationPolicy[] policies;
		if ( single != null ) {
			policies = new TemporalPreservationPolicy[] { single };
		}
		else if ( container != null ) {
			policies = container.value();
		}
		else {
			return null;
		}
		if ( member && policies.length > 1 ) {
			throw failure( source, "a field or property accepts only one declaration" );
		}

		Selection untargeted = null;
		Selection targeted = null;
		final var seen = new HashSet<Class<?>>();
		for ( var policy : policies ) {
			final var selection = new Selection( policy.value(), "@TemporalPreservationPolicy on " + source );
			if ( policy.types().length == 0 ) {
				if ( untargeted != null ) {
					throw failure( source, "more than one declaration names no types" );
				}
				untargeted = selection;
			}
			else {
				if ( member ) {
					throw failure( source, "a declaration on a field or property must not name types" );
				}
				for ( var type : policy.types() ) {
					if ( !TemporalPolicySettings.appliesTo( type ) ) {
						throw failure(
								source,
								"type '" + type.getName() + "' is not OffsetTime, OffsetDateTime or ZonedDateTime"
						);
					}
					if ( !seen.add( type ) ) {
						throw failure( source, "type '" + type.getName() + "' is named more than once" );
					}
					if ( type == javaClass ) {
						targeted = selection;
					}
				}
			}
		}
		return targeted != null ? targeted : untargeted;
	}

	private static MappingException failure(String source, String problem) {
		return new MappingException( "Invalid @TemporalPreservationPolicy on " + source + ": " + problem );
	}
}
