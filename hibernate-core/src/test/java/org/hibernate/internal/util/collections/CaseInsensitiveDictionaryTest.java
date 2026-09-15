/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util.collections;

import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CaseInsensitiveDictionaryTest {

	@Test
	void retainAll_retainsMatchingKeys() {
		CaseInsensitiveDictionary<String> dict = new CaseInsensitiveDictionary<>();
		dict.put( "Foo", "fooValue" );
		dict.put( "Bar", "barValue" );
		dict.put( "Baz", "bazValue" );

		dict.retainAll( Set.of( "foo", "baz" ) );

		assertNotNull( dict.get( "Foo" ) );
		assertNull( dict.get( "Bar" ) );
		assertNotNull( dict.get( "Baz" ) );
	}

	@Test
	void retainAll_isCaseInsensitive() {
		CaseInsensitiveDictionary<String> dict = new CaseInsensitiveDictionary<>();
		dict.put( "count", "countValue" );
		dict.put( "SUM", "sumValue" );
		dict.put( "Avg", "avgValue" );

		dict.retainAll( Set.of( "COUNT", "avg" ) );

		assertNotNull( dict.get( "count" ) );
		assertNotNull( dict.get( "Avg" ) );
		assertNull( dict.get( "SUM" ) );
	}

	@Test
	void retainAll_withEmptySet_clearsAll() {
		CaseInsensitiveDictionary<String> dict = new CaseInsensitiveDictionary<>();
		dict.put( "a", "1" );
		dict.put( "b", "2" );

		dict.retainAll( Set.of() );

		assertNull( dict.get( "a" ) );
		assertNull( dict.get( "b" ) );
		assertTrue( dict.unmodifiableKeySet().isEmpty() );
	}

	@Test
	void retainAll_withAllKeys_isNoOp() {
		CaseInsensitiveDictionary<String> dict = new CaseInsensitiveDictionary<>();
		dict.put( "a", "1" );
		dict.put( "b", "2" );

		dict.retainAll( Set.of( "a", "b" ) );

		assertEquals( "1", dict.get( "a" ) );
		assertEquals( "2", dict.get( "b" ) );
		assertEquals( 2, dict.unmodifiableKeySet().size() );
	}

	@Test
	void retainAll_withNonExistentKeys_removesAll() {
		CaseInsensitiveDictionary<String> dict = new CaseInsensitiveDictionary<>();
		dict.put( "a", "1" );
		dict.put( "b", "2" );

		dict.retainAll( Set.of( "x", "y" ) );

		assertNull( dict.get( "a" ) );
		assertNull( dict.get( "b" ) );
		assertTrue( dict.unmodifiableKeySet().isEmpty() );
	}

	@Test
	void retainAll_onEmptyDictionary_isNoOp() {
		CaseInsensitiveDictionary<String> dict = new CaseInsensitiveDictionary<>();

		dict.retainAll( Set.of( "a", "b" ) );

		assertTrue( dict.unmodifiableKeySet().isEmpty() );
	}

	@Test
	void retainAll_preservesValues() {
		CaseInsensitiveDictionary<Integer> dict = new CaseInsensitiveDictionary<>();
		dict.put( "count", 42 );
		dict.put( "sum", 100 );
		dict.put( "avg", 50 );

		dict.retainAll( Set.of( "count", "avg" ) );

		assertEquals( 42, dict.get( "count" ) );
		assertEquals( 50, dict.get( "avg" ) );
		assertFalse( dict.containsKey( "sum" ) );
	}
}
