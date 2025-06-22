/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitygraph.parser;

import jakarta.persistence.AttributeNode;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.Subgraph;
import org.junit.Assert;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Steve Ebersole
 */
public class AssertionHelper {
	static <C extends Collection<?>> void assertNullOrEmpty(C collection) {
		if ( collection != null ) {
			Assert.assertEquals( 0, collection.size() );
		}
	}

	public static <M extends Map<?, ?>> void assertNullOrEmpty(M map) {
		if ( map != null ) {
			Assert.assertEquals( 0, map.size() );
		}
	}

	public static void assertBasicAttributes(EntityGraph<?> graph, String... names) {
		Assert.assertNotNull( graph );
		assertBasicAttributes( graph.getAttributeNodes(), names );
	}

	public static void assertBasicAttributes(Subgraph<?> graph, String... names) {
		Assert.assertNotNull( graph );
		assertBasicAttributes( graph.getAttributeNodes(), names );
	}

	public static void assertBasicAttributes(List<AttributeNode<?>> attrs, String... names) {
		if ( ( names == null ) || ( names.length == 0 ) ) {
			assertNullOrEmpty( attrs );
		}
		else {
			Assert.assertNotNull( attrs );
			Assert.assertTrue( names.length <= attrs.size() );

			for ( String name : names ) {
				AttributeNode<?> node = null;
				for ( AttributeNode<?> candidate : attrs ) {
					if ( candidate.getAttributeName().equals( name ) ) {
						node = candidate;
						break;
					}
				}
				Assert.assertNotNull( node );
				assertNullOrEmpty( node.getKeySubgraphs() );
				assertNullOrEmpty( node.getSubgraphs() );
			}
		}
	}

	public static AttributeNode<?> getAttributeNodeByName(EntityGraph<?> graph, String name, boolean required) {
		return getAttributeNodeByName( graph.getAttributeNodes(), name, required );
	}

	public static AttributeNode<?> getAttributeNodeByName(Subgraph<?> graph, String name, boolean required) {
		return getAttributeNodeByName( graph.getAttributeNodes(), name, required );
	}

	public static AttributeNode<?> getAttributeNodeByName(List<AttributeNode<?>> attrs, String name, boolean required) {
		for ( AttributeNode<?> attr : attrs ) {
			if ( name.equals( attr.getAttributeName() ) )
				return attr;
		}
		if ( required )
			Assert.fail( "Required attribute not found." );
		return null;
	}
}
