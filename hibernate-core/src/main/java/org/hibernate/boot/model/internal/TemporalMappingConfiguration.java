package org.hibernate.boot.model.internal;

import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.annotations.TimeZoneStorageType;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneColumn;
import org.hibernate.cfg.MappingSettings;

import java.lang.annotation.Annotation;
import java.time.DateTimeException;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.hibernate.MappingException;
import org.hibernate.annotations.TimeZoneAdjustment;
import org.hibernate.annotations.TimeZoneRepresentation;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;

/// Declaration-based temporal annotation lookup. Configuration belongs to the
/// mapped member's declaring context, never to a subclass or embedding site.
///
/// @author Steve Ebersole
/// @since 8.0
public record TemporalMappingConfiguration(
		TimeZoneRepresentation.Type representation,
		ZoneId adjustment,
		boolean adjustmentDeclared,
		boolean nativeStorage) {

	static boolean excluded(MemberDetails member) {
		return member.hasDirectAnnotationUsage( jakarta.persistence.Id.class )
				|| member.hasDirectAnnotationUsage( jakarta.persistence.EmbeddedId.class )
				|| member.hasDirectAnnotationUsage( jakarta.persistence.Version.class )
				|| member.isArray()
				|| member.getType().determineRawClass().isRealClass()
						&& java.util.Map.class.isAssignableFrom( member.getType().determineRawClass().toJavaClass() )
				|| member.isPlural() && !member.hasDirectAnnotationUsage( jakarta.persistence.ElementCollection.class )
				|| excludedSqlType( member );
	}

	private static boolean excludedSqlType(MemberDetails member) {
		final var jdbcType = member.getDirectAnnotationUsage( org.hibernate.annotations.JdbcTypeCode.class );
		return jdbcType != null && switch ( jdbcType.value() ) {
			case org.hibernate.type.SqlTypes.ARRAY, org.hibernate.type.SqlTypes.STRUCT,
					org.hibernate.type.SqlTypes.JSON, org.hibernate.type.SqlTypes.SQLXML -> true;
			default -> false;
		};
	}

	static void rejectExplicitDeclarations(MemberDetails member) {
		rejectExplicitDeclarations( member, true );
	}

	static void rejectExplicitDeclarations(MemberDetails member, boolean direct) {
		if ( member.hasDirectAnnotationUsage( TimeZoneRepresentation.class )
				|| member.hasDirectAnnotationUsage( TimeZoneAdjustment.class )
				|| direct && member.hasDirectAnnotationUsage( TimeZoneColumn.class ) ) {
			throw new MappingException( "Temporal annotations are not supported on excluded role '"
					+ member.getDeclaringType().getName() + "." + member.getName() + "'" );
		}
	}

	public static boolean supportsRepresentation(Class<?> type) {
		return type == OffsetTime.class || type == OffsetDateTime.class || type == ZonedDateTime.class;
	}

	public static TemporalMappingConfiguration resolve(MemberDetails member, Class<?> type, boolean direct) {
		if ( excluded( member ) ) {
			rejectExplicitDeclarations( member, direct );
			return new TemporalMappingConfiguration( null, null, false, false );
		}
		final var representation = find( member, TimeZoneRepresentation.class );
		final var adjustment = find( member, TimeZoneAdjustment.class );
		final String role = member.getDeclaringType().getName() + "." + member.getName();
		final boolean eligible = supportsRepresentation( type );
		if ( !eligible && member.hasDirectAnnotationUsage( TimeZoneRepresentation.class ) ) {
			throw new MappingException( "Timezone representation is not applicable to '" + role + "' (" + type.getName() + ")" );
		}
		if ( !direct && (eligible && representation != null || adjustment != null && applicable( adjustment.value(), type )) ) {
			throw new MappingException( "Temporal annotations on '" + role + "' require hibernate.type.temporal_preservation=true (deprecated alias: hibernate.type.java_time_use_direct_jdbc=true)" );
		}
		ZoneId target = null;
		// Broader defaults have no target semantics for ineligible domain types.
		if ( adjustment != null && !TimeZoneAdjustment.NONE.equals( adjustment.value() )
				&& (eligible || member.hasDirectAnnotationUsage( TimeZoneAdjustment.class )) ) {
			try {
				target = ZoneId.of( adjustment.value() );
			}
			catch ( DateTimeException | NullPointerException ex ) {
				throw new MappingException( "Invalid timezone adjustment '" + adjustment.value() + "' for '" + role + "'", ex );
			}
			if ( !eligible || type == OffsetTime.class && !(target instanceof ZoneOffset) ) {
				if ( member.hasDirectAnnotationUsage( TimeZoneAdjustment.class ) ) {
					throw new MappingException( "Timezone adjustment '" + target + "' is not applicable to '" + role + "'" );
				}
				target = null;
			}
		}
		return new TemporalMappingConfiguration(
				eligible && representation != null ? representation.value() : null,
				target,
				// A skipped nearer declaration still prevents adjustment fallback.
				adjustment != null,
				false
		);
	}

	public static TemporalMappingConfiguration resolve(
			MemberDetails member,
			Class<?> type,
			MetadataBuildingContext context) {
		final boolean direct = context.isPreferJavaTimeJdbcTypesEnabled();
		final var scoped = resolve( member, type, direct );
		if ( !direct || excluded( member ) ) {
			return scoped;
		}
		final String role = member.getDeclaringType().getName() + "." + member.getName();
		final var column = member.getDirectAnnotationUsage( TimeZoneColumn.class );
		if ( !supportsRepresentation( type ) ) {
			if ( column != null ) {
				throw new MappingException( "TimeZoneColumn is not applicable to '" + role + "'" );
			}
			return scoped;
		}
		var representation = scoped.representation();
		var target = scoped.adjustment();
		boolean nativeStorage = false;
		final var localRepresentation = member.getDirectAnnotationUsage( TimeZoneRepresentation.class );
		final var legacy = member.getDirectAnnotationUsage( TimeZoneStorage.class );
		if ( column != null ) {
			if ( localRepresentation != null && localRepresentation.value() == TimeZoneRepresentation.Type.INLINE ) {
				throw new MappingException( "Conflicting INLINE and TimeZoneColumn on '" + role + "'" );
			}
			representation = TimeZoneRepresentation.Type.COLUMN;
		}
		if ( legacy != null ) {
			final var policy = legacy.value();
			if ( policy == TimeZoneStorageType.NORMALIZE
					|| policy == TimeZoneStorageType.AUTO
					|| policy == TimeZoneStorageType.DEFAULT ) {
				throw legacyFailure( role, policy );
			}
			final var legacyRepresentation = policy == TimeZoneStorageType.COLUMN
					? TimeZoneRepresentation.Type.COLUMN : TimeZoneRepresentation.Type.INLINE;
			if ( localRepresentation != null && localRepresentation.value() != legacyRepresentation
					|| column != null && legacyRepresentation != TimeZoneRepresentation.Type.COLUMN ) {
				throw new MappingException( "Conflicting temporal storage declarations on '" + role + "'" );
			}
			representation = legacyRepresentation;
			nativeStorage = policy == TimeZoneStorageType.NATIVE;
			if ( policy == TimeZoneStorageType.NORMALIZE_UTC ) {
				if ( member.hasDirectAnnotationUsage( TimeZoneAdjustment.class ) ) {
					throw new MappingException( "Remove legacy NORMALIZE_UTC before declaring TimeZoneAdjustment on '" + role + "'" );
				}
				target = ZoneOffset.UTC;
			}
		}
		final Object global = context.getBootstrapContext().getServiceRegistry()
				.requireService( ConfigurationService.class )
				.getSettings().get( MappingSettings.TIMEZONE_DEFAULT_STORAGE );
		if ( global != null ) {
			final var policy = TimeZoneStorageType.valueOf( global.toString() );
			switch ( policy ) {
				case NORMALIZE, AUTO, DEFAULT -> {
					if ( representation == null ) {
						throw new MappingException( "Explicit global setting '" + MappingSettings.TIMEZONE_DEFAULT_STORAGE
								+ "=" + policy + "' has no narrower storage replacement for temporal mapping '" + role
								+ "'; choose representation and adjustment explicitly with @TimeZoneRepresentation and @TimeZoneAdjustment"
								+ " (an adjustment target or NONE alone does not replace storage)" );
					}
				}
				case COLUMN -> {
					if ( representation == null ) representation = TimeZoneRepresentation.Type.COLUMN;
				}
				case NATIVE -> {
					if ( representation == null ) {
						representation = TimeZoneRepresentation.Type.INLINE;
						nativeStorage = true;
					}
				}
				case NORMALIZE_UTC -> {
					if ( representation == null ) representation = TimeZoneRepresentation.Type.INLINE;
					if ( !scoped.adjustmentDeclared()
							&& (legacy == null || legacy.value() != TimeZoneStorageType.NORMALIZE_UTC) ) {
						target = ZoneOffset.UTC;
					}
				}
			}
		}
		return new TemporalMappingConfiguration(
				representation == null ? TimeZoneRepresentation.Type.INLINE : representation,
				target, scoped.adjustmentDeclared(), nativeStorage );
	}

	private static MappingException legacyFailure(String role, TimeZoneStorageType policy) {
		return new MappingException( "Explicit local @TimeZoneStorage(" + policy + ") is not supported for '" + role
				+ "'; choose representation and adjustment explicitly with @TimeZoneRepresentation and @TimeZoneAdjustment" );
	}

	private static boolean applicable(String target, Class<?> type) {
		if ( TimeZoneAdjustment.NONE.equals( target ) ) {
			return true;
		}
		if ( !supportsRepresentation( type ) ) {
			return false;
		}
		try {
			return type != OffsetTime.class || ZoneId.of( target ) instanceof ZoneOffset;
		}
		catch ( DateTimeException | NullPointerException ex ) {
			return true; // Report the invalid declaration instead of hiding it as ineligible.
		}
	}

	public static <A extends Annotation> A find(MemberDetails member, Class<A> annotation) {
		final A local = member.getDirectAnnotationUsage( annotation );
		if ( local != null ) {
			return local;
		}
		final ClassDetails declaring = member.getDeclaringType();
		for ( var type = declaring; type != null; type = type.getSuperClass() ) {
			final A candidate = type.getDirectAnnotationUsage( annotation );
			if ( candidate != null ) {
				return candidate;
			}
		}
		final var packageDetails = declaring.getPackage();
		if ( packageDetails != null ) {
			final A candidate = packageDetails.getDirectAnnotationUsage( annotation );
			if ( candidate != null ) {
				return candidate;
			}
		}
		final var module = declaring.getModule();
		return module == null ? null : module.getDirectAnnotationUsage( annotation );
	}
}
