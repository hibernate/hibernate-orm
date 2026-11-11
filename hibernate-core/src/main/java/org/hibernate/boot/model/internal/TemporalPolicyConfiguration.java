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

/// Resolves mapping-scoped temporal preservation policies and validates their declarations.
/// Precedence is member, declaring class, nearest superclass, declaring package,
/// declaring module, then factory settings. Within a non-member scope, an exact
/// type selector takes precedence over that scope's untargeted default.
///
/// All visited annotation scopes are checked for malformed or overlapping declarations,
/// even when a nearer scope has already supplied the effective policy. This class
/// determines policy and provenance; validation of the selected Java/JDBC pair and
/// execution of the policy belong to the type handling layer.
///
/// @author Steve Ebersole
public final class TemporalPolicyConfiguration {
	private TemporalPolicyConfiguration() {
	}

	/// Rejects an explicit member declaration on a role that cannot carry a policy.
	/// This early check also covers embedded and association members whose values are
	/// not resolved as ordinary basic mappings. A version member is not excluded merely
	/// because it is annotated with [Version]. Broader defaults are handled by [#resolve].
	///
	/// @param member the mapped field or property to inspect, never null
	/// @param excludedContext whether the containing mapping context excludes temporal policies
	/// @throws MappingException if the member declares a policy on an excluded role
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

	/// Resolves the policy for one mapping without modifying its annotations or factory settings.
	/// An excluded mapping rejects a local declaration but ignores broader defaults and
	/// returns [Selection#DEFAULT]. Without a member, only exclusion and factory-default
	/// resolution apply, as for mappings without a backing Java field or property.
	///
	/// A selected annotation requires preservation to be enabled. Factory-setting
	/// restrictions are validated separately by [TemporalPolicySettings#validate]; the
	/// {@code enabled} argument does not replace that factory-wide validation.
	///
	/// @param member the field or property supplying annotation scopes, or null when
	/// there is no backing Java member; class/package/module lookup starts from its
	/// declaring type, not the entity type that happens to inherit the attribute
	/// @param javaType the Java class used to match annotation type selectors and
	/// type-specific settings. At final basic-value resolution this is the preservation
	/// boundary type: an application converter's relational Java type, otherwise the
	/// mapped domain type. Early declaration checks may use the declared attribute type
	/// before converter resolution
	/// @param settings the owning factory's configuration, with policy settings already validated
	/// @param enabled the effective temporal-preservation flag, not a claim that this
	/// particular mapping or JDBC descriptor can fulfill the selected policy
	/// @param excluded whether this mapping role is outside policy application; the
	/// caller supplies mapping-context knowledge which cannot be inferred from the
	/// member alone, and must account for supported version mappings
	/// @param role the mapping path used in diagnostics, typically {@code Entity.attribute};
	/// it is not used to discover declarations or determine precedence
	/// @return the selected policy and declaration source, never null; the default
	/// selection for excluded mappings does not itself enable preservation validation
	/// @throws MappingException for malformed declarations, an explicit declaration
	/// on an excluded role, or a selected annotation when preservation is disabled
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
