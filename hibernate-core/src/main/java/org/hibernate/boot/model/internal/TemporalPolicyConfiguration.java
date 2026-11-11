package org.hibernate.boot.model.internal;

import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Map;

import org.hibernate.MappingException;
import jakarta.persistence.Version;
import jakarta.persistence.Embedded;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.ManyToMany;
import org.hibernate.annotations.TemporalPreservationPolicies;
import org.hibernate.annotations.TemporalPreservationPolicy;
import org.hibernate.models.spi.AnnotationTarget;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.type.internal.TemporalPolicySettings;
import org.hibernate.type.internal.TemporalPolicySettings.Selection;

/// Resolves repeatable policy declarations at the validated relational Java type boundary.
///
/// @author Steve Ebersole
public final class TemporalPolicyConfiguration {
	private TemporalPolicyConfiguration() {
	}

	/// Resolve the [Selection] allowing for scopes.
	public static void rejectExcludedDeclaration(MemberDetails member, boolean excludedContext) {
		final boolean declared = member.hasDirectAnnotationUsage( TemporalPreservationPolicy.class )
				|| member.hasDirectAnnotationUsage( TemporalPreservationPolicies.class );
		if ( declared && (excludedContext
				|| TemporalMappingConfiguration.excluded( member ) && !member.hasDirectAnnotationUsage( Version.class )
				|| member.hasDirectAnnotationUsage( Embedded.class )
				|| member.hasDirectAnnotationUsage( ManyToOne.class ) || member.hasDirectAnnotationUsage( OneToOne.class )
				|| member.hasDirectAnnotationUsage( OneToMany.class ) || member.hasDirectAnnotationUsage( ManyToMany.class )) ) {
			throw failure( member.getDeclaringType().getName() + "." + member.getName(), "declaration on an excluded mapping role" );
		}
	}

	public static Selection resolve(
			MemberDetails member,
			Class<?> javaType,
			Map<String, Object> settings,
			boolean enabled,
			boolean excluded,
			String role) {
		Selection selected = null;
		if ( member != null ) {
			final var local = atScope( member, javaType, true, role );
			if ( excluded && local != null ) {
				throw new MappingException( "Temporal preservation policy is not supported on excluded role '" + role + "'" );
			}
			selected = local;
			final var declaring = member.getDeclaringType();
			for ( var type = declaring; type != null; type = type.getSuperClass() ) {
				final var candidate = atScope( type, javaType, false, type.getName() );
				if ( selected == null ) selected = candidate;
			}
			if ( declaring.getPackage() != null ) {
				final var candidate = atScope( declaring.getPackage(), javaType, false, "package of " + declaring.getName() );
				if ( selected == null ) selected = candidate;
			}
			if ( declaring.getModule() != null ) {
				final var candidate = atScope( declaring.getModule(), javaType, false, "module of " + declaring.getName() );
				if ( selected == null ) selected = candidate;
			}
		}
		if ( excluded ) return Selection.DEFAULT;
		if ( selected != null ) {
			if ( !enabled ) {
				throw new MappingException( "Temporal preservation policy for '" + role + "' from " + selected.source()
						+ " requires hibernate.type.temporal_preservation=true" );
			}
			return selected;
		}
		return TemporalPolicySettings.resolve( settings, javaType );
	}

	private static Selection atScope(AnnotationTarget target, Class<?> javaType, boolean member, String source) {
		final var single = target.getDirectAnnotationUsage( TemporalPreservationPolicy.class );
		final var container = target.getDirectAnnotationUsage( TemporalPreservationPolicies.class );
		final var declarations = container == null ? new TemporalPreservationPolicy[0] : container.value();
		if ( single != null && container != null ) {
			throw failure( source, "both a policy and its repeatable container are declared" );
		}
		final var policies = single == null ? declarations : new TemporalPreservationPolicy[] { single };
		if ( member && policies.length > 1 ) throw failure( source, "a member accepts only one policy" );
		Selection fallback = null;
		Selection specific = null;
		final var seen = new HashSet<Class<?>>();
		for ( var policy : policies ) {
			if ( policy.value() == null || policy.types() == null ) throw failure( source, "null policy or types" );
			if ( member && policy.types().length != 0 ) throw failure( source, "member declarations must omit types" );
			final var selection = new Selection( policy.value(), source, member );
			if ( policy.types().length == 0 ) {
				if ( fallback != null ) throw failure( source, "multiple untargeted defaults" );
				fallback = selection;
			}
			else {
				for ( var type : policy.types() ) {
					if ( type != OffsetTime.class && type != OffsetDateTime.class && type != ZonedDateTime.class ) {
						throw failure( source, "type selectors must be OffsetTime, OffsetDateTime or ZonedDateTime" );
					}
					if ( !seen.add( type ) ) throw failure( source, "overlapping type selector " + type.getName() );
					if ( type == javaType ) specific = selection;
				}
			}
		}
		return specific == null ? fallback : specific;
	}

	private static MappingException failure(String source, String message) {
		return new MappingException( "Invalid temporal preservation policy at '" + source + "': " + message );
	}
}
