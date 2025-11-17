/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.graph;

import jakarta.persistence.AttributeNode;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.Subgraph;
import org.hibernate.graph.GraphParser;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;


@Jpa(
		annotatedClasses = {
				GraphParsingTestEntity.class,
				GraphParsingTestSubentity.class
		}
)
public abstract class AbstractEntityGraphTest {

	protected <T> RootGraphImplementor<T> parseGraph(Class<T> entityType, String graphString, EntityManagerFactoryScope scope) {
		return scope.fromEntityManager( entityManager ->
				(RootGraphImplementor<T>) GraphParser.parse( entityType, graphString, entityManager )
		);
	}

	protected <T> RootGraphImplementor<GraphParsingTestEntity> parseGraph(String graphString, EntityManagerFactoryScope scope) {
		return parseGraph( GraphParsingTestEntity.class, graphString, scope );
	}

	private <C extends Collection<?>> void assertNullOrEmpty(C collection) {
		if ( collection != null ) {
			assertThat( collection).hasSize( 0 );
		}
	}

	protected <M extends Map<?, ?>> void assertNullOrEmpty(M map) {
		if ( map != null ) {
			assertThat( map.size()).isEqualTo( 0 );
		}
	}

	protected void assertBasicAttributes(EntityGraph<?> graph, String... names) {
		assertThat( graph ).isNotNull();
		assertBasicAttributes( graph.getAttributeNodes(), names );
	}

	protected void assertBasicAttributes(Subgraph<?> graph, String... names) {
		assertThat( graph ).isNotNull();
		assertBasicAttributes( graph.getAttributeNodes(), names );
	}

	private void assertBasicAttributes(List<AttributeNode<?>> attrs, String... names) {
		if ( (names == null) || (names.length == 0) ) {
			assertNullOrEmpty( attrs );
		}
		else {
			assertThat( attrs ).isNotNull();
			assertThat( names.length).isLessThanOrEqualTo( attrs.size() );

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

	protected AttributeNode<?> getAttributeNodeByName(EntityGraph<?> graph, String name, boolean required) {
		return getAttributeNodeByName( graph.getAttributeNodes(), name, required );
	}

	protected AttributeNode<?> getAttributeNodeByName(Subgraph<?> graph, String name, boolean required) {
		return getAttributeNodeByName( graph.getAttributeNodes(), name, required );
	}

	private AttributeNode<?> getAttributeNodeByName(List<AttributeNode<?>> attrs, String name, boolean required) {
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
