/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.function;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqmFunctionRegistryTest {

	private SqmFunctionRegistry registry;
	private SqmFunctionDescriptor countFn;
	private SqmFunctionDescriptor sumFn;
	private SqmFunctionDescriptor avgFn;
	private SqmFunctionDescriptor jsonObjectFn;
	private SqmFunctionDescriptor arrayAggFn;

	@BeforeEach
	void setUp() {
		registry = new SqmFunctionRegistry();
		countFn = Mockito.mock( SqmFunctionDescriptor.class );
		sumFn = Mockito.mock( SqmFunctionDescriptor.class );
		avgFn = Mockito.mock( SqmFunctionDescriptor.class );
		jsonObjectFn = Mockito.mock( SqmFunctionDescriptor.class );
		arrayAggFn = Mockito.mock( SqmFunctionDescriptor.class );

		registry.register( "count", countFn );
		registry.register( "sum", sumFn );
		registry.register( "avg", avgFn );
		registry.register( "json_object", jsonObjectFn );
		registry.register( "array_agg", arrayAggFn );
	}

	@Test
	void retainOnly_retainsSpecifiedFunctions() {
		registry.retainOnly( Set.of( "count", "sum" ) );

		assertNotNull( registry.findFunctionDescriptor( "count" ) );
		assertNotNull( registry.findFunctionDescriptor( "sum" ) );
		assertNull( registry.findFunctionDescriptor( "avg" ) );
		assertNull( registry.findFunctionDescriptor( "json_object" ) );
		assertNull( registry.findFunctionDescriptor( "array_agg" ) );
	}

	@Test
	void retainOnly_isCaseInsensitive() {
		registry.retainOnly( Set.of( "COUNT", "SUM" ) );

		assertNotNull( registry.findFunctionDescriptor( "count" ) );
		assertNotNull( registry.findFunctionDescriptor( "sum" ) );
		assertNull( registry.findFunctionDescriptor( "avg" ) );
	}

	@Test
	void retainOnly_withEmptySet_removesAll() {
		registry.retainOnly( Set.of() );

		assertNull( registry.findFunctionDescriptor( "count" ) );
		assertNull( registry.findFunctionDescriptor( "sum" ) );
		assertNull( registry.findFunctionDescriptor( "avg" ) );
		assertTrue( registry.getValidFunctionKeys().isEmpty() );
	}

	@Test
	void retainOnly_withAllKeys_isNoOp() {
		registry.retainOnly( Set.of( "count", "sum", "avg", "json_object", "array_agg" ) );

		assertNotNull( registry.findFunctionDescriptor( "count" ) );
		assertNotNull( registry.findFunctionDescriptor( "sum" ) );
		assertNotNull( registry.findFunctionDescriptor( "avg" ) );
		assertNotNull( registry.findFunctionDescriptor( "json_object" ) );
		assertNotNull( registry.findFunctionDescriptor( "array_agg" ) );
		assertEquals( 5, registry.getValidFunctionKeys().size() );
	}

	@Test
	void retainOnly_retainsAlternateKeyWhenTargetIsRetained() {
		// "character_length" is the primary key, "length" is an alias
		SqmFunctionDescriptor charLengthFn = Mockito.mock( SqmFunctionDescriptor.class );
		registry.register( "character_length", charLengthFn );
		registry.registerAlternateKey( "length", "character_length" );

		registry.retainOnly( Set.of( "character_length", "count" ) );

		// Primary key retained
		assertNotNull( registry.findFunctionDescriptor( "character_length" ) );
		// Alias should still work because its target is retained
		assertNotNull( registry.findFunctionDescriptor( "length" ) );
		// Other functions removed
		assertNull( registry.findFunctionDescriptor( "sum" ) );
		assertNull( registry.findFunctionDescriptor( "json_object" ) );
	}

	@Test
	void retainOnly_removesAlternateKeyWhenTargetIsPruned() {
		SqmFunctionDescriptor charLengthFn = Mockito.mock( SqmFunctionDescriptor.class );
		registry.register( "character_length", charLengthFn );
		registry.registerAlternateKey( "length", "character_length" );

		// Retain only "count" — neither "character_length" nor "length" are in the set
		registry.retainOnly( Set.of( "count" ) );

		assertNotNull( registry.findFunctionDescriptor( "count" ) );
		assertNull( registry.findFunctionDescriptor( "character_length" ) );
		// Alias should also be removed since its target was pruned
		assertNull( registry.findFunctionDescriptor( "length" ) );
	}

	@Test
	void retainOnly_prunesSetReturningFunctions() {
		SqmSetReturningFunctionDescriptor unnestFn = Mockito.mock( SqmSetReturningFunctionDescriptor.class );
		SqmSetReturningFunctionDescriptor generateSeriesFn = Mockito.mock( SqmSetReturningFunctionDescriptor.class );
		registry.register( "unnest", unnestFn );
		registry.register( "generate_series", generateSeriesFn );

		registry.retainOnly( Set.of( "count", "unnest" ) );

		assertNotNull( registry.findFunctionDescriptor( "count" ) );
		assertNotNull( registry.findSetReturningFunctionDescriptor( "unnest" ) );
		assertNull( registry.findSetReturningFunctionDescriptor( "generate_series" ) );
	}

	@Test
	void retainOnly_preservesDescriptorInstances() {
		registry.retainOnly( Set.of( "count", "sum" ) );

		// Verify the exact same descriptor instances are returned
		assertEquals( countFn, registry.findFunctionDescriptor( "count" ) );
		assertEquals( sumFn, registry.findFunctionDescriptor( "sum" ) );
	}

	@Test
	void retainOnly_multipleAlternateKeysForSameTarget() {
		SqmFunctionDescriptor charLengthFn = Mockito.mock( SqmFunctionDescriptor.class );
		registry.register( "character_length", charLengthFn );
		registry.registerAlternateKey( "length", "character_length" );
		registry.registerAlternateKey( "char_length", "character_length" );

		registry.retainOnly( Set.of( "character_length" ) );

		assertNotNull( registry.findFunctionDescriptor( "character_length" ) );
		assertNotNull( registry.findFunctionDescriptor( "length" ) );
		assertNotNull( registry.findFunctionDescriptor( "char_length" ) );
		// Others pruned
		assertNull( registry.findFunctionDescriptor( "count" ) );
	}

	@Test
	void retainOnly_calledTwice_furtherPrunes() {
		registry.retainOnly( Set.of( "count", "sum", "avg" ) );

		assertEquals( 3, registry.getValidFunctionKeys().size() );

		registry.retainOnly( Set.of( "count" ) );

		assertEquals( 1, registry.getValidFunctionKeys().size() );
		assertNotNull( registry.findFunctionDescriptor( "count" ) );
		assertNull( registry.findFunctionDescriptor( "sum" ) );
		assertNull( registry.findFunctionDescriptor( "avg" ) );
	}
}
