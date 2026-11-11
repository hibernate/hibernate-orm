package org.hibernate.orm.test.mapping.javatime.redesign.context;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.orm.test.mapping.javatime.redesign.TemporalLiveProfile;
import org.hibernate.dialect.SpannerPostgreSQLDialect;
import org.opentest4j.TestAbortedException;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.assertj.core.api.Assertions.assertThat;
import org.hibernate.boot.MetadataSources;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/// Declaration errors remain observable even when the current live profile is unsupported.
/// All factories resolve a pinned H2 dialect offline and forbid connection acquisition.
///
/// @author Steve Ebersole
class TemporalOfflineDeclarationTests {

	@Test
	void unsupportedLiveProfileAbortsOutsideMappingAssertion() {
		final var reachedAssertion = new AtomicBoolean();
		assertThatThrownBy( () -> {
			TemporalLiveProfile.requireSupported( SpannerPostgreSQLDialect.class.getName() );
			reachedAssertion.set( true );
			assertThatThrownBy( () -> { throw new MappingException( "declaration" ); } )
					.isInstanceOf( MappingException.class );
		} ).isInstanceOf( TestAbortedException.class );
		assertThat( reachedAssertion ).isFalse();
		assertThat( TemporalLiveProfile.requireSupported( H2Dialect.class.getName() ) ).isEqualTo( TemporalLiveProfile.H2 );
	}

	@Test
	void rejectsWholeValueConvertersWithoutAdjustment() throws SQLException {
		for ( Class<?> model : List.of( TemporalColumnConverterTests.OffsetDateTimeScalarAbsent.class, TemporalColumnConverterTests.OffsetDateTimeScalarNone.class, TemporalColumnConverterTests.OffsetDateTimeSetAbsent.class, TemporalColumnConverterTests.OffsetDateTimeSetNone.class, TemporalColumnConverterTests.OffsetDateTimeListAbsent.class, TemporalColumnConverterTests.OffsetDateTimeListNone.class, TemporalColumnConverterTests.OffsetDateTimeBagAbsent.class, TemporalColumnConverterTests.OffsetDateTimeBagNone.class, TemporalColumnConverterTests.OffsetTimeScalarAbsent.class, TemporalColumnConverterTests.OffsetTimeScalarNone.class, TemporalColumnConverterTests.OffsetTimeSetAbsent.class, TemporalColumnConverterTests.OffsetTimeSetNone.class, TemporalColumnConverterTests.ZonedDateTimeScalarAbsent.class, TemporalColumnConverterTests.ZonedDateTimeScalarNone.class, TemporalColumnConverterTests.ZonedDateTimeListAbsent.class, TemporalColumnConverterTests.ZonedDateTimeListNone.class ) ) {
			reject( List.of( "Whole-value user converter", "COLUMN" ), Map.of(), model, TemporalColumnConverterTests.DateTimeText.class, TemporalColumnConverterTests.TimeText.class, TemporalColumnConverterTests.ZonedText.class );
		}
	}

	@Test
	void activeConvertersRejectBeforeExecution() throws SQLException {
		for ( Class<?> model : List.of( TemporalConverterContextTests.ScalarExplicit.class,TemporalConverterContextTests.ScalarAuto.class,TemporalConverterContextTests.SetExplicit.class,TemporalConverterContextTests.SetAuto.class,TemporalConverterContextTests.ListExplicit.class,TemporalConverterContextTests.ListAuto.class,TemporalConverterContextTests.BagExplicit.class,TemporalConverterContextTests.BagAuto.class,TemporalConverterContextTests.ScalarColumnExplicit.class,TemporalConverterContextTests.ScalarColumnAuto.class,TemporalConverterContextTests.SetColumnExplicit.class,TemporalConverterContextTests.SetColumnAuto.class,TemporalConverterContextTests.ListColumnExplicit.class,TemporalConverterContextTests.ListColumnAuto.class,TemporalConverterContextTests.BagColumnExplicit.class,TemporalConverterContextTests.BagColumnAuto.class ) ) {
			reject( List.of( "converter", "adjustment" ), Map.of(), model, TemporalConverterContextTests.Identity.class );
		}
	}

	@Test
	void explicitExcludedDeclarationsFail() throws SQLException {
		for ( Class<?> model : List.of( TemporalRoleExclusionTests.IdNone.class,TemporalRoleExclusionTests.IdColumn.class,TemporalRoleExclusionTests.VersionNone.class,TemporalRoleExclusionTests.VersionColumn.class,TemporalRoleExclusionTests.EmbeddedId.class,TemporalRoleExclusionTests.MapNone.class,TemporalRoleExclusionTests.MapColumn.class,TemporalRoleExclusionTests.MapRep.class,TemporalRoleExclusionTests.ArrayNone.class,TemporalRoleExclusionTests.ArrayColumn.class,TemporalRoleExclusionTests.ArrayRep.class,TemporalRoleExclusionTests.BasicList.class ) ) {
			reject( List.of( "excluded role" ), Map.of(), model );
		}
	}

	@Test
	void embeddingDeclarationsFailAtTheirOwnRole() throws SQLException {
		for ( Class<?> model : List.of( TemporalRoleExclusionTests.EmbeddingTarget.class,TemporalRoleExclusionTests.EmbeddingInline.class,TemporalRoleExclusionTests.EmbeddingColumn.class,TemporalRoleExclusionTests.EmbeddingAux.class ) ) {
			reject( List.of( "member" ), Map.of(), model );
		}
	}

	@Test
	void nativeConstraintRejectsOffline() throws SQLException {
		reject( List.of( "native", "stamp" ), Map.of( "hibernate.timezone.default_storage", "NATIVE" ),
				TemporalNativeConstraintTests.TargetPlain.class );
		reject( List.of( "native", "stamp" ), Map.of(), TemporalLegacyDeclarationTests.NativePlain.class );
	}

	@Test
	void excludedEmbeddingDeclarationRejectsInLegacyMode() throws SQLException {
		reject( List.of( "TimeZoneAdjustment" ), Map.of( MappingSettings.TEMPORAL_PRESERVATION, false ),
				TemporalRoleExclusionTests.EmbeddingNone.class );
	}

	@Test
	void identityColumnConverterRejectsOffline() throws SQLException {
		reject( List.of( "Whole-value user converter", "COLUMN" ), Map.of(),
				TemporalColumnConverterTests.OffsetDateTimeScalarNone.class, TemporalConverterContextTests.Identity.class );
		reject( List.of( "Whole-value user converter", "COLUMN" ), Map.of(),
				TemporalConverterContextTests.WholeColumnConverter.class );
	}

	private static void reject(List<String> diagnostic, Map<String, Object> settings, Class<?>... classes) throws SQLException {
		final var guard = mock( ConnectionProvider.class );
		when( guard.getConnection() ).thenThrow( new AssertionError( "Offline declaration attempted JDBC" ) );
		try ( var registry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( JdbcSettings.DIALECT, new H2Dialect() )
				.applySetting( JdbcSettings.ALLOW_METADATA_ON_BOOT, false )
				.applySetting( MappingSettings.TEMPORAL_PRESERVATION, true )
				.applySetting( "hibernate.hbm2ddl.auto", "none" )
				.applySettings( settings )
				.addService( ConnectionProvider.class, guard ).build() ) {
			final var assertion = assertThatThrownBy( () -> {
				final var sources = new MetadataSources( registry );
				for ( var type : classes ) sources.addAnnotatedClass( type );
				try ( var ignored = sources.buildMetadata().buildSessionFactory() ) { }
			} ).isInstanceOf( MappingException.class );
			for ( var fragment : diagnostic ) assertion.hasMessageContaining( fragment );
		}
		finally {
			verify( guard, never() ).getConnection();
		}
	}
}
