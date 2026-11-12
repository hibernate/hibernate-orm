package org.hibernate.orm.test.mapping.javatime.redesign.context;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;

import org.hibernate.CustomEntityDirtinessStrategy;
import org.hibernate.Interceptor;
import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.TimeZoneRepresentation;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.type.Type;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/// Explicit dirty decisions remain authoritative, and preparation observes the
/// final callback value of an attribute already scheduled for assignment.
///
/// @author Steve Ebersole
@RequiresDialect(H2Dialect.class)
class TemporalColumnAuthorityTests {
	@ParameterizedTest
	@CsvSource({ "graph,interceptor", "legacy,interceptor", "graph,strategy", "legacy,strategy" })
	void authoritativeDirtyDecisionAndCallback(String queue, String mechanism) {
		final var settings = new HashMap<String, Object>();
		settings.put( "hibernate.type.temporal_preservation", true );
		settings.put( "hibernate.flush.queue.type", queue );
		settings.put( "hibernate.jdbc.batch_size", 5 );
		if ( mechanism.equals( "interceptor" ) ) {
			settings.put( "hibernate.session_factory.interceptor", new Interceptor() {
				@Override public int[] findDirty(Object entity, Object id, Object[] state, Object[] previous, String[] names, Type[] types) {
					return entity instanceof Row row && row.force ? new int[] { Arrays.asList( names ).indexOf( "stamp" ) } : null;
				}
			} );
		}
		else {
			settings.put( "hibernate.entity_dirtiness_strategy", new CustomEntityDirtinessStrategy() {
				@Override public boolean canDirtyCheck(Object entity, EntityPersister persister, Session session) { return entity instanceof Row; }
				@Override public boolean isDirty(Object entity, EntityPersister persister, Session session) { return ((Row) entity).force; }
				@Override public void resetDirty(Object entity, EntityPersister persister, Session session) { ((Row) entity).force = false; }
				@Override public void findDirty(Object entity, EntityPersister persister, Session session, DirtyCheckContext context) {
					context.doDirtyChecking( attribute -> attribute.getName().equals( "stamp" ) );
				}
			} );
		}
		try ( var scope = ContextScope.open( false, false, settings, List.of(), Row.class ) ) {
			final var row = new Row(); row.stamp = OffsetDateTime.parse( "2026-01-02T12:34:56.123100+05:45" );
			scope.tx( session -> {
				session.persist( row ); session.flush(); scope.measure();
				row.stamp = row.stamp.withNano( 123200000 ); row.force = true;
				session.flush(); row.force = false;
				assertThat( scope.factory.getStatistics().getEntityUpdateCount() ).isEqualTo( 1 );
				assertThat( row.version ).isEqualTo( 1 );
				row.replacement = row.stamp.plusSeconds( 10 ).withNano( 987600000 ); row.force = true;
				session.flush(); row.force = false;
				assertThat( scope.factory.getStatistics().getEntityUpdateCount() ).isEqualTo( 2 );
			} );
			for ( int phase = 0; phase < 2; phase++ ) {
				if ( phase == 1 ) { scope.factory.getCache().evictAllRegions(); }
				scope.tx( session -> assertThat( (Object) session.find( Row.class, 1 ).stamp )
						.isEqualTo( OffsetDateTime.parse( "2026-01-02T12:35:06.988+05:45" ) ) );
			}
		}
	}
	@Entity(name = "ColumnAuthorityRow") @DynamicUpdate @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	static class Row {
		@Id int id = 1;
		@Version int version;
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN) @Column(secondPrecision = 3) OffsetDateTime stamp;
		@Transient boolean force;
		@Transient OffsetDateTime replacement;
		@PreUpdate void replace() { if ( replacement != null ) { stamp = replacement; replacement = null; } }
	}
}
