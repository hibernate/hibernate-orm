package org.hibernate.orm.test.mapping.javatime.redesign.context;

import java.time.*;
import java.util.*;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.JdbcTypeRegistration;
import org.hibernate.annotations.Temporal;
import org.hibernate.dialect.*;
import org.hibernate.dialect.temporaltype.spi.StandardTemporalPreservation;
import org.hibernate.dialect.temporaltype.spi.TemporalPreservation;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.TemporalJdbcType;
import org.hibernate.type.descriptor.jdbc.TimestampUtcAsJdbcTimestampJdbcType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Durable provider history checks use actual period mappings and an independent
/// database clock, including exact closed endpoints and temporal collection membership.
///
/// @author Steve Ebersole
@RequiresDialect(H2Dialect.class)
@RequiresDialect(PostgreSQLDialect.class)
@RequiresDialect(OracleDialect.class)
class TemporalProviderPeriodTests {
	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void historyWithMetadataDisabled(boolean server) throws Exception {
		try ( var fixture = new TemporalPeriodFixture(
				false, "HISTORY_TABLE", server, TemporalPeriodFixture.Period.class ) ) {
			fixture.lifecycle( 1, TemporalPeriodFixture.SEASONAL );
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void historySurvivesSeparateJvm(boolean server, @org.junit.jupiter.api.io.TempDir(cleanup = org.junit.jupiter.api.io.CleanupMode.ON_SUCCESS) java.nio.file.Path directory) throws Exception {
		TemporalHandoffProcess.periods( directory, server );
	}

	@ParameterizedTest
	@org.junit.jupiter.params.provider.CsvSource({"graph,false", "graph,true", "legacy,false"})
	void historyValueChangeClosesPreviousRow(String queue, boolean server) throws Exception {
		try ( var fixture = new TemporalPeriodFixture(
				false, "HISTORY_TABLE", server, TemporalPeriodFixture.Period.class,
				true, true, Map.of( "hibernate.flush.queue.type", queue ) ) ) {
			Instant first = fixture.collectionBoundaryAfter( 1, TemporalPeriodFixture.SEASONAL[0], () -> fixture.insert( 1 ) );
			Instant second = fixture.collectionBoundaryAfter( 1, TemporalPeriodFixture.SEASONAL[1],
					() -> fixture.tx( session -> session.find( TemporalPeriodFixture.Period.class, 1 ).tags.set( 0, "changed" ) ) );
			assertThat( fixture.intervals( true, 1 ) ).containsExactly(
					new TemporalPeriodFixture.Interval( first, second, "one", 0 ),
					new TemporalPeriodFixture.Interval( second, null, "changed", 0 )
			);
			fixture.at( 1, first, "v1", List.of( "one" ) );
			fixture.at( 1, second, "v1", List.of( "changed" ) );
		}
	}

	@ParameterizedTest
	@org.junit.jupiter.params.provider.CsvSource({"graph,false", "graph,true", "legacy,false"})
	void historyElementRemovalClosesOnlyRemovedRow(String queue, boolean server) throws Exception {
		try ( var fixture = new TemporalPeriodFixture(
				false, "HISTORY_TABLE", server, TemporalPeriodFixture.Period.class,
				true, true, Map.of( "hibernate.flush.queue.type", queue ) ) ) {
			Instant first = fixture.collectionBoundaryAfter( 1, TemporalPeriodFixture.SEASONAL[0], () -> fixture.tx( session -> {
				var row = fixture.row( 1 );
				row.tags.add( "two" );
				session.persist( row );
			} ) );
			Instant second = fixture.collectionBoundaryAfter( 1, TemporalPeriodFixture.SEASONAL[1],
					() -> fixture.tx( session -> session.find( TemporalPeriodFixture.Period.class, 1 ).tags.remove( 1 ) ) );
			assertThat( fixture.intervals( true, 1 ) ).containsExactlyInAnyOrder(
					new TemporalPeriodFixture.Interval( first, null, "one", 0 ),
					new TemporalPeriodFixture.Interval( first, second, "two", 1 )
			);
			fixture.at( 1, first, "v1", List.of( "one", "two" ) );
			fixture.at( 1, second, "v1", List.of( "one" ) );
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void legacyQueueHistoryControl(boolean metadata) throws Exception {
		for ( boolean server : new boolean[] {false, true} ) {
			try ( var fixture = new TemporalPeriodFixture(
					metadata, "HISTORY_TABLE", server, TemporalPeriodFixture.Period.class,
					true, true, Map.of( "hibernate.flush.queue.type", "legacy" ) ) ) {
				fixture.lifecycle( 1, TemporalPeriodFixture.SEASONAL );
			}
		}
	}

	static List<String> strategies() {
		return ContextEnvironment.is( OracleDialect.class )
				?List.of("HISTORY_TABLE"):List.of("SINGLE_TABLE","HISTORY_TABLE");
	}
	@ParameterizedTest @ValueSource(booleans={true,false})
	void boundIntervalsAndCollections(boolean metadata) throws Exception {
		for(var strategy:strategies())try(var f=new TemporalPeriodFixture(metadata,strategy,false,TemporalPeriodFixture.Period.class)) {
			f.lifecycle(1,TemporalPeriodFixture.SEASONAL);f.lifecycle(2,TemporalPeriodFixture.FOLD);
		}
	}
	@ParameterizedTest @ValueSource(booleans={true,false})
	void serverIntervalsAndCollections(boolean metadata) throws Exception {
		for(var strategy:strategies())try(var f=new TemporalPeriodFixture(metadata,strategy,true,TemporalPeriodFixture.Period.class)) {
			f.lifecycle(1,TemporalPeriodFixture.SEASONAL);
			boolean inline=f.scope.factory.getChangesetCoordinator().useServerTimestamp(f.scope.factory.getJdbcServices().getDialect());
			assertThat(inline).isEqualTo(!f.oracle() && !ContextEnvironment.is(PostgresPlusDialect.class));
			assertThat(f.scope.sql).anyMatch(sql->sql.contains("current_timestamp")||sql.contains("current_timestamp()"));
		}
	}
	@ParameterizedTest @ValueSource(booleans={true,false})
	void explicitAndDefaultPrecision(boolean metadata) throws Exception {
		for(var type:List.of(TemporalPeriodFixture.Millis.class,TemporalPeriodFixture.DefaultPrecision.class)) {
			try(var f=new TemporalPeriodFixture(metadata,"HISTORY_TABLE",false,type)) {
				assertThat(f.precision).isEqualTo(type==TemporalPeriodFixture.Millis.class?3:f.scope.factory.getJdbcServices().getDialect().getTemporalTableSupport().getTemporalColumnPrecision());
				var aligned=Arrays.stream(TemporalPeriodFixture.SEASONAL).map(i->i.minusNanos(i.getNano()%f.tick)).toArray(Instant[]::new);
				f.lifecycle(1,aligned);
			}
		}
	}
	@ParameterizedTest @ValueSource(booleans={true,false})
	void supplierIsStableAcrossFlushes(boolean metadata) throws Exception {
		try(var f=new TemporalPeriodFixture(metadata,"HISTORY_TABLE",false,TemporalPeriodFixture.Period.class)) {
			f.tx(s->{
				s.persist(f.row(10));s.flush();
				assertThat(((SharedSessionContractImplementor)s).getCurrentChangesetIdentifier()).isEqualTo(TemporalPeriodFixture.SEASONAL[0]);
				f.supplier.value=TemporalPeriodFixture.SEASONAL[1];
				s.persist(f.row(11));s.flush();
				assertThat(((SharedSessionContractImplementor)s).getCurrentChangesetIdentifier()).isEqualTo(TemporalPeriodFixture.SEASONAL[0]);
			});
			assertThat(f.supplier.calls).isEqualTo(1);
			assertThat(f.intervals(false,10).get(0).start()).isEqualTo(TemporalPeriodFixture.SEASONAL[0]);
			assertThat(f.intervals(false,11).get(0).start()).isEqualTo(TemporalPeriodFixture.SEASONAL[0]);
			f.update(10);assertThat(f.supplier.calls).isEqualTo(2);
			assertThat(f.intervals(false,10).get(1).start()).isEqualTo(TemporalPeriodFixture.SEASONAL[1]);
		}
	}
	@ParameterizedTest @ValueSource(booleans={true,false})
	@RequiresDialect(H2Dialect.class)
	void explicitSpiDoesNotInventAProducer(boolean metadata) {
		var settings=new HashMap<String,Object>();
		settings.put("hibernate.type.temporal_preservation",true);settings.put("hibernate.temporal.table_strategy","HISTORY_TABLE");
		settings.put("hibernate.temporal.changeset_id_supplier",new TemporalPeriodFixture.Fixed());
		try(var scope=ContextScope.open(metadata,false,settings,List.of(),CustomPeriod.class)) {
			scope.tx(s->{var row=new CustomPeriod();row.id=1;s.persist(row);});
			scope.tx(s->assertThat(s.find(CustomPeriod.class,1)).isNotNull());
		}
		settings.remove("hibernate.temporal.changeset_id_supplier");settings.put("hibernate.temporal.use_server_transaction_timestamps",true);
		assertThatThrownBy(()->{try(var ignored=ContextScope.boot(metadata,false,settings,CustomPeriod.class)){}}).hasMessageContaining("producer");
	}
	@org.junit.jupiter.api.Test
	@RequiresDialect(H2Dialect.class)
	void unassessedDialectAndNativePeriodProfilesFailBeforeJdbc() {
		for ( var dialect : List.of( UnassessedDialect.class, OracleDialect.class ) ) {
			String strategy = dialect == OracleDialect.class ? "NATIVE" : "HISTORY_TABLE";
			assertThatThrownBy( () -> {
				try ( var ignored = ContextScope.boot( false, false, Map.of(
						"hibernate.dialect", dialect,
						"jakarta.persistence.database-product-version", "23.26.1",
						"hibernate.type.temporal_preservation", true,
						"hibernate.temporal.table_strategy", strategy ), TemporalPeriodFixture.Period.class ) ) {}
			} ).hasMessageContaining( "No preserving system-period profile" ).hasMessageContaining( strategy );
		}
	}
	public static class UnassessedDialect extends Dialect {
		public UnassessedDialect() { super( org.hibernate.dialect.DatabaseVersion.make( 1 ) ); }
	}
	public static class CustomUtc extends TimestampUtcAsJdbcTimestampJdbcType implements TemporalJdbcType {
		@Override public int getDdlTypeCode(){return SqlTypes.TIMESTAMP;}
		@Override public Optional<TemporalPreservation> getTemporalPreservation(JavaType<?> javaType,Dialect dialect){return Optional.of(StandardTemporalPreservation.instantOnly(javaType));}
	}
	@Entity(name="ProviderCustomPeriod") @Temporal(secondPrecision=6)
	@JdbcTypeRegistration(value=CustomUtc.class,registrationCode=SqlTypes.TIMESTAMP)
	static class CustomPeriod {@Id int id;}
}
