/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.function;

import java.util.List;

import org.hibernate.dialect.function.json.JsonPathHelper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JsonPathHelperTest {

	@Test
	public void testRoot() {
		assertEquals(
				List.of(),
				JsonPathHelper.parseJsonPathElements( "$" )
		);
	}

	@Test
	public void testRootArray() {
		assertEquals(
				List.of( new JsonPathHelper.JsonIndexAccess( 0 ) ),
				JsonPathHelper.parseJsonPathElements( "$[0]" )
		);
	}

	@Test
	public void testDeReferenceRootArray() {
		assertEquals(
				List.of( new JsonPathHelper.JsonIndexAccess( 0 ), new JsonPathHelper.JsonAttribute( "attribute" ) ),
				JsonPathHelper.parseJsonPathElements( "$[0].attribute" )
		);
	}

	@Test
	public void testSimplePath() {
		assertEquals(
				List.of( new JsonPathHelper.JsonAttribute( "attribute" ) ),
				JsonPathHelper.parseJsonPathElements( "$.attribute" )
		);
	}

	@Test
	public void testArrayPath() {
		assertEquals(
				List.of( new JsonPathHelper.JsonAttribute( "attribute" ), new JsonPathHelper.JsonIndexAccess( 0 ) ),
				JsonPathHelper.parseJsonPathElements( "$.attribute[0]" )
		);
	}

	@Test
	public void testDeepArrayPath() {
		assertEquals(
				List.of(
						new JsonPathHelper.JsonAttribute( "attribute" ),
						new JsonPathHelper.JsonIndexAccess( 0 ),
						new JsonPathHelper.JsonAttribute( "subAttribute" )
				),
				JsonPathHelper.parseJsonPathElements( "$.attribute[0].subAttribute" )
		);
	}
}
