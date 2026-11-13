package org.hibernate.orm.test.mapping.javatime.redesign.context;

import java.nio.file.Path;
import java.time.*;
import java.util.*;
import org.hibernate.MappingException;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;

/// Named, automatic and unnamed modules and identically named loader realms use
/// their own declaration defaults, including package-over-module precedence.
///
/// @author Steve Ebersole
@org.hibernate.testing.orm.junit.RequiresDialect( org.hibernate.dialect.H2Dialect.class )
class TemporalModuleDeclarationTests {
	@TempDir
	Path temporary;

	@ParameterizedTest
	@ValueSource( booleans = { true, false } )
	void actualModuleAndPackagePrecedenceAreIsolated( boolean metadata ) throws Exception {
		for ( String target : List.of( "Z", "+05:30" ) ) {
			final var sources = new LinkedHashMap<String, String>();
			sources.put( "declaration.realm.Event",
					TemporalRealm.entitySource( "declaration.realm", "Event", "", "public OffsetDateTime stamp;" ) );
			sources.put( "declaration.packaged.EventWithPackage", TemporalRealm.entitySource( "declaration.packaged",
					"EventWithPackage", "", "public OffsetDateTime stamp;" ) );
			sources.put( "declaration.packaged.package-info", TemporalRealm.packageSource( "declaration.packaged",
					"@org.hibernate.annotations.TimeZoneAdjustment(\"America/New_York\") @org.hibernate.annotations.TimeZoneRepresentation(org.hibernate.annotations.TimeZoneRepresentation.Type.COLUMN)" ) );
			try ( var realm = TemporalRealm.create(
					temporary.resolve( "named" + target.replace( ':', '_' ).replace( '+', 'p' ) ),
					TemporalRealm.Kind.NAMED,
					"@org.hibernate.annotations.TimeZoneAdjustment(\"" + target
							+ "\") @org.hibernate.annotations.TimeZoneRepresentation(org.hibernate.annotations.TimeZoneRepresentation.Type.INLINE)",
					sources ) ) {
				final var entity = realm.entity( "declaration.realm.Event" );
				final var packaged = realm.entity( "declaration.packaged.EventWithPackage" );
				assertThat( entity.getModule() ).isSameAs( realm.module );
				assertThat( realm.module.isNamed() ).isTrue();
				for ( Map<String, Object> settings : List.<Map<String, Object>>of( Map.of(),
						Map.of( "hibernate.timezone.default_storage", "AUTO" ) ) )
					try ( var scope = ContextScope.boot( metadata, true, settings, entity, packaged ) ) {
						TemporalDeclarationAssertions.mapping( scope, entity, "stamp", false, ZoneId.of( target ) );
						TemporalDeclarationAssertions.mapping( scope, packaged, "stamp", true,
								ZoneId.of( "America/New_York" ) );
					}
				assertThatThrownBy( () -> {
					try ( var ignored = ContextScope.boot( metadata, false, entity ) ) {
					}
				} ).isInstanceOf( MappingException.class ).hasMessageContaining( "java_time_use_direct_jdbc" );
			}
		}
	}

	@ParameterizedTest
	@ValueSource( booleans = { true, false } )
	void automaticAndUnnamedModulesHaveNoModuleDefault( boolean metadata ) throws Exception {
		for ( var kind : List.of( TemporalRealm.Kind.AUTOMATIC, TemporalRealm.Kind.UNNAMED ) ) {
			try ( var realm = TemporalRealm.create( temporary.resolve( kind.name() ), kind, "",
					Map.of( "declaration.realm.Event", TemporalRealm.entitySource( "declaration.realm", "Event", "",
							"public OffsetDateTime stamp;" ) ) ) ) {
				final var entity = realm.entity( "declaration.realm.Event" );
				assertThat( entity.getModule().isNamed() ).isEqualTo( kind == TemporalRealm.Kind.AUTOMATIC );
				try ( var scope = ContextScope.boot( metadata, true, entity ) ) {
					TemporalDeclarationAssertions.mapping( scope, entity, "stamp", false, null );
				}
			}
		}
	}

	@ParameterizedTest
	@ValueSource( booleans = { true, false } )
	void moduleRepresentationCanReplaceUntranslatableGlobal( boolean metadata ) throws Exception {
		try ( var realm = TemporalRealm.create( temporary.resolve( "covered" ), TemporalRealm.Kind.NAMED,
				"@org.hibernate.annotations.TimeZoneRepresentation(org.hibernate.annotations.TimeZoneRepresentation.Type.COLUMN)",
				Map.of( "declaration.realm.Event", TemporalRealm.entitySource( "declaration.realm", "Event", "",
						"public OffsetDateTime stamp;" ) ) ) ) {
			final var entity = realm.entity( "declaration.realm.Event" );
			try ( var scope = ContextScope.boot( metadata, true, Map.of( "hibernate.timezone.default_storage", "AUTO" ),
					entity ) ) {
				TemporalDeclarationAssertions.mapping( scope, entity, "stamp", true, null );
			}
		}
	}

	@ParameterizedTest
	@ValueSource( booleans = { true, false } )
	void packageAloneCanReplaceUntranslatableGlobal( boolean metadata ) throws Exception {
		final var sources = Map.of( "covered.only.Event",
				TemporalRealm.entitySource( "covered.only", "Event", "", "public OffsetDateTime stamp;" ),
				"covered.only.package-info", TemporalRealm.packageSource( "covered.only",
						"@org.hibernate.annotations.TimeZoneRepresentation(org.hibernate.annotations.TimeZoneRepresentation.Type.COLUMN)" ) );
		try ( var realm = TemporalRealm.create( temporary.resolve( "package-only" ), TemporalRealm.Kind.UNNAMED, "",
				sources ) ) {
			final var entity = realm.entity( "covered.only.Event" );
			try ( var scope = ContextScope.boot( metadata, true, Map.of( "hibernate.timezone.default_storage", "AUTO" ),
					entity ) ) {
				TemporalDeclarationAssertions.mapping( scope, entity, "stamp", true, null );
			}
			assertThatThrownBy( () -> {
				try ( var ignored = ContextScope.boot( metadata, false, entity ) ) {
				}
			} ).isInstanceOf( MappingException.class ).hasMessageContaining( "java_time_use_direct_jdbc" );
		}
	}

}
