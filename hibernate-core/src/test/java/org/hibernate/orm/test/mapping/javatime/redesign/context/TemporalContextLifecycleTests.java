package org.hibernate.orm.test.mapping.javatime.redesign.context;

import java.time.*;
import jakarta.persistence.*;
import org.hibernate.annotations.TimeZoneAdjustment;
import org.hibernate.annotations.TimeZoneRepresentation;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.mapping.Component;
import org.hibernate.type.UserComponentType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;

/// Reused member declarations, context-specific copies, mutable snapshots and both
/// cache origins retain their own representation without mutating supplied values.
///
/// @author Steve Ebersole
class TemporalContextLifecycleTests {
	static final ZoneId NY = ZoneId.of( "America/New_York" );
	static final ZoneOffset INDIA = ZoneOffset.ofHoursMinutes( 5, 30 );
	static final ZonedDateTime INPUT = ZonedDateTime.parse( "2026-11-01T01:30:16-04:00[America/New_York]" ).withZoneSameInstant( ZoneId.of( "Europe/Paris" ) );

	@ParameterizedTest @ValueSource(booleans = {true, false})
	void mutableContextsAndCacheOrigins(boolean metadata) {
		try ( var scope = ContextScope.open( metadata, Event.class ) ) {
			final var binding = scope.metadata.getEntityBinding( Event.class.getName() );
			final var raw = (Component) binding.getProperty( "raw" ).getValue();
			final var ny = (Component) binding.getProperty( "newYork" ).getValue();
			assertThat( raw.getContextualCompositeUserType() ).isNotSameAs( ny.getContextualCompositeUserType() );
			assertThat( raw.copy().getContextualCompositeUserType() ).isSameAs( raw.getContextualCompositeUserType() );
			assertThat( raw.copy().getType() ).isInstanceOf( UserComponentType.class );
			assertThat( scope.factory.getMappingMetamodel().getEntityDescriptor( Event.class ).canUseReferenceCacheEntries() ).isFalse();
			final var source = new Event( 1, INPUT );
			scope.tx( session -> {
				session.persist( source ); session.flush(); scope.measure(); session.flush(); scope.noMutation();
				assertThat( source.newYork ).isSameAs( INPUT );
				assertThat( source.raw ).isSameAs( INPUT );
				assertThat( source.utc ).isEqualTo( INPUT.toOffsetDateTime() );
				assertThat( source.india ).isEqualTo( INPUT.toOffsetDateTime() );
				assertThat( source.inlineZone ).isSameAs( INPUT );
				assertThat( source.inlineTime ).isEqualTo( INPUT.toOffsetDateTime().toOffsetTime() );
			} );
			scope.measure();
			scope.tx( session -> { check( session.find( Event.class, 1 ), INPUT ); session.flush(); } );
			assertThat( scope.factory.getStatistics().getSecondLevelCacheHitCount() ).isEqualTo( 1 ); scope.noSelect(); scope.noMutation();
			// Another hit cannot mutate an earlier result or the shared payload.
			scope.tx( session -> { check( session.find( Event.class, 1 ), INPUT ); session.flush(); } ); scope.noMutation();
			scope.factory.getCache().evictAllRegions(); scope.measure();
			scope.tx( session -> { check( session.find( Event.class, 1 ), INPUT ); session.flush(); } );
			assertThat( scope.factory.getStatistics().getSecondLevelCachePutCount() ).isPositive(); scope.noMutation();
			scope.measure();
			scope.tx( session -> { check( session.find( Event.class, 1 ), INPUT ); session.flush(); } );
			assertThat( scope.factory.getStatistics().getSecondLevelCacheHitCount() ).isEqualTo( 1 ); scope.noSelect(); scope.noMutation();
			final var changed = INPUT.plusHours( 2 );
			scope.measure();
			scope.tx( session -> { final var row = session.find( Event.class, 1 ); row.set( changed ); session.flush(); assertThat( row.raw ).isSameAs( changed ); } );
			assertThat( scope.factory.getStatistics().getEntityUpdateCount() ).isEqualTo( 1 );
			// Observe cache maintenance before any explicit eviction could hide stale data.
			scope.measure();
			scope.tx( session -> { check( session.find( Event.class, 1 ), changed ); session.flush(); } );
			assertThat( scope.factory.getStatistics().getSecondLevelCacheHitCount() ).isEqualTo( 1 );
			scope.noSelect(); scope.noMutation();
			scope.factory.getCache().evictAllRegions();
			scope.tx( session -> check( session.find( Event.class, 1 ), changed ) );
			scope.measure();
			scope.tx( session -> { check( session.find( Event.class, 1 ), changed ); session.flush(); } ); scope.noSelect(); scope.noMutation();
			for ( ZonedDateTime sample : contextSamples() ) {
				scope.tx( session -> session.find( Event.class, 1 ).set( sample ) );
				scope.measure();
				scope.tx( session -> { check( session.find( Event.class, 1 ), sample ); session.flush(); } );
				scope.noSelect(); scope.noMutation();
				scope.factory.getCache().evictAllRegions();
				scope.tx( session -> { check( session.find( Event.class, 1 ), sample ); scope.measure(); session.flush(); scope.noMutation(); } );
			}

		}
	}
	static java.util.List<ZonedDateTime> contextSamples() {
		return java.util.List.of(
				ZonedDateTime.parse("2026-11-01T01:30:16-05:00[America/New_York]"),
				ZonedDateTime.parse("2026-07-01T12:00+02:00[Europe/Paris]"),
				ZonedDateTime.parse("2026-01-15T12:00+01:00[Europe/Paris]") );
	}

	static void check(Event row, ZonedDateTime input) {
		assertThat( row.newYork ).isEqualTo( input.withZoneSameInstant( NY ) );
		assertThat( row.raw ).isEqualTo( input.toInstant().atZone( input.getOffset() ) );
		assertThat( row.utc ).isEqualTo( input.toInstant().atOffset( ZoneOffset.UTC ) );
		assertThat( row.india ).isEqualTo( input.toInstant().atOffset( INDIA ) );
		assertThat( row.inlineZone ).isEqualTo( input.withZoneSameInstant( NY ) );
		assertThat( row.time ).isEqualTo( input.toOffsetDateTime().toOffsetTime().withOffsetSameInstant( INDIA ) );
		assertThat( row.inlineTime ).isEqualTo( input.toOffsetDateTime().toOffsetTime().withOffsetSameInstant( INDIA ) );
	}

	@ParameterizedTest @ValueSource(booleans = {true, false})
	void reusedNestedMembersIgnoreOwnerAndEmbeddingDefaults(boolean metadata) {
		try ( var scope = ContextScope.open( metadata, Owner.class, OtherOwner.class ) ) {
			final var owner = new Owner(); owner.id = 1; owner.left = new Member( INPUT ); owner.right = new Member( INPUT ); owner.nested = new Nested(); owner.nested.member = new Member( INPUT );
			final var other = new OtherOwner(); other.id = 2; other.member = new Member( INPUT );
			scope.tx( session -> { session.persist( owner ); session.persist( other ); } );
			for ( int origin = 0; origin < 2; origin++ ) {
				if ( origin == 1 ) scope.factory.getCache().evictAllRegions();
				scope.tx( session -> {
					assertThat( session.find( OtherOwner.class, 2 ).member.value ).isEqualTo( INPUT.withZoneSameInstant( NY ) );
					final var actual = session.find( Owner.class, 1 );
					assertThat( actual.left.value ).isEqualTo( INPUT.withZoneSameInstant( NY ) );
					assertThat( actual.right.value ).isEqualTo( INPUT.withZoneSameInstant( NY ) );
					assertThat( actual.nested.member.value ).isEqualTo( INPUT.withZoneSameInstant( NY ) );
					scope.measure(); session.flush(); scope.noMutation();
				} );
			}
			assertThat( owner.left.value ).isSameAs( INPUT );
		}
	}

	@ParameterizedTest @ValueSource(booleans = {true, false})
	void cachePrecisionOriginsResolveTheirOwnTransitionInstant(boolean metadata) {
		try ( var scope = ContextScope.open( metadata, Rounded.class ) ) {
			final var input = Instant.parse( "2026-03-08T06:59:59.600Z" ).atZone( ZoneId.of( "Europe/Paris" ) );
			final var row = new Rounded(); row.id = 1; row.value = input; row.raw = input;
			scope.tx( session -> { session.persist( row ); session.flush(); scope.measure(); session.flush(); scope.noMutation(); } );
			scope.measure();
			scope.tx( session -> { final var hit = session.find( Rounded.class, 1 ); assertThat( hit.value ).isEqualTo( input.withZoneSameInstant( NY ) ); assertThat( hit.raw.getZone() ).isEqualTo( input.getOffset() ); session.flush(); } );
			scope.noSelect(); scope.noMutation(); assertThat( scope.factory.getStatistics().getSecondLevelCacheHitCount() ).isEqualTo( 1 );
			scope.factory.getCache().evictAllRegions();
			scope.tx( session -> { final var loaded = session.find( Rounded.class, 1 ); assertThat( loaded.value ).isEqualTo( Instant.parse( "2026-03-08T07:00:00Z" ).atZone( NY ) ); scope.measure(); session.flush(); scope.noMutation(); } );
			scope.measure();
			scope.tx( session -> { assertThat( session.find( Rounded.class, 1 ).value.getOffset() ).isEqualTo( ZoneOffset.ofHours( -4 ) ); session.flush(); } ); scope.noMutation(); scope.noSelect();
			assertThat( row.value ).isSameAs( input );
		}
	}

	@ParameterizedTest @ValueSource(booleans={true,false})
	void genericColumnCopiesKeepTheirDeclaringTargets(boolean metadata) {
		try ( var scope=ContextScope.open(metadata,UtcChild.class,FixedChild.class) ) {
			final var first=new UtcChild();first.id=1;first.value=INPUT;
			final var second=new FixedChild();second.id=2;second.value=INPUT;
			final var utc=(Component)scope.metadata.getEntityBinding(UtcChild.class.getName()).getProperty("value").getValue();
			final var fixed=(Component)scope.metadata.getEntityBinding(FixedChild.class.getName()).getProperty("value").getValue();
			assertThat(utc.getContextualCompositeUserType()).isNotSameAs(fixed.getContextualCompositeUserType());
			assertThat(utc.getColumns()).extracting(org.hibernate.mapping.Column::getName).containsExactlyInAnyOrder("value","value_tz");
			assertThat(utc.copy().getType()).isInstanceOf(UserComponentType.class);
			assertThat(scope.metadata.getEntityBinding(UtcChild.class.getName()).getProperty("value").copy().getType()).isInstanceOf(UserComponentType.class);
			scope.tx(session->{session.persist(first);session.persist(second);});
			for(int origin=0;origin<2;origin++) {
				if(origin==1)scope.factory.getCache().evictAllRegions();
				scope.tx(session->{
					assertThat(session.find(FixedChild.class,2).value).isEqualTo(INPUT.withZoneSameInstant(INDIA));
					assertThat(session.find(UtcChild.class,1).value).isEqualTo(INPUT.withZoneSameInstant(ZoneOffset.UTC));
					scope.measure();session.flush();scope.noMutation();
				});
			}
			int sampleId=10;
			for ( ZonedDateTime sample : contextSamples() ) {
				final int id=sampleId++;final var a=new UtcChild();a.id=id;a.value=sample;final var b=new FixedChild();b.id=id;b.value=sample;
				scope.tx(session->{session.persist(a);session.persist(b);});
				scope.tx(session->{assertThat(session.find(FixedChild.class,id).value).isEqualTo(sample.withZoneSameInstant(INDIA));assertThat(session.find(UtcChild.class,id).value).isEqualTo(sample.withZoneSameInstant(ZoneOffset.UTC));});
				scope.factory.getCache().evictAllRegions();
				scope.tx(session->{assertThat(session.find(UtcChild.class,id).value).isEqualTo(sample.withZoneSameInstant(ZoneOffset.UTC));assertThat(session.find(FixedChild.class,id).value).isEqualTo(sample.withZoneSameInstant(INDIA));});
				assertThat(a.value).isSameAs(sample);assertThat(b.value).isSameAs(sample);
			}
			assertThat(first.value).isSameAs(INPUT);assertThat(second.value).isSameAs(INPUT);
		}
	}
	@MappedSuperclass @TimeZoneAdjustment("Z")
	static class UtcBase<T> { @Id int id; @TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN) T value; }
	@MappedSuperclass @TimeZoneAdjustment("+05:30")
	static class FixedBase<T> { @Id int id; @TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN) T value; }
	@Entity(name="ContextUtcChild") @TimeZoneAdjustment("America/New_York") @Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
	static class UtcChild extends UtcBase<ZonedDateTime> {}
	@Entity(name="ContextFixedChild") @TimeZoneAdjustment("Z") @Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
	static class FixedChild extends FixedBase<ZonedDateTime> {}

	@Entity(name="ContextMutableEvent") @Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
	static class Event {
		@Id int id;
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN) @TimeZoneAdjustment("America/New_York") ZonedDateTime newYork;
		@Column(name="raw_instant") @TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN) ZonedDateTime raw;
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN) @TimeZoneAdjustment("Z") OffsetDateTime utc;
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN) @TimeZoneAdjustment("+05:30") OffsetTime time;
		@TimeZoneAdjustment("+05:30") OffsetDateTime india;
		@TimeZoneAdjustment("+05:30") OffsetTime inlineTime;
		@TimeZoneAdjustment("America/New_York") ZonedDateTime inlineZone;
		Event() {} Event(int id, ZonedDateTime input) { this.id = id; set( input ); }
		void set(ZonedDateTime input) { newYork = input; raw = input; utc = input.toOffsetDateTime(); time = input.toOffsetDateTime().toOffsetTime(); india = input.toOffsetDateTime(); inlineZone = input; inlineTime = input.toOffsetDateTime().toOffsetTime(); }
	}
	@Embeddable @TimeZoneAdjustment("America/New_York")
	static class Member {
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN) ZonedDateTime value;
		Member() {} Member(ZonedDateTime value) { this.value = value; }
	}
	@Embeddable static class Nested { @Embedded Member member; }
	@Entity(name="ContextOwner") @TimeZoneAdjustment("Z") @Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
	static class Owner {
		@Id int id;
		@Embedded @TimeZoneAdjustment(TimeZoneAdjustment.NONE)
		@AttributeOverrides({@AttributeOverride(name="value.instant",column=@Column(name="left_instant")), @AttributeOverride(name="value.zoneOffset",column=@Column(name="left_offset"))}) Member left;
		@Embedded @AttributeOverrides({@AttributeOverride(name="value.instant",column=@Column(name="right_instant")), @AttributeOverride(name="value.zoneOffset",column=@Column(name="right_offset"))}) Member right;
		@Embedded @AttributeOverrides({@AttributeOverride(name="member.value.instant",column=@Column(name="nested_instant")), @AttributeOverride(name="member.value.zoneOffset",column=@Column(name="nested_offset"))}) Nested nested;
	}
	@Entity(name="ContextOtherOwner") @TimeZoneAdjustment("+05:30") @Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
	static class OtherOwner { @Id int id; @Embedded Member member; }
	@Entity(name="ContextRounded") @Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
	static class Rounded {
		@Id int id;
		@Column(secondPrecision=0) @TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN) @TimeZoneAdjustment("America/New_York") ZonedDateTime value;
		@Column(name="raw_instant",secondPrecision=0) @TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN) ZonedDateTime raw;
	}
}
