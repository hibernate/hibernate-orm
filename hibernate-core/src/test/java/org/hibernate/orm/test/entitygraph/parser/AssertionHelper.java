/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitygraph.parser;

import jakarta.persistence.AttributeNode;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.Subgraph;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
public class AssertionHelper {
	static <C extends Collection<?>> void assertNullOrEmpty(C collection) {
		if ( collection != null ) {
			assertThat( collection ).hasSize( 0 );
		}
	}

	public static <M extends Map<?, ?>> void assertNullOrEmpty(M map) {
		if ( map != null ) {
			assertThat( map.size() ).isEqualTo( 0 );
		}
	}

	public static void assertBasicAttributes(EntityGraph<?> graph, String... names) {
		assertThat( graph ).isNotNull();
		assertBasicAttributes( graph.getAttributeNodes(), names );
	}

	public static void assertBasicAttributes(Subgraph<?> graph, String... names) {
		assertThat( graph ).isNotNull();
		assertBasicAttributes( graph.getAttributeNodes(), names );
	}

	public static void assertBasicAttributes(List<AttributeNode<?>> attrs, String... names) {
		if ( (names == null) || (names.length == 0) ) {
			assertNullOrEmpty( attrs );
		}
		else {
			assertThat( attrs ).isNotNull();
			assertThat( names.length ).isLessThanOrEqualTo( attrs.size() );

			for ( String name : names ) {
				AttributeNode<?> node = null;
				for ( AttributeNode<?> candidate : attrs ) {
					if ( candidate.getAttributeName().equals( name ) ) {
						node = candidate;
						break;
					}
				}
				assertThat( node ).isNotNull();
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
			if ( name.equals( attr.getAttributeName() ) ) {
				return attr;
			}
		}
		if ( required ) {
			fail( "Required attribute not found." );
		}
		return null;
	}
}
