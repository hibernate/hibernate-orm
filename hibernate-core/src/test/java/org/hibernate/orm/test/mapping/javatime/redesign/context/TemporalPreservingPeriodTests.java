package org.hibernate.orm.test.mapping.javatime.redesign.context;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OrderColumn;

import org.hibernate.SharedSessionContract;
import org.hibernate.annotations.Temporal;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.temporal.spi.ChangesetIdentifierSupplier;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/// Preserving period values keep distinct DST-fold instants and exact history boundaries.
///
/// @author Steve Ebersole
@RequiresDialect(H2Dialect.class)
class TemporalPreservingPeriodTests {
	static final Instant FIRST=Instant.parse("2024-11-03T07:30:00Z");
	static final Instant SECOND=Instant.parse("2024-11-03T08:30:00Z");

	@ParameterizedTest
	@ValueSource(booleans={true,false})
	void exactBoundariesAndCollections(boolean metadata) {
		for ( String strategy : List.of( "SINGLE_TABLE", "HISTORY_TABLE" ) ) {
			final var supplier=new Fixed();
			try ( var scope=ContextScope.open(metadata, false, Map.of(
					MappingSettings.TEMPORAL_PRESERVATION,true,
					"hibernate.temporal.table_strategy",strategy,
					"hibernate.temporal.changeset_id_supplier",supplier,
					"hibernate.jdbc.time_zone","Asia/Tokyo"),List.of(),Period.class) ) {
				scope.tx(s->{var row=new Period();row.tags.add("first");s.persist(row);});
				supplier.value=SECOND;
				scope.tx(s->{var row=s.find(Period.class,1);row.payload="second";row.tags.add("second");});
				for ( var point : List.of(FIRST.minusNanos(1000),FIRST,SECOND.minusNanos(1000),SECOND) ) {
					try(var session=scope.factory.withOptions().asOf(point).open()) {
						session.inTransaction(tx->{
							var row=session.find(Period.class,1);
							if(point.isBefore(FIRST)){assertThat(row).isNull();}
							else {assertThat(row.payload).isEqualTo(point.isBefore(SECOND)?"first":"second");
								assertThat(row.tags).hasSize(point.isBefore(SECOND)?1:2);}
						});
					}
				}
				final var table=scope.metadata.getEntityBinding(Period.class.getName()).getAuxiliaryTable().getName();
				scope.tx(s->s.doWork(c->{try(var st=c.createStatement();var rs=st.executeQuery("select valid_from from "+table+" order by valid_from")){
					for(var instant:List.of(FIRST,SECOND)){assertThat(rs.next()).isTrue();assertThat(rs.getObject(1,LocalDateTime.class)).isEqualTo(LocalDateTime.ofInstant(instant,ZoneOffset.UTC));}
				}}));
			}
		}
	}

	@ParameterizedTest
	@ValueSource(booleans={true,false})
	void serverProducerUsesUtcEncoding(boolean metadata) {
		try(var scope=ContextScope.open(metadata,false,Map.of(
				MappingSettings.TEMPORAL_PRESERVATION,true,
				"hibernate.temporal.use_server_transaction_timestamps",true,
				"hibernate.temporal.table_strategy","HISTORY_TABLE",
				"hibernate.jdbc.time_zone","Asia/Tokyo"),List.of(),Period.class)) {
			final var before=Instant.now().minusSeconds(1);
			scope.tx(s->s.persist(new Period()));
			final var after=Instant.now().plusSeconds(1);
			final var table=scope.metadata.getEntityBinding(Period.class.getName()).getAuxiliaryTable().getName();
			final Instant start=scope.factory.fromTransaction(s->s.doReturningWork(c->{try(var st=c.createStatement();var rs=st.executeQuery("select valid_from from "+table)){
				rs.next();return rs.getObject(1,LocalDateTime.class).toInstant(ZoneOffset.UTC);
			}}));
			assertThat(start).isBetween(before,after);
			scope.tx(s->s.find(Period.class,1).payload="second");
			try(var session=scope.factory.withOptions().asOf(start).open()) {
				session.inTransaction(tx->assertThat(session.find(Period.class,1).payload).isEqualTo("first"));
			}
			assertThat(scope.sql).anyMatch(sql->sql.contains("formatdatetime"));
		}
	}

	static class Fixed implements ChangesetIdentifierSupplier<Instant>{Instant value=FIRST;public Instant generateIdentifier(SharedSessionContract session){return value;}}
	@Entity(name="PreservingPeriod") @Temporal(rowStart="valid_from", rowEnd="valid_to", secondPrecision=6)
	static class Period {@Id int id=1;String payload="first";@ElementCollection @OrderColumn @Temporal List<String> tags=new ArrayList<>();}
}
