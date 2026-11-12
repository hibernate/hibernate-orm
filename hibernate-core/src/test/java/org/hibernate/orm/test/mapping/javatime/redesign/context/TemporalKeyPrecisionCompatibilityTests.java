package org.hibernate.orm.test.mapping.javatime.redesign.context;

import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.util.EnumSet;
import jakarta.persistence.Transient;
import jakarta.persistence.Embedded;
import org.hibernate.annotations.IdGeneratorType;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NaturalIdCache;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.hibernate.exception.ConstraintViolationException;

/// These controls also run unchanged against the preservation-enabled commit
/// preceding precision canonicalization. Existing inexact key lookup behavior
/// is characterized independently of ordinary attribute preparation.
///
/// @author Steve Ebersole
@RequiresDialect(H2Dialect.class)
class TemporalKeyPrecisionCompatibilityTests {
	@ParameterizedTest
	@CsvSource({ "graph,0,false", "legacy,5,false", "graph,5,true", "legacy,0,true" })
	void keyAndForeignKeyBehavior(String queue, int batch, boolean inexact) {
		final var exact = LocalDateTime.parse( "2026-01-02T12:34:56.123" );
		final var key = inexact ? exact.withNano( 123456000 ) : exact;
		try ( var scope = ContextScope.open( false, false, Map.of(
				"hibernate.type.temporal_preservation", true, "hibernate.flush.queue.type", queue,
				"hibernate.jdbc.batch_size", batch ), List.of(), TargetRow.class, Owner.class, Composite.class, CompositeNatural.class, Generated.class ) ) {
			final var target = new TargetRow(); target.id = key; target.natural = key; target.ordinary = key;
			final var owner = new Owner(); owner.id = 1; owner.target = target; owner.uniqueTarget = target;
			owner.values.put( key, "value" ); owner.compositeValues.put( new Key( 1, key ), "value" );
			final var composite = new Composite(); composite.id = new Key( 1, key );
			final var natural = new CompositeNatural(); natural.id = 1; natural.natural = new Key( 1, key );
			final var generated = new Generated(); generated.requested = key;
			scope.tx( session -> { session.persist( target ); session.persist( composite ); session.persist( owner ); session.persist( natural ); session.persist( generated ); } );
			assertThat( generated.id ).isEqualTo( key );
			// Write-side identity cache continues to use the assigned identity.
			scope.tx( session -> assertThat( session.find( TargetRow.class, key ) ).isNotNull() );
			scope.factory.getCache().evictAllRegions();
			scope.tx( session -> {
				assertThat( session.find( TargetRow.class, key ) != null ).isEqualTo( !inexact );
				assertThat( session.find( Composite.class, new Key( 1, key ) ) != null ).isEqualTo( !inexact );
				assertThat( session.find( Generated.class, key ) != null ).isEqualTo( !inexact );
				assertThat( session.bySimpleNaturalId( CompositeNatural.class ).load( new Key( 1, key ) ) != null ).isEqualTo( !inexact );
				assertThat( session.bySimpleNaturalId( TargetRow.class ).load( key ) != null ).isEqualTo( !inexact );
				final var loaded = session.find( Owner.class, 1 );
				assertThat( loaded.target.id ).isEqualTo( exact );
				assertThat( loaded.uniqueTarget.id ).isEqualTo( exact );
				assertThat( loaded.values ).containsExactlyEntriesOf( Map.of( exact, "value" ) );
				assertThat( loaded.compositeValues ).containsExactlyEntriesOf( Map.of( new Key( 1, exact ), "value" ) );
				assertThat( session.find( Composite.class, new Key( 1, exact ) ) ).isNotNull();
			} );
			scope.tx( session -> assertThat( session.find( TargetRow.class, exact ).id ).isEqualTo( exact ) );
		}
	}

	@ParameterizedTest
	@CsvSource({ "graph,5", "legacy,0" })
	void mismatchedForeignKeyPrecisionKeepsBaselineFailure(String queue, int batch) {
		try ( var scope = ContextScope.open( false, false, Map.of(
				"hibernate.type.temporal_preservation", true, "hibernate.flush.queue.type", queue,
				"hibernate.jdbc.batch_size", batch ), List.of(), TargetRow.class, MismatchedOwner.class ) ) {
			final var target = new TargetRow(); target.id = LocalDateTime.parse( "2026-01-02T12:34:56.123456" );
			target.natural = target.id;
			scope.tx( session -> session.persist( target ) );
			final var owner = new MismatchedOwner(); owner.id = 1; owner.target = target;
			assertThatThrownBy( () -> scope.tx( session -> session.persist( owner ) ) )
					.isInstanceOf( ConstraintViolationException.class );
		}
	}
	@Entity(name = "TemporalMismatchedKeyOwner")
	static class MismatchedOwner {
		@Id int id;
		@ManyToOne @JoinColumn(name = "target_id") TargetRow target;
	}

	@Entity(name = "TemporalKeyTarget") @Cache(usage = CacheConcurrencyStrategy.READ_WRITE) @NaturalIdCache
	static class TargetRow {
		@Id @Column(secondPrecision = 3) LocalDateTime id;
		@NaturalId @Column(secondPrecision = 3, unique = true) LocalDateTime natural;
		@Column(secondPrecision = 3) LocalDateTime ordinary;
	}
	@Entity(name = "TemporalKeyOwner") @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	static class Owner {
		@Id int id;
		@ManyToOne @JoinColumn(name = "target_id", columnDefinition = "timestamp(3)") TargetRow target;
		@ManyToOne @JoinColumn(name = "natural_id", referencedColumnName = "natural", columnDefinition = "timestamp(3)") TargetRow uniqueTarget;
		@ElementCollection @MapKeyColumn(name = "map_time", columnDefinition = "timestamp(3)")
		@Cache(usage = CacheConcurrencyStrategy.READ_WRITE) Map<LocalDateTime, String> values = new HashMap<>();
		@ElementCollection @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
		Map<Key, String> compositeValues = new HashMap<>();
	}
	@Entity(name = "TemporalCompositeKey") @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	static class Composite { @EmbeddedId Key id; }
	@Entity(name = "TemporalCompositeNatural") @Cache(usage = CacheConcurrencyStrategy.READ_WRITE) @NaturalIdCache
	static class CompositeNatural { @Id int id; @NaturalId @Embedded Key natural; }
	@Entity(name = "TemporalGeneratedKey") @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	static class Generated {
		@Id @GeneratedTime @Column(secondPrecision = 3) LocalDateTime id;
		@Transient LocalDateTime requested;
	}
	@IdGeneratorType(TimeGenerator.class) @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.FIELD)
	@interface GeneratedTime {}
	public static class TimeGenerator implements BeforeExecutionGenerator {
		@Override public Object generate(SharedSessionContractImplementor session, Object owner, Object current, EventType event) {
			return ((Generated) owner).requested;
		}
		@Override public EnumSet<EventType> getEventTypes() { return EnumSet.of( EventType.INSERT ); }
	}

	@Embeddable
	static class Key implements Serializable {
		int number;
		@Column(secondPrecision = 3) LocalDateTime time;
		Key() {}
		Key(int number, LocalDateTime time) { this.number = number; this.time = time; }
		@Override public boolean equals(Object other) { return other instanceof Key key && number == key.number && Objects.equals( time, key.time ); }
		@Override public int hashCode() { return Objects.hash( number, time ); }
	}
}
