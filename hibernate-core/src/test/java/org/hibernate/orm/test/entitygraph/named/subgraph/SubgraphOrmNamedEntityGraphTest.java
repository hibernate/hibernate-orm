/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitygraph.named.subgraph;

import java.util.List;
import jakarta.persistence.AttributeNode;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.testing.hamcrest.CollectionMatchers.hasSize;

@DomainModel(
		xmlMappings = "org/hibernate/orm/test/entitygraph/named/subgraph/orm.xml"
)
@SessionFactory
public class SubgraphOrmNamedEntityGraphTest {

	@Test
	@JiraKey( value = "HHH-10633" )
	void testSubgraphsAreLoadedFromOrmXml(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					EntityManager entityManager = session.unwrap( EntityManager.class );
					List<EntityGraph<? super Book>> lneg = entityManager.getEntityGraphs( Book.class );

					assertThat( lneg, notNullValue() );
					assertThat( lneg, hasSize( 2 ) );
					for ( EntityGraph<? super Book> neg : lneg ){
						if ( neg.getName().equalsIgnoreCase( "full" ) ) {
							assertThat( neg.getAttributeNodes(), notNullValue() );
							for ( AttributeNode<?> n : neg.getAttributeNodes() ) {
								if ( n.getAttributeName().equalsIgnoreCase( "authors" ) ) {
									assertThat(n.getSubgraphs().entrySet(), hasSize( 1 ) );
									List<AttributeNode<?>> attributeNodes = n.getSubgraphs().get(Author.class).getAttributeNodes();
									assertThat( attributeNodes, notNullValue() );
									assertThat(attributeNodes, hasSize( 3 ) );
								}
							}
						}
					}
				}
		);
	}
}
