package org.hibernate.orm.test.mapping.javatime.redesign.context;

import java.util.*;
import org.hibernate.MappingException;
import org.hibernate.testing.orm.logger.LoggerInspectionExtension;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;
import static org.hibernate.internal.log.DeprecationLogger.DEPRECATION_LOGGER;

/// Count the explicit-setting event per bootstrap, independently of unrelated
/// deprecations, provisioning registries and mapping success or rejection.
///
/// @author Steve Ebersole
@org.hibernate.testing.orm.junit.RequiresDialect( org.hibernate.dialect.H2Dialect.class )
class TemporalDeclarationLogTests {
	@RegisterExtension
	LoggerInspectionExtension logger = LoggerInspectionExtension.builder().setLogger( DEPRECATION_LOGGER ).build();

	@ParameterizedTest
	@ValueSource( booleans = { true, false } )
	void explicitGlobalIsReportedOnceEvenWhenOverriddenUnusedOrRejected( boolean metadata ) {
		final var events = new ArrayList<String>();
		logger.registerListener( ( level, message, error ) -> {
			if ( message.contains( "hibernate.timezone.default_storage" ) )
				events.add( message );
		} );
		for ( Class<?> type : List.of( TemporalLegacyDeclarationTests.CoveredType.class,
				TemporalLegacyDeclarationTests.Unused.class, TemporalLegacyDeclarationTests.Plain.class ) ) {
			for ( int boot = 0; boot < 2; boot++ ) {
				events.clear();
				if ( type == TemporalLegacyDeclarationTests.Plain.class )
					assertThatThrownBy( () -> {
						try ( var ignored = ContextScope.boot( metadata, true,
								Map.of( "hibernate.timezone.default_storage", "AUTO" ), type ) ) {
						}
					} ).isInstanceOf( MappingException.class );
				else
					try ( var ignored = ContextScope.boot( metadata, true,
							Map.of( "hibernate.timezone.default_storage", "AUTO" ), type ) ) {
					}
				assertThat( events ).as( type.getName() ).hasSize( 1 );
				assertThat( events.get( 0 ) ).contains( "AUTO", "TimeZoneRepresentation", "TimeZoneAdjustment" );
			}
		}
		events.clear();
		try ( var ignored = ContextScope.boot( metadata, true, TemporalLegacyDeclarationTests.Plain.class ) ) {
		}
		assertThat( events ).isEmpty();
		events.clear();
		try ( var ignored = ContextScope.boot( metadata, false,
				Map.of( "hibernate.timezone.default_storage", "NORMALIZE_UTC" ),
				TemporalLegacyDeclarationTests.Plain.class ) ) {
		}
		assertThat( events ).isEmpty();
	}
}
