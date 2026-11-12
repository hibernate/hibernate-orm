package org.hibernate.orm.test.mapping.javatime.redesign.context;

import java.io.Serializable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Converter;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Version;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Transient;

import org.hibernate.SharedSessionContract;
import org.hibernate.collection.spi.AbstractPersistentCollection;
import org.hibernate.MappingException;
import org.hibernate.type.BasicType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.internal.TemporalPrecisionBasicType;
import org.junit.jupiter.api.Test;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Mutability;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.type.descriptor.java.MutableMutabilityPlan;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Exercises relational snapshots through actual collection cache hits, partial
/// updates and rollback. Queue/batch variants share identical value assertions.
///
/// @author Steve Ebersole
@RequiresDialect(H2Dialect.class)
class TemporalConvertedCacheLifecycleTests {
	private static final LocalDateTime INPUT = LocalDateTime.parse( "2026-03-08T12:34:56.123456789" );
	private static final String MILLIS = "2026-03-08T12:34:56.123";
	private static final String MICROS = "2026-03-08T12:34:56.123457";

	private ContextScope open(String queue, int batch) {
		return ContextScope.open( false, Map.of(
				"hibernate.type.temporal_preservation", true,
				"hibernate.flush.queue.type", queue,
				"hibernate.jdbc.batch_size", batch ), Row.class );
	}

	@ParameterizedTest
	@CsvSource({ "graph,0", "graph,5", "legacy,0", "legacy,5" })
	void collectionOccurrencesAndMutations(String queue, int batch) {
		try ( var scope = open( queue, batch ) ) {
			final var row = row();
			row.list.addAll( List.of( INPUT.toString(), INPUT.plusSeconds( 1 ).toString(), INPUT.toString() ) );
			row.bag.addAll( row.list );
			row.set.addAll( row.list );
			final var repeated = new Clock( INPUT.toString() );
			row.clocks.addAll( List.of( repeated, repeated, new Clock( INPUT.plusSeconds( 1 ).toString() ) ) );
			scope.tx( session -> session.persist( row ) );
			assertThat( row.list.get( 0 ) ).isEqualTo( INPUT.toString() );
			Text.writes.set( 0 );
			// First read populates collection regions; second must use them.
			for ( int pass = 0; pass < 2; pass++ ) {
				scope.measure();
				scope.tx( session -> {
					final var loaded = session.find( Row.class, 1 );
					assertThat( loaded.list ).containsExactly( MILLIS, INPUT.plusSeconds( 1 ).withNano( 123_000_000 ).toString(), MILLIS );
					assertThat( loaded.bag ).containsExactlyInAnyOrderElementsOf( loaded.list );
					assertThat( loaded.set ).containsExactlyInAnyOrder( MILLIS, INPUT.plusSeconds( 1 ).withNano( 123_000_000 ).toString() );
					assertThat( loaded.clocks ).extracting( clock -> clock.stamp ).containsExactly(
							MICROS, MICROS, INPUT.plusSeconds( 1 ).withNano( 123_457_000 ).toString() );
				} );
				assertThat( Text.writes ).hasValue( 0 );
				if ( pass == 1 ) {
					assertThat( scope.factory.getStatistics().getSecondLevelCacheHitCount() ).isEqualTo( 5 );
					scope.noSelect();
				}
			}
			scope.tx( session -> {
				final var loaded = session.find( Row.class, 1 );
				Collections.swap( loaded.list, 0, 1 );
				loaded.list.remove( 2 );
				loaded.bag.remove( 0 );
				loaded.set.remove( MILLIS );
				loaded.clocks.remove( 0 );
				session.flush();
				loaded.list.set( 1, INPUT.plusSeconds( 2 ).toString() );
				session.flush();
			} );
			Text.writes.set( 0 );
			for ( int pass = 0; pass < 2; pass++ ) {
				scope.tx( session -> {
					final var loaded = session.find( Row.class, 1 );
					assertThat( loaded.list ).containsExactly( INPUT.plusSeconds( 1 ).withNano( 123_000_000 ).toString(),
							INPUT.plusSeconds( 2 ).withNano( 123_000_000 ).toString() );
					assertThat( loaded.bag ).hasSize( 2 );
					assertThat( loaded.set ).hasSize( 1 );
					assertThat( loaded.clocks ).extracting( clock -> clock.stamp ).containsExactly(
							MICROS, INPUT.plusSeconds( 1 ).withNano( 123_457_000 ).toString() );
				} );
			}
			assertThat( Text.writes ).hasValue( 0 );
			scope.tx( session -> {
				final var loaded = session.find( Row.class, 1 );
				loaded.list = new ArrayList<>( List.of( INPUT.toString() ) );
				loaded.bag.clear(); loaded.set.clear(); loaded.clocks.clear();
			} );
			scope.tx( session -> {
				final var loaded = session.find( Row.class, 1 );
				assertThat( loaded.list ).containsExactly( MILLIS );
				assertThat( loaded.bag ).isEmpty();
				assertThat( loaded.set ).isEmpty();
				assertThat( loaded.clocks ).isEmpty();
			} );
		}
	}

	@ParameterizedTest
	@CsvSource({ "graph,0", "graph,5", "legacy,0", "legacy,5" })
	void partialUpdatesNullsAndRollback(String queue, int batch) {
		try ( var scope = open( queue, batch ) ) {
			final var row = row();
			row.nullable = null;
			scope.tx( session -> session.persist( row ) );
			Text.writes.set( 0 );
			scope.measure();
			scope.tx( session -> {
				final var loaded = session.find( Row.class, 1 );
				assertThat( loaded.nullable ).isEqualTo( "FROM_DOMAIN_NULL" );
				assertThat( loaded.first.stamp ).isEqualTo( MICROS );
				assertThat( loaded.second.stamp ).isEqualTo( MILLIS );
				loaded.description = "partial";
			} );
			assertThat( scope.factory.getStatistics().getSecondLevelCacheHitCount() ).isEqualTo( 1 );
			assertThat( Text.writes ).hasValue( 0 );
			scope.factory.getCache().evictEntityData( Row.class );
			scope.tx( session -> session.find( Row.class, 1 ).description = "jdbc partial" );
			assertThat( Text.writes ).hasValue( 0 );
			scope.tx( session -> session.find( Row.class, 1 ).nullable = "SQL_NULL" );
			for ( int pass = 0; pass < 2; pass++ ) {
				scope.tx( session -> assertThat( session.find( Row.class, 1 ).nullable ).isEqualTo( "FROM_SQL_NULL" ) );
				scope.factory.getCache().evictEntityData( Row.class );
			}
			try ( var session = scope.factory.openSession() ) {
				final var transaction = session.beginTransaction();
				session.find( Row.class, 1 ).first.stamp = INPUT.plusDays( 1 ).toString();
				session.flush();
				transaction.rollback();
			}
			scope.tx( session -> assertThat( session.find( Row.class, 1 ).first.stamp ).isEqualTo( MICROS ) );
		}
	}

	@ParameterizedTest
	@CsvSource({ "graph,0", "graph,5", "legacy,0", "legacy,5" })
	void mutableDomainKeepsItsDirtySemantics(String queue, int batch) {
		try ( var scope = open( queue, batch ) ) {
			final var row = row();
			scope.tx( session -> session.persist( row ) );
			MutableText.writes.set( 0 );
			scope.tx( session -> session.find( Row.class, 1 ).description = "unrelated" );
			assertThat( MutableText.writes ).hasValue( 0 );
			scope.measure();
			scope.tx( session -> {
				final var loaded = session.find( Row.class, 1 );
				loaded.mutable.text = INPUT.withNano( 123_100_000 ).toString();
			} );
			assertThat( scope.factory.getStatistics().getEntityUpdateCount() ).isEqualTo( 1 );
			assertThat( MutableText.writes ).hasValue( 1 );
			scope.tx( session -> assertThat( session.find( Row.class, 1 ).mutable.text ).isEqualTo( MILLIS ) );
		}
	}

	@ParameterizedTest
	@CsvSource({ "graph,0", "graph,5", "legacy,0", "legacy,5" })
	void finalCallbacksAndFailedWritesDoNotLeakIntoCache(String queue, int batch) {
		try ( var scope = open( queue, batch ) ) {
			scope.tx( session -> session.persist( row() ) );
			scope.tx( session -> {
				final var loaded = session.find( Row.class, 1 );
				loaded.description = "callback";
				loaded.first.stamp = INPUT.plusSeconds( 2 ).toString();
				loaded.changeOnUpdate = true;
			} );
			scope.tx( session -> assertThat( session.find( Row.class, 1 ).first.stamp )
					.isEqualTo( INPUT.plusSeconds( 3 ).withNano( 123_457_000 ).toString() ) );
			assertThatThrownBy( () -> scope.tx( session -> {
				final var loaded = session.find( Row.class, 1 );
				loaded.first.stamp = "not-a-timestamp";
			} ) ).isInstanceOf( RuntimeException.class );
			assertThatThrownBy( () -> scope.tx( session -> {
				final var loaded = session.find( Row.class, 1 );
				loaded.first.stamp = INPUT.plusDays( 2 ).toString();
				loaded.description = "x".repeat( 300 );
			} ) ).isInstanceOf( RuntimeException.class );
			scope.tx( session -> assertThat( session.find( Row.class, 1 ).first.stamp )
					.isEqualTo( INPUT.plusSeconds( 3 ).withNano( 123_457_000 ).toString() ) );
		}
	}

	@Test
	void payloadSerializationAndContractMismatch() throws Exception {
		try ( var scope = open( "graph", 0 ); var session = scope.factory.openSession() ) {
			final var persister = scope.factory.getMappingMetamodel().getEntityDescriptor( Row.class );
			final var first = (CompositeType) persister.getPropertyType( "first" );
			final var second = (CompositeType) persister.getPropertyType( "second" );
			final var micros = TemporalPrecisionBasicType.find( (BasicType<?>) first.getSubtypes()[0] );
			final var millis = TemporalPrecisionBasicType.find( (BasicType<?>) second.getSubtypes()[0] );
			final var bytes = new ByteArrayOutputStream();
			try ( var out = new ObjectOutputStream( bytes ) ) {
				out.writeObject( micros.disassemblePrepared( INPUT.withNano( 123_457_000 ), session ) );
			}
			final Serializable payload;
			try ( var in = new ObjectInputStream( new ByteArrayInputStream( bytes.toByteArray() ) ) ) {
				payload = (Serializable) in.readObject();
			}
			Text.writes.set( 0 );
			assertThat( micros.assemble( payload, session, null ) ).isEqualTo( MICROS );
			assertThat( Text.writes ).hasValue( 0 );
			assertThatThrownBy( () -> millis.assemble( payload, session, null ) )
					.isInstanceOf( MappingException.class ).hasMessageContaining( "Incompatible temporal cache payload" );
			assertThatThrownBy( () -> micros.assemble( INPUT.toString(), session, null ) )
					.isInstanceOf( MappingException.class ).hasMessageContaining( "evict affected cache regions" );
			assertThatThrownBy( () -> micros.assemble( null, session, null ) )
					.isInstanceOf( MappingException.class ).hasMessageContaining( "Incompatible temporal cache payload" );
			assertThat( persister.canUseReferenceCacheEntries() ).isFalse();
			try ( var other = ContextScope.boot( false, true, Map.of( "hibernate.type.temporal_preservation", true ), Row.class );
					var otherSession = other.factory.openSession() ) {
				final var otherPersister = other.factory.getMappingMetamodel().getEntityDescriptor( Row.class );
				final var otherFirst = (CompositeType) otherPersister.getPropertyType( "first" );
				final var otherSecond = (CompositeType) otherPersister.getPropertyType( "second" );
				final var otherMicros = TemporalPrecisionBasicType.find( (BasicType<?>) otherFirst.getSubtypes()[0] );
				final var otherMillis = TemporalPrecisionBasicType.find( (BasicType<?>) otherSecond.getSubtypes()[0] );
				assertThat( otherMicros ).isNotSameAs( micros );
				assertThat( otherMicros.assemble( payload, otherSession, null ) ).isEqualTo( MICROS );
				assertThatThrownBy( () -> otherMillis.assemble( payload, otherSession, null ) ).isInstanceOf( MappingException.class );
			}
		}
	}

	@Test
	void compatibilityModeKeepsExistingConverterTypes() {
		try ( var scope = ContextScope.open( false, Map.of( "hibernate.type.temporal_preservation", false ), Row.class ) ) {
			final var persister = scope.factory.getMappingMetamodel().getEntityDescriptor( Row.class );
			final var first = (CompositeType) persister.getPropertyType( "first" );
			assertThat( TemporalPrecisionBasicType.find( (BasicType<?>) first.getSubtypes()[0] ) ).isNull();
			scope.tx( session -> session.persist( row() ) );
			scope.tx( session -> assertThat( session.find( Row.class, 1 ).first.stamp ).isEqualTo( INPUT.toString() ) );
		}
	}

	@Test
	void equalDomainOccurrencesRetainDistinctRelationalValues() {
		try ( var scope = ContextScope.open( false, Map.of( "hibernate.type.temporal_preservation", true ), SequenceOwner.class ) ) {
			final var owner = new SequenceOwner();
			owner.values.addAll( List.of( "equal", "equal", "equal" ) );
			Sequence.writes.set( 0 );
			scope.tx( session -> session.persist( owner ) );
			assertThat( Sequence.writes ).hasValue( 3 );
			for ( int pass = 0; pass < 2; pass++ ) {
				Sequence.reads.clear();
				scope.measure();
				scope.tx( session -> assertThat( session.find( SequenceOwner.class, 1 ).values )
						.containsExactly( "equal", "equal", "equal" ) );
				assertThat( Sequence.reads ).containsExactlyInAnyOrder( INPUT.withNano( 123_000_000 ),
						INPUT.plusSeconds( 1 ).withNano( 123_000_000 ), INPUT.plusSeconds( 2 ).withNano( 123_000_000 ) );
				assertThat( Sequence.writes ).hasValue( 3 );
				if ( pass == 1 ) { assertThat( scope.factory.getStatistics().getSecondLevelCacheHitCount() ).isEqualTo( 1 ); }
			}
			scope.tx( session -> {
				final var loaded = session.find( SequenceOwner.class, 1 );
				assertThat( loaded.values ).hasSize( 3 );
				final var collection = (AbstractPersistentCollection<?>) loaded.values;
				final var retained = collection.getTemporalPreparedState();
				session.clear();
				assertThat( collection.getTemporalPreparedState() ).isNotSameAs( retained );
				final var reloaded = session.find( SequenceOwner.class, 1 );
				assertThat( reloaded.values ).containsExactly( "equal", "equal", "equal" );
				assertThat( reloaded.values ).isNotSameAs( loaded.values );
			} );
			assertThat( Sequence.writes ).hasValue( 3 );
		}
	}

	@Entity(name = "SequenceCacheOwner")
	static class SequenceOwner {
		@Id int id = 1;
		@ElementCollection @Convert(converter = Sequence.class) @Column(name = "stamp", secondPrecision = 3)
		@Cache(usage = CacheConcurrencyStrategy.READ_WRITE) List<String> values = new ArrayList<>();
	}

	@Converter @Immutable
	public static class Sequence implements AttributeConverter<String, LocalDateTime> {
		static final AtomicInteger writes = new AtomicInteger();
		static final List<LocalDateTime> reads = new ArrayList<>();
		@Override public LocalDateTime convertToDatabaseColumn(String value) { return INPUT.plusSeconds( writes.getAndIncrement() ); }
		@Override public String convertToEntityAttribute(LocalDateTime value) { reads.add( value ); return "equal"; }
	}

	private static Row row() {
		final var row = new Row(); row.id = 1;
		row.first = new Clock( INPUT.toString() ); row.second = new Clock( INPUT.toString() );
		row.mutable = new MutableTextValue( INPUT.toString() );
		return row;
	}

	@Entity(name = "ConvertedCacheRow")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	@DynamicUpdate
	static class Row {
		@Id int id;
		@Version int version;
		String description;
		@Transient boolean changeOnUpdate;
		@PreUpdate void prepareFinalValue() {
			if ( changeOnUpdate ) { first.stamp = INPUT.plusSeconds( 3 ).toString(); changeOnUpdate = false; }
		}
		@Embedded @AttributeOverride(name = "stamp", column = @Column(name = "first_stamp", secondPrecision = 6)) Clock first;
		@Embedded @AttributeOverride(name = "stamp", column = @Column(name = "second_stamp", secondPrecision = 3)) Clock second;
		@Convert(converter = NullText.class) @Column(secondPrecision = 3) String nullable;
		@Convert(converter = MutableText.class) @Mutability(TextCopy.class) @Column(secondPrecision = 3) MutableTextValue mutable;
		@ElementCollection @OrderColumn @Convert(converter = Text.class) @Column(secondPrecision = 3)
		@Cache(usage = CacheConcurrencyStrategy.READ_WRITE) List<String> list = new ArrayList<>();
		@ElementCollection @Convert(converter = Text.class) @Column(secondPrecision = 3)
		@Cache(usage = CacheConcurrencyStrategy.READ_WRITE) List<String> bag = new ArrayList<>();
		@ElementCollection @Convert(converter = Text.class) @Column(secondPrecision = 3)
		@Cache(usage = CacheConcurrencyStrategy.READ_WRITE) Set<String> set = new HashSet<>();
		@ElementCollection @OrderColumn @Cache(usage = CacheConcurrencyStrategy.READ_WRITE) List<Clock> clocks = new ArrayList<>();
	}

	@Embeddable
	static class Clock {
		@Convert(converter = Text.class) @Column(secondPrecision = 6) String stamp;
		Clock() {}
		Clock(String stamp) { this.stamp = stamp; }
	}

	@Converter @Immutable
	public static class Text implements AttributeConverter<String, LocalDateTime> {
		static final AtomicInteger writes = new AtomicInteger();
		@Override public LocalDateTime convertToDatabaseColumn(String value) {
			writes.incrementAndGet();
			return value == null ? null : LocalDateTime.parse( value );
		}
		@Override public String convertToEntityAttribute(LocalDateTime value) { return value == null ? null : value.toString(); }
	}

	@Converter @Immutable
	public static class NullText implements AttributeConverter<String, LocalDateTime> {
		@Override public LocalDateTime convertToDatabaseColumn(String value) {
			return value == null || value.equals( "FROM_DOMAIN_NULL" ) ? INPUT.withNano( 0 ) : null;
		}
		@Override public String convertToEntityAttribute(LocalDateTime value) {
			return value == null ? "FROM_SQL_NULL" : "FROM_DOMAIN_NULL";
		}
	}

	static class MutableTextValue implements Serializable {
		String text;
		MutableTextValue(String text) { this.text = text; }
		@Override public boolean equals(Object other) { return other instanceof MutableTextValue value && text.equals( value.text ); }
		@Override public int hashCode() { return text.hashCode(); }
	}

	public static class TextCopy extends MutableMutabilityPlan<MutableTextValue> {
		@Override protected MutableTextValue deepCopyNotNull(MutableTextValue value) { return new MutableTextValue( value.text ); }
		@Override public Serializable disassemble(MutableTextValue value, SharedSessionContract session) { return deepCopy( value ); }
		@Override public MutableTextValue assemble(Serializable value, SharedSessionContract session) { return deepCopy( (MutableTextValue) value ); }
	}

	@Converter
	public static class MutableText implements AttributeConverter<MutableTextValue, LocalDateTime> {
		static final AtomicInteger writes = new AtomicInteger();
		@Override public LocalDateTime convertToDatabaseColumn(MutableTextValue value) {
			writes.incrementAndGet(); return value == null ? null : LocalDateTime.parse( value.text );
		}
		@Override public MutableTextValue convertToEntityAttribute(LocalDateTime value) {
			return value == null ? null : new MutableTextValue( value.toString() );
		}
	}
}
