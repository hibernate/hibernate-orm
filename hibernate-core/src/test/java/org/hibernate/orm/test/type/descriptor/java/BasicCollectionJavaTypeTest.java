package org.hibernate.orm.test.type.descriptor.java;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import org.hibernate.collection.internal.CustomCollectionTypeSemantics;
import org.hibernate.collection.internal.StandardBagSemantics;
import org.hibernate.collection.internal.StandardListSemantics;
import org.hibernate.collection.internal.StandardSetSemantics;
import org.hibernate.collection.internal.StandardSortedSetSemantics;
import org.hibernate.collection.spi.CollectionSemantics;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.type.OrderedSetType;
import org.hibernate.type.descriptor.java.ArrayJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.LongJavaType;
import org.hibernate.type.descriptor.java.SerializableJavaType;
import org.hibernate.type.descriptor.java.StringJavaType;
import org.hibernate.type.descriptor.java.spi.BasicCollectionJavaType;
import org.hibernate.type.internal.ParameterizedTypeImpl;
import org.hibernate.type.spi.TypeConfiguration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@JiraKey("HHH-21149")
class BasicCollectionJavaTypeTest {
	@Test
	void equalListsAreComparedLinearly() {
		final var elementType = new CountingLongJavaType();
		final BasicCollectionJavaType<List<Long>, Long> javaType = descriptor(
				StandardListSemantics.INSTANCE, elementType );
		final var values = LongStream.range( 0, 1_000 ).boxed().toList();

		assertTrue( javaType.areEqual( values, new ArrayList<>( values ) ) );
		assertEquals( values.size(), elementType.comparisons );
	}

	@Test
	void arraysResolveToLinearArrayDescriptor() {
		final var elementType = new CountingLongJavaType();
		final var registry = new TypeConfiguration().getJavaTypeRegistry();
		registry.addDescriptor( elementType );
		final var javaType = registry.resolveDescriptor( Long[].class );
		final var values = LongStream.range( 0, 1_000 ).boxed().toArray( Long[]::new );

		assertInstanceOf( ArrayJavaType.class, javaType );
		assertTrue( javaType.areEqual( values, values.clone() ) );
		assertEquals( values.length, elementType.comparisons );
	}

	@Test
	void bagsWithDifferentMultiplicitiesAreNotEqual() {
		final BasicCollectionJavaType<Collection<Long>, Long> javaType = descriptor(
				StandardBagSemantics.INSTANCE, LongJavaType.INSTANCE );

		assertFalse( javaType.areEqual( List.of( 1L, 1L, 2L ), List.of( 1L, 2L, 2L ) ) );
	}

	@Test
	void reorderedDuplicateHeavyBagsAreComparedLinearly() {
		final var elementType = new CountingValueJavaType();
		final BasicCollectionJavaType<Collection<CountingValue>, CountingValue> javaType = descriptor(
				StandardBagSemantics.INSTANCE, elementType );
		final int repeats = 1_000;
		final var comparisons = new AtomicInteger();
		final var grouped = new ArrayList<CountingValue>( 2 * repeats );
		final var alternating = new ArrayList<CountingValue>( 2 * repeats );
		for ( int i = 0; i < repeats; i++ ) {
			grouped.add( new CountingValue( 1, comparisons ) );
			alternating.add( new CountingValue( 0, comparisons ) );
			alternating.add( new CountingValue( 1, comparisons ) );
		}
		for ( int i = 0; i < repeats; i++ ) {
			grouped.add( new CountingValue( 0, comparisons ) );
		}

		assertTrue( javaType.areEqual( grouped, alternating ) );
		assertTrue( comparisons.get() <= 2 * grouped.size(),
				() -> "Compared elements " + comparisons.get() + " times" );
	}

	private static class CountingValueJavaType extends SerializableJavaType<CountingValue> {
		CountingValueJavaType() {
			super( CountingValue.class );
		}

		@Override
		public boolean useObjectEqualsHashCode() {
			return true;
		}

		@Override
		public boolean areEqual(CountingValue one, CountingValue another) {
			return Objects.equals( one, another );
		}
	}

	private record CountingValue(int id, AtomicInteger comparisons) implements Serializable {
		@Override
		public boolean equals(Object obj) {
			comparisons.incrementAndGet();
			return obj instanceof CountingValue that && id == that.id;
		}

		@Override
		public int hashCode() {
			return id;
		}
	}

	@Test
	void bagsDoNotConfuseElementsWithTheSameHashCode() {
		final var type = descriptor( StandardBagSemantics.INSTANCE, LongJavaType.INSTANCE );
		// Both Long values hash to 1.
		assertTrue( type.areEqual( List.of( 1L, 1L << 32 ), List.of( 1L << 32, 1L ) ) );
		assertFalse( type.areEqual( List.of( 1L, 1L << 32 ), List.of( 1L << 32, 1L << 32 ) ) );
	}

	@Test
	void reorderedBagsRespectSerializableEqualityEvenWithDifferentHashes() {
		final var javaType = new SerializableJavaType<>( SerializableValue.class );
		final var bagType = descriptor( StandardBagSemantics.INSTANCE, javaType );
		final var one = new SerializableValue( 1, "first" );
		final var another = new SerializableValue( 1, "second" );
		final var other = new SerializableValue( 2, "other" );
		assertTrue( javaType.areEqual( one, another ) );
		assertNotEquals( javaType.extractHashCode( one ), javaType.extractHashCode( another ) );
		assertTrue( bagType.areEqual( List.of( one, other ), List.of( other, another ) ) );
	}

	@Test
	void reorderedBagsRetainTheFirstMatchForSerializableValues() {
		final var javaType = new SerializableJavaType<>( TransitionalValue.class );
		final var bagType = descriptor( StandardBagSemantics.INSTANCE, javaType );
		final var a = new TransitionalValue( "first", 1 );
		final var b = new TransitionalValue( "second", 1 );
		final var c = new TransitionalValue( "second", 2 );
		assertTrue( javaType.areEqual( a, b ) );
		assertTrue( javaType.areEqual( b, c ) );
		assertFalse( javaType.areEqual( a, c ) );
		assertFalse( bagType.areEqual( List.of( a, c ), List.of( b, a ) ) );
	}

	private static class TransitionalValue implements Serializable {
		private final String stored;
		private final transient int key;

		TransitionalValue(String stored, int key) {
			this.stored = stored;
			this.key = key;
		}

		@Override
		public boolean equals(Object obj) {
			return obj instanceof TransitionalValue that && key == that.key;
		}

		@Override
		public int hashCode() {
			return key;
		}
	}

	private record SerializableValue(int id, String note) implements Serializable {
		@Override
		public boolean equals(Object obj) {
			return obj instanceof SerializableValue that && id == that.id;
		}

		@Override
		public int hashCode() {
			return id;
		}
	}

	@ParameterizedTest
	@MethodSource("additionalOrderedSemantics")
	void equalOrderedSetsAreComparedLinearly(CollectionSemantics<?, ?> semantics) {
		final var elementType = new CountingLongJavaType();
		final BasicCollectionJavaType<Collection<Long>, Long> javaType = descriptor( semantics, elementType );
		final var values = LongStream.range( 0, 1_000 ).boxed().toList();

		assertTrue( javaType.areEqual( collection( semantics, values ), collection( semantics, values ) ) );
		assertEquals( values.size(), elementType.comparisons );
	}

	@ParameterizedTest
	@MethodSource("orderedSemantics")
	void orderedCollectionsDetectDifferentElements(CollectionSemantics<?, ?> semantics) {
		final BasicCollectionJavaType<Collection<Long>, Long> javaType = descriptor( semantics, LongJavaType.INSTANCE );

		assertFalse( javaType.areEqual(
				collection( semantics, List.of( 1L, 2L, 3L ) ),
				collection( semantics, List.of( 1L, 2L, 4L ) )
		) );
	}

	@ParameterizedTest
	@MethodSource("sequenceSemantics")
	void orderedCollectionsDetectDifferentOrder(CollectionSemantics<?, ?> semantics) {
		final BasicCollectionJavaType<Collection<Long>, Long> javaType = descriptor( semantics, LongJavaType.INSTANCE );

		assertFalse( javaType.areEqual(
				collection( semantics, List.of( 1L, 2L, 3L ) ),
				collection( semantics, List.of( 3L, 2L, 1L ) )
		) );
	}

	@Test
	void sortedSetsDetectDifferentIterationOrder() {
		final BasicCollectionJavaType<Collection<Long>, Long> javaType = descriptor(
				StandardSortedSetSemantics.INSTANCE, LongJavaType.INSTANCE );
		final var ascending = new TreeSet<>( List.of( 1L, 2L, 3L ) );
		final var descending = new TreeSet<Long>( Comparator.reverseOrder() );
		descending.addAll( ascending );

		assertFalse( javaType.areEqual( ascending, descending ) );
	}

	@Test
	void listsRetainDuplicatePositions() {
		final BasicCollectionJavaType<List<Long>, Long> javaType = descriptor(
				StandardListSemantics.INSTANCE, LongJavaType.INSTANCE );

		assertFalse( javaType.areEqual( List.of( 1L, 1L, 2L ), List.of( 1L, 2L, 1L ) ) );
	}

	@ParameterizedTest
	@MethodSource("allSemantics")
	void nullAndEmptyCollections(CollectionSemantics<?, ?> semantics) {
		final BasicCollectionJavaType<Collection<Long>, Long> javaType = descriptor( semantics, LongJavaType.INSTANCE );
		final var empty = collection( semantics, List.<Long>of() );

		assertTrue( javaType.areEqual( null, null ) );
		assertFalse( javaType.areEqual( null, empty ) );
		assertFalse( javaType.areEqual( empty, null ) );
		assertTrue( javaType.areEqual( empty, collection( semantics, List.of() ) ) );
		assertFalse( javaType.areEqual( empty, collection( semantics, List.of( 1L ) ) ) );
	}

	@ParameterizedTest
	@MethodSource("bagCases")
	void bagsCompareOccurrences(Collection<Long> one, Collection<Long> another, boolean expected) {
		final BasicCollectionJavaType<Collection<Long>, Long> javaType = descriptor(
				StandardBagSemantics.INSTANCE, LongJavaType.INSTANCE );

		assertEquals( expected, javaType.areEqual( one, another ) );
	}

	private static Stream<Arguments> bagCases() {
		return Stream.of(
				Arguments.of( List.of( 1L, 1L, 2L ), List.of( 2L, 1L, 1L ), true ),
				Arguments.of( List.of( 1L, 2L, 2L ), List.of( 1L, 1L, 2L ), false ),
				Arguments.of( List.of( 1L, 1L, 1L ), List.of( 1L, 1L, 2L ), false ),
				Arguments.of( List.of( 1L, 1L ), List.of( 1L, 1L ), true ),
				Arguments.of( Arrays.asList( null, 1L, null ), Arrays.asList( null, null, 1L ), true ),
				Arguments.of( Arrays.asList( null, null, 1L ), Arrays.asList( null, 1L, 1L ), false ),
				Arguments.of( Arrays.asList( null, 1L, 1L ), Arrays.asList( null, null, 1L ), false ),
				Arguments.of( Arrays.asList( (Long) null ), List.of( 1L ), false )
		);
	}

	@ParameterizedTest
	@MethodSource("allSemantics")
	void collectionsUseCustomElementEquality(CollectionSemantics<?, ?> semantics) {
		final BasicCollectionJavaType<Collection<String>, String> javaType = descriptor(
				semantics, new CaseInsensitiveStringJavaType() );

		assertTrue( javaType.areEqual(
				collection( semantics, List.of( "ALPHA", "BETA" ) ),
				collection( semantics, List.of( "alpha", "beta" ) )
		) );
	}

	@Test
	void bagsMatchCustomEqualDuplicates() {
		final BasicCollectionJavaType<Collection<String>, String> javaType = descriptor(
				StandardBagSemantics.INSTANCE, new CaseInsensitiveStringJavaType() );

		assertTrue( javaType.areEqual(
				Arrays.asList( null, "A", "a", "B" ), Arrays.asList( "a", "b", null, "A" ) ) );
	}

	@Test
	void bagsCountCustomEqualElements() {
		final BasicCollectionJavaType<Collection<String>, String> javaType = descriptor(
				StandardBagSemantics.INSTANCE, new CaseInsensitiveStringJavaType() );

		assertFalse( javaType.areEqual(
				Arrays.asList( null, "A", "a", "B" ), Arrays.asList( null, "a", "b", "B" ) ) );
	}

	@Test
	void setsIgnoreIterationOrder() {
		final var semantics = StandardSetSemantics.INSTANCE;
		final BasicCollectionJavaType<Collection<Long>, Long> javaType = descriptor( semantics, LongJavaType.INSTANCE );

		assertTrue( javaType.areEqual(
				new LinkedHashSet<>( List.of( 1L, 2L, 3L ) ),
				new LinkedHashSet<>( List.of( 3L, 2L, 1L ) )
		) );
	}

	@Test
	void setsDetectDifferentMembers() {
		final var semantics = StandardSetSemantics.INSTANCE;
		final BasicCollectionJavaType<Collection<Long>, Long> javaType = descriptor( semantics, LongJavaType.INSTANCE );

		assertFalse( javaType.areEqual(
				collection( semantics, List.of( 1L, 2L ) ), collection( semantics, List.of( 1L, 3L ) ) ) );
	}

	private static Stream<CollectionSemantics<?, ?>> sequenceSemantics() {
		return Stream.of(
				StandardListSemantics.INSTANCE,
				new CustomCollectionTypeSemantics<>( new OrderedSetType( null, null ) )
		);
	}

	private static Stream<CollectionSemantics<?, ?>> orderedSemantics() {
		return Stream.concat( sequenceSemantics(), Stream.of( StandardSortedSetSemantics.INSTANCE ) );
	}

	private static Stream<CollectionSemantics<?, ?>> additionalOrderedSemantics() {
		return Stream.of(
				new CustomCollectionTypeSemantics<>( new OrderedSetType( null, null ) ),
				StandardSortedSetSemantics.INSTANCE
		);
	}

	private static Stream<CollectionSemantics<?, ?>> allSemantics() {
		return Stream.concat( orderedSemantics(), Stream.of( StandardBagSemantics.INSTANCE, StandardSetSemantics.INSTANCE ) );
	}

	@SuppressWarnings("unchecked")
	private static <E> Collection<E> collection(CollectionSemantics<?, ?> semantics, Collection<E> values) {
		final var collection = (Collection<E>) semantics.instantiateRaw( values.size(), null );
		collection.addAll( values );
		return collection;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static <C extends Collection<E>, E> BasicCollectionJavaType<C, E> descriptor(
			CollectionSemantics<?, ?> semantics, JavaType<E> elementType) {
		return new BasicCollectionJavaType<>(
				new ParameterizedTypeImpl( semantics.getCollectionJavaType(), new Type[] { elementType.getJavaType() }, null ),
				elementType,
				(CollectionSemantics) semantics
		);
	}

	private static class CaseInsensitiveStringJavaType extends StringJavaType {
		@Override
		public boolean areEqual(String one, String another) {
			return one == null ? another == null : another != null && one.equalsIgnoreCase( another );
		}

		@Override
		public int extractHashCode(String value) {
			return value.toLowerCase( Locale.ROOT ).hashCode();
		}

		@Override
		public boolean useObjectEqualsHashCode() {
			return false;
		}
	}

	private static class CountingLongJavaType extends LongJavaType {
		private int comparisons;

		@Override
		public boolean areEqual(Long one, Long another) {
			comparisons++;
			return super.areEqual( one, another );
		}
	}
}
