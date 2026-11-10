package org.hibernate.orm.test.mapping.javatime.redesign.context;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.testing.orm.junit.RequiresDialect;

import java.time.*;
import java.nio.file.Path;
import java.util.*;
import jakarta.persistence.*;
import org.hibernate.MappingException;
import org.hibernate.annotations.TimeZoneAdjustment;
import org.hibernate.annotations.TimeZoneRepresentation;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;

/// Scope/access/package selection is independent for representation and target;
/// unused accessors, parent packages and interface defaults never contribute.
///
/// @author Steve Ebersole
@org.hibernate.testing.orm.junit.RequiresDialect( org.hibernate.dialect.H2Dialect.class )
@RequiresDialect(H2Dialect.class)
@RequiresDialect(PostgreSQLDialect.class)
@RequiresDialect(OracleDialect.class)
class TemporalScopeDeclarationTests {
	@TempDir
	Path temporary;

	@ParameterizedTest
	@ValueSource( booleans = { true, false } )
	void accessEligibilityAndIdentity( boolean metadata ) throws Exception {
		for ( boolean direct : List.of( true, false ) )
			try ( var scope = ContextScope.boot( metadata, direct, IneligibleInvalid.class ) ) {
				for ( String member : List.of( "date", "instant" ) )
					TemporalDeclarationAssertions.mapping( scope, IneligibleInvalid.class, member, false, null );
			}
		assertThat( TimeZoneAdjustment.class.getMethod( "value" ).getDefaultValue() ).isNull();
		assertThat( TimeZoneRepresentation.class.getMethod( "value" ).getDefaultValue() ).isNull();
		try ( var scope = ContextScope.boot( metadata, true, FieldAccess.class, PropertyAccess.class,
				InterfaceControl.class, ShadowedInvalid.class ) ) {
			TemporalDeclarationAssertions.mapping( scope, FieldAccess.class, "stamp", true, null );
			TemporalDeclarationAssertions.mapping( scope, PropertyAccess.class, "stamp", false, ZoneId.of( "+05:30" ) );
			TemporalDeclarationAssertions.mapping( scope, InterfaceControl.class, "stamp", false, null );
			TemporalDeclarationAssertions.mapping( scope, ShadowedInvalid.class, "stamp", false, ZoneOffset.UTC );
		}
		for ( Class<?> type : List.of( Empty.class, Invalid.class, RegionTime.class, FixedRegionTime.class ) )
			assertThatThrownBy( () -> {
				try ( var ignored = ContextScope.boot( metadata, true, type ) ) {
				}
			} ).isInstanceOf( MappingException.class ).hasMessageContaining( "stamp" );
		try ( var scope = ContextScope.open( metadata, ZoneIdentity.class ) ) {
			final var row = new ZoneIdentity();
			row.id = 1;
			row.region = ZonedDateTime.parse( "2026-07-01T12:00+02:00[Europe/Paris]" );
			row.offset = row.region;
			scope.tx( session -> session.persist( row ) );
			scope.tx( session -> {
				var actual = session.find( ZoneIdentity.class, 1 );
				assertThat( actual.region.getZone() ).isEqualTo( ZoneId.of( "UTC" ) );
				assertThat( actual.offset.getZone() ).isEqualTo( ZoneOffset.UTC );
				assertThat( actual.region.toInstant() ).isEqualTo( row.region.toInstant() );
			} );
		}
	}

	@ParameterizedTest
	@ValueSource( booleans = { true, false } )
	void declarationPackagesAndSuperclassTypesRemainSeparate( boolean metadata ) throws Exception {
		final var sources = new LinkedHashMap<String, String>();
		sources.put( "decl.base.package-info", TemporalRealm.packageSource( "decl.base",
				"@org.hibernate.annotations.TimeZoneAdjustment(\"+04:00\")" ) );
		sources.put( "decl.child.package-info", TemporalRealm.packageSource( "decl.child",
				"@org.hibernate.annotations.TimeZoneAdjustment(\"America/New_York\") @org.hibernate.annotations.TimeZoneRepresentation(org.hibernate.annotations.TimeZoneRepresentation.Type.COLUMN)" ) );
		sources.put( "decl.parent.package-info", TemporalRealm.packageSource( "decl.parent",
				"@org.hibernate.annotations.TimeZoneAdjustment(\"+09:00\")" ) );
		sources.put( "decl.base.Base",
				"package decl.base; @jakarta.persistence.MappedSuperclass @org.hibernate.annotations.TimeZoneAdjustment(\"Z\") @org.hibernate.annotations.TimeZoneRepresentation(org.hibernate.annotations.TimeZoneRepresentation.Type.INLINE) public class Base {public java.time.OffsetDateTime inheritedStamp;}" );
		sources.put( "decl.base.PackageBase",
				"package decl.base; @jakarta.persistence.MappedSuperclass public class PackageBase {public java.time.OffsetDateTime inheritedStamp;}" );
		sources.put( "decl.child.Child",
				TemporalRealm.entitySource( "decl.child", "Child",
						"@TimeZoneAdjustment(\"+05:30\") @TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN)",
						"public OffsetDateTime stamp;" )
						.replace( "class Child {", "class Child extends decl.base.Base {" ) );
		sources.put( "decl.child.TypeWins",
				TemporalRealm.entitySource( "decl.child", "TypeWins", "", "public OffsetDateTime stamp;" )
						.replace( "class TypeWins {", "class TypeWins extends decl.base.Base {" ) );
		sources.put( "decl.parent.child.NoParent",
				TemporalRealm.entitySource( "decl.parent.child", "NoParent", "", "public OffsetDateTime stamp;" )
						.replace( "class NoParent {", "class NoParent extends decl.base.PackageBase {" ) );
		try ( var realm = TemporalRealm.create( temporary.resolve( "packages" ), TemporalRealm.Kind.UNNAMED, "",
				sources ) ) {
			final var child = realm.entity( "decl.child.Child" );
			final var typeWins = realm.entity( "decl.child.TypeWins" );
			final var noParent = realm.entity( "decl.parent.child.NoParent" );
			try ( var scope = ContextScope.boot( metadata, true, child, typeWins, noParent ) ) {
				TemporalDeclarationAssertions.mapping( scope, child, "inheritedStamp", false, ZoneOffset.UTC );
				TemporalDeclarationAssertions.mapping( scope, child, "stamp", true, ZoneId.of( "+05:30" ) );
				TemporalDeclarationAssertions.mapping( scope, typeWins, "stamp", false, ZoneOffset.UTC );
				TemporalDeclarationAssertions.mapping( scope, noParent, "stamp", false, null );
				TemporalDeclarationAssertions.mapping( scope, noParent, "inheritedStamp", false,
						ZoneId.of( "+04:00" ) );
			}
			assertThatThrownBy( () -> {
				try ( var ignored = ContextScope.boot( metadata, false, noParent ) ) {
				}
			} ).isInstanceOf( MappingException.class ).hasMessageContaining( "java_time_use_direct_jdbc" );
		}
	}

	@Entity( name = "DeclarationFieldAccess" )
	@Access( AccessType.FIELD )
	@TimeZoneAdjustment( "Z" )
	@TimeZoneRepresentation( TimeZoneRepresentation.Type.INLINE )
	static class FieldAccess {
		@Id
		int id;
		@TimeZoneAdjustment( TimeZoneAdjustment.NONE )
		@TimeZoneRepresentation( TimeZoneRepresentation.Type.COLUMN )
		OffsetDateTime stamp;

		@TimeZoneAdjustment( "+09:00" )
		@TimeZoneRepresentation( TimeZoneRepresentation.Type.INLINE )
		public OffsetDateTime getStamp() {
			return stamp;
		}
	}

	@Entity( name = "DeclarationPropertyAccess" )
	@Access( AccessType.PROPERTY )
	@TimeZoneAdjustment( TimeZoneAdjustment.NONE )
	@TimeZoneRepresentation( TimeZoneRepresentation.Type.COLUMN )
	static class PropertyAccess {
		int id;
		@TimeZoneAdjustment( "Z" )
		@TimeZoneRepresentation( TimeZoneRepresentation.Type.COLUMN )
		OffsetDateTime stamp;

		@Id
		public int getId() {
			return id;
		}

		public void setId( int id ) {
			this.id = id;
		}

		@TimeZoneAdjustment( "+05:30" )
		@TimeZoneRepresentation( TimeZoneRepresentation.Type.INLINE )
		public OffsetDateTime getStamp() {
			return stamp;
		}

		public void setStamp( OffsetDateTime value ) {
			stamp = value;
		}
	}

	@TimeZoneAdjustment( "+09:00" )
	@TimeZoneRepresentation( TimeZoneRepresentation.Type.COLUMN )
	interface Defaults {
	}

	@Entity( name = "DeclarationInterfaceControl" )
	static class InterfaceControl implements Defaults {
		@Id
		int id;
		OffsetDateTime stamp;
	}

	@Entity( name = "DeclarationShadowedInvalid" )
	@TimeZoneAdjustment( "not/a/zone" )
	static class ShadowedInvalid {
		@Id
		int id;
		@TimeZoneAdjustment( "Z" )
		OffsetDateTime stamp;
	}

	@Entity( name = "DeclarationIneligibleInvalid" )
	@TimeZoneAdjustment( "not/a/zone" )
	static class IneligibleInvalid {
		@Id
		int id;
		LocalDate date;
		Instant instant;
	}

	@Entity( name = "DeclarationEmpty" )
	static class Empty {
		@Id
		int id;
		@TimeZoneAdjustment( "" )
		OffsetDateTime stamp;
	}

	@Entity( name = "DeclarationInvalid" )
	static class Invalid {
		@Id
		int id;
		@TimeZoneAdjustment( "not/a/zone" )
		OffsetDateTime stamp;
	}

	@Entity( name = "DeclarationRegionTime" )
	static class RegionTime {
		@Id
		int id;
		@TimeZoneAdjustment( "UTC" )
		OffsetTime stamp;
	}

	@Entity( name = "DeclarationFixedRegionTime" )
	static class FixedRegionTime {
		@Id
		int id;
		@TimeZoneAdjustment( "Etc/GMT-5" )
		OffsetTime stamp;
	}

	@Entity( name = "DeclarationZoneIdentity" )
	static class ZoneIdentity {
		@Id
		int id;
		@TimeZoneAdjustment( "UTC" )
		ZonedDateTime region;
		@Column( name = "offset_stamp" )
		@TimeZoneAdjustment( "Z" )
		ZonedDateTime offset;
	}
}
