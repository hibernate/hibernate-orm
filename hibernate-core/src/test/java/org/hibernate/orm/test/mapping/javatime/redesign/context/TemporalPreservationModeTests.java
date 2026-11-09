package org.hibernate.orm.test.mapping.javatime.redesign.context;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

import org.hibernate.annotations.CurrentTimestamp;
import org.hibernate.annotations.SourceType;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.mapping.BasicValue;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.internal.TemporalPreservationSettings;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// The semantic setting wins over its deprecated alias for mapping and runtime use.
/// Tests exercise actual version predicates, generated values and independent factories.
///
/// @author Steve Ebersole
@RequiresDialect(H2Dialect.class)
@RequiresDialect(org.hibernate.dialect.PostgreSQLDialect.class)
@RequiresDialect(org.hibernate.dialect.OracleDialect.class)
class TemporalPreservationModeTests {
	@Test
	void configurationPrecedenceDoesNotMutateInputs() {
		for ( var settings : List.<Map<String,Object>>of(
				Map.of(), Map.of( MappingSettings.JAVA_TIME_USE_DIRECT_JDBC, true ),
				Map.of( MappingSettings.TEMPORAL_PRESERVATION, true, MappingSettings.JAVA_TIME_USE_DIRECT_JDBC, false ) ) ) {
			final var resolved = TemporalPreservationSettings.resolve( settings );
			assertThat( resolved.enabled() ).isTrue();
			assertThat( resolved.baseInstantJdbcType() ).isEqualTo( SqlTypes.INSTANT );
		}
		for ( var settings : List.<Map<String,Object>>of(
				Map.of( MappingSettings.JAVA_TIME_USE_DIRECT_JDBC, false ),
				Map.of( MappingSettings.TEMPORAL_PRESERVATION, false, MappingSettings.JAVA_TIME_USE_DIRECT_JDBC, true ) ) ) {
			assertThat( TemporalPreservationSettings.resolve( settings ).enabled() ).isFalse();
			assertThat( TemporalPreservationSettings.resolve( settings ).baseInstantJdbcType() ).isEqualTo( SqlTypes.TIMESTAMP_UTC );
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void versionLifecycleAndMappingViews(boolean metadata) {
		final var dialect = org.hibernate.testing.orm.junit.DialectContext.getDialect();
		final var types = new java.util.ArrayList<Class<?>>( List.of( InstantVersion.class, LocalVersion.class,
				OffsetVersion.class, DbInstantVersion.class, DbLocalVersion.class, DbOffsetVersion.class ) );
		if ( dialect instanceof org.hibernate.dialect.OracleDialect ) {
			types.addAll( List.of( ZonedVersion.class, DbZonedVersion.class ) );
		}
		for ( boolean preservation : List.of( true, false ) ) {
			for ( Class<?> type : types ) {
				if ( preservation && dialect instanceof org.hibernate.dialect.PostgreSQLDialect
						&& (type == OffsetVersion.class || type == DbOffsetVersion.class) ) {
					continue;
				}
				try ( var scope = ContextScope.open( metadata, !preservation,
						Map.of( MappingSettings.TEMPORAL_PRESERVATION, preservation ), List.of(), type ) ) {
					assertThat( scope.factory.getSessionFactoryOptions().isTemporalPreservationEnabled() ).isEqualTo( preservation );
					final var value = (BasicValue) scope.metadata.getEntityBinding( type.getName() ).getVersion().getValue();
					final var resolution = value.resolve();
					if ( preservation ) {
						assertThat( resolution.getLegacyResolvedBasicType().getJdbcType() ).isSameAs( resolution.getJdbcType() );
						assertThat( resolution.getJdbcMapping().getJdbcType() ).isSameAs( resolution.getJdbcType() );
					}
					final var row = instantiate( type );
					scope.tx( s -> s.persist( row ) );
					final var original = token( row );
					scope.tx( s -> {
						final var loaded = s.find( type, 1 );
						if ( !preservation && original instanceof OffsetDateTime offset ) {
							assertThat( ((OffsetDateTime) token( loaded )).toInstant() ).isEqualTo( offset.toInstant() );
						}
						else if ( !preservation && original instanceof ZonedDateTime zoned ) {
							assertThat( ((ZonedDateTime) token( loaded )).toInstant() ).isEqualTo( zoned.toInstant() );
						}
						else {
							assertThat( token( loaded ) ).isEqualTo( original );
						}
						s.createSelectionQuery( "from " + type.getSimpleName() + " where stamp=:v", type )
								.setParameter( "v", original ).getSingleResult();
					} );
					// A real precision tick avoids treating equal clock reads as a lock failure.
					try { Thread.sleep( 15 ); } catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new AssertionError( ex ); }
					scope.tx( s -> setPayload( s.find( type, 1 ), "changed" ) );
					assertThatThrownBy( () -> scope.tx( s -> s.merge( row ) ) )
							.isInstanceOf( jakarta.persistence.OptimisticLockException.class );
					scope.tx( s -> s.remove( s.find( type, 1 ) ) );
				}
			}
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void zoneIdentityCannotBeSilentlyReducedToOffset(boolean metadata) {
		org.junit.jupiter.api.Assumptions.assumeTrue( org.hibernate.testing.orm.junit.DialectContext.getDialect() instanceof H2Dialect );
		assertThatThrownBy( () -> {
			try ( var ignored = ContextScope.boot( metadata, false,
					Map.of( MappingSettings.TEMPORAL_PRESERVATION, true ), ZonedVersion.class ) ) { }
		} ).hasMessageContaining( "ZONE_ID" );
		try ( var scope = ContextScope.open( metadata, true,
				Map.of( MappingSettings.TEMPORAL_PRESERVATION, false ), List.of(), ZonedVersion.class ) ) {
			scope.tx( s -> s.persist( new ZonedVersion() ) );
			scope.tx( s -> assertThat( s.find( ZonedVersion.class, 1 ).stamp ).isNotNull() );
		}
	}

	private static Object instantiate(Class<?> type) {
		try { return type.getDeclaredConstructor().newInstance(); }
		catch (ReflectiveOperationException e) { throw new AssertionError( e ); }
	}
	private static Object token(Object row) {
		try { return row.getClass().getDeclaredField( "stamp" ).get( row ); }
		catch (ReflectiveOperationException e) { throw new AssertionError( e ); }
	}
	private static void setPayload(Object row, String payload) {
		try { row.getClass().getDeclaredField( "payload" ).set( row, payload ); }
		catch (ReflectiveOperationException e) { throw new AssertionError( e ); }
	}

	@Entity(name="InstantVersion") static class InstantVersion { @Id int id=1; @Version Instant stamp; String payload="initial"; }
	@Entity(name="LocalVersion") static class LocalVersion { @Id int id=1; @Version LocalDateTime stamp; String payload="initial"; }
	@Entity(name="OffsetVersion") static class OffsetVersion { @Id int id=1; @Version OffsetDateTime stamp; String payload="initial"; }
	@Entity(name="ZonedVersion") static class ZonedVersion { @Id int id=1; @Version ZonedDateTime stamp; String payload="initial"; }
	@Entity(name="DbInstantVersion") static class DbInstantVersion { @Id int id=1; @Version @CurrentTimestamp(source=SourceType.DB) Instant stamp; String payload="initial"; }
	@Entity(name="DbLocalVersion") static class DbLocalVersion { @Id int id=1; @Version @CurrentTimestamp(source=SourceType.DB) LocalDateTime stamp; String payload="initial"; }
	@Entity(name="DbZonedVersion") static class DbZonedVersion { @Id int id=1; @Version @CurrentTimestamp(source=SourceType.DB) ZonedDateTime stamp; String payload="initial"; }
	@Entity(name="DbOffsetVersion") static class DbOffsetVersion { @Id int id=1; @Version @CurrentTimestamp(source=SourceType.DB) OffsetDateTime stamp; String payload="initial"; }
}
